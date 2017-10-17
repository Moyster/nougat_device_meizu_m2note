/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.os.FileUtils;
import android.util.Slog;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Support for storage of key-value pairs which persists across device wipes /
 * factory resets. This is used for various security related features. A
 * reasonable implementation of this needs to store items on a partition which
 * is unaffected by factory reset. The actual format of the storage is left up
 * to the implementation. The implementation in this file is suitable for
 * devices which have a /persist partition (recent QCOM devices fit this
 * criteria).
 */

public class PersistentStorage {

    public static final int MAX_KEY_LEN = 64;
    public static final int MAX_VALUE_LEN = 4096;

    private static final String TAG = "PersistentStorage";

    private static final String PERSIST_DATA_PATH = "/protect_s/properties";

    private static final boolean DEBUG = false;

    private static final ReadWriteLock rwl = new ReentrantReadWriteLock();

    /**
     * Whether device supports persistent properties
     *
     * @return boolean Supported devices must return always true
     */
    public static boolean isSupported() {
        final File file = new File(PERSIST_DATA_PATH);
        return file.exists() && file.isDirectory() && file.canRead() && file.canWrite();
    }

    /**
     * Gets an array of bytes from persistent storage.
     *
     * @param key
     * @return previously stored byte array, null if not found
     */
    public static byte[] get(String key) {
        if (!isSupported() || key.length() > MAX_KEY_LEN) {
            return null;
        }

        final String encodedKey = encode(key);
        if (encodedKey == null) {
            return null;
        }

        FileInputStream fis = null;

        try {
            rwl.readLock().lock();

            final File prop = new File(PERSIST_DATA_PATH + "/" + encodedKey);

            if (!prop.exists()) {
                if (DEBUG) {
                    Slog.e(TAG, "Unable to read from " + prop.getAbsolutePath());
                }
                return null;
            }

            final byte[] buffer = new byte[MAX_VALUE_LEN];
            fis = new FileInputStream(prop);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len = fis.read(buffer);
            bos.write(buffer, 0, len);
            return bos.toByteArray();

        } catch (Exception e) {
            Slog.e(TAG, e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    // skip it
                }
            }
            rwl.readLock().unlock();
        }

        return null;
    }

    /**
     * Writes a byte array to persistent storage.
     *
     * The storage of the data is implementation specific. In this implementation,
     * we hash the key with SHA-256 and use the hex string as the stored key.
     *
     * Implementations should delete the key if a null buffer is passed.
     *
     * @param key
     * @param buffer
     * @return true if the operation succeeded
     */
    public static boolean set(String key, byte[] buffer) {
        if (!isSupported() ||
                key == null || key.length() > MAX_KEY_LEN ||
                (buffer != null && buffer.length > MAX_VALUE_LEN)) {
            return false;
        }

        final String encodedKey = encode(key);
        if (encodedKey == null) {
            return false;
        }

        try {
            rwl.writeLock().lock();

            final File prop = new File(PERSIST_DATA_PATH + "/" + encodedKey);

            File tmp = null;
            if (buffer != null) {
                tmp = File.createTempFile(key, "tmp",
                        new File(PERSIST_DATA_PATH));
                if (!FileUtils.copyToFile(new ByteArrayInputStream(buffer), tmp)) {
                    Slog.e(TAG, "Unable to create temporary file!");
                    return false;
                }
            }

            if (prop.exists()) {
                prop.delete();
            }

            if (tmp != null) {
                tmp.renameTo(prop);
            }

        } catch (Exception e) {
            Slog.e(TAG, e.getMessage(), e);
            return false;

        } finally {
            rwl.writeLock().unlock();
        }
        return true;
    }

    /**
     * Used for encoding keys with SHA-256
     *
     * @param key
     * @return
     */
    private static String encode(String key) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] hash = d.digest(key.getBytes("UTF-8"));
            StringBuilder encoded = new StringBuilder();

            for (byte b : hash) {
                encoded.append(String.format("%02x", b & 0xff));
            }

            return encoded.toString();
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage(), e);
        }
        return null;
    }
}
