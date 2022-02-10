package com.airbnb.lottie.model.animatable;

import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.ModifierContent;
import com.airbnb.lottie.animation.keyframe.TransformKeyframeAnimation;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.model.content.ContentModel;
import com.airbnb.lottie.model.layer.BaseLayer;
import ohos.agp.utils.Point;
import org.jetbrains.annotations.Nullable;

public class AnimatableTransform implements ModifierContent, ContentModel {
  @Nullable
  private final AnimatablePathValue anchorPoint;
  @Nullable
  private final AnimatableValue<Point, Point> position;
  @Nullable
  private final AnimatableScaleValue scale;
  @Nullable
  private final AnimatableFloatValue rotation;
  @Nullable
  private final AnimatableIntegerValue opacity;
  @Nullable
  private final AnimatableFloatValue skew;
  @Nullable
  private final AnimatableFloatValue skewAngle;

  // Used for repeaters
  @Nullable
  private final AnimatableFloatValue startOpacity;
  @Nullable
  private final AnimatableFloatValue endOpacity;

    public AnimatableTransform() {
        this((AnimatablePathValue) null, null, null, null, null, null, null, null, null);

    }

  public AnimatableTransform(@Nullable AnimatablePathValue anchorPoint,
      @Nullable AnimatableValue<Point, Point> position, @Nullable AnimatableScaleValue scale,
      @Nullable AnimatableFloatValue rotation, @Nullable AnimatableIntegerValue opacity,
      @Nullable AnimatableFloatValue startOpacity, @Nullable AnimatableFloatValue endOpacity,
      @Nullable AnimatableFloatValue skew, @Nullable AnimatableFloatValue skewAngle) {
    this.anchorPoint = anchorPoint;
    this.position = position;
    this.scale = scale;
    this.rotation = rotation;
    this.opacity = opacity;
    this.startOpacity = startOpacity;
    this.endOpacity = endOpacity;
    this.skew = skew;
    this.skewAngle = skewAngle;
  }

  @Nullable
  public AnimatablePathValue getAnchorPoint() {
    return anchorPoint;
  }

  @Nullable
  public AnimatableValue<Point, Point> getPosition() {
    return position;
  }

  @Nullable
  public AnimatableScaleValue getScale() {
    return scale;
  }

  @Nullable
  public AnimatableFloatValue getRotation() {
    return rotation;
  }

  @Nullable
  public AnimatableIntegerValue getOpacity() {
    return opacity;
  }

  @Nullable
  public AnimatableFloatValue getStartOpacity() {
    return startOpacity;
  }

  @Nullable
  public AnimatableFloatValue getEndOpacity() {
    return endOpacity;
  }

  @Nullable
  public AnimatableFloatValue getSkew() {
    return skew;
  }

  @Nullable
  public AnimatableFloatValue getSkewAngle() {
    return skewAngle;
  }

  public TransformKeyframeAnimation createAnimation() {
    return new TransformKeyframeAnimation(this);
  }

  @Nullable
  @Override
  public Content toContent(LottieDrawable drawable, BaseLayer layer) {
    return null;
  }
}
