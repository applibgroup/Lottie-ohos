package com.airbnb.lottie.value;

import com.airbnb.lottie.utils.MiscUtils;

import ohos.agp.animation.Animator;

public class LottieInterpolatedFloatValue extends LottieInterpolatedValue<Float> {
    public LottieInterpolatedFloatValue(Float startValue, Float endValue) {
        super(startValue, endValue);
    }

    public LottieInterpolatedFloatValue(Float startValue, Float endValue, Animator.CurveType interpolator) {
        super(startValue, endValue, interpolator);
    }

    @Override
    Float interpolateValue(Float startValue, Float endValue, float progress) {
        return MiscUtils.lerp(startValue, endValue, progress);
    }
}
