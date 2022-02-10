package com.airbnb.lottie;


import com.airbnb.lottie.model.Font;
import com.airbnb.lottie.model.FontCharacter;
import com.airbnb.lottie.model.Marker;
import com.airbnb.lottie.model.layer.Layer;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.utils.HMOSLogUtil;

import ohos.agp.utils.Rect;
import ohos.app.Context;
import ohos.utils.LongPlainArray;
import ohos.utils.PlainArray;
import ohos.utils.zson.ZSONObject;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * After Effects/Bodymovin composition model. This is the serialized model from which the
 * animation will be created.
 * <p>
 * To create one, use {@link LottieCompositionFactory}.
 * <p>
 * It can be used with a {@link LottieAnimationView} or
 * {@link LottieDrawable}.
 */
public class LottieComposition {

    private final PerformanceTracker perfTracker = new PerformanceTracker();

    private final HashSet<String> warnings = new HashSet<>();

    private Map<String, List<Layer>> precomps;

    private Map<String, LottieImageAsset> images;

    /**
     * Map of font names to fonts
     */
    private Map<String, Font> fonts;

    private List<Marker> markers;

    private PlainArray<FontCharacter> characters;

    private List<Layer> layers;

    // This is stored as a set to avoid duplicates.
    private Rect bounds;

    private float startFrame;

    private float endFrame;

    private float frameRate;

    private LongPlainArray<Layer> layerMap;

    /**
     * Used to determine if an animation can be drawn with hardware acceleration.
     */
    private boolean hasDashPattern;

    /**
     * Counts the number of mattes and masks., using hardware acceleration with mattes and masks
     * was only faster until you had ~4 masks after which it would actually become slower.
     */
    private int maskAndMatteCount = 0;

    public void init(Rect bounds, float startFrame, float endFrame, float frameRate,
        List<Layer> layers, LongPlainArray<Layer> layerMap, Map<String, List<Layer>> precomps,
        Map<String, LottieImageAsset> images, PlainArray<FontCharacter> characters, Map<String, Font> fonts,
        List<Marker> markers) {
        this.bounds = bounds;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.frameRate = frameRate;
        this.layers = layers;
        this.layerMap = layerMap;
        this.precomps = precomps;
        this.images = images;
        this.characters = characters;
        this.fonts = fonts;
        this.markers = markers;
    }

    public void addWarning(String warning) {
        HMOSLogUtil.warn(L.TAG, warning);
    }

    public void setHasDashPattern(boolean hasDashPattern) {
        this.hasDashPattern = hasDashPattern;
    }

    public void incrementMatteOrMaskCount(int amount) {
        maskAndMatteCount += amount;
    }

    /**
     * Used to determine if an animation can be drawn with hardware acceleration.
     * @return hasDashPattern
     */
    public boolean hasDashPattern() {
        return hasDashPattern;
    }

    /**
     * Used to determine if an animation can be drawn with hardware acceleration.
     * @return maskAndMatteCount
     */
    public int getMaskAndMatteCount() {
        return maskAndMatteCount;
    }

    public ArrayList<String> getWarnings() {
        return new ArrayList<>(Arrays.asList(warnings.toArray(new String[warnings.size()])));
    }

    public void setPerfTrackingEnabled(boolean enabled) {
        perfTracker.setEnabled(enabled);
    }

    public PerformanceTracker getPerfTracker() {
        return perfTracker;
    }

    public Layer layerModelForId(long id) {
        Optional<Layer> optVal = layerMap.get(id);
        if(optVal.isPresent())
        {
            return optVal.get();
        }
        return null;
    }

    public Rect getBounds() {
        return bounds;
    }

    public float getDuration() {
        return (long) (getDurationFrames() / frameRate * 1000);
    }

    public float getStartFrame() {
        return startFrame;
    }

    public float getEndFrame() {
        return endFrame;
    }

    public float getFrameRate() {
        return frameRate;
    }

    public List<Layer> getLayers() {
        return layers;
    }

    @Nullable
    public List<Layer> getPrecomps(String id) {
        return precomps.get(id);
    }

    public PlainArray<FontCharacter> getCharacters() {
        return characters;
    }

    public Map<String, Font> getFonts() {
        return fonts;
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    @Nullable
    public Marker getMarker(String markerName) {
        int size = markers.size();
        for (int i = 0; i < size; i++) {
            Marker marker = markers.get(i);
            if (marker.matchesName(markerName)) {
                return marker;
            }
        }
        return null;
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }

    public Map<String, LottieImageAsset> getImages() {
        return images;
    }

    public float getDurationFrames() {
        return endFrame - startFrame;
    }


    public String toString() {
//        final StringBuilder sb = new StringBuilder("LottieComposition:\n");
//        for (Layer layer : layers) {
//            sb.append(layer.toString("\t"));
//        }
//        return sb.toString();
        return "S";
    }


    /**
     * This will be removed in the next version of Lottie. {@link LottieCompositionFactory} has improved
     * API names, failure handlers, and will return in-progress tasks so you will never parse the same
     * animation twice in parallel.
     *
     * @see LottieCompositionFactory
     */
    @Deprecated
    public static class Factory {
        private Factory() {
        }

        /**
         * Return the listener
         * @see LottieCompositionFactory#fromAsset(ohos.app.Context, String)
         * @param context pass context
         * @param fileName of the asset
         * @param l OnCompositionLoadedListener
         * @return listener
         */
        @Deprecated
        public static Cancellable fromAssetFileName(Context context, String fileName,
            OnCompositionLoadedListener l) {
            ListenerAdapter listener = new ListenerAdapter(l);
            LottieCompositionFactory.fromAsset(context, fileName).addListener(listener);
            return listener;
        }

        /**
         * Return the listener
         * @see LottieCompositionFactory#fromRawRes(ohos.app.Context, int)
         * @param context pass context
         * @param resId raw id
         * @param l OnCompositionLoadedListener
         * @return fromRawRes listener
         */
        @Deprecated
        public static Cancellable fromRawFile(Context context, int resId, OnCompositionLoadedListener l) {
            ListenerAdapter listener = new ListenerAdapter(l);
            LottieCompositionFactory.fromRawRes(context, resId).addListener(listener);
            return listener;
        }

        /**
         * Return InputStream listener of ListenerAdapter
         * @see LottieCompositionFactory#fromJsonInputStream(InputStream, String)
         * @param stream InputStream
         * @param l OnCompositionLoadedListener
         * @return listener
         */
        @Deprecated
        public static Cancellable fromInputStream(InputStream stream, OnCompositionLoadedListener l) {
            ListenerAdapter listener = new ListenerAdapter(l);
            LottieCompositionFactory.fromJsonInputStream(stream, null).addListener(listener);
            return listener;
        }

        /**
         * Return JsonString listener
         * @see LottieCompositionFactory#fromJsonString(String, String)
         * @param jsonString string
         * @param l OnCompositionLoadedListener
         * @return listener
         */
        @Deprecated
        public static Cancellable fromJsonString(String jsonString, OnCompositionLoadedListener l) {
            ListenerAdapter listener = new ListenerAdapter(l);
            LottieCompositionFactory.fromJsonString(jsonString, null).addListener(listener);
            return listener;
        }

        /**
         * Return JsonReader listener
         * @see LottieCompositionFactory#fromJsonReader(JsonReader, String)
         * @param reader JsonReader
         * @param l OnCompositionLoadedListener
         * @return listener
         */
        @Deprecated
        public static Cancellable fromJsonReader(JsonReader reader, OnCompositionLoadedListener l) {
            ListenerAdapter listener = new ListenerAdapter(l);
            LottieCompositionFactory.fromJsonReader(reader, null).addListener(listener);
            return listener;
        }

        /**
         * value of an async task or an exception if it failed.
         * @see LottieCompositionFactory#fromAssetSync(ohos.app.Context, String)
         * @param context pass context
         * @param fileName asset file name
         * @return value of an async task or an exception if it failed.
         */
        @Nullable
        @Deprecated
        public static LottieComposition fromFileSync(Context context, String fileName) {
            return LottieCompositionFactory.fromAssetSync(context, fileName).getValue();
        }

        /**
         * value of an async task or an exception if it failed.
         * @see LottieCompositionFactory#fromJsonInputStreamSync(InputStream, String)
         * @param stream InputStream
         * @return value of an async task or an exception if it failed.
         */
        @Nullable
        @Deprecated
        public static LottieComposition fromInputStreamSync(InputStream stream) {
            return LottieCompositionFactory.fromJsonInputStreamSync(stream, null).getValue();
        }

        /**
         * This will now auto-close the input stream!
         *
         * @see LottieCompositionFactory#fromJsonInputStreamSync(InputStream, String)
         * @param stream auto-close the input stream
         * @param close true if close else false
         * @return value of an async task or an exception if it failed.
         */
        @Nullable
        @Deprecated
        public static LottieComposition fromInputStreamSync(InputStream stream, boolean close) {
            if (close) {
                HMOSLogUtil.warn(L.TAG, "Lottie now auto-closes input stream!");
            }
            return LottieCompositionFactory.fromJsonInputStreamSync(stream, null).getValue();
        }

        /**
         * value of an async task or an exception if it failed.
         * Prefer passing in the json string directly.
         * @see LottieCompositionFactory#fromJsonSync(ZSONObject, String)
         * @param json string
         * @return value of an async task or an exception if it failed.
         */
        @Nullable
        @Deprecated
        public static LottieComposition fromJsonSync(ZSONObject json) {
            return LottieCompositionFactory.fromJsonSync(json, null).getValue();
        }

        /**
         * value of an async task or an exception if it failed.
         * @see LottieCompositionFactory#fromJsonStringSync(String, String)
         * @param json raw json string
         * @return value of an async task or an exception if it failed.
         */
        @Nullable
        @Deprecated
        public static LottieComposition fromJsonSync(String json) {
            return LottieCompositionFactory.fromJsonStringSync(json, null).getValue();
        }

        /**
         * value of an async task or an exception if it failed.
         * @see LottieCompositionFactory#fromJsonReaderSync(JsonReader, String)
         * @param reader JsonReader
         * @return value of an async task or an exception if it failed.
         * @throws IOException if fails
         */
        @Nullable
        @Deprecated
        public static LottieComposition fromJsonSync(JsonReader reader) {
            return LottieCompositionFactory.fromJsonReaderSync(reader, null).getValue();
        }

        private static final class ListenerAdapter implements LottieListener<LottieComposition>, Cancellable {
            private final OnCompositionLoadedListener listener;

            private boolean cancelled = false;

            private ListenerAdapter(OnCompositionLoadedListener listener) {
                this.listener = listener;
            }

            @Override
            public void onResult(LottieComposition composition) {
                if (cancelled) {
                    return;
                }
                listener.onCompositionLoaded(composition);
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        }
    }
}
