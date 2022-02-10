package com.airbnb.lottie.animation.keyframe;

import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.Keyframe;

import java.util.List;

public class IntegerKeyframeAnimation extends KeyframeAnimation<Integer> {

    public IntegerKeyframeAnimation(List<Keyframe<Integer>> keyframes) {
        super(keyframes);
    }

    @Override
    Integer getValue(Keyframe<Integer> keyframe, float keyframeProgress) {
        return getIntValue(keyframe, keyframeProgress);
    }

    /**
     * Optimization to avoid autoboxing.
     * @param keyframeProgress keyframe progress
     * @param keyframe keyframes
     * @return MiscUtils.lerp(keyframe.getStartValueInt(), keyframe.getEndValueInt(), keyframeProgress)
     */
    int getIntValue(Keyframe<Integer> keyframe, float keyframeProgress) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }

        if (valueCallback != null) {
            Integer value = valueCallback.getValueInternal(keyframe.startFrame, keyframe.endFrame, 
					keyframe.startValue, keyframe.endValue, 
					keyframeProgress, getLinearCurrentKeyframeProgress(), getProgress());
            if (value != null) {
                return value;
            }
        }

        return MiscUtils.lerp(keyframe.getStartValueInt(), keyframe.getEndValueInt(), keyframeProgress);
    }

    /**
     * Optimization to avoid autoboxing.
     * @return getIntValue(getCurrentKeyframe(), getInterpolatedCurrentKeyframeProgress())
     */
    public int getIntValue() {
        return getIntValue(getCurrentKeyframe(), getInterpolatedCurrentKeyframeProgress());
    }
}
