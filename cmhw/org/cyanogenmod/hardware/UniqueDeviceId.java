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

package org.cyanogenmod.hardware;

import org.cyanogenmod.internal.util.FileUtils;

/**
 * Generate a unique but deterministic ID for this hardware, based on unchangeable
 * hardware serial numbers.
 */

public class UniqueDeviceId {
    private static final int TYPE_MMC0_CID = 0;

    private static String sUniqueId = null;

    /**
     * Whether device supports reporting a unique device id.
     *
     * @return boolean Supported devices must return always true
     */
    public static boolean isSupported() {
        return getUniqueDeviceIdInternal() != null;
    }

    /**
     * This method retreives a unique ID for the device.
     *
     * @return String The unique device ID
     */
    public static String getUniqueDeviceId() {
        return getUniqueDeviceIdInternal();
    }

    private static String getUniqueDeviceIdInternal() {
        if (sUniqueId != null) {
            return sUniqueId;
        }

        String sMmcType = FileUtils.readOneLine("/sys/block/mmcblk0/device/type");
        String sCid = FileUtils.readOneLine("/sys/block/mmcblk0/device/cid");
        if ("MMC".equals(sMmcType) && sCid != null) {
            sCid = sCid.trim();
            if (sCid.length() == 32) {
                sUniqueId = String.format("%03x00000%32s", TYPE_MMC0_CID, sCid);
                return sUniqueId;
            }
        }

        /* Any additional types should be added here. */

        return null;
    }
}
