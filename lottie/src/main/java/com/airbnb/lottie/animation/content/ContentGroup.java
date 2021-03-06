package com.airbnb.lottie.animation.content;

import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;
import ohos.agp.render.Path;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.RectFloat;

import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.TransformKeyframeAnimation;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.animation.LPaint;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.animatable.AnimatableTransform;
import com.airbnb.lottie.model.content.ContentModel;
import com.airbnb.lottie.model.content.ShapeGroup;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.model.KeyPathElement;
import com.airbnb.lottie.model.layer.BaseLayer;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

public class ContentGroup implements DrawingContent, PathContent,
        BaseKeyframeAnimation.AnimationListener, KeyPathElement {

    private Paint offScreenPaint = new LPaint();
    private RectFloat offScreenRectF = new RectFloat();

    private static List<Content> contentsFromModels(LottieDrawable drawable, BaseLayer layer,
                                                    List<ContentModel> contentModels) {
        List<Content> contents = new ArrayList<>(contentModels.size());
        for (int i = 0; i < contentModels.size(); i++) {
            Content content = contentModels.get(i).toContent(drawable, layer);
            if (content != null) {
                contents.add(content);
            }
        }
        return contents;
    }

    @Nullable static AnimatableTransform findTransform(List<ContentModel> contentModels) {
        for (int i = 0; i < contentModels.size(); i++) {
            ContentModel contentModel = contentModels.get(i);
            if (contentModel instanceof AnimatableTransform) {
                return (AnimatableTransform) contentModel;
            }
        }
        return null;
    }

    private final Matrix matrix = new Matrix();

    private final Path path = new Path();

    private final RectFloat rectFloat = new RectFloat();

    private final String name;

    private final boolean hidden;

    private final List<Content> contents;

    private final LottieDrawable lottieDrawable;

    @Nullable private List<PathContent> pathContents;

    @Nullable private TransformKeyframeAnimation transformAnimation;

    public ContentGroup(final LottieDrawable lottieDrawable, BaseLayer layer, ShapeGroup shapeGroup) {
        this(lottieDrawable, layer, shapeGroup.getName(),
                shapeGroup.isHidden(), contentsFromModels(lottieDrawable, layer, shapeGroup.getItems()),
                findTransform(shapeGroup.getItems()));
    }

    ContentGroup( final LottieDrawable lottieDrawable,BaseLayer layer,
            String name, boolean hidden, List<Content> contents, AnimatableTransform transform) {
        this.name = name;
        this.lottieDrawable = lottieDrawable;
        this.hidden = hidden;
        this.contents = contents;

        if (transform != null) {
            transformAnimation = transform.createAnimation();
            transformAnimation.addAnimationsToLayer(layer);
            transformAnimation.addListener(this);
        }

        List<GreedyContent> greedyContents = new ArrayList<>();
        for (int i = contents.size() - 1; i >= 0; i--) {
            Content content = contents.get(i);
            if (content instanceof GreedyContent) {
                greedyContents.add((GreedyContent) content);
            }
        }

        for (int i = greedyContents.size() - 1; i >= 0; i--) {
            greedyContents.get(i).absorbContent(contents.listIterator(contents.size()));
        }
    }

    @Override
    public void onValueChanged() {
        lottieDrawable.invalidateSelf();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setContents(List<Content> contentsBefore, List<Content> contentsAfter) {
        // Do nothing with contents after.
        List<Content> myContentsBefore = new ArrayList<>(contentsBefore.size() + contents.size());
        myContentsBefore.addAll(contentsBefore);

        for (int i = contents.size() - 1; i >= 0; i--) {
            Content content = contents.get(i);
            content.setContents(myContentsBefore, contents.subList(0, i));
            myContentsBefore.add(content);
        }
    }

    List<PathContent> getPathList() {
        if (pathContents == null) {
            pathContents = new ArrayList<>();
            for (int i = 0; i < contents.size(); i++) {
                Content content = contents.get(i);
                if (content instanceof PathContent) {
                    pathContents.add((PathContent) content);
                }
            }
        }
        return pathContents;
    }

    Matrix getTransformationMatrix() {
        if (transformAnimation != null) {
            return transformAnimation.getMatrix();
        }
        matrix.reset();
        return matrix;
    }

    @Override
    public Path getPath() {
        // TODO: cache this somehow.
        matrix.reset();
        if (transformAnimation != null) {
            matrix.setMatrix(transformAnimation.getMatrix());
        }
        path.reset();
        if (hidden) {
            return path;
        }
        for (int i = contents.size() - 1; i >= 0; i--) {
            Content content = contents.get(i);
            if (content instanceof PathContent) {
                path.addPath(((PathContent) content).getPath(),matrix,Path.AddPathMode.EXTEND_ADD_PATH_MODE);
            }
        }
        return path;
    }

    @Override
    public void draw(Canvas canvas, Matrix parentMatrix, float parentAlpha) {
        if (hidden) {
            return;
        }
        matrix.setMatrix(parentMatrix);
        float layerAlpha;
        if (transformAnimation != null) {
            matrix.preConcat(transformAnimation.getMatrix());
            int opacity = transformAnimation.getOpacity() == null ? 100 : transformAnimation.getOpacity().getValue();
            layerAlpha = (int) ((opacity / 100f * parentAlpha / 255.0f) * 255);
        } else {
            layerAlpha = parentAlpha;
        }

        // Apply off-screen rendering only when needed in order to improve rendering performance.
        boolean isRenderingWithOffScreen =
                lottieDrawable.isApplyingOpacityToLayersEnabled() && hasTwoOrMoreDrawableContent() && layerAlpha != 255;
        if (isRenderingWithOffScreen) {
            offScreenRectF.modify(0, 0, 0, 0);
            getBounds(offScreenRectF, matrix, true);
            offScreenPaint.setAlpha(layerAlpha/255.0f);
            Utils.saveLayerCompat(canvas, offScreenRectF, offScreenPaint);
        }

        float childAlpha = isRenderingWithOffScreen ? 255 : layerAlpha;
        for (int i = contents.size() - 1; i >= 0; i--) {
            Object content = contents.get(i);
            if (content instanceof DrawingContent) {
                ((DrawingContent) content).draw(canvas, matrix, childAlpha);
            }
        }

        if (isRenderingWithOffScreen) {
            canvas.restore();
        }
    }

    private boolean hasTwoOrMoreDrawableContent() {
        int drawableContentCount = 0;
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i) instanceof DrawingContent) {
                drawableContentCount += 1;
                if (drawableContentCount >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void getBounds(RectFloat outBounds, Matrix parentMatrix, boolean applyParents) {
        matrix.setMatrix(parentMatrix);
        if (transformAnimation != null) {
            matrix.preConcat(transformAnimation.getMatrix());
        }
        rectFloat.modify(0, 0, 0, 0);
        for (int i = contents.size() - 1; i >= 0; i--) {
            Content content = contents.get(i);
            if (content instanceof DrawingContent) {
                ((DrawingContent) content).getBounds(rectFloat, matrix, applyParents);
                outBounds.fuse(rectFloat);
            }
        }
    }

    @Override
    public void resolveKeyPath(KeyPath keyPath, int depth, List<KeyPath> accumulator, KeyPath currentPartialKeyPath) {
        if (!keyPath.matches(getName(), depth) && !"__container".equals(getName())) {
            return;
        }

        if (!"__container".equals(getName())) {
            currentPartialKeyPath = currentPartialKeyPath.addKey(getName());

            if (keyPath.fullyResolvesTo(getName(), depth)) {
                accumulator.add(currentPartialKeyPath.resolve(this));
            }
        }

        if (keyPath.propagateToChildren(getName(), depth)) {
            int newDepth = depth + keyPath.incrementDepthBy(getName(), depth);
            for (int i = 0; i < contents.size(); i++) {
                Content content = contents.get(i);
                if (content instanceof KeyPathElement) {
                    KeyPathElement element = (KeyPathElement) content;
                    element.resolveKeyPath(keyPath, newDepth, accumulator, currentPartialKeyPath);
                }
            }
        }
    }

    @Override
    public <T> void addValueCallback(T property, @Nullable LottieValueCallback<T> callback) {
        if (transformAnimation != null) {
            transformAnimation.applyValueCallback(property, callback);
        }
    }
}
