package com.airbnb.lottie.network;

import com.airbnb.lottie.L;
import com.airbnb.lottie.utils.HMOSLogUtil;
import ohos.app.Context;
import ohos.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class to save and restore animations fetched from an URL to the app disk cache.
 */
public class NetworkCache {

    @NotNull
    private final LottieNetworkCacheProvider cacheProvider;

    public NetworkCache(@NotNull LottieNetworkCacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    public void clear() {
        File parentDir = parentDir();
        if (parentDir.exists()) {
            File[] files = parentDir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : parentDir.listFiles()) {
                    file.delete();
                }
            }
            parentDir.delete();
        }
    }

    /**
     * If the animation doesn't exist in the cache, null will be returned.
     * <p>
     * Once the animation is successfully parsed, {#renameTempFile(FileExtension)} must be
     * called to move the file from a temporary location to its permanent cache location so it can
     * be used in the future.
     * @param url of animation
     * @throws IOException
     * @return null or pair
     */
	@Nullable
    Pair<FileExtension, InputStream> fetchPair(String url) throws IOException {
        File cachedFile;
        try {
            cachedFile = getCachedFile(url);
        } catch (FileNotFoundException e) {
            return null;
        }
        if (cachedFile == null) {
            return null;
        }

        //FileInputStream inputStream = null;
        try {
            FileInputStream inputStream = new FileInputStream(cachedFile);
            FileExtension extension;
            if (cachedFile.getAbsolutePath().endsWith(".zip")) {
                extension = FileExtension.ZIP;
            } else {
                extension = FileExtension.JSON;
            }

            HMOSLogUtil.warn(L.TAG, "Cache hit for " + url + " at " + cachedFile.getCanonicalPath());
            return new Pair<>(extension, (InputStream) inputStream);
        } catch (FileNotFoundException e) {
            return null;
        }
//        finally {
//            if(inputStream != null){
//                try{
//                    inputStream.close();
//                } catch (IOException e) {
//                    HMOSLogUtil.warn(L.TAG,"InputStream  close failed "+ e.getLocalizedMessage());
//                }
//            }
//        }

    }

    /**
     * Writes an InputStream from a network response to a temporary file. If the file successfully parses
     * to an composition, {#renameTempFile(FileExtension)} should be called to move the file
     * to its final location for future cache hits.
     * @param url of animation
     * @param stream inout stream
     * @param extension file extention
     * @throws IOException
     * @return file
     */
    File writeTempCacheFile(String url, InputStream stream, FileExtension extension) throws IOException {
        String fileName = filenameForUrl(url, extension, true);
        File file = new File(parentDir(), fileName);
        try (OutputStream output = new FileOutputStream(file);){
            try {
                byte[] buffer = new byte[1024];
                int read;

                while ((read = stream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }

                output.flush();
            } finally {
                output.close();
            }
        } finally {
            stream.close();
        }
        return file;
    }

    /**
     * If the file created by {#writeTempCacheFile(InputStream, FileExtension)} was successfully parsed,
     * this should be called to remove the temporary part of its name which will allow it to be a cache hit in the future.
     * @param extension FileExtension
     * @param url of animation
     * @throws IOException
     */
    void renameTempFile(String url, FileExtension extension) throws IOException {
        String fileName = filenameForUrl(url, extension, true);
        File file = new File(parentDir(), fileName);
        String newFileName = file.getAbsolutePath().replace(".temp", "");
        File newFile = new File(newFileName);
        boolean renamed = file.renameTo(newFile);
        HMOSLogUtil.debug(L.TAG, "Copying temp file to real file (" + newFile + ")");
        if (!renamed) {
            HMOSLogUtil.warn(L.TAG,
                "Unable to rename cache file " + file.getCanonicalPath() + " to " + newFile.getCanonicalPath() + ".");

        }
    }

    /**
     * Returns the cache file for the given url if it exists. Checks for both json and zip.
     * Returns null if neither exist.
     * @param url of animation
     * @throws IOException
     * @return Returns the cache file
     */
	@Nullable
    private File getCachedFile(String url) throws FileNotFoundException {
        File jsonFile = new File(parentDir(), filenameForUrl(url, FileExtension.JSON, false));
        if (jsonFile.exists()) {
            return jsonFile;
        }
        File zipFile = new File(parentDir(), filenameForUrl(url, FileExtension.ZIP, false));
        if (zipFile.exists()) {
            return zipFile;
        }
        return null;
    }

    private File parentDir() {
        File file = cacheProvider.getCacheDir();
        if (file.isFile()) {
            file.delete();
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private static String filenameForUrl(String url, FileExtension extension, boolean isTemp) {
        return "lottie_cache_" + url.replaceAll("\\W+", "") + (isTemp
            ? extension.tempExtension()
            : extension.extension);
    }
}
