package com.airbnb.lottie.value;

import ohos.agp.animation.Animator;

abstract class LottieInterpolatedValue<T> extends LottieValueCallback<T> {
    private T startValue = null;

    private T endValue = null;

    private Animator.CurveType interpolator = null;

    LottieInterpolatedValue(T startValue, T endValue) {
        this(startValue, endValue, new Animator.CurveType());
    }

    LottieInterpolatedValue(T startValue, T endValue, Animator.CurveType interpolator) {
        this.startValue = startValue;
        this.endValue = endValue;
        this.interpolator = interpolator;
    }

    @Override
    public T getValue(LottieFrameInfo<T> frameInfo) {
        // float progress = interpolator.getInterpolation(frameInfo.getOverallProgress());
        float progress = frameInfo.getOverallProgress();//Lineat interpolator, same value returned - nothing calculated
        return interpolateValue(startValue, endValue, progress);
    }

    abstract T interpolateValue(T startValue, T endValue, float progress);
}
