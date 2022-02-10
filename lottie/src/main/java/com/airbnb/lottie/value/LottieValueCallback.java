package com.airbnb.lottie.value;

import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.model.KeyPath;
import org.jetbrains.annotations.Nullable;

/**
 * Allows you to set a callback on a resolved {@link KeyPath} to modify
 * its animation values at runtime.
 */
public class LottieValueCallback<T> {
    private final LottieFrameInfo<T> frameInfo = new LottieFrameInfo<>();

    @Nullable private BaseKeyframeAnimation<?, ?> animation;

    /**
     * This can be set with {@link #setValue(Object)} to use a value instead of deferring
     * to the callback.
     **/
    @Nullable protected T value = null;

    public LottieValueCallback() {
    }

    public LottieValueCallback(@Nullable T staticValue) {
        value = staticValue;
    }

    /**
     * Override this if you haven't set a static value in the constructor or with setValue.
     * @param frameInfo of type LottieFrameInfo.
     * @return null to resort to the default value.
     */
    @Nullable
    public T getValue(LottieFrameInfo<T> frameInfo) {
        return value;
    }

    public final void setValue(@Nullable T value) {
        this.value = value;
        if (animation != null) {
            animation.notifyListeners();
        }
    }

    @Nullable
    public final T getValueInternal(float startFrame, float endFrame, T startValue, T endValue,
        float linearKeyframeProgress, float interpolatedKeyframeProgress, float overallProgress) {
        return getValue(frameInfo.set(startFrame, endFrame, startValue, endValue, linearKeyframeProgress,
            interpolatedKeyframeProgress, overallProgress));
    }

    public final void setAnimation(@Nullable BaseKeyframeAnimation<?, ?> animation) {
        this.animation = animation;
    }
}
