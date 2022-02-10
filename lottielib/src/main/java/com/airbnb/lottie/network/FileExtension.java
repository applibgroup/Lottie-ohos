package com.airbnb.lottie.network;

import com.airbnb.lottie.L;
import com.airbnb.lottie.utils.HMOSLogUtil;

/**
 * Helpers for known Lottie file types.
 */
public enum FileExtension {
    JSON(".json"),
    ZIP(".zip");

    public final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    public String tempExtension() {
        return ".temp" + extension;
    }

    @Override
    public String toString() {
        return extension;
    }

    public static FileExtension forFile(String filename) {
        for (FileExtension e : values()) {
            if (filename.endsWith(e.extension)) {
                return e;
            }
        }
        // Default to Json.
        HMOSLogUtil.warn(L.TAG, "Unable to find correct extension for " + filename);
        return JSON;
    }
}
