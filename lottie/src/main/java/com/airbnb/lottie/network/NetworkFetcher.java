package com.airbnb.lottie.network;

import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieResult;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.utils.ContextUtil;
import com.airbnb.lottie.utils.HMOSLogUtil;
import ohos.agp.utils.LayoutAlignment;
import ohos.agp.window.dialog.ToastDialog;
import ohos.app.Context;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipInputStream;

public class NetworkFetcher {

    @NotNull
    private final NetworkCache networkCache;
    @NotNull
    private final LottieNetworkFetcher fetcher;

    public NetworkFetcher(@NotNull NetworkCache networkCache, @NotNull LottieNetworkFetcher fetcher) {
        this.networkCache = networkCache;
        this.fetcher = fetcher;
    }

    @NotNull
    public LottieResult<LottieComposition> fetchSync(@NotNull String url, @Nullable String cacheKey) throws IOException {
        LottieComposition result = fetchFromCache(url, cacheKey);
        if (result != null) {
            return new LottieResult<>(result);
        }

        HMOSLogUtil.debug(L.TAG,"Animation for " + url + " not found in cache. Fetching from network.");

        return fetchFromNetwork(url, cacheKey);
    }

    @Nullable
    private LottieComposition fetchFromCache(@NotNull String url, @Nullable String cacheKey) throws IOException {
        if (cacheKey == null) {
            return null;
        }
        Pair<FileExtension, InputStream> cacheResult = networkCache.fetchPair(url);
        if (cacheResult == null) {
            return null;
        }

        FileExtension extension = cacheResult.f;
        InputStream inputStream = cacheResult.s;
        LottieResult<LottieComposition> result;
        if (extension == FileExtension.ZIP) {
            result = LottieCompositionFactory.fromZipStreamSync(new ZipInputStream(inputStream), url);
        } else {
            result = LottieCompositionFactory.fromJsonInputStreamSync(inputStream, url);
        }
        if (result.getValue() != null) {
            return result.getValue();
        }
        return null;
    }

    @NotNull
    private LottieResult<LottieComposition> fetchFromNetwork(@NotNull String url, @Nullable String cacheKey) {
        HMOSLogUtil.debug(L.TAG,"Fetching " + url);

        LottieFetchResult fetchResult = null;
        try {
            fetchResult = fetcher.fetchSync(url);
            if (fetchResult.isSuccessful()) {
                InputStream inputStream = fetchResult.bodyByteStream();
                String contentType = fetchResult.contentType();
                LottieResult<LottieComposition> result = fromInputStream(url, inputStream, contentType, cacheKey);
                HMOSLogUtil.debug(L.TAG,"Completed fetch from network. Success: " + (result.getValue() != null));
                return result;
            } else {
                return new LottieResult<>(new IllegalArgumentException(fetchResult.error()));
            }
        } catch (Exception e) {
            showErrorToast();
            return new LottieResult<>(e);
        } finally {
            if (fetchResult != null) {
                try {
                    fetchResult.close();
                } catch (IOException e) {
                        HMOSLogUtil.warn(L.TAG,"LottieFetchResult close failed "+ e.getLocalizedMessage());
                }
            }
        }
    }

    private void showErrorToast() {
        EventHandler eventHandler = new EventHandler(EventRunner.getMainEventRunner());
        eventHandler.postTask(() -> {
            if (ContextUtil.getContext() != null) {
                ToastDialog toastDialog = new ToastDialog(ContextUtil.getContext());
                toastDialog.setText("Network unavailable");
                toastDialog.setAlignment(LayoutAlignment.CENTER);
                toastDialog.show();
            }
        });
    }

    @NotNull
    private LottieResult<LottieComposition> fromInputStream(@NotNull String url, @NotNull InputStream inputStream, @Nullable String contentType,
                                                            @Nullable String cacheKey) throws IOException {
        FileExtension extension;
        LottieResult<LottieComposition> result;
        if (contentType == null) {
            // Assume JSON for best effort parsing. If it fails, it will just deliver the parse exception
            // in the result which is more useful than failing here.
            contentType = "application/json";
        }
        if (contentType.contains("application/zip") || url.split("\\?")[0].endsWith(".lottie")) {
            HMOSLogUtil.debug(L.TAG,"Handling zip response.");
            extension = FileExtension.ZIP;
            result = fromZipStream(url, inputStream, cacheKey);
        } else {
            HMOSLogUtil.debug(L.TAG,"Received json response.");
            extension = FileExtension.JSON;
            result = fromJsonStream(url, inputStream, cacheKey);
        }

        if (cacheKey != null && result.getValue() != null) {
            networkCache.renameTempFile(url, extension);
        }

        return result;
    }

    @NotNull
    private LottieResult<LottieComposition> fromZipStream(@NotNull String url, @NotNull InputStream inputStream, @Nullable String cacheKey)
            throws IOException {
        if (cacheKey == null) {
            return LottieCompositionFactory.fromZipStreamSync(new ZipInputStream(inputStream), null);
        }
        File file = networkCache.writeTempCacheFile(url, inputStream, FileExtension.ZIP);
        return LottieCompositionFactory.fromZipStreamSync(new ZipInputStream(new FileInputStream(file)), url);
    }

    @NotNull
    private LottieResult<LottieComposition> fromJsonStream(@NotNull String url, @NotNull InputStream inputStream, @Nullable String cacheKey)
            throws IOException {
        if (cacheKey == null) {
            return LottieCompositionFactory.fromJsonInputStreamSync(inputStream, null);
        }
        File file = networkCache.writeTempCacheFile(url, inputStream, FileExtension.JSON);
        return LottieCompositionFactory.fromJsonInputStreamSync(new FileInputStream(new File(file.getCanonicalPath())), url);
    }
}
