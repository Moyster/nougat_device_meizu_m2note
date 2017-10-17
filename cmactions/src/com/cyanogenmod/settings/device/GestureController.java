/*
 * Copyright (C) 2015 The CyanogenMod Project
 * Copyright (C) 2016 faust93 adaptation for Meizu PRO5 FTS Driver
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

package com.cyanogenmod.settings.device;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;
import android.util.SparseIntArray;

import com.cyanogenmod.settings.device.utils.FileUtils;

public class GestureController {
    private static final String TAG = GestureController.class.getSimpleName();

    public static final String TOUCHSCREEN_GESTURE_CONTROL_NODE = "/sys/devices/platform/mx-gs/gesture_control";
    public static final String TOUCHSCREEN_GESTURE_CONTROL_KEY = "touchscreen_gesture_control";

    // M2 Note gesture keys
    public static final int DOUBLE_TAP = 0xA0; //160

    public static final int SWIPE_X_LEFT = 0xB0; //176
    public static final int SWIPE_X_RIGHT = 0xB1;
    public static final int SWIPE_Y_UP = 0xB2;
    public static final int SWIPE_Y_DOWN = 0xB3;

    public static final int UNICODE_E = 0xC0; // 192
    public static final int UNICODE_C = 0xC1;
    public static final int UNICODE_W = 0xC2;
    public static final int UNICODE_M = 0xC3;
    public static final int UNICODE_O = 0xC4;
    public static final int UNICODE_S = 0xC5;
    public static final int UNICODE_V_UP = 0xC6;
    public static final int UNICODE_V_DOWN = 0xC7;
    public static final int UNICODE_V_L = 0xC8;
    public static final int UNICODE_V_R = 0xC9;
    public static final int UNICODE_Z = 0xCA;


    public static final int GESTURES_DISABLED_MASK = 0x100;
    public static final int GESTURES_ENABLED_MASK = 0x1000100;

    // Supported gesture keys and masks
    public static SparseIntArray keysToMasks = new SparseIntArray();

    static {
        keysToMasks.put(DOUBLE_TAP, 0x000100);

        keysToMasks.put(SWIPE_X_LEFT, 0x000001);
        keysToMasks.put(SWIPE_X_RIGHT, 0x000002);
        keysToMasks.put(SWIPE_Y_DOWN, 0x000004);
        keysToMasks.put(SWIPE_Y_UP, 0x000008);

        keysToMasks.put(UNICODE_E, 0x040000);
        keysToMasks.put(UNICODE_C, 0x020000);
        keysToMasks.put(UNICODE_M, 0x100000);
        keysToMasks.put(UNICODE_O, 0x800000);
        keysToMasks.put(UNICODE_S, 0x200000);
        keysToMasks.put(UNICODE_V_UP, 0x010000);
        keysToMasks.put(UNICODE_W, 0x080000);
        keysToMasks.put(UNICODE_Z, 0x400000);
    }

    public static void updateGestureControl(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean(TOUCHSCREEN_GESTURE_CONTROL_KEY, false)) {
            int gestureMask = 0x000000;
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_CONTROL_NODE, GESTURES_ENABLED_MASK);
            for (int i = 0; i < keysToMasks.size(); i++) {
                int key = keysToMasks.keyAt(i);
                int mask = keysToMasks.get(key);
                if (sharedPreferences.getBoolean(key + "_enabled", false))
                    gestureMask += mask;
            }
            byte swipeMask = (byte) (gestureMask & 0xff);
            FileUtils.writeByteArray(TOUCHSCREEN_GESTURE_CONTROL_NODE, new byte[]{swipeMask, 0, 4, 0});
            byte doubleTapMask = (byte) ((gestureMask >> 8) & 0xff);
            FileUtils.writeByteArray(TOUCHSCREEN_GESTURE_CONTROL_NODE, new byte[]{doubleTapMask, 0, 2, 0});
            byte unicodeMask = (byte) ((gestureMask >> 16) & 0xff);
            FileUtils.writeByteArray(TOUCHSCREEN_GESTURE_CONTROL_NODE, new byte[]{unicodeMask, 0, 3, 0});
        } else {
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_CONTROL_NODE, GESTURES_DISABLED_MASK);
        }
    }

    public static void masterSwitch(boolean enabled) {
        if (enabled) {
            Log.i(TAG, "Gestures enabled");
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_CONTROL_NODE, GESTURES_ENABLED_MASK);
        } else {
            Log.i(TAG, "Gestures disabled");
            FileUtils.writeAsByte(TOUCHSCREEN_GESTURE_CONTROL_NODE, GESTURES_DISABLED_MASK);
        }
    }
}


