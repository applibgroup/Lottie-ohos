package com.airbnb.lottie.value;

import com.airbnb.lottie.utils.MiscUtils;

import ohos.agp.animation.Animator;

public class LottieInterpolatedIntegerValue extends LottieInterpolatedValue<Integer> {
    public LottieInterpolatedIntegerValue(Integer startValue, Integer endValue) {
        super(startValue, endValue);
    }

    public LottieInterpolatedIntegerValue(Integer startValue, Integer endValue, Animator.CurveType interpolator) {
        super(startValue, endValue, interpolator);
    }

    @Override
    Integer interpolateValue(Integer startValue, Integer endValue, float progress) {
        return MiscUtils.lerp(startValue, endValue, progress);
    }
}
