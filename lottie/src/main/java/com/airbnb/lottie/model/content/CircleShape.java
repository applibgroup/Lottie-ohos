package com.airbnb.lottie.model.content;

import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.EllipseContent;
import com.airbnb.lottie.model.animatable.AnimatablePointValue;
import com.airbnb.lottie.model.animatable.AnimatableValue;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.model.layer.BaseLayer;
import ohos.agp.utils.Point;

public class CircleShape implements ContentModel {
    private final String name;

    private final AnimatableValue<Point, Point> position;
    private final AnimatablePointValue size;
    private final boolean isReversed;
    private final boolean hidden;

    public CircleShape(String name, AnimatableValue<Point, Point> position, 
						AnimatablePointValue size, boolean isReversed, boolean hidden) {
        this.name = name;
        this.position = position;
        this.size = size;
        this.isReversed = isReversed;
        this.hidden = hidden;
    }


    @Override public Content toContent(LottieDrawable drawable, BaseLayer layer) {
        return new EllipseContent(drawable, layer, this);
    }

    public String getName() {
        return name;
    }

    public AnimatableValue<Point, Point> getPosition() {
        return position;
    }

    public AnimatablePointValue getSize() {
        return size;
    }

    public boolean isReversed() {
        return isReversed;
    }

    public boolean isHidden() {
        return hidden;
    }
}
