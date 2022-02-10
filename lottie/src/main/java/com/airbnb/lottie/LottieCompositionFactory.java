package com.airbnb.lottie;

import static okio.Okio.buffer;
import static okio.Okio.source;

import com.airbnb.lottie.model.LottieCompositionCache;
import com.airbnb.lottie.network.NetworkCache;
import com.airbnb.lottie.network.NetworkFetcher;
import com.airbnb.lottie.parser.LottieCompositionMoshiParser;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.utils.HMOSLogUtil;
import com.airbnb.lottie.utils.Utils;

import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.utils.zson.ZSONObject;
import okio.BufferedSource;
import okio.Okio;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Helpers to create or cache a LottieComposition.
 * <p>
 * All factory methods take a cache key. The animation will be stored in an LRU cache for future use.
 * In-progress tasks will also be held so they can be returned for subsequent requests for the same
 * animation prior to the cache being populated.
 */
public class LottieCompositionFactory {
    /**
     * Keep a map of cache keys to in-progress tasks and return them for new requests.
     * Without this, simultaneous requests to parse a composition will trigger multiple parallel
     * parse tasks prior to the cache getting populated.
     */
    private static final Map<String, LottieTask<LottieComposition>> taskCache = new HashMap<>();

    /**
     * reference magic bytes for zip compressed files.
     * useful to determine if an InputStream is a zip file or not
     */
    private static final byte[] MAGIC = new byte[]{0x50, 0x4b, 0x03, 0x04};

    private LottieCompositionFactory() {
    }

    /**
     * Set the maximum number of compositions to keep cached in memory.
     * This must be > 0.
     *
     * @param size in int
     */
    public static void setMaxCacheSize(int size) {
        LottieCompositionCache.getInstance().resize(size);
    }

    public static void clearCache(Context context) {
        taskCache.clear();
        LottieCompositionCache.getInstance().clear();
        L.networkCache(context).clear();
    }

    /**
     * Fetch an animation from an http url. Once it is downloaded once, Lottie will cache the file to disk for
     * future use. Because of this, you may call `fromUrl` ahead of time to warm the cache if you think you
     * might need an animation in the future.
     * <p>
     * To skip the cache, add null as a third parameter.
     *
     * @param context context
     * @param url     in string
     * @return animation from an http url
     */
    public static LottieTask<LottieComposition> fromUrl(final Context context, final String url) {
        return fromUrl(context, url, "url_" + url);
    }

    /**
     * Fetch an animation from an http url. Once it is downloaded once, Lottie will cache the file to disk for
     * future use. Because of this, you may call `fromUrl` ahead of time to warm the cache if you think you
     * might need an animation in the future.
     *
     * @param context  Application context
     * @param url      of animation
     * @param cacheKey key for caching
     * @return animation from an http url
     */
    public static LottieTask<LottieComposition> fromUrl(
            final Context context, final String url, final String cacheKey) {
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() throws IOException {
                        LottieResult<LottieComposition> result = L.networkFetcher(context).fetchSync(url, cacheKey);
                        if (cacheKey != null && result.getValue() != null) {
                            LottieCompositionCache.getInstance().put(cacheKey, result.getValue());
                        }
                        return result;
                    }
                });
    }

    /**
     * Fetch an animation from an http url. Once it is downloaded once, Lottie will cache the file to disk for
     * future use. Because of this, you may call `fromUrl` ahead of time to warm the cache if you think you
     * might need an animation in the future.
     *
     * @param context application context
     * @param url     animation url
     * @return animation from an http url
     * @throws IOException
     */
    public static LottieResult<LottieComposition> fromUrlSync(Context context, String url) throws IOException {
        return fromUrlSync(context, url, url);
    }

    /**
     * Fetch an animation from an http url. Once it is downloaded once, Lottie will cache the file to disk for
     * future use. Because of this, you may call `fromUrl` ahead of time to warm the cache if you think you
     * might need an animation in the future.
     *
     * @param context  Application context
     * @param url      of animation
     * @param cacheKey key for caching
     * @return animation from an http url
     * @throws IOException
     */
    public static LottieResult<LottieComposition> fromUrlSync(Context context, String url, String cacheKey) throws IOException {
    LottieResult<LottieComposition> result = L.networkFetcher(context).fetchSync(url, cacheKey);
    if (cacheKey != null && result.getValue() != null) {
        LottieCompositionCache.getInstance().put(cacheKey, result.getValue());
    }
    return result;
}


    /**
     * Parse an animation from src/main/assets. It is recommended to use {@link #fromRawRes(Context, int)} instead.
     * The asset file name will be used as a cache key so future usages won't have to parse the json again.
     * However, if your animation has images, you may package the json and images as a single flattened zip file in assets.
     * <p>
     * To skip the cache, add null as a third parameter.
     *
     * @param context  application context
     * @param fileName from asset
     * @return task
     * @see #fromZipStream(ZipInputStream, String)
     */
    public static LottieTask<LottieComposition> fromAsset(Context context, final String fileName) {
        String cacheKey = "asset_" + fileName;
        return fromAsset(context, fileName, cacheKey);
    }

    /**
     * Parse an animation from src/main/assets. It is recommended to use {@link #fromRawRes(Context, int)} instead.
     * The asset file name will be used as a cache key so future usages won't have to parse the json again.
     * However, if your animation has images, you may package the json and images as a single flattened zip file in assets.
     * <p>
     * Pass null as the cache key to skip the cache.
     *
     * @param context  application context
     * @param fileName from asset
     * @param cacheKey used as a cache key
     * @return task
     * @see #fromZipStream(ZipInputStream, String)
     */
    public static LottieTask<LottieComposition> fromAsset(
            Context context, final String fileName, final String cacheKey) {
        // Prevent accidentally leaking an Activity.
        final Context appContext = context.getApplicationContext();
        String rawfilePath = "entry/resources/rawfile/";
        String newFileName = rawfilePath + fileName;
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        LottieResult<LottieComposition> composition = fromAssetSync(appContext, fileName, cacheKey);
                        return composition;
                    }
                });
    }

    /**
     * Parse an animation from src/main/assets. It is recommended to use {@link #fromRawRes(Context, int)} instead.
     * The asset file name will be used as a cache key so future usages won't have to parse the json again.
     * However, if your animation has images, you may package the json and images as a single flattened zip file in assets.
     * <p>
     * To skip the cache, add null as a third parameter.
     *
     * @param context  of application
     * @param fileName of asset
     * @return Return a LottieComposition for the given asset
     * @see #fromZipStreamSync(ZipInputStream, String)
     */
    public static LottieResult<LottieComposition> fromAssetSync(Context context, String fileName) {
        String cacheKey = "asset_" + fileName;
        return fromAssetSync(context, fileName, cacheKey);
    }

    /**
     * Parse an animation from src/main/assets. It is recommended to use {@link #fromRawRes(Context, int)} instead.
     * The asset file name will be used as a cache key so future usages won't have to parse the json again.
     * However, if your animation has images, you may package the json and images as a single flattened zip file in assets.
     * <p>
     * Pass null as the cache key to skip the cache.
     *
     * @param fileName json file name
     * @param context  of application
     * @param cacheKey key for cache
     * @return Return a LottieComposition for the given raw file
     * @see #fromZipStreamSync(ZipInputStream, String)
     */
    public static LottieResult<LottieComposition> fromAssetSync(Context context, String fileName, String cacheKey) {
        try {
            String rawfilePath = "entry/resources/rawfile/";
            String newFileName = rawfilePath + fileName;
            InputStream inputStream = context.getResourceManager().getRawFileEntry(newFileName).openRawFile();
            if (fileName.endsWith(".zip") || fileName.endsWith(".lottie")) {
                return fromZipStreamSync(
                    new ZipInputStream(inputStream), cacheKey);
            }
            return fromJsonInputStreamSync(inputStream, cacheKey);
        } catch (IOException e) {
            return new LottieResult<>(e);
        }
    }

    /**
     * Parse an animation from raw/res. This is recommended over putting your animation in assets because
     * it uses a hard reference to R.
     * The resource id will be used as a cache key so future usages won't parse the json again.
     * Note: to correctly load dark mode (-night) resources, make sure you pass Activity as a context (instead of e.g. the application context).
     * The Activity won't be leaked.
     * <p>
     * To skip the cache, add null as a third parameter.
     *
     * @param context of application
     * @param rawRes  animation file
     * @return Return a LottieComposition for the given raw/res animation
     */
    public static LottieTask<LottieComposition> fromRawRes(Context context, final int rawRes) {
        return fromRawRes(context, rawRes, rawResCacheKey(context, rawRes));
    }

    /**
     * Parse an animation from raw/res. This is recommended over putting your animation in assets because
     * it uses a hard reference to R.
     * The resource id will be used as a cache key so future usages won't parse the json again.
     * Note: to correctly load dark mode (-night) resources, make sure you pass Activity as a context (instead of e.g. the application context).
     * The Activity won't be leaked.
     * <p>
     * Pass null as the cache key to skip caching.
     *
     * @param rawRes   animation file
     * @param context  of application
     * @param cacheKey key for cache
     * @return Return a LottieComposition
     */
    public static LottieTask<LottieComposition> fromRawRes(Context context, final int rawRes, @Nullable final String cacheKey) {
        // Prevent accidentally leaking an Activity.
        final WeakReference<Context> contextRef = new WeakReference<>(context);
        final Context appContext = context.getApplicationContext();
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        Context originalContext = contextRef.get();
                        Context context = originalContext != null ? originalContext : appContext;
                        return fromRawResSync(context, rawRes, cacheKey);
                    }
                });
    }

    /**
     * Parse an animation from raw/res. This is recommended over putting your animation in assets because
     * it uses a hard reference to R.
     * The resource id will be used as a cache key so future usages won't parse the json again.
     * Note: to correctly load dark mode (-night) resources, make sure you pass Activity as a context (instead of e.g. the application context).
     * The Activity won't be leaked.
     * <p>
     * To skip the cache, add null as a third parameter.
     *
     * @param rawRes  animation file
     * @param context of application
     * @return Return a LottieComposition
     */
    public static LottieResult<LottieComposition> fromRawResSync(Context context, int rawRes) {
        return fromRawResSync(context, rawRes, rawResCacheKey(context, rawRes));
    }

    /**
     * Parse an animation from raw/res. This is recommended over putting your animation in assets because
     * it uses a hard reference to R.
     * The resource id will be used as a cache key so future usages won't parse the json again.
     * Note: to correctly load dark mode (-night) resources, make sure you pass Activity as a context (instead of e.g. the application context).
     * The Activity won't be leaked.
     * <p>
     * Pass null as the cache key to skip caching.
     *
     * @param rawRes   animation file
     * @param context  of application
     * @param cacheKey key for cache
     * @return Return a LottieComposition
     */
    public static LottieResult<LottieComposition> fromRawResSync(Context context, int rawRes, String cacheKey) {
        try {
           // BufferedSource source = Okio.buffer(source(context.getResources().openRawResource(rawRes)));
            BufferedSource source = Okio.buffer(source(context.getResourceManager().getResource(rawRes)));
            if (isZipCompressed(source)) {
                return fromZipStreamSync(new ZipInputStream(source.inputStream()), cacheKey);
            }
            return fromJsonInputStreamSync(source.inputStream(), cacheKey);
        } catch (NotExistException | IOException e) {
            return new LottieResult<>(e);
        }
    }

    // TODO - UI mode (day/night) is not supported in HMOS
    private static String rawResCacheKey(Context context, int resId) {
        return "rawRes" + /*(isNightMode(context) ? "_night_" : "_day_")*/ +resId;
    }

    /**
     * It is important to include day/night in the cache key so that if it changes, the cache won't return an animation from the wrong bucket.
     */
    /*private static boolean isNightMode(Context context) {
     *     int nightModeMasked = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
     *     return nightModeMasked == Configuration.UI_MODE_NIGHT_YES;
    }*/

    /**
     * Auto-closes the stream.
     *
     * @param cacheKey key for cache
     * @param stream   json input stream
     * @return the LottieComposition
     * @see #fromJsonInputStreamSync(InputStream, String, boolean)
     */
    public static LottieTask<LottieComposition> fromJsonInputStream(final InputStream stream, final String cacheKey) {
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        return fromJsonInputStreamSync(stream, cacheKey);
                    }
                });
    }

    /**
     * Return a LottieComposition for the given InputStream to json.
     *
     * @param stream   json input stream
     * @param cacheKey key for cache
     * @return Return a LottieComposition for the given InputStream to json
     */
    public static LottieResult<LottieComposition> fromJsonInputStreamSync(InputStream stream, String cacheKey) {
        return fromJsonInputStreamSync(stream, cacheKey, true);
    }

    private static LottieResult<LottieComposition> fromJsonInputStreamSync(
            InputStream stream, String cacheKey, boolean close) {
        try {
            return fromJsonReaderSync(JsonReader.of(buffer(source(stream))), cacheKey);
        } finally {
            if (close) {
                Utils.closeQuietly(stream);
            }
        }
    }

    /**
     * Prefer passing in the json string directly. This method just calls `toString()` on your ZSONObject.
     * If you are loading this animation from the network, just use the response body string instead of
     * parsing it first for improved performance.
     *
     * @param cacheKey key for cache
     * @param json     animation json
     * @return Lottie composition
     * @see #fromJsonSync(ZSONObject, String)
     */
    @Deprecated
    public static LottieTask<LottieComposition> fromJson(final ZSONObject json, final String cacheKey) {
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        return fromJsonSync(json, cacheKey);
                    }
                });
    }

    /**
     * Prefer passing in the json string directly. This method just calls `toString()` on your ZSONObject.
     * If you are loading this animation from the network, just use the response body string instead of
     * parsing it first for improved performance.
     *
     * @param json     animation
     * @param cacheKey key for cache
     * @return Lottie composition
     */
    @Deprecated
    public static LottieResult<LottieComposition> fromJsonSync(ZSONObject json, String cacheKey) {
        return fromJsonStringSync(json.toString(), cacheKey);
    }

    /**
     * Retuns cachedComposition for the given Json
     *
     * @param cacheKey key for cache
     * @param json     animation
     * @return cachedComposition
     * @see #fromJsonStringSync(String, String)
     */
    public static LottieTask<LottieComposition> fromJsonString(final String json, final String cacheKey) {
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        return fromJsonStringSync(json, cacheKey);
                    }
                });
    }

    /**
     * Return a LottieComposition for the specified raw json string.
     * If loading from a file, it is preferable to use the InputStream or rawRes version.
     *
     * @param json     raw json string
     * @param cacheKey for caching
     * @return Return a LottieComposition for the specified raw json string.
     */
    public static LottieResult<LottieComposition> fromJsonStringSync(String json, String cacheKey) {
        ByteArrayInputStream stream = new ByteArrayInputStream(json.getBytes());
        return fromJsonReaderSync(JsonReader.of(buffer(source(stream))), cacheKey);
    }

    public static LottieTask<LottieComposition> fromJsonReader(final JsonReader reader, final String cacheKey) {
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        return fromJsonReaderSync(reader, cacheKey);
                    }
                });
    }

    public static LottieResult<LottieComposition> fromJsonReaderSync(JsonReader reader, String cacheKey) {
        return fromJsonReaderSyncInternal(reader, cacheKey, true);
    }

    private static LottieResult<LottieComposition> fromJsonReaderSyncInternal(
            JsonReader reader, String cacheKey, boolean close) {
        try {
            LottieComposition composition = LottieCompositionMoshiParser.parse(reader);
            if (cacheKey != null) {
                LottieCompositionCache.getInstance().put(cacheKey, composition);
            }
            return new LottieResult<>(composition);
        } catch (Exception e) {
            return new LottieResult<>(e);
        } finally {
            if (close) {
                Utils.closeQuietly(reader);
            }
        }
    }

    /**
     * Prefer passing in the json from the ZipInputStream .
     * If you are loading this animation from the network, just use the response body string instead of
     * parsing it first for improved performance.
     *
     * @param inputStream
     * @param cacheKey
     * @return cachedComposition
     */
    public static LottieTask<LottieComposition> fromZipStream(final ZipInputStream inputStream, final String cacheKey) {
        return cache(
                cacheKey,
                new Callable<LottieResult<LottieComposition>>() {
                    @Override
                    public LottieResult<LottieComposition> call() {
                        return fromZipStreamSync(inputStream, cacheKey);
                    }
                });
    }

    /**
     * Parses a zip input stream into a Lottie composition.
     * Your zip file should just be a folder with your json file and images zipped together.
     * It will automatically store and configure any images inside the animation if they exist.
     *
     * @param cacheKey    key to cache
     * @param inputStream zip input stream
     * @return Lottie Composition
     */
    public static LottieResult<LottieComposition> fromZipStreamSync(ZipInputStream inputStream, String cacheKey) {
        try {
            return fromZipStreamSyncInternal(inputStream, cacheKey);
        } finally {
            Utils.closeQuietly(inputStream);
        }
    }

    private static void findImageAsset(Map<String, PixelMap> images, LottieComposition composition){
        for (Map.Entry<String, PixelMap> e : images.entrySet()) {
            LottieImageAsset imageAsset = findImageAssetForFileName(composition, e.getKey());
            if (imageAsset != null) {
                imageAsset.setPixelMap(e.getValue());
                // HMOSUtils.resizeBitmapIfNeeded(e.getValue(), imageAsset.getWidth(), imageAsset.getHeight()));
            }
        }
    }

    private static LottieResult<LottieComposition> fromZipStreamSyncInternal(
            ZipInputStream inputStream, String cacheKey) {
        LottieComposition composition = null;
        Map<String, PixelMap> images = new HashMap<>();

        try {
            ZipEntry entry = inputStream.getNextEntry();
            while (entry != null) {
                final String entryName = entry.getName();
                if (entryName.contains("__MACOSX")) {
                    inputStream.closeEntry();
                } else if (entry.getName().equalsIgnoreCase("manifest.json")) { //ignore .lottie manifest
                    inputStream.closeEntry();
                } else if (entry.getName().contains(".json")) {
                    com.airbnb.lottie.parser.moshi.JsonReader reader = JsonReader.of(buffer(source(inputStream)));
                    composition = LottieCompositionFactory.fromJsonReaderSyncInternal(reader, null, false).getValue();
                } else if (entryName.contains(".png") || entryName.contains(".webp")) {
                    String[] splitName = entryName.split("/");
                    String name = splitName[splitName.length - 1];
                    ImageSource im = ImageSource.create(inputStream, new ImageSource.SourceOptions());
                    images.put(name, im.createPixelmap(new ImageSource.DecodingOptions()));
                } else {
                    inputStream.closeEntry();
                }

                entry = inputStream.getNextEntry();
            }
        } catch (IOException e) {
            return new LottieResult<>(e);
        }

        if (composition == null) {
            return new LottieResult<>(new IllegalArgumentException("Unable to parse composition"));
        }

        findImageAsset(images,composition);

        // Ensure that all bitmaps have been set.
        for (Map.Entry<String, LottieImageAsset> entry : composition.getImages().entrySet()) {
            if (entry.getValue().getPixelmap() == null) {
                return new LottieResult<>(
                        new IllegalStateException("There is no image for " + entry.getValue().getFileName()));
            }
        }

        if (cacheKey != null) {
            LottieCompositionCache.getInstance().put(cacheKey, composition);
        }
        return new LottieResult<>(composition);
    }

    /**
     * Check if a given InputStream points to a .zip compressed file
     * @param inputSource given InputStream
     * @return boolean
     */
    private static Boolean isZipCompressed(BufferedSource inputSource) {

        try {
            BufferedSource peek = inputSource.peek();
            for (byte b : MAGIC) {
                if (peek.readByte() != b) {
                    return false;
                }
            }
            peek.close();
            return true;
        } catch (Exception e) {
            HMOSLogUtil.error(L.TAG,"Failed to check zip file header", e);
            return false;
        }
    }

    private static LottieImageAsset findImageAssetForFileName(LottieComposition composition, String fileName) {
        for (LottieImageAsset asset : composition.getImages().values()) {
            if (asset.getFileName().equals(fileName)) {
                return asset;
            }
        }
        return null;
    }

    /**
     * First, check to see if there are any in-progress tasks associated with the cache key and return it if there is.
     * If not, create a new task for the callable.
     * Then, add the new task to the task cache and set up listeners so it gets cleared when done.
     *
     * @param cacheKey task cache
     * @param callable of LottieResult<LottieComposition>
     * @return task
     */
    private static LottieTask<LottieComposition> cache(
            final String cacheKey, Callable<LottieResult<LottieComposition>> callable) {
        final LottieComposition cachedComposition =
                cacheKey == null ? null : LottieCompositionCache.getInstance().get(cacheKey);
        if (cachedComposition != null) {
            return new LottieTask<>(
                    new Callable<LottieResult<LottieComposition>>() {
                        @Override
                        public LottieResult<LottieComposition> call() {
                            return new LottieResult<>(cachedComposition);
                        }
                    });
        }
        if (cacheKey != null && taskCache.containsKey(cacheKey)) {
            return taskCache.get(cacheKey);
        }

        LottieTask<LottieComposition> task = new LottieTask<>(callable);
        if (cacheKey != null) {
            task.addListener(
                    new LottieListener<LottieComposition>() {
                        @Override
                        public void onResult(LottieComposition result) {
                            taskCache.remove(cacheKey);
                        }
                    });
            task.addFailureListener(
                    new LottieListener<Throwable>() {
                        @Override
                        public void onResult(Throwable result) {
                            taskCache.remove(cacheKey);
                        }
                    });
            taskCache.put(cacheKey, task);
        }
        return task;
    }
}
