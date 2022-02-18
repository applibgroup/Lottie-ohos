package com.airbnb.lottie.parser;

import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.Keyframe;

import ohos.agp.animation.Animator;
import ohos.agp.utils.Point;
import ohos.utils.PlainArray;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Optional;

class KeyframeParser {
    /**
     * Some animations get exported with insane cp values in the tens of thousands.
     * PathInterpolator fails to create the interpolator in those cases and hangs.
     * Clamping the cp helps prevent that.
     */
    private static final float MAX_CP_VALUE = 100;

    private static final Animator.CurveType LINEAR_INTERPOLATOR = new Animator.CurveType();

    private static PlainArray<WeakReference<Animator.CurveType>> pathInterpolatorCache;

    static JsonReader.Options NAMES = JsonReader.Options.of("t", "s", "e", "o", "i", "h", "to", "ti");

    static JsonReader.Options INTERPOLATOR_NAMES = JsonReader.Options.of(
            "x",  // 1
            "y"   // 2
    );

    private static PlainArray<WeakReference<Animator.CurveType>> pathInterpolatorCache() {
        if (pathInterpolatorCache == null) {
            pathInterpolatorCache = new PlainArray<>();
        }
        return pathInterpolatorCache;
    }

    private static WeakReference<Animator.CurveType> getInterpolator(int hash) {
        // This must be synchronized because get and put isn't thread safe because
        // SparseArrayCompat has to create new sized arrays sometimes.
        synchronized (KeyframeParser.class) {
            Optional<WeakReference<Animator.CurveType>> optionalVal = pathInterpolatorCache().get(hash);

            if(optionalVal.isPresent())
            {
                return optionalVal.get();
            }
            return null;
        }
    }

    private static void putInterpolator(int hash, WeakReference<Animator.CurveType> interpolator) {
        // This must be synchronized because get and put isn't thread safe because
        // SparseArrayCompat has to create new sized arrays sometimes.
        synchronized (KeyframeParser.class) {
            pathInterpolatorCache.put(hash, interpolator);
        }
    }

    static <T> Keyframe<T> parse(JsonReader reader, LottieComposition composition,
                                 float scale, ValueParser<T> valueParser, boolean animated, boolean multiDimensional) throws IOException {
        if (animated && multiDimensional) {
            return parseMultiDimensionalKeyframe(composition, reader, scale, valueParser);
        } else if (animated) {
            return parseKeyframe(composition, reader, scale, valueParser);
        } else {
            return parseStaticValue(reader, scale, valueParser);
        }
    }

    /**
     * beginObject will already be called on the keyframe so it can be differentiated with
     * a non animated value.
     * @param reader JsonReader
     * @param scale in float
     * @param composition LottieComposition
     * @param valueParser ValueParser<T>
     * @throws IOException
     * @return keyframe
     */
    private static <T> Keyframe<T> parseKeyframe(LottieComposition composition, JsonReader reader, float scale,
        ValueParser<T> valueParser) throws IOException {
        Point cp1 = null;
        Point cp2 = null;
        float startFrame = 0;
        T startValue = null;
        T endValue = null;
        boolean hold = false;
        Animator.CurveType interpolator = null;

        // Only used by PathKeyframe
        Point pathCp1 = null;
        Point pathCp2 = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(NAMES)) {
                case 0:
                    startFrame = (float) reader.nextDouble();
                    break;
                case 1:
                    startValue = valueParser.parse(reader, scale);
                    break;
                case 2:
                    endValue = valueParser.parse(reader, scale);
                    break;
                case 3:
                    cp1 = JsonUtils.jsonToPoint(reader, 1f);
                    break;
                case 4:
                    cp2 = JsonUtils.jsonToPoint(reader, 1f);
                    break;
                case 5:
                    hold = reader.nextInt() == 1;
                    break;
                case 6:
                    pathCp1 = JsonUtils.jsonToPoint(reader, scale);
                    break;
                case 7:
                    pathCp2 = JsonUtils.jsonToPoint(reader, scale);
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (hold) {
            endValue = startValue;
            // TODO: create a HoldInterpolator so progress changes don't invalidate.
            interpolator = new Animator.CurveType();
        } else if (cp1 != null && cp2 != null) {
            interpolator = interpolatorFor(cp1, cp2);
        } else {
            interpolator = LINEAR_INTERPOLATOR;
        }

        Keyframe<T> keyframe = new Keyframe<>(composition, startValue, endValue, interpolator, startFrame, null);

        keyframe.pathCp1 = pathCp1;
        keyframe.pathCp2 = pathCp2;
        return keyframe;
    }

    private static ArrayList<Point> oKeyFrame(JsonReader reader, float scale) throws IOException {

        Point cp1 = null;
        Point xCp1 = null;
        Point yCp1 = null;
        ArrayList<Point> oKeyFramePoint = new ArrayList<>();

        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject();
            float xCp1x = 0f;
            float xCp1y = 0f;
            float yCp1x = 0f;
            float yCp1y = 0f;


            while (reader.hasNext()) {
                switch (reader.selectName(INTERPOLATOR_NAMES)) {
                    case 0: // x
                        if (reader.peek() == JsonReader.Token.NUMBER) {
                            xCp1x = (float) reader.nextDouble();
                            yCp1x = xCp1x;
                        } else {
                            reader.beginArray();
                            xCp1x = (float) reader.nextDouble();
                            yCp1x = (float) reader.nextDouble();
                            reader.endArray();
                        }
                        break;
                    case 1: // y
                        if (reader.peek() == JsonReader.Token.NUMBER) {
                            xCp1y = (float) reader.nextDouble();
                            yCp1y = xCp1y;
                        } else {
                            reader.beginArray();
                            xCp1y = (float) reader.nextDouble();
                            yCp1y = (float) reader.nextDouble();
                            reader.endArray();
                        }
                        break;
                    default:
                        reader.skipValue();
                }
            }
            xCp1 = new Point(xCp1x, xCp1y);
            yCp1 = new Point(yCp1x, yCp1y);
            reader.endObject();
        } else {
            cp1 = JsonUtils.jsonToPoint(reader, scale);
        }
        oKeyFramePoint.add(xCp1);
        oKeyFramePoint.add(yCp1);
        oKeyFramePoint.add(cp1);
        return oKeyFramePoint;
    }

    private static ArrayList<Point> iKeyFrame(JsonReader reader,float scale) throws IOException {

        Point cp2 = null;
        Point xCp2 = null;
        Point yCp2 = null;

        ArrayList<Point> iKeyFramePoint = new ArrayList<>();

        if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
            reader.beginObject();
            float xCp2x = 0f;
            float xCp2y = 0f;
            float yCp2x = 0f;
            float yCp2y = 0f;
            while (reader.hasNext()) {
                switch (reader.selectName(INTERPOLATOR_NAMES)) {
                    case 0: // x
                        if (reader.peek() == JsonReader.Token.NUMBER) {
                            xCp2x = (float) reader.nextDouble();
                            yCp2x = xCp2x;
                        } else {
                            reader.beginArray();
                            xCp2x = (float) reader.nextDouble();
                            yCp2x = (float) reader.nextDouble();
                            reader.endArray();
                        }
                        break;
                    case 1: // y
                        if (reader.peek() == JsonReader.Token.NUMBER) {
                            xCp2y = (float) reader.nextDouble();
                            yCp2y = xCp2y;
                        } else {
                            reader.beginArray();
                            xCp2y = (float) reader.nextDouble();
                            yCp2y = (float) reader.nextDouble();
                            reader.endArray();
                        }
                        break;
                    default:
                        reader.skipValue();
                }
            }
            xCp2 = new Point(xCp2x, xCp2y);
            yCp2 = new Point(yCp2x, yCp2y);
            reader.endObject();
        } else {
            cp2 = JsonUtils.jsonToPoint(reader, scale);
        }
        iKeyFramePoint.add(xCp2);
        iKeyFramePoint.add(yCp2);
        iKeyFramePoint.add(cp2);
        return iKeyFramePoint;
    }

    private static <T> Keyframe<T> parseMultiDimensionalKeyframe(LottieComposition composition, JsonReader reader,
                                                                 float scale, ValueParser<T> valueParser) throws IOException {
        Point cp1 = null;
        Point cp2 = null;

        Point xCp1 = null;
        Point xCp2 = null;
        Point yCp1 = null;
        Point yCp2 = null;

        ArrayList<Point> keyPoints;

        float startFrame = 0;
        T startValue = null;
        T endValue = null;
        boolean hold = false;
        Animator.CurveType interpolator = null;
        Animator.CurveType xInterpolator = null;
        Animator.CurveType yInterpolator = null;

        // Only used by PathKeyframe
        Point pathCp1 = null;
        Point pathCp2 = null;

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.selectName(NAMES)) {
                case 0: // t
                    startFrame = (float) reader.nextDouble();
                    break;
                case 1: // s
                    startValue = valueParser.parse(reader, scale);
                    break;
                case 2: // e
                    endValue = valueParser.parse(reader, scale);
                    break;
                case 3: // o
                    keyPoints= oKeyFrame(reader,scale);
                    xCp1 = keyPoints.get(0);
                    yCp1 = keyPoints.get(1);
                    cp1 = keyPoints.get(3);
                    break;
                case 4: // i
                    keyPoints = iKeyFrame(reader,scale);
                    xCp2 = keyPoints.get(0);
                    yCp2 = keyPoints.get(1);
                    cp2 = keyPoints.get(2);
                    break;
                case 5: // h
                    hold = reader.nextInt() == 1;
                    break;
                case 6: // to
                    pathCp1 = JsonUtils.jsonToPoint(reader, scale);
                    break;
                case 7: // ti
                    pathCp2 = JsonUtils.jsonToPoint(reader, scale);
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();

        if (hold) {
            endValue = startValue;
            // TODO: create a HoldInterpolator so progress changes don't invalidate.
            interpolator = LINEAR_INTERPOLATOR;
        } else if (cp1 != null && cp2 != null) {
            interpolator = interpolatorFor(cp1, cp2);
        } else if (xCp1 != null && yCp1 != null && xCp2 != null && yCp2 != null) {
            xInterpolator = interpolatorFor(xCp1, xCp2);
            yInterpolator = interpolatorFor(yCp1, yCp2);
        } else {
            interpolator = LINEAR_INTERPOLATOR;
        }

        Keyframe<T> keyframe;
        if (xInterpolator != null && yInterpolator != null) {
            keyframe = new Keyframe<>(composition, startValue, endValue, xInterpolator, yInterpolator, startFrame, null);
        } else {
            keyframe = new Keyframe<>(composition, startValue, endValue, interpolator, startFrame, null);
        }

        keyframe.pathCp1 = pathCp1;
        keyframe.pathCp2 = pathCp2;
        return keyframe;
    }

    private static Animator.CurveType interpolatorFor(Point cp1, Point cp2) {
        Animator.CurveType interpolator = null;
        float x1 = MiscUtils.clamp(cp1.getPointX(), -1f, 1f);
        float y1 = MiscUtils.clamp(cp1.getPointY(), -MAX_CP_VALUE, MAX_CP_VALUE);
        float x2 = MiscUtils.clamp(cp2.getPointX(), -1f, 1f);
        float y2 = MiscUtils.clamp(cp2.getPointY(), -MAX_CP_VALUE, MAX_CP_VALUE);
        int hash = Utils.hashFor(x1, y1, x2, y2);
        WeakReference<Animator.CurveType> interpolatorRef = getInterpolator(hash);
        if (interpolatorRef != null) {
            interpolator = interpolatorRef.get();
        }
        if (interpolatorRef == null || interpolator == null) {
            interpolator = new Animator.CurveType();
            try {
                putInterpolator(hash, new WeakReference<Animator.CurveType>(interpolator));
            } catch (ArrayIndexOutOfBoundsException e) {
                // It is not clear why but SparseArrayCompat sometimes fails with this:
                // Because this is not a critical operation, we can safely just ignore it.
                // I was unable to repro this to attempt a proper fix.
            }
        }
        return interpolator;
    }

    private static <T> Keyframe<T> parseStaticValue(JsonReader reader, float scale, ValueParser<T> valueParser)
        throws IOException {
        T value = valueParser.parse(reader, scale);
        return new Keyframe<>((T) value);
    }
}