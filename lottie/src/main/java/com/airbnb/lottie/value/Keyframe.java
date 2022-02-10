package com.airbnb.lottie.value;

import com.airbnb.lottie.LottieComposition;

import ohos.agp.animation.Animator;
import ohos.agp.utils.Point;
import org.jetbrains.annotations.Nullable;

public class Keyframe<T> {
    private static final float UNSET_FLOAT = -3987645.78543923f;

    private static final int UNSET_INT = 784923401;

    private final LottieComposition composition;

    public T startValue;

    public T endValue;

    public Animator.CurveType interpolator;

    @Nullable public  Animator.CurveType xInterpolator;
    @Nullable
    public  Animator.CurveType yInterpolator;


    public final float startFrame;

    public Float endFrame;

    private float startValueFloat = UNSET_FLOAT;

    private float endValueFloat = UNSET_FLOAT;

    private int startValueInt = UNSET_INT;

    private int endValueInt = UNSET_INT;

    private float startProgress = Float.MIN_VALUE;

    private float endProgress = Float.MIN_VALUE;

    // Used by openharmony PathKeyframe but it has to be parsed by KeyFrame because we use a JsonReader to
    // deserialzie the data so we have to parse everything in order
    public Point pathCp1 = null;

    public Point pathCp2 = null;

    public Keyframe(LottieComposition composition, T startValue, T endValue, Animator.CurveType interpolator,
        float startFrame, Float endFrame) {
        //HMOSLogUtil.error("Lottie Keyframe","Keyframe hitting constructor 1-->"+composition.getLayers().toString());
        this.composition = composition;
        this.startValue = startValue;
        this.endValue = endValue;
        this.interpolator = interpolator;
        xInterpolator = null;
        yInterpolator = null;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    public Keyframe(@SuppressWarnings("NullableProblems") LottieComposition composition,
                    @Nullable T startValue, @Nullable T endValue,
                    @Nullable Animator.CurveType xInterpolator, @Nullable Animator.CurveType yInterpolator, float startFrame, @Nullable Float endFrame) {
        this.composition = composition;
        this.startValue = startValue;
        this.endValue = endValue;
        interpolator = null;
        this.xInterpolator = xInterpolator;
        this.yInterpolator = yInterpolator;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    protected Keyframe(@SuppressWarnings("NullableProblems") LottieComposition composition,
                       @Nullable T startValue, @Nullable T endValue,
                       @Nullable Animator.CurveType interpolator, @Nullable Animator.CurveType xInterpolator, @Nullable Animator.CurveType yInterpolator,
                       float startFrame, @Nullable Float endFrame) {
        this.composition = composition;
        this.startValue = startValue;
        this.endValue = endValue;
        this.interpolator = interpolator;
        this.xInterpolator = xInterpolator;
        this.yInterpolator = yInterpolator;

        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    /**
     * Non-animated value.
     */
    public Keyframe(T value) {
        composition = null;
        startValue = value;
        endValue = value;
        if (interpolator != null) {
            interpolator = null;
        }
        xInterpolator = null;
        yInterpolator = null;
        startFrame = Float.MIN_VALUE;
        endFrame = Float.MAX_VALUE;
    }

    public float getStartProgress() {
        if (composition == null) {
            return 0f;
        }
        if (startProgress == Float.MIN_VALUE) {
            startProgress = (startFrame - composition.getStartFrame()) / composition.getDurationFrames();
        }
        return startProgress;
    }

    public float getEndProgress() {
        if (composition == null) {
            return 1f;
        }
        if (endProgress == Float.MIN_VALUE) {
            if (endFrame == null) {
                endProgress = 1f;
            } else {
                float startProgress = getStartProgress();
                float durationFrames = endFrame - startFrame;
                float durationProgress = durationFrames / composition.getDurationFrames();
                endProgress = startProgress + durationProgress;
            }
        }
        return endProgress;
    }

    public boolean isStatic() {
        return interpolator == null && xInterpolator == null && yInterpolator == null;
    }

    //FloatRange(from = 0f, to = 1f)
    public boolean containsProgress(float progress) {
        return progress >= getStartProgress() && progress < getEndProgress();
    }

    /**
     * Optimization to avoid autoboxing.
     * @return the start value in float.
     */
    public float getStartValueFloat() {
        if (startValueFloat == UNSET_FLOAT) {
            startValueFloat = (float) (Float) startValue;
        }
        return startValueFloat;
    }

    /**
     * Optimization to avoid autoboxing.
     * @return the end value in float.
     */
    public float getEndValueFloat() {
        if (endValueFloat == UNSET_FLOAT) {
            endValueFloat = (float) (Float) endValue;
        }
        return endValueFloat;
    }

    /**
     * Optimization to avoid autoboxing.
     * @return the start value in integer.
     */
    public int getStartValueInt() {
        if (startValueInt == UNSET_INT) {
            startValueInt = (int) (Integer) startValue;
        }
        return startValueInt;
    }

    /**
     * Optimization to avoid autoboxing.
     * @return the end value in integer.
     */
    public int getEndValueInt() {
        if (endValueInt == UNSET_INT) {
            endValueInt = (int) (Integer) endValue;
        }
        return endValueInt;
    }

    @Override
    public String toString() {
        /*HMOSLogUtil.error("Lottie Keyframe","Keyframe toString-->"+ "Keyframe{" + "startValue=" + startValue + ", endValue=" + endValue + ", startFrame=" + startFrame
                + ", endFrame=" + endFrame + ", interpolator=" + interpolator + '}');*/
        return "Keyframe{" + "startValue=" + startValue + ", endValue=" + endValue + ", startFrame=" + startFrame
            + ", endFrame=" + endFrame + ", interpolator=" + interpolator + '}';
    }
}
