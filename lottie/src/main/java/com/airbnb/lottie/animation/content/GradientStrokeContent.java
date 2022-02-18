package com.airbnb.lottie.animation.content;

import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.ValueCallbackKeyframeAnimation;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.model.content.GradientColor;
import com.airbnb.lottie.model.content.GradientStroke;
import com.airbnb.lottie.model.content.GradientType;
import com.airbnb.lottie.model.layer.BaseLayer;
import com.airbnb.lottie.value.LottieValueCallback;

import ohos.agp.render.*;
import ohos.agp.utils.Color;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.Point;
import ohos.agp.utils.RectFloat;
import ohos.utils.LongPlainArray;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class GradientStrokeContent extends BaseStrokeContent {
    /**
     * Cache the gradients such that it runs at 30fps.
     */
    private static final int CACHE_STEPS_MS = 32;
    private final String name;
    private final boolean hidden;
    private final LongPlainArray<LinearShader> linearShaderCache = new LongPlainArray<>();
    private final LongPlainArray<RadialShader> radialShaderCache = new LongPlainArray<>();
    private final RectFloat boundsRect = new RectFloat();
    private final GradientType type;
    private final int cacheSteps;
    private final BaseKeyframeAnimation<GradientColor, GradientColor> colorAnimation;
    private final BaseKeyframeAnimation<Point, Point> startPointAnimation;
    private final BaseKeyframeAnimation<Point, Point> endPointAnimation;
    @Nullable
    private ValueCallbackKeyframeAnimation colorCallbackAnimation;

    public GradientStrokeContent(final LottieDrawable lottieDrawable, BaseLayer layer, GradientStroke stroke) {
        super(lottieDrawable,layer,stroke.getCapType().toPaintCap(),
                stroke.getJoinType().toPaintJoin(),stroke.getMiterLimit(),stroke.getOpacity(),
                stroke.getWidth(),stroke.getLineDashPattern(),stroke.getDashOffset());
				
        name = stroke.getName();
        type = stroke.getGradientType();
        hidden = stroke.isHidden();
        cacheSteps = (int) (lottieDrawable.getComposition().getDuration() / CACHE_STEPS_MS);

        colorAnimation = stroke.getGradientColor().createAnimation();
        colorAnimation.addUpdateListener(this);
        layer.addAnimation(colorAnimation);

        startPointAnimation = stroke.getStartPoint().createAnimation();
        startPointAnimation.addUpdateListener(this);
        layer.addAnimation(startPointAnimation);

        endPointAnimation = stroke.getEndPoint().createAnimation();
        endPointAnimation.addUpdateListener(this);
        layer.addAnimation(endPointAnimation);
    }

    @Override
    public void draw(Canvas canvas, Matrix parentMatrix, float parentAlpha) {
        if (hidden) {
            return;
        }
        getBounds(boundsRect, parentMatrix, false);

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
        super.draw(canvas, parentMatrix, parentAlpha);
    }

    @Override
    public String getName() {
        return name;
    }

    
    private LinearShader getLinearShader() {
        int gradientHash = getGradientHash();

        Optional<LinearShader> optVal = linearShaderCache.get(gradientHash);
        if (null != optVal && optVal.isPresent()) {
            return optVal.get();
        }
        Point startPoint = startPointAnimation.getValue();
        Point endPoint = endPointAnimation.getValue();
        GradientColor gradientColor = colorAnimation.getValue();
        Color[] colors = applyDynamicColorsIfNeeded(gradientColor.getColors());
        float[] positions = gradientColor.getPositions();
        Point[] point = {new Point(startPoint.getPointX(), startPoint.getPointY()), new Point(endPoint.getPointX(), endPoint.getPointY())};

        LinearShader gradient = new LinearShader(point, positions, colors, Shader.TileMode.CLAMP_TILEMODE);
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
        Color[] color = new Color[colors.length];
        if (colorCallbackAnimation != null) {
            Integer[] dynamicColors = (Integer[]) colorCallbackAnimation.getValue();
            if (colors.length == dynamicColors.length) {
                for (int i = 0; i < colors.length; i++) {
                    color[i] = new Color(dynamicColors[i]);
                }
            } else {
                colors = new int[dynamicColors.length];
                for (int i = 0; i < dynamicColors.length; i++) {
                    color[i] = new Color(dynamicColors[i]);
                }
            }
        }
        return color;
    }

    @Override
    public <T> void addValueCallback(T property, @Nullable LottieValueCallback<T> callback) {
        super.addValueCallback(property, callback);
        if (property == LottieProperty.GRADIENT_COLOR) {
            if (colorCallbackAnimation != null) {
                layer.removeAnimation(colorCallbackAnimation);
            }

            if (callback == null) {
                colorCallbackAnimation = null;
            } else {
                colorCallbackAnimation = new ValueCallbackKeyframeAnimation<>(callback);
                colorCallbackAnimation.addUpdateListener(this);
                layer.addAnimation(colorCallbackAnimation);
            }
        }
    }
}
