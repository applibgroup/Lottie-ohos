package com.airbnb.lottie;

import com.airbnb.lottie.network.*;
import ohos.app.Context;
import ohos.hiviewdfx.HiTrace;
import ohos.hiviewdfx.HiTraceId;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class L {

    public static boolean DBG = false;

    public static final String TAG = "LOTTIE";

    private static final int MAX_DEPTH = 20;

    private static boolean traceEnabled = false;

    private static String[] sections;

    private static long[] startTimeNs;

    private static int traceDepth = 0;

    private static int depthPastMaxDepth = 0;

    private static LottieNetworkFetcher fetcher;
    private static LottieNetworkCacheProvider cacheProvider;

    private static volatile NetworkFetcher networkFetcher;
    private static volatile NetworkCache networkCache;

    private L() {
    }


    public static void setTraceEnabled(boolean enabled) {
        if (traceEnabled == enabled) {
            return;
        }
        traceEnabled = enabled;
        if (traceEnabled) {
            sections = new String[MAX_DEPTH];
            startTimeNs = new long[MAX_DEPTH];
        }
    }

    public static HiTraceId beginSection(String section) {
        HiTraceId traceID = null;
        sections[traceDepth] = section;
        startTimeNs[traceDepth] = System.nanoTime();
        traceID = HiTrace.begin(section, HiTrace.HITRACE_FLAG_TP_INFO);
        traceDepth++;
        return traceID;
    }

    public static float endSection(HiTraceId traceId) {
        if (depthPastMaxDepth > 0) {
            depthPastMaxDepth--;
            return 0;
        }
        if (!traceEnabled) {
            return 0;
        }
        traceDepth--;
        if (traceDepth == -1) {
            throw new IllegalStateException("Can't end trace section. There are none.");
        }
        HiTrace.end(traceId);
        return (System.nanoTime() - startTimeNs[traceDepth]) / 1000000f;
    }
    public static void setFetcher(LottieNetworkFetcher customFetcher) {
        fetcher = customFetcher;
    }

    public static void setCacheProvider(LottieNetworkCacheProvider customProvider) {
        cacheProvider = customProvider;
    }

    @NotNull
    public static NetworkFetcher networkFetcher(@NotNull Context context) {
        NetworkFetcher local = networkFetcher;
        if (local == null) {
            synchronized (NetworkFetcher.class) {
                local = networkFetcher;
                if (local == null) {
                    networkFetcher = local = new NetworkFetcher(networkCache(context), fetcher != null ? fetcher : new DefaultLottieNetworkFetcher());
                }
            }
        }
        return local;
    }

    @NotNull
    public static NetworkCache networkCache(@NotNull final Context context) {
        NetworkCache local = networkCache;
        if (local == null) {
            synchronized (NetworkCache.class) {
                local = networkCache;
                if (local == null) {
                    networkCache = local = new NetworkCache(cacheProvider != null ? cacheProvider : new LottieNetworkCacheProvider() {
                        @Override @NotNull
                        public File getCacheDir() {
                            return new File(context.getCacheDir(), "lottie_network_cache");
                        }
                    });
                }
            }
        }
        return local;
    }
}
