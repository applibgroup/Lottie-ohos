package com.airbnb.lottie.animation.keyframe;

import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.ScaleXY;
import com.airbnb.lottie.value.Keyframe;

import java.util.List;

public class ScaleKeyframeAnimation extends KeyframeAnimation<ScaleXY> {

    private final ScaleXY scaleXY = new ScaleXY();

    public ScaleKeyframeAnimation(List<Keyframe<ScaleXY>> keyframes) {
        super(keyframes);
    }

    @Override
    public ScaleXY getValue(Keyframe<ScaleXY> keyframe, float keyframeProgress) {
        if (keyframe.startValue == null || keyframe.endValue == null) {
            throw new IllegalStateException("Missing values for keyframe.");
        }
        ScaleXY startTransform = keyframe.startValue;
        ScaleXY endTransform = keyframe.endValue;

        if (valueCallback != null) {
            ScaleXY value = valueCallback.getValueInternal(keyframe.startFrame, keyframe.endFrame, 
					startTransform, endTransform, 
					keyframeProgress, getLinearCurrentKeyframeProgress(), getProgress());
            if (value != null) {
                return value;
            }
        }

        scaleXY.set(
			MiscUtils.lerp(startTransform.getScaleX(), endTransform.getScaleX(), keyframeProgress),
            MiscUtils.lerp(startTransform.getScaleY(), endTransform.getScaleY(), keyframeProgress)
		);
        return scaleXY;
    }
}
