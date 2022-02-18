package com.airbnb.lottie.animation.keyframe;


import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.animatable.AnimatableTransform;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.value.Keyframe;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.value.ScaleXY;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.Point;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

public class TransformKeyframeAnimation {
    private final Matrix matrix = new Matrix();
    private final Matrix skewMatrix1;
    private final Matrix skewMatrix2;
    private final Matrix skewMatrix3;
    private final float[] skewValues;

    @NotNull private BaseKeyframeAnimation<Point, Point> anchorPoint;
    @NotNull private BaseKeyframeAnimation<?, Point> position;
    @NotNull private BaseKeyframeAnimation<ScaleXY, ScaleXY> scale;
    @NotNull private BaseKeyframeAnimation<Float, Float> rotation;
    @NotNull private BaseKeyframeAnimation<Integer, Integer> opacity;
    @Nullable private FloatKeyframeAnimation skew;
    @Nullable private FloatKeyframeAnimation skewAngle;

    // Used for repeaters
    @Nullable private BaseKeyframeAnimation<?, Float> startOpacity;
    @Nullable private BaseKeyframeAnimation<?, Float> endOpacity;

    public TransformKeyframeAnimation(AnimatableTransform animatableTransform) {

        anchorPoint = animatableTransform.getAnchorPoint() == null ? null : animatableTransform.getAnchorPoint().createAnimation();

        position = animatableTransform.getPosition() == null ? null : animatableTransform.getPosition().createAnimation();
        scale = animatableTransform.getScale() == null ? null : animatableTransform.getScale().createAnimation();
        rotation = animatableTransform.getRotation() == null ? null : animatableTransform.getRotation().createAnimation();
        skew = animatableTransform.getSkew() == null ? null : (FloatKeyframeAnimation) animatableTransform.getSkew().createAnimation();
        if (skew != null) {
            skewMatrix1 = new Matrix();
            skewMatrix2 = new Matrix();
            skewMatrix3 = new Matrix();
            skewValues = new float[9];
        } else {
            skewMatrix1 = null;
            skewMatrix2 = null;
            skewMatrix3 = null;
            skewValues = null;
        }
        skewAngle = animatableTransform.getSkewAngle() == null ? null : (FloatKeyframeAnimation) animatableTransform.getSkewAngle().createAnimation();
        if (animatableTransform.getOpacity() != null) {
            opacity = animatableTransform.getOpacity().createAnimation();
        }
        if (animatableTransform.getStartOpacity() != null) {
            startOpacity = animatableTransform.getStartOpacity().createAnimation();
        } else {
            startOpacity = null;
        }
        if (animatableTransform.getEndOpacity() != null) {
            endOpacity = animatableTransform.getEndOpacity().createAnimation();
        } else {
            endOpacity = null;
        }
    }

    public void addAnimationsToLayer(BaseLayer layer) {
        layer.addAnimation(opacity);
        layer.addAnimation(startOpacity);
        layer.addAnimation(endOpacity);

        layer.addAnimation(anchorPoint);
        layer.addAnimation(position);
        layer.addAnimation(scale);
        layer.addAnimation(rotation);
        layer.addAnimation(skew);
        layer.addAnimation(skewAngle);
    }

    public void addListener(final BaseKeyframeAnimation.AnimationListener listener) {
        if (opacity != null) {
            opacity.addUpdateListener(listener);
        }
        if (startOpacity != null) {
            startOpacity.addUpdateListener(listener);
        }
        if (endOpacity != null) {
            endOpacity.addUpdateListener(listener);
        }

        if (anchorPoint != null) {
            anchorPoint.addUpdateListener(listener);
        }
        if (position != null) {
            position.addUpdateListener(listener);
        }
        if (scale != null) {
            scale.addUpdateListener(listener);
        }
        if (rotation != null) {
            rotation.addUpdateListener(listener);
        }
        if (skew != null) {
            skew.addUpdateListener(listener);
        }
        if (skewAngle != null) {
            skewAngle.addUpdateListener(listener);
        }
    }

    public void setProgress(float progress) {
        if (opacity != null) {
            opacity.setProgress(progress);
        }
        if (startOpacity != null) {
            startOpacity.setProgress(progress);
        }
        if (endOpacity != null) {
            endOpacity.setProgress(progress);
        }

        if (anchorPoint != null) {
            anchorPoint.setProgress(progress);
        }
        if (position != null) {
            position.setProgress(progress);
        }
        if (scale != null) {
            scale.setProgress(progress);
        }
        if (rotation != null) {
            rotation.setProgress(progress);
        }
        if (skew != null) {
            skew.setProgress(progress);
        }
        if (skewAngle != null) {
            skewAngle.setProgress(progress);
        }
    }

    @Nullable public BaseKeyframeAnimation<?, Integer> getOpacity() {
        return opacity;
    }

    @Nullable public BaseKeyframeAnimation<?, Float> getStartOpacity() {
        return startOpacity;
    }

    @Nullable
    public BaseKeyframeAnimation<?, Float> getEndOpacity() {
        return endOpacity;
    }

    private void positionCheck(){
        if (position != null) {
            Point position = this.position.getValue();
            if (position.getPointX() != 0 || position.getPointY() != 0) {
                matrix.preTranslate(position.getPointX(), position.getPointY());
            }
        }
    }

    private void rotationCheck(){
        if (rotation != null) {
            float rotation;
            if (this.rotation instanceof ValueCallbackKeyframeAnimation) {
                rotation = this.rotation.getValue();
            } else {
                rotation = ((FloatKeyframeAnimation) this.rotation).getFloatValue();
            }
            if (rotation != 0f) {
                matrix.preRotate(rotation);
            }
        }
    }

    public Matrix getMatrix() {
        matrix.reset();
        positionCheck();
        rotationCheck();

        if (skew != null) {
            float mCos = skewAngle == null ? 0f : (float) Math.cos(Math.toRadians(-skewAngle.getFloatValue() + 90));
            float mSin = skewAngle == null ? 1f : (float) Math.sin(Math.toRadians(-skewAngle.getFloatValue() + 90));
            float aTan = (float) Math.tan(Math.toRadians(skew.getFloatValue()));
            clearSkewValues();
            skewValues[0] = mCos;
            skewValues[1] = mSin;
            skewValues[3] = -mSin;
            skewValues[4] = mCos;
            skewValues[8] = 1f;
            skewMatrix1.setElements(skewValues);
            clearSkewValues();

            skewValues[0] = 1f;
            skewValues[3] = aTan;
            skewValues[4] = 1f;
            skewValues[8] = 1f;
            skewMatrix2.setElements(skewValues);
            clearSkewValues();

            skewValues[0] = mCos;
            skewValues[1] = -mSin;
            skewValues[3] = mSin;
            skewValues[4] = mCos;
            skewValues[8] = 1;
            skewMatrix3.setElements(skewValues);

            skewMatrix2.preConcat(skewMatrix1);
            skewMatrix3.preConcat(skewMatrix2);
            matrix.preConcat(skewMatrix3);
        }

        if (scale != null) {
            ScaleXY scaleTransform = this.scale.getValue();
            if (scaleTransform.getScaleX() != 1f || scaleTransform.getScaleY() != 1f) {
                matrix.preScale(scaleTransform.getScaleX(), scaleTransform.getScaleY());
            }
        }

        if (anchorPoint != null) {
            Point anchorPoint = this.anchorPoint.getValue();
            if (anchorPoint.getPointX() != 0 || anchorPoint.getPointY() != 0) {
                matrix.preTranslate(-anchorPoint.getPointX(), -anchorPoint.getPointY());
            }
        }
        return matrix;
    }

    private void clearSkewValues() {
        for (int i = 0; i < 9; i++) {
            skewValues[i] = 0f;
        }
    }

    /**
     * TODO: see if we can use this for the main {@link #getMatrix()} method.
     * @param amount in float
     * @return matrix
     */
    public Matrix getMatrixForRepeater(float amount) {
        Point position = this.position == null ? null : this.position.getValue();
        ScaleXY scale = this.scale == null ? null : this.scale.getValue();

        matrix.reset();
        if (position != null) {
            matrix.preTranslate(position.getPointX() * amount, position.getPointY() * amount);
        }
        if (scale != null) {
            matrix.preScale(
			(float) Math.pow(scale.getScaleX(), amount), 
			(float) Math.pow(scale.getScaleY(), amount));
        }
        if (this.rotation != null) {
            float rotation = this.rotation.getValue();
            Point anchorPoint = this.anchorPoint == null ? null : this.anchorPoint.getValue();
            matrix.preRotate(rotation * amount, anchorPoint == null ? 0f : anchorPoint.getPointX(), anchorPoint == null ? 0f : anchorPoint.getPointY());
        }

        return matrix;
    }

    /**
     * Returns whether the callback was applied.
     * @param property LottieProperty
     * @param callback of LottieValueCallback<T>
     * @return boolean
     */
    public <T> boolean applyValueCallback(T property, @Nullable LottieValueCallback<T> callback) {
        if (property == LottieProperty.TRANSFORM_ANCHOR_POINT) {
            anchorPoint.setValueCallback((LottieValueCallback<Point>) callback);
        } else if (property == LottieProperty.TRANSFORM_POSITION) {
            position.setValueCallback((LottieValueCallback<Point>) callback);
        } else if (property == LottieProperty.TRANSFORM_POSITION_X && position instanceof SplitDimensionPathKeyframeAnimation) {
            ((SplitDimensionPathKeyframeAnimation) position).setXValueCallback((LottieValueCallback<Float>) callback);
        } else if (property == LottieProperty.TRANSFORM_POSITION_Y && position instanceof SplitDimensionPathKeyframeAnimation) {
            ((SplitDimensionPathKeyframeAnimation) position).setYValueCallback((LottieValueCallback<Float>) callback);
        } else if (property == LottieProperty.TRANSFORM_SCALE) {
            scale.setValueCallback((LottieValueCallback<ScaleXY>) callback);
        } else if (property == LottieProperty.TRANSFORM_ROTATION) {
            rotation.setValueCallback((LottieValueCallback<Float>) callback);
        } else if (property == LottieProperty.TRANSFORM_OPACITY) {
            opacity.setValueCallback((LottieValueCallback<Integer>) callback);
        } else if (property == LottieProperty.TRANSFORM_START_OPACITY && startOpacity != null) {
            startOpacity.setValueCallback((LottieValueCallback<Float>) callback);
        } else if (property == LottieProperty.TRANSFORM_END_OPACITY && endOpacity != null) {
            endOpacity.setValueCallback((LottieValueCallback<Float>) callback);
        } else if (property == LottieProperty.TRANSFORM_SKEW && skew != null) {
            skew.setValueCallback((LottieValueCallback<Float>) callback);
        } else if (property == LottieProperty.TRANSFORM_SKEW_ANGLE && skewAngle != null) {
            skewAngle.setValueCallback((LottieValueCallback<Float>) callback);
        } else {
            return false;
        }
        return true;
    }
}