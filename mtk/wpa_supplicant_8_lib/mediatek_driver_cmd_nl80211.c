/*
 * Driver interaction with extended Linux CFG8021
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Alternatively, this software may be distributed under the terms of BSD
 * license.
 *
 */
#include "includes.h"
#include <linux/wireless.h>
#include "netlink/genl/genl.h"

#include "common.h"
#include "driver_nl80211.h"
#include "linux_ioctl.h"
#include "wpa_supplicant_i.h"
#include "config.h"

#ifdef ANDROID
#include "android_drv.h"
#endif

#include "driver_i.h"

#include "eloop.h"

/**********************************************************************
* OVERLAPPED functins, previous defination is in driver_nl80211.c,
* it will be modified
***********************************************************************/

/**********************************************************************/
static int wpa_driver_mediatek_set_country(void *priv, const char *alpha2_arg) {
    struct i802_bss *bss = priv;
    struct wpa_driver_nl80211_data *drv = bss->drv;
    int ioctl_sock = -1;
    struct iwreq iwr;
    int ret = -1;
    char buf[11];

    wpa_printf(MSG_DEBUG, "wpa_driver_nl80211_set_country");
    ioctl_sock = socket(PF_INET, SOCK_DGRAM, 0);
    if (ioctl_sock < 0) {
        wpa_printf(MSG_ERROR, "%s: socket(PF_INET,SOCK_DGRAM)", __func__);
        return -1;
    }

    os_memset(&iwr, 0, sizeof(iwr));
    os_strlcpy(iwr.ifr_name, drv->first_bss->ifname, IFNAMSIZ);
    sprintf(buf, "COUNTRY %s", alpha2_arg);
    iwr.u.data.pointer = buf;
    iwr.u.data.length = strlen(buf);
    if ((ret = ioctl(ioctl_sock, 0x8B0C, &iwr)) < 0) {  // SIOCSIWPRIV
        wpa_printf(MSG_DEBUG, "ioctl[SIOCSIWPRIV]: %s", buf);
        close(ioctl_sock);
        return ret;
    } else {
        close(ioctl_sock);
        return 0;
    }

}

/*
* update channel list in wpa_supplicant
* if coutry code chanaged
*/
static void wpa_driver_notify_country_change(void *ctx, char *cmd) {
    if (os_strncasecmp(cmd, "COUNTRY", 7) == 0) {
        union wpa_event_data event;

        os_memset(&event, 0, sizeof(event));
        event.channel_list_changed.initiator = REGDOM_SET_BY_USER;
        if (os_strncasecmp(cmd, "COUNTRY", 7) == 0) {
            event.channel_list_changed.type = REGDOM_TYPE_COUNTRY;
            if (os_strlen(cmd) > 9) {
                event.channel_list_changed.alpha2[0] = cmd[8];
                event.channel_list_changed.alpha2[1] = cmd[9];
            }
        } else
            event.channel_list_changed.type = REGDOM_TYPE_UNKNOWN;
        wpa_supplicant_event(ctx, EVENT_CHANNEL_LIST_CHANGED, &event);
    }
}

int wpa_driver_nl80211_driver_cmd(void *priv, char *cmd, char *buf,
                  size_t buf_len ) {
    struct i802_bss *bss = priv;
    struct wpa_driver_nl80211_data *drv = bss->drv;
    struct ifreq ifr;
    struct wpa_supplicant *wpa_s;
    struct hostapd_data *hapd;
    int handled = 0;
    int cmd_len = 0;
    union wpa_event_data event;
    static int user_force_band = 0;
    int ret = -1;

    if (drv == NULL) {
        wpa_printf(MSG_ERROR, "%s: drv is NULL. Exiting", __func__);
        return -1;
    }

    if (drv->ctx == NULL) {
        wpa_printf(MSG_ERROR, "%s: drv->ctx is NULL. Exiting", __func__);
        return -1;
    }

    if (os_strcmp(bss->ifname, "ap0") == 0) {
        hapd = (struct hostapd_data *)(drv->ctx);
    } else {
        wpa_s = (struct wpa_supplicant *)(drv->ctx);
        if (wpa_s->conf == NULL) {
            wpa_printf(MSG_ERROR, "%s: wpa_s->conf is NULL. Exiting", __func__);
            return -1;
        }
    }

    wpa_printf(MSG_DEBUG, "iface %s recv cmd %s", bss->ifname, cmd);
    handled = 1;

    if (os_strncasecmp(cmd, "POWERMODE ", 10) == 0) {
        int state;
        state = atoi(cmd + 10);
        wpa_printf(MSG_DEBUG, "POWERMODE=%d", state);
    } else if (os_strncmp(cmd, "MACADDR", os_strlen("MACADDR")) == 0) {
        u8 macaddr[ETH_ALEN] = {};
        os_memcpy(&macaddr, wpa_s->own_addr, ETH_ALEN);
        ret = snprintf(buf, buf_len, "Macaddr = " MACSTR "\n", MAC2STR(macaddr));
        wpa_printf(MSG_DEBUG, "%s", buf);
    } else if(os_strncasecmp(cmd, "COUNTRY", os_strlen("COUNTRY"))==0) {
        if (os_strlen(cmd) != os_strlen("COUNTRY") + 3) {
            wpa_printf(MSG_DEBUG, "Ignore COUNTRY cmd %s", cmd);
            ret = 0;
        } else {
            wpa_printf(MSG_INFO, "set country: %s", cmd+8);
            ret = wpa_driver_mediatek_set_country(priv, cmd+8);
            if (ret == 0) {
                wpa_printf(MSG_DEBUG, "Update channel list after country code changed");
                wpa_driver_notify_country_change(wpa_s, cmd);
            }
        }
    } else if (os_strcasecmp(cmd, "start") == 0) {
        if (ret = linux_set_iface_flags(drv->global->ioctl_sock,
            drv->first_bss->ifname, 1)) {
            wpa_printf(MSG_INFO, "nl80211: Could not set interface UP, ret=%d \n", ret);
        } else {
            wpa_msg(drv->ctx, MSG_INFO, "CTRL-EVENT-DRIVER-STATE STARTED");
        }
    } else if (os_strcasecmp(cmd, "stop") == 0) {
        if (drv->associated) {
            ret = wpa_drv_deauthenticate(wpa_s, drv->bssid, WLAN_REASON_DEAUTH_LEAVING);
            if (ret != 0)
                wpa_printf(MSG_DEBUG, "DRIVER-STOP error, ret=%d", ret);
        } else {
            wpa_printf(MSG_INFO, "nl80211: not associated, no need to deauthenticate \n");
        }

        if (ret = linux_set_iface_flags(drv->global->ioctl_sock,
            drv->first_bss->ifname, 0)) {
            wpa_printf(MSG_INFO, "nl80211: Could not set interface Down, ret=%d \n", ret);
        } else {
            wpa_msg(drv->ctx, MSG_INFO, "CTRL-EVENT-DRIVER-STATE STOPPED");
        }
    } else if (os_strncasecmp(cmd, "getpower", 8) == 0) {
        u32 mode;
        if (ret == 0) {
            ret = snprintf(buf, buf_len, "powermode = %u\n", mode);
            wpa_printf(MSG_DEBUG, "%s", buf);
            if (ret < (int)buf_len)
                return ret;
        }
    } else if (os_strncasecmp(cmd, "get-rts-threshold", 17) == 0) {
        u32 thd;
        if (ret == 0) {
            ret = snprintf(buf, buf_len, "rts-threshold = %u\n", thd);
            wpa_printf(MSG_DEBUG, "%s", buf);
            if (ret < (int)buf_len)
                return ret;
        }
    } else if (os_strncasecmp(cmd, "set-rts-threshold", 17) == 0) {
        u32 thd = 0;
        char *cp = cmd + 17;
        char *endp;
        if (*cp != '\0') {
            thd = (u32)strtol(cp, &endp, 0);
        }
    } else if (os_strcasecmp(cmd, "btcoexscan-start") == 0) {
        ret = 0; /* mt5921 linux driver not implement yet */
    } else if (os_strcasecmp(cmd, "btcoexscan-stop") == 0) {
        ret = 0; /* mt5921 linux driver not implement yet */
    } else if (os_strncasecmp(cmd, "btcoexmode", 10) == 0) {
        ret = 0; /* mt5921 linux driver not implement yet */
    } else {
        handled = 0;
        wpa_printf(MSG_INFO, "Unsupported command");
    }

    return ret;
}

int wpa_driver_set_p2p_noa(void *priv, u8 count, int start, int duration) {
    struct i802_bss *bss = priv;
    struct wpa_driver_nl80211_data *drv = bss->drv;

    wpa_printf(MSG_DEBUG, "iface %s P2P_SET_NOA %d %d %d, ignored", bss->ifname, count, start, duration);
    return -1;
}

int wpa_driver_get_p2p_noa(void *priv, u8 *buf, size_t len) {
    struct i802_bss *bss = priv;
    struct wpa_driver_nl80211_data *drv = bss->drv;

    wpa_printf(MSG_DEBUG, "iface %s P2P_GET_NOA, ignored", bss->ifname);
    return -1;
}

int wpa_driver_set_p2p_ps(void *priv, int legacy_ps, int opp_ps, int ctwindow) {
    struct i802_bss *bss = priv;
    struct wpa_driver_nl80211_data *drv = bss->drv;

    wpa_printf(MSG_DEBUG, "iface %s P2P_SET_PS, ignored", bss->ifname);
    return -1;
}

int wpa_driver_set_ap_wps_p2p_ie(void *priv, const struct wpabuf *beacon,
                 const struct wpabuf *proberesp,
                 const struct wpabuf *assocresp) {
    struct i802_bss *bss = priv;
    struct wpa_driver_nl80211_data *drv = bss->drv;

    wpa_printf(MSG_DEBUG, "iface %s set_ap_wps_p2p_ie, ignored", bss->ifname);
    return 0;
}
