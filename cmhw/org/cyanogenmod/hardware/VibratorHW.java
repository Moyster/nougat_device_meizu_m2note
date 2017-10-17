/*
 * Copyright (C) 2013 The CyanogenMod Project
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

/* 
 * Vibrator intensity adjustment
 *
 * Exports methods to get the valid value boundaries, the
 * default and current intensities, and a method to set
 * the vibrator.
 *
 * Values exported by min/max can be the direct values required
 * by the hardware, or a local (to VibratorHW) abstraction that's
 * internally converted to something else prior to actual use. The
 * Settings user interface will normalize these into a 0-100 (percentage)
 * scale before showing them to the user, but all values passed to/from
 * the client (Settings) are in this class' scale.
 */

/* This would be just "Vibrator", but it conflicts with android.os.Vibrator */
public class VibratorHW {

    // Keep this synced to immvibe impl
    private static final String INTENSITY_FILE = "/sys/kernel/thunderquake_engine/level";

    public static boolean isSupported() {
        return true;
    }

    public static boolean setIntensity(int intensity) {
        return FileUtils.writeLine(INTENSITY_FILE, Integer.toString(intensity));
    }

    public static int getMaxIntensity() {
        return 7;
    }

    public static int getMinIntensity() {
        return 0;
    }

    public static int getWarningThreshold() {
        // actually this is rather arbitrary
        return 7;
    }

    public static int getCurIntensity() {
        final String result = FileUtils.readOneLine(INTENSITY_FILE);
        if (result == null) {
            return 5;
        }

        try {
            return Integer.parseInt(result.trim());
        } catch (final NumberFormatException ignored) {
            return 5;
        }
    }

    public static int getDefaultIntensity() {
        return 5;
    }
}
