package com.airbnb.lottie.model.layer;

import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.ContentGroup;
import com.airbnb.lottie.model.content.ShapeGroup;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.model.KeyPath;
import ohos.agp.render.Canvas;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.RectFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ShapeLayer extends BaseLayer {
    private final ContentGroup contentGroup;

    ShapeLayer(LottieDrawable lottieDrawable, Layer layerModel) {
        super(lottieDrawable, layerModel);

        // Naming this __container allows it to be ignored in KeyPath matching.
        ShapeGroup shapeGroup = new ShapeGroup("__container", layerModel.getShapes(), false);
        contentGroup = new ContentGroup(lottieDrawable, this, shapeGroup);
        contentGroup.setContents(Collections.<Content>emptyList(), Collections.<Content>emptyList());
    }

    @Override
    void drawLayer(@NotNull Canvas canvas, Matrix parentMatrix, float parentAlpha) {
        contentGroup.draw(canvas, parentMatrix, parentAlpha);

    }

    @Override
    public void getBounds(RectFloat outBounds, Matrix parentMatrix, boolean applyParents) {
        super.getBounds(outBounds, parentMatrix, applyParents);
        contentGroup.getBounds(outBounds, boundsMatrix, applyParents);
    }

    @Override
    protected void resolveChildKeyPath(KeyPath keyPath, int depth, List<KeyPath> accumulator,
                                       KeyPath currentPartialKeyPath) {
        contentGroup.resolveKeyPath(keyPath, depth, accumulator, currentPartialKeyPath);
    }
}