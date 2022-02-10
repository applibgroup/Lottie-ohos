/*
 * Copyright (C) 2021 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airbnb.lottie.utils;

import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;

import java.util.Locale;

/**
 * Logger
 */
public class HMOSLogUtil {
    private static final String TAG_LOG = "Lottie-Harmony ";

    private static final int DOMAIN_ID = 0xD000F00;

    private static final HiLogLabel LABEL_LOG = new HiLogLabel(3, DOMAIN_ID, HMOSLogUtil.TAG_LOG);

    private static final String LOG_FORMAT = "%s: %s";

    private HMOSLogUtil() {
        /* Do nothing */
    }

    /**
     * Print debug log
     *
     * @param tag log tag
     * @param msg log message
     */
    public static void debug(String tag, String msg) {
        HiLog.debug(LABEL_LOG, String.format(Locale.ROOT, LOG_FORMAT, tag, msg));
    }

    /**
     * Print info log
     *
     * @param tag log tag
     * @param msg log message
     */
    public static void info(String tag, String msg) {
        HiLog.info(LABEL_LOG, String.format(Locale.ROOT, LOG_FORMAT, tag, msg));
    }

    /**
     * Print warn log
     *
     * @param tag log tag
     * @param msg log message
     */
    public static void warn(String tag, String msg) {
        HiLog.warn(LABEL_LOG, String.format(Locale.ROOT, LOG_FORMAT, tag, msg));
    }

    /**
     * Print error log
     *
     * @param tag log tag
     * @param msg log message
     */
    public static void error(String tag, String msg) {
        HiLog.error(LABEL_LOG, String.format(Locale.ROOT, LOG_FORMAT, tag, msg));
    }

    /**
     * Print error log
     *
     * @param tag log tag
     * @param msg log message
     * @param exception if any
     */
    public static void error(String tag, String msg, Throwable exception) {
        HiLog.error(LABEL_LOG, String.format(Locale.ROOT, LOG_FORMAT, tag, msg), exception);
    }
}
