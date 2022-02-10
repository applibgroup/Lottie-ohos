package com.airbnb.lottie.animation.content;

import static com.airbnb.lottie.utils.MiscUtils.clamp;

import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.ValueCallbackKeyframeAnimation;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.animation.LPaint;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.model.content.GradientFill;
import com.airbnb.lottie.model.content.GradientType;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.LottieValueCallback;

import ohos.agp.render.*;
import ohos.agp.utils.Color;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.Point;
import ohos.agp.utils.RectFloat;
import ohos.hiviewdfx.HiTraceId;
import ohos.utils.LongPlainArray;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GradientFillContent
        implements DrawingContent, BaseKeyframeAnimation.AnimationListener, KeyPathElementContent {
    /**
     * Cache the gradients such that it runs at 30fps.
     */
    private static final int CACHE_STEPS_MS = 32;
    @NotNull private final String name;
    private final boolean hidden;
    private final BaseLayer layer;
    private final LongPlainArray<LinearShader> linearShaderCache = new LongPlainArray<LinearShader>();
    private final LongPlainArray<RadialShader> radialShaderCache = new LongPlainArray<RadialShader>();
    private final Path path = new Path();
    private final Paint paint = new LPaint(true);
    private final RectFloat boundsRect = new RectFloat();
    private final List<PathContent> paths = new ArrayList<>();
    private final GradientType type;
    private final BaseKeyframeAnimation<GradientColor, GradientColor> colorAnimation;
    private final BaseKeyframeAnimation<Integer, Integer> opacityAnimation;
    private final BaseKeyframeAnimation<Point, Point> startPointAnimation;
    private final BaseKeyframeAnimation<Point, Point> endPointAnimation;
    @Nullable
    private BaseKeyframeAnimation<ColorFilter, ColorFilter> colorFilterAnimation;
    @Nullable private ValueCallbackKeyframeAnimation colorCallbackAnimation;
    private final LottieDrawable lottieDrawable;
    private final int cacheSteps;

    public GradientFillContent(final LottieDrawable lottieDrawable, BaseLayer layer, GradientFill fill) {
        this.layer = layer;
        name = fill.getName();
        hidden = fill.isHidden();
        this.lottieDrawable = lottieDrawable;
        type = fill.getGradientType();
        path.setFillType(fill.getFillType());
        cacheSteps = (int) (lottieDrawable.getComposition().getDuration() / CACHE_STEPS_MS);

        colorAnimation = fill.getGradientColor().createAnimation();
        colorAnimation.addUpdateListener(this);
        layer.addAnimation(colorAnimation);

        opacityAnimation = fill.getOpacity().createAnimation();
        opacityAnimation.addUpdateListener(this);
        layer.addAnimation(opacityAnimation);

        startPointAnimation = fill.getStartPoint().createAnimation();
        startPointAnimation.addUpdateListener(this);
        layer.addAnimation(startPointAnimation);

        endPointAnimation = fill.getEndPoint().createAnimation();
        endPointAnimation.addUpdateListener(this);
        layer.addAnimation(endPointAnimation);
    }

    @Override
    public void onValueChanged() {
        lottieDrawable.invalidateSelf();
    }

    @Override
    public void setContents(List<Content> contentsBefore, List<Content> contentsAfter) {
        for (int i = 0; i < contentsAfter.size(); i++) {
            Content content = contentsAfter.get(i);
            if (content instanceof PathContent) {
                paths.add((PathContent) content);
            }
        }
    }

    @Override
    public void draw(Canvas canvas, Matrix parentMatrix, float parentAlpha) {
        if (hidden) {
            return;
        }
        HiTraceId id = L.beginSection("GradientFillContent#draw");
        path.reset();
        for (int i = 0; i < paths.size(); i++) {
            path.addPath(paths.get(i).getPath(),parentMatrix,Path.AddPathMode.EXTEND_ADD_PATH_MODE);
        }

        path.computeBounds(boundsRect);

        Shader shader;
        if (type == GradientType.LINEAR) {
            shader = getLinearShader();
            shader.setShaderMatrix(parentMatrix);
            paint.setShader(shader, Paint.ShaderType.LINEAR_SHADER);
        } else {
            shader = getRadialShader();
            shader.setShaderMatrix(parentMatrix);
            paint.setShader(shader, Paint.ShaderType.RADIAL_SHADER);
        }


        if (colorFilterAnimation != null) {
            paint.setColorFilter(colorFilterAnimation.getValue());
        }

        int alpha = (int) ((parentAlpha / 255.0f * opacityAnimation.getValue() / 100f) * 255);
        paint.setAlpha(MiscUtils.clamp(alpha, 0, 255));

        canvas.drawPath(path, paint);
        L.endSection(id);
    }

    @Override
    public void getBounds(RectFloat outBounds, Matrix parentMatrix, boolean applyParents) {
        path.reset();
        for (int i = 0; i < paths.size(); i++) {
            path.addPath(paths.get(i).getPath());
        }

        path.computeBounds(outBounds);
        // Add padding to account for rounding errors.
        outBounds.modify(outBounds.left - 1, outBounds.top - 1, outBounds.right + 1, outBounds.bottom + 1);
    }

    @Override
    public String getName() {
        return name;
    }

    private LinearShader getLinearShader() {
        int gradientHash = getGradientHash();

        Optional<LinearShader> optVal = linearShaderCache.get(gradientHash);
        if (optVal.isPresent()) {
            return optVal.get();
        }
        Point startPoint = startPointAnimation.getValue();
        Point endPoint = endPointAnimation.getValue();
        GradientColor gradientColor = colorAnimation.getValue();
        Color[] colors = applyDynamicColorsIfNeeded(gradientColor.getColors());
        float[] positions = gradientColor.getPositions();

        Point[] pointArr = {new Point(startPoint.getPointX(), startPoint.getPointY()),
                            new Point(endPoint.getPointX(), endPoint.getPointY())};

        LinearShader gradient = new LinearShader(pointArr, positions, colors, Shader.TileMode.CLAMP_TILEMODE);
        linearShaderCache.put(gradientHash, gradient);
        return gradient;
    }

    private RadialShader getRadialShader() {
        int gradientHash = getGradientHash();

        Optional<RadialShader> optVal = radialShaderCache.get(gradientHash);
        if (optVal.isPresent()) {
            return optVal.get();
        }
        Point startPoint = startPointAnimation.getValue();
        Point endPoint = endPointAnimation.getValue();
        GradientColor gradientColor = colorAnimation.getValue();
        Color[] colors = applyDynamicColorsIfNeeded(gradientColor.getColors());
        float[] positions = gradientColor.getPositions();
        float x0 = startPoint.getPointX();
        float y0 = startPoint.getPointY();
        float x1 = endPoint.getPointX();
        float y1 = endPoint.getPointY();
        float r = (float) Math.hypot(x1 - x0, y1 - y0);
        if (r <= 0) {
            r = 0.001f;
        }
        RadialShader gradient = new RadialShader(new Point(x0, y0), r, positions, colors, Shader.TileMode.CLAMP_TILEMODE);
        radialShaderCache.put(gradientHash, gradient);
        return gradient;
    }

    private int getGradientHash() {
        int startPointProgress = Math.round(startPointAnimation.getProgress() * cacheSteps);
        int endPointProgress = Math.round(endPointAnimation.getProgress() * cacheSteps);
        int colorProgress = Math.round(colorAnimation.getProgress() * cacheSteps);
        int hash = 17;
        if (startPointProgress != 0) {
            hash = hash * 31 * startPointProgress;
        }
        if (endPointProgress != 0) {
            hash = hash * 31 * endPointProgress;
        }
        if (colorProgress != 0) {
            hash = hash * 31 * colorProgress;
        }
        return hash;
    }

    private Color[] applyDynamicColorsIfNeeded(int[] colors) {
        Color[] colorArr = new Color[colors.length];
        if (colorCallbackAnimation != null) {
            Integer[] dynamicColors = (Integer[]) colorCallbackAnimation.getValue();
            if (colors.length == dynamicColors.length) {
                for (int i = 0; i < colors.length; i++) {
                    colorArr[i] = new Color(dynamicColors[i]);
                }
            } else {
                colors = new int[dynamicColors.length];
                for (int i = 0; i < dynamicColors.length; i++) {
                    colorArr[i] = new Color(dynamicColors[i]);
                }
            }
        }
        return colorArr;
    }

    @Override
    public void resolveKeyPath(KeyPath keyPath, int depth, List<KeyPath> accumulator, KeyPath currentPartialKeyPath) {
        MiscUtils.resolveKeyPath(keyPath, depth, accumulator, currentPartialKeyPath, this);
    }

    @Override
    public <T> void addValueCallback(T property, LottieValueCallback<T> callback) {
        if (property == LottieProperty.OPACITY) {
            opacityAnimation.setValueCallback((LottieValueCallback<Integer>) callback);
        } else if (property == LottieProperty.COLOR_FILTER) {
            if (colorFilterAnimation != null) {
                layer.removeAnimation(colorFilterAnimation);
            }

            if (callback == null) {
                colorFilterAnimation = null;
            } else {
                colorFilterAnimation =
                        new ValueCallbackKeyframeAnimation<>((LottieValueCallback<ColorFilter>) callback);
                colorFilterAnimation.addUpdateListener(this);
                layer.addAnimation(colorFilterAnimation);
            }
        } else if (property == LottieProperty.GRADIENT_COLOR) {
            if (colorCallbackAnimation != null) {
                layer.removeAnimation(colorCallbackAnimation);
            }

            if (callback == null) {
                colorCallbackAnimation = null;
            } else {
                linearShaderCache.clear();
                radialShaderCache.clear();
                colorCallbackAnimation = new ValueCallbackKeyframeAnimation<>(callback);
                colorCallbackAnimation.addUpdateListener(this);
                layer.addAnimation(colorCallbackAnimation);
            }
        }
    }
}
