/*
 * Copyright (C) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.telephony;

import static com.android.internal.telephony.RILConstants.*;

import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.telephony.uicc.IccRefreshResponse;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Message;
import android.os.Parcel;
import android.os.SystemProperties;

import android.provider.Settings;
import android.provider.Settings.Global;

import android.telephony.TelephonyManager;

import com.android.internal.telephony.MtkEccList;


/**
 * Custom wrapper for MTK requests
 *
 * {@hide}
 */
public class MediaTekRIL extends RIL implements CommandsInterface {
    static final String LOG_TAG = "MediaTekRIL";

    static final int REFRESH_SESSION_RESET = 6; /* Session reset */

    static final int RIL_REQUEST_VENDOR_BASE = 2000;
    static final int RIL_REQUEST_MODEM_POWEROFF = (RIL_REQUEST_VENDOR_BASE + 10);
    static final int RIL_REQUEST_MODEM_POWERON = (RIL_REQUEST_VENDOR_BASE + 28);
    static final int RIL_REQUEST_RESUME_REGISTRATION  = (RIL_REQUEST_VENDOR_BASE + 65);
    static final int RIL_REQUEST_SET_CALL_INDICATION = (RIL_REQUEST_VENDOR_BASE + 86);
    static final int RIL_REQUEST_EMERGENCY_DIAL = (RIL_REQUEST_VENDOR_BASE + 87);
    static final int RIL_REQUEST_SET_ECC_SERVICE_CATEGORY = (RIL_REQUEST_VENDOR_BASE + 88);
    static final int RIL_REQUEST_SET_ECC_LIST = (RIL_REQUEST_VENDOR_BASE + 89);
    static final int RIL_REQUEST_GENERAL_SIM_AUTH = (RIL_REQUEST_VENDOR_BASE + 90);
    static final int RIL_REQUEST_QUERY_AVAILABLE_NETWORK_WITH_ACT = (RIL_REQUEST_VENDOR_BASE + 95);
    static final int RIL_REQUEST_SWITCH_CARD_TYPE = (RIL_REQUEST_VENDOR_BASE + 131);

    static final int RIL_UNSOL_VENDOR_BASE = 3000;
    static final int RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED = (RIL_UNSOL_VENDOR_BASE + 15);
    static final int RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED = (RIL_UNSOL_VENDOR_BASE + 24);
    static final int RIL_UNSOL_INCOMING_CALL_INDICATION = (RIL_UNSOL_VENDOR_BASE + 42);
    static final int RIL_UNSOL_CALL_INFO_INDICATION = (RIL_UNSOL_VENDOR_BASE + 49);
    static final int RIL_UNSOL_MD_STATE_CHANGE = (RIL_UNSOL_VENDOR_BASE + 53);
    static final int RIL_UNSOL_SET_ATTACH_APN = (RIL_UNSOL_VENDOR_BASE + 73);

    private int[] dataCallCids = { -1, -1, -1, -1, -1 };

    private Context mContext;
    private TelephonyManager mTelephonyManager;
    private MtkEccList mEccList;

    //***** Constructors
    public MediaTekRIL(Context context, int preferredNetworkType, int cdmaSubscription) {
        super(context, preferredNetworkType, cdmaSubscription, null);
    }

    public MediaTekRIL(Context context, int preferredNetworkType,
            int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
        mContext = context;
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mEccList = new MtkEccList();
    }

    private static String
    localRequestToString(int request) {
        switch(request) {
            case RIL_REQUEST_RESUME_REGISTRATION: return "RIL_REQUEST_RESUME_REGISTRATION";
            case RIL_REQUEST_SET_CALL_INDICATION: return "RIL_REQUEST_SET_CALL_INDICATION";
            case RIL_REQUEST_EMERGENCY_DIAL: return "RIL_REQUEST_EMERGENCY_DIAL";
            case RIL_REQUEST_SET_ECC_SERVICE_CATEGORY: return "RIL_REQUEST_SET_ECC_SERVICE_CATEGORY";
            case RIL_REQUEST_SET_ECC_LIST: return "RIL_REQUEST_SET_ECC_LIST";
            case RIL_REQUEST_MODEM_POWEROFF: return "RIL_REQUEST_MODEM_POWEROFF";
            case RIL_REQUEST_MODEM_POWERON: return "RIL_REQUEST_MODEM_POWERON";
            default: return "<unknown response>";
        }
    }

    @Override
    protected void
    processUnsolicited (Parcel p, int type) {
        Object ret;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        int response = p.readInt();

        switch(response) {
            case RIL_UNSOL_RESPONSE_REGISTRATION_SUSPENDED: ret = responseRegSuspended(p); break;
            case RIL_UNSOL_INCOMING_CALL_INDICATION: ret = responseIncomingCallIndication(p); break;
            case RIL_UNSOL_CALL_INFO_INDICATION: ret = responseCallProgress(p); break;
	        case RIL_UNSOL_MD_STATE_CHANGE: ret = responseInts(p); break;
            case RIL_UNSOL_SET_ATTACH_APN: ret = responseSetAttachApn(p); break;
            case RIL_UNSOL_ON_USSD: ret =  responseStrings(p); break;
            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED: ret = responseInts(p); break;
            default:
                // Rewind the Parcel
                p.setDataPosition(dataPosition);
                // Forward responses that we are not overriding to the super class
                super.processUnsolicited(p, type);
                return;
        }
        switch(response) {
            case RIL_UNSOL_INCOMING_CALL_INDICATION:
                mCallStateRegistrants
                    .notifyRegistrants(new AsyncResult(null, null, null));
            break;

            case RIL_UNSOL_CALL_INFO_INDICATION:
                if (ret == null) {
                        mCallStateRegistrants
                            .notifyRegistrants(new AsyncResult(null, null, null));
                }
            break;

            case RIL_UNSOL_ON_USSD:
                String[] resp = (String[])ret;

                if (resp.length < 2) {
                    resp = new String[2];
                    resp[0] = ((String[])ret)[0];
                    resp[1] = null;
                }

                if (resp[0] != null &&
                         Integer.parseInt(resp[0]) >= 2 &&
                         Integer.parseInt(resp[0]) <=5 ) {
                    // support USSD_SESSION_END and USSD_HANDLED_BY_STK as normal finishes
                    resp[0] = "0";
                }

                if (RILJ_LOGD) unsljLogMore(response, resp[0]);

                if (mUSSDRegistrant != null) {
                    mUSSDRegistrant.notifyRegistrant(
                        new AsyncResult (null, resp, null));
                }
            break;

            case RIL_UNSOL_RESPONSE_PS_NETWORK_STATE_CHANGED:
                if (((int[])ret)[0] != 4)
                    mVoiceNetworkStateRegistrants
                        .notifyRegistrants(new AsyncResult(null, null, null));
            break;
        }

    }

    protected Object
    responseRegSuspended(Parcel p) {
        int numInts;
        int response[];

        numInts = p.readInt();

        response = new int[numInts];

        for (int i = 0 ; i < numInts ; i++) {
            response[i] = p.readInt();
        }

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_RESUME_REGISTRATION, null);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + localRequestToString(rr.mRequest)
                + " sessionId " + response[0]);

        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(response[0]);

        send(rr);

        return response;
    }

    protected Object
    responseIncomingCallIndication(Parcel p) {
        String response[];

        response = p.readStringArray();

        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_CALL_INDICATION, null);

        if (RILJ_LOGD) riljLog(rr.serialString() + "> " + localRequestToString(rr.mRequest));

        rr.mParcel.writeInt(3);
        rr.mParcel.writeInt(Integer.parseInt(response[3]));
        rr.mParcel.writeInt(Integer.parseInt(response[0]));
        rr.mParcel.writeInt(Integer.parseInt(response[4]));
        send(rr);

        return response;
    }

    protected Object
    responseCallProgress(Parcel p) {
        String response[];

        response = p.readStringArray();

        if (response.length >= 2) {
            if (response[1].equals("129")) { // Call termination
                return null;
            }
        }
        return response;
    }

    @Override
    public void setInitialAttachApn(String apn, String protocol, int authType, String username,
            String password, Message result) {
        RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_INITIAL_ATTACH_APN, null);

        String operatorNumber = mTelephonyManager.getSimOperatorNumericForPhone(mInstanceId);
        if (RILJ_LOGD) { riljLog("Set RIL_REQUEST_SET_INITIAL_ATTACH_APN"); }

        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(protocol);
        rr.mParcel.writeInt(authType);
        rr.mParcel.writeString(username);
        rr.mParcel.writeString(password);

        rr.mParcel.writeString(operatorNumber);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeStringArray(null);

        if (RILJ_LOGD) { riljLog(rr.serialString() + "> " + requestToString(rr.mRequest)
                + ", apn:" + apn + ", protocol:" + protocol + ", authType:" + authType
                + ", username:" + username + ", password:" + password + ", operator:" + operatorNumber);
        }

        send(rr);
    }

    protected Object
    responseSetAttachApn(Parcel p) {
        // The stack refuses to attach to LTE unless an IA APN was set, and
        // will loop until it happens. Set an empty one to unblock.
        setInitialAttachApn("","",0,"","",null);
        return null;
    }

    @Override
    protected Object
    responseSimRefresh(Parcel p) {
        IccRefreshResponse response = new IccRefreshResponse();

        response.refreshResult = p.readInt();
        String rawefId = p.readString();
        response.efId   = rawefId == null ? 0 : Integer.parseInt(rawefId);
        response.aid = p.readString();

        if (response.refreshResult > IccRefreshResponse.REFRESH_RESULT_RESET) {
            if (response.refreshResult == REFRESH_SESSION_RESET) {
                response.refreshResult = IccRefreshResponse.REFRESH_RESULT_RESET;
            } else {
                response.refreshResult = IccRefreshResponse.REFRESH_RESULT_INIT;
            }
        }

        return response;
    }

    @Override
    public void
    setupDataCall(int radioTechnology, int profile, String apn,
            String user, String password, int authType, String protocol,
            Message result) {
        int interfaceId=0;
        RILRequest rr
                = RILRequest.obtain(RIL_REQUEST_SETUP_DATA_CALL, result);

        rr.mParcel.writeInt(8); //bumped by one

        rr.mParcel.writeString(Integer.toString(radioTechnology));
        rr.mParcel.writeString(Integer.toString(profile));
        rr.mParcel.writeString(apn);
        rr.mParcel.writeString(user);
        rr.mParcel.writeString(password);
        rr.mParcel.writeString(Integer.toString(authType));
        rr.mParcel.writeString(protocol);

        /* Find the first available interfaceId */
        for (int i=0; i < 4; i++) {
            if (dataCallCids[i] < 0) {
                interfaceId = i+1;
                break;
            }
        }
        rr.mParcel.writeString(interfaceId+"");

        if (RILJ_LOGD) riljLog(rr.serialString() + "> "
                + requestToString(rr.mRequest) + " " + radioTechnology + " "
                + profile + " " + apn + " " + user + " "
                + password + " " + authType + " " + protocol + " " + interfaceId);

        send(rr);
    }

    @Override
    public void
    deactivateDataCall(int cid, int reason, Message result) {
        for (int i=0; i < 4; i++) {
            if (dataCallCids[i] == cid) {
                dataCallCids[i] = -1;
                break;
            }
        }
        super.deactivateDataCall(cid, reason, result);
    }

    @Override
    public void
    dial(String address, int clirMode, UUSInfo uusInfo, Message result) {
        if (mEccList.isEmergencyNumberExt(address)) {
            int serviceCategory = mEccList.getServiceCategoryFromEcc(address);

            RILRequest rr = RILRequest.obtain(RIL_REQUEST_SET_ECC_SERVICE_CATEGORY, null);

            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(serviceCategory);

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + localRequestToString(rr.mRequest)
                    + " " + serviceCategory);

            send(rr);


            rr = RILRequest.obtain(RIL_REQUEST_EMERGENCY_DIAL, result);
            rr.mParcel.writeString(address);
            rr.mParcel.writeInt(clirMode);
            rr.mParcel.writeInt(0); // UUS information is absent

            if (uusInfo == null) {
                rr.mParcel.writeInt(0); // UUS information is absent
            } else {
                rr.mParcel.writeInt(1); // UUS information is present
                rr.mParcel.writeInt(uusInfo.getType());
                rr.mParcel.writeInt(uusInfo.getDcs());
                rr.mParcel.writeByteArray(uusInfo.getUserData());
            }

            if (RILJ_LOGD) riljLog(rr.serialString() + "> " + localRequestToString(rr.mRequest));

            send(rr);

        } else {
            super.dial(address, clirMode, uusInfo, result);
        }
    }

    private void refreshEmergencyList() {
        if (mEccList != null) mEccList.updateEmergencyNumbersProperty();
    }

    @Override
    public void
    setRadioPower(boolean on, Message result) {
        boolean isInApm = Settings.Global.getInt(mContext.getContentResolver(),
                                        Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        boolean wasInApm = SystemProperties.get("persist.radio.airplane.mode.on").equals("true");

        SystemProperties.set("persist.radio.airplane.mode.on", isInApm ? "true" : "false");

        if (on && wasInApm && !isInApm) {
            SystemProperties.set("gsm.ril.eboot", "0");
            RILRequest rr = RILRequest.obtain(RIL_REQUEST_MODEM_POWERON, result);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            send(rr);
        } else if (!on && isInApm) {
            SystemProperties.set("gsm.ril.eboot", "1");
            RILRequest rr = RILRequest.obtain(RIL_REQUEST_MODEM_POWEROFF, result);
            if (RILJ_LOGD) {
                riljLog(rr.serialString() + "> " + requestToString(rr.mRequest));
            }
            send(rr);
        } else {
            super.setRadioPower(on, result);
        }
    }

    // Solicited request handling
    @Override
    protected RILRequest
    processSolicited (Parcel p, int type) {
        int serial, error;
        int dataPosition = p.dataPosition(); // save off position within the Parcel
        serial = p.readInt();
        error = p.readInt();

        RILRequest rr = null;

        /* Pre-process the reply before popping it */
        synchronized (mRequestList) {
            RILRequest tr = mRequestList.get(serial);
            if (tr != null && tr.mSerial == serial) {
                if (error == 0 || p.dataAvail() > 0) {
                    try {switch (tr.mRequest) {
                        /* Get those we're interested in */
                        case RIL_REQUEST_EMERGENCY_DIAL:
                        case RIL_REQUEST_SET_ECC_SERVICE_CATEGORY:
                        case RIL_REQUEST_DATA_REGISTRATION_STATE:
                        case RIL_REQUEST_SETUP_DATA_CALL:
                        case RIL_REQUEST_ALLOW_DATA:
                            rr = tr;
                            break;
                        // vendor ril refreshes the properties while generating this answer. Do our own updates afterwards
                        case RIL_REQUEST_GET_SIM_STATUS: refreshEmergencyList(); // no break, we want the superclass to process this
                    }} catch (Throwable thr) {
                        // Exceptions here usually mean invalid RIL responses
                        if (tr.mResult != null) {
                            AsyncResult.forMessage(tr.mResult, null, thr);
                            tr.mResult.sendToTarget();
                        }
                        return tr;
                    }
                }
            }
        }

        if (rr == null) {
            /* Nothing we care about, go up */
            p.setDataPosition(dataPosition);

            // Forward responses that we are not overriding to the super class
            return super.processSolicited(p, type);
        }


        rr = findAndRemoveRequestFromList(serial);

        if (rr == null) {
            return rr;
        }

        Object ret = null;

        if (error == 0 || p.dataAvail() > 0) {
            switch (rr.mRequest) {
                case RIL_REQUEST_EMERGENCY_DIAL: ret =  responseVoid(p); break;
                case RIL_REQUEST_SET_ECC_SERVICE_CATEGORY: ret =  responseVoid(p); break;
                case RIL_REQUEST_DATA_REGISTRATION_STATE: ret =  fixupPSBearerDataRegistration(p); break;
                case RIL_REQUEST_SETUP_DATA_CALL: ret =  fetchCidFromDataCall(p); break;
                case RIL_REQUEST_ALLOW_DATA: ret =  responseVoid(p); mVoiceNetworkStateRegistrants.notifyRegistrants(new AsyncResult(null, null, null)); break;
                default:
                    throw new RuntimeException("Shouldn't be here: " + rr.mRequest);
            }
            //break;
        }

        if (RILJ_LOGD) riljLog(rr.serialString() + "< " + requestToString(rr.mRequest)
            + " " + retToString(rr.mRequest, ret));

        if (rr.mResult != null) {
            AsyncResult.forMessage(rr.mResult, ret, null);
            rr.mResult.sendToTarget();
        }

        return rr;
    }

    private Object
    fixupPSBearerDataRegistration(Parcel p) {
        int num;
        String response[];

        response = p.readStringArray();

        if (response.length >= 4 &&
            response[3] != null &&
            Integer.parseInt(response[3]) >= 128) {
            // extended values >= RIL_RADIO_TECHNOLOGY_MTK (128) == HSPAP (15)
            response[3] = "15";
        }

        return response;
    }

    private Object
    fetchCidFromDataCall(Parcel p) {
        DataCallResponse ret = (DataCallResponse)super.responseSetupDataCall(p);

        if (ret.cid >= 0) {
            for (int i = 0; i < 4; i++) {
                if (dataCallCids[i] < 0) {
                    dataCallCids[i] = ret.cid;
                    break;
                }
            }
        }

        return ret;
    }

    @Override
    public void
    iccIOForApp (int command, int fileid, String path, int p1, int p2, int p3,
            String data, String pin2, String aid, Message result) {
        if (command == 0xc0 && p3 == 0) {
            riljLog("Override the size for the COMMAND_GET_RESPONSE 0 => 15");
            p3 = 15;
        }
        super.iccIOForApp(command, fileid, path, p1, p2, p3, data, pin2, aid, result);
    }
}
