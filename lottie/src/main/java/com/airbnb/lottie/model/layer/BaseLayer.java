package com.airbnb.lottie.model.layer;

import com.airbnb.lottie.L;
import com.airbnb.lottie.animation.LPaint;
import com.airbnb.lottie.animation.content.Content;
import com.airbnb.lottie.animation.content.DrawingContent;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.FloatKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.MaskKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.TransformKeyframeAnimation;
import com.airbnb.lottie.model.content.Mask;
import com.airbnb.lottie.model.content.ShapeData;
import com.airbnb.lottie.utils.HMOSLogUtil;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.KeyPathElement;

import ohos.agp.render.BlendMode;
import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;
import ohos.agp.render.Path;
import ohos.agp.utils.Color;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.RectFloat;
import ohos.hiviewdfx.HiTraceId;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BaseLayer implements DrawingContent, BaseKeyframeAnimation.AnimationListener, KeyPathElement {
    /**
     * These flags were in Canvas but they were deprecated and removed.
     */
    private static final int CLIP_SAVE_FLAG = 0x02;

    private static final int CLIP_TO_LAYER_SAVE_FLAG = 0x10;

    private static final int MATRIX_SAVE_FLAG = 0x01;

    private static final String SAVE_LAYER = "Layer#saveLayer";

    private static final String RESTORE_LAYER ="Layer#restoreLayer";

    private static final int SAVE_FLAGS = CLIP_SAVE_FLAG | CLIP_TO_LAYER_SAVE_FLAG | MATRIX_SAVE_FLAG;

	@Nullable
    static BaseLayer forModel(Layer layerModel, LottieDrawable drawable, LottieComposition composition) {
        switch (layerModel.getLayerType()) {
            case SHAPE:
                return new ShapeLayer(drawable, layerModel);
            case PRE_COMP:
                return new CompositionLayer(drawable, layerModel, composition.getPrecomps(layerModel.getRefId()),
                    composition);
            case SOLID:
                return new SolidLayer(drawable, layerModel);
            case IMAGE:
                return new ImageLayer(drawable, layerModel);
            case NULL:
                return new NullLayer(drawable, layerModel);
            case TEXT:
                return new TextLayer(drawable, layerModel);
            case UNKNOWN:
            default:
                // Do nothing
                HMOSLogUtil.warn(L.TAG, "Unknown layer type " + layerModel.getLayerType());
                return null;
        }
    }

    private final Path path = new Path();
    private final Matrix matrix = new Matrix();
    private final Paint contentPaint = new LPaint(true);
    private final Paint dstInPaint = new LPaint(true, BlendMode.DST_IN);
    private final Paint dstOutPaint = new LPaint(true, BlendMode.DST_OUT);
    private final Paint mattePaint = new LPaint(true);
    private final Paint clearPaint = new LPaint(BlendMode.CLEAR);
    private final RectFloat rect = new RectFloat();
    private final RectFloat maskBoundsRect = new RectFloat();
    private final RectFloat matteBoundsRect = new RectFloat();
    private final RectFloat tempMaskBoundsRect = new RectFloat();
    private final String drawTraceName;
    final Matrix boundsMatrix = new Matrix();
    final LottieDrawable lottieDrawable;
    final Layer layerModel;
    @Nullable
    private MaskKeyframeAnimation mask;
    @Nullable
    private FloatKeyframeAnimation inOutAnimation;
    @Nullable
    private BaseLayer matteLayer;

    /**
     * This should only be used by {@link #buildParentLayerListIfNeeded()}
     * to construct the list of parent layers.
     */
	@Nullable
    private BaseLayer parentLayer;

    private List<BaseLayer> parentLayers;

    private final List<BaseKeyframeAnimation<?, ?>> animations = new ArrayList<>();

    final TransformKeyframeAnimation transform;

    private boolean visible = true;

    private boolean outlineMasksAndMattes;
    @Nullable private Paint outlineMasksAndMattesPaint;

    BaseLayer(LottieDrawable lottieDrawable, Layer layerModel) {
        this.lottieDrawable = lottieDrawable;
        this.layerModel = layerModel;
        drawTraceName = layerModel.getName() + "#draw";
        if (layerModel.getMatteType() == Layer.MatteType.INVERT) {
            mattePaint.setBlendMode(BlendMode.DST_OUT);
        } else {
            mattePaint.setBlendMode(BlendMode.DST_IN);
        }

        transform = layerModel.getTransform().createAnimation();
        transform.addListener(this);

        if (layerModel.getMasks() != null && !layerModel.getMasks().isEmpty()) {
            this.mask = new MaskKeyframeAnimation(layerModel.getMasks());
            for (BaseKeyframeAnimation<?, Path> animation : mask.getMaskAnimations()) {
                // Don't call addAnimation() because progress gets set manually in setProgress to
                // properly handle time scale.
                animation.addUpdateListener(this);
            }
            for (BaseKeyframeAnimation<Integer, Integer> animation : mask.getOpacityAnimations()) {
                addAnimation(animation);
                animation.addUpdateListener(this);
            }
        }
        setupInOutAnimations();
    }

    /**
     * Enable this to debug slow animations by outlining masks and mattes. The performance overhead of the masks and mattes will
     * be proportional to the surface area of all of the masks/mattes combined.
     * <p>
     * DO NOT leave this enabled in production.
     * @param outline boolean
     */
    void setOutlineMasksAndMattes(boolean outline) {
        if (outline && outlineMasksAndMattesPaint == null) {
            outlineMasksAndMattesPaint = new LPaint();
        }
        outlineMasksAndMattes = outline;
    }

    @Override
    public void onValueChanged() {
        invalidateSelf();
    }

    Layer getLayerModel() {
        return layerModel;
    }

    void setMatteLayer(@Nullable BaseLayer matteLayer) {
        this.matteLayer = matteLayer;
    }

    boolean hasMatteOnThisLayer() {
        return matteLayer != null;
    }

    void setParentLayer(@Nullable BaseLayer parentLayer) {
        this.parentLayer = parentLayer;
    }

    private void setupInOutAnimations() {
        if (!layerModel.getInOutKeyframes().isEmpty()) {
            inOutAnimation = new FloatKeyframeAnimation(layerModel.getInOutKeyframes());
            inOutAnimation.setIsDiscrete();
            inOutAnimation.addUpdateListener(new BaseKeyframeAnimation.AnimationListener() {
                @Override
                public void onValueChanged() {
                    setVisible(inOutAnimation.getFloatValue() == 1f);
                }
            });
            setVisible(inOutAnimation.getValue() == 1f);
            addAnimation(inOutAnimation);
        } else {
            setVisible(true);
        }
    }

    private void invalidateSelf() {
        if (null != lottieDrawable) {
            lottieDrawable.invalidateSelf();
        }
    }

    public void addAnimation(@Nullable BaseKeyframeAnimation<?, ?> newAnimation) {
        if (newAnimation == null) {
            return;
        }
        animations.add(newAnimation);
    }

    public void removeAnimation(BaseKeyframeAnimation<?, ?> animation) {
        animations.remove(animation);
    }

    @Override
    public void getBounds(RectFloat outBounds, Matrix parentMatrix, boolean applyParents) {
        rect.modify(0, 0, 0, 0);
        buildParentLayerListIfNeeded();
        boundsMatrix.setMatrix(parentMatrix);

        if (applyParents) {
            if (parentLayers != null) {
                for (int i = parentLayers.size() - 1; i >= 0; i--) {
                    boundsMatrix.preConcat(parentLayers.get(i).transform.getMatrix());
                }
            } else if (parentLayer != null) {
                boundsMatrix.preConcat(parentLayer.transform.getMatrix());
            }
        }

        boundsMatrix.preConcat(transform.getMatrix());
    }

    @Override
    public void draw(Canvas canvas, Matrix parentMatrix, float parentAlpha) {
//        HiTraceId traceID = L.beginSection(drawTraceName);
        if (!visible || layerModel.isHidden()) {
            //L.endSection(traceID);
            return;
        }
        buildParentLayerListIfNeeded();
        HiTraceId lyrTraceID = L.beginSection("Layer#parentMatrix");
        matrix.reset();
        matrix.setMatrix(parentMatrix);
        for (int i = parentLayers.size() - 1; i >= 0; i--) {
            matrix.preConcat(parentLayers.get(i).transform.getMatrix());
        }
        L.endSection(lyrTraceID);
        int opacity = transform.getOpacity() == null ? 100 : transform.getOpacity().getValue();
        int alpha = (int) ((parentAlpha / 255.0f * (float) opacity / 100f) * 255);
        if (!hasMatteOnThisLayer() && !hasMasksOnThisLayer()) {
            matrix.preConcat(transform.getMatrix());
//            HiTraceId ldlTraceID = L.beginSection("Layer#drawLayer");
            drawLayer(canvas, matrix, alpha);
//            L.endSection(ldlTraceID);
//            recordRenderTime(L.endSection(traceID));
            return;
        }

//        HiTraceId lcbTraceID = L.beginSection("Layer#computeBounds");
        getBounds(rect, matrix, false);

        intersectBoundsWithMatte(rect, parentMatrix);

        matrix.preConcat(transform.getMatrix());
        intersectBoundsWithMask(rect, matrix);

        if(canvas.getLocalClipBounds()!=null) {
//            if (!rect.isIntersect(0, 0, canvas.getLocalClipBounds().getWidth(), canvas.getLocalClipBounds().getHeight())) {
//               //rect.modify(0, 0, 0, 0);
//            }
        }

//        L.endSection(lcbTraceID);

            // Ensure that what we are drawing is >=1px of width and height.
            // On older devices, drawing to an offscreen buffer of <1px would draw back as a black bar.
            if (rect.getWidth() >= 1f && rect.getHeight() >= 1f) {
                HiTraceId id = L.beginSection(SAVE_LAYER);
            contentPaint.setAlpha(1.0f);
            Utils.saveLayerCompat(canvas, rect, contentPaint);
            L.endSection(id);

            // Clear the off screen buffer. This is necessary for some phones.
            clearCanvas(canvas);
            id = L.beginSection("Layer#drawLayer");
            drawLayer(canvas, matrix, alpha);
            L.endSection(id);

            if (hasMasksOnThisLayer()) {
                applyMasks(canvas, matrix);
            }

            if (hasMatteOnThisLayer()) {
                HiTraceId ldmTraceID = L.beginSection("Layer#drawMatte");
                HiTraceId tmpTraceID = L.beginSection(SAVE_LAYER);
                Utils.saveLayerCompat(canvas, rect, mattePaint, SAVE_FLAGS);
                L.endSection(tmpTraceID);
                clearCanvas(canvas);
                matteLayer.draw(canvas, parentMatrix, alpha);
                tmpTraceID = L.beginSection(RESTORE_LAYER);
                canvas.restore();
                L.endSection(tmpTraceID);
                L.endSection(ldmTraceID);
            }

            HiTraceId resTraceID = L.beginSection(RESTORE_LAYER);
            canvas.restore();
            L.endSection(resTraceID);
            }
        if (outlineMasksAndMattes && outlineMasksAndMattesPaint != null) {
            outlineMasksAndMattesPaint.setStyle(Paint.Style.STROKE_STYLE);
            //outlineMasksAndMattesPaint.setColor(0xFFFC2803);
            outlineMasksAndMattesPaint.setColor(Color.YELLOW);
            outlineMasksAndMattesPaint.setStrokeWidth(4);
            canvas.drawRect(rect, outlineMasksAndMattesPaint);
            outlineMasksAndMattesPaint.setStyle(Paint.Style.FILL_STYLE);
            //outlineMasksAndMattesPaint.setColor(0x50EBEBEB);
            outlineMasksAndMattesPaint.setColor(Color.CYAN);
            canvas.drawRect(rect, outlineMasksAndMattesPaint);
        }
//        recordRenderTime(L.endSection(traceID));
    }

    private void recordRenderTime(float ms) {
        lottieDrawable.getComposition().getPerfTracker().recordRenderTime(layerModel.getName(), ms);
    }

    private void clearCanvas(Canvas canvas) {
//        HiTraceId id = L.beginSection("Layer#clearLayer");
        // If we don't pad the clear draw, some phones leave a 1px border of the graphics buffer.
        canvas.drawRect(new RectFloat(rect.left - 1, rect.top - 1, rect.right + 1, rect.bottom + 1),
            clearPaint);
//        L.endSection(id);
    }

    private void intersectBoundsWithMask(RectFloat rect, Matrix matrix) {
        maskBoundsRect.modify(0, 0, 0, 0);
        if (!hasMasksOnThisLayer()) {
            return;
        }
        int size = mask.getMasks().size();
        for (int i = 0; i < size; i++) {
            Mask mask = this.mask.getMasks().get(i);
            BaseKeyframeAnimation<?, Path> maskAnimation = this.mask.getMaskAnimations().get(i);
            Path maskPath = maskAnimation.getValue();
            path.set(maskPath);
            path.transform(matrix);

            switch (mask.getMaskMode()) {
                case MASK_MODE_NONE:
                    // Mask mode none will just render the original content so it is the whole bounds.
                    return;
                case MASK_MODE_SUBTRACT:
                    // If there is a subtract mask, the mask could potentially be the size of the entire
                    // canvas so we can't use the mask bounds.
                    return;
                case MASK_MODE_INTERSECT:
                case MASK_MODE_ADD:
                    if (mask.isInverted()) {
                        return;
                    }
                    else {
                        continue;
                    }
                default:
                    path.computeBounds(tempMaskBoundsRect);
                    // As we iterate through the masks, we want to calculate the union region of the masks.
                    // We initialize the rect with the first mask. If we don't call set() on the first call,
                    // the rect will always extend to (0,0).
                    if (i == 0) {
                        maskBoundsRect.modify(tempMaskBoundsRect.left, tempMaskBoundsRect.top, tempMaskBoundsRect.right,
                            tempMaskBoundsRect.bottom);
                    } else {
                        maskBoundsRect.modify(Math.min(maskBoundsRect.left, tempMaskBoundsRect.left),
                            Math.min(maskBoundsRect.top, tempMaskBoundsRect.top),
                            Math.max(maskBoundsRect.right, tempMaskBoundsRect.right),
                            Math.max(maskBoundsRect.bottom, tempMaskBoundsRect.bottom));
                    }
            }
        }

        boolean intersects = rect.isIntersect(maskBoundsRect);
        if (!intersects) {
            rect.modify(0f, 0f, 0f, 0f);
        }
    }

    private void intersectBoundsWithMatte(RectFloat rect, Matrix matrix) {
        if (!hasMatteOnThisLayer()) {
            return;
        }

        if (layerModel.getMatteType() == Layer.MatteType.INVERT) {
            // We can't trim the bounds if the mask is inverted since it extends all the way to the
            // composition bounds.
            return;
        }
        matteBoundsRect.modify(0f, 0f, 0f, 0f);
        matteLayer.getBounds(matteBoundsRect, matrix, true);
        boolean intersects = rect.isIntersect(matteBoundsRect);
        if (!intersects) {
            rect.modify(0f, 0f, 0f, 0f);
        }
    }

    abstract void drawLayer(Canvas canvas, Matrix parentMatrix,
        float parentAlpha);

    private void maskMode(Canvas canvas,Matrix matrix,Mask mask,BaseKeyframeAnimation<ShapeData, Path> maskAnimation,BaseKeyframeAnimation<Integer, Integer> opacityAnimation, int i){
        switch (mask.getMaskMode()) {
            case MASK_MODE_NONE:
                // None mask should have no effect. If all masks are NONE, fill the
                // mask canvas with a rectangle so it fully covers the original layer content.
                // However, if there are other masks, they should be the only ones that have an effect so
                // this should noop.
                if (areAllMasksNone()) {
                    contentPaint.setAlpha(1.0f);
                    canvas.drawRect(rect, contentPaint);
                }
                break;
            case MASK_MODE_ADD:
                if (mask.isInverted()) {
                    applyInvertedAddMask(canvas, matrix, mask, maskAnimation, opacityAnimation);
                } else {
                    applyAddMask(canvas, matrix, mask, maskAnimation, opacityAnimation);
                }
                break;
            case MASK_MODE_SUBTRACT:
                if (i == 0) {
                    contentPaint.setColor(Color.BLACK);
                    contentPaint.setAlpha(1.0f);
                    canvas.drawRect(rect, contentPaint);
                }
                if (mask.isInverted()) {
                    applyInvertedSubtractMask(canvas, matrix, mask, maskAnimation, opacityAnimation);
                } else {
                    applySubtractMask(canvas, matrix, mask, maskAnimation, opacityAnimation);
                }
                break;
            case MASK_MODE_INTERSECT:
                if (mask.isInverted()) {
                    applyInvertedIntersectMask(canvas, matrix, mask, maskAnimation, opacityAnimation);
                } else {
                    applyIntersectMask(canvas, matrix, mask, maskAnimation, opacityAnimation);
                }
                break;
        }
    }

    private void applyMasks(Canvas canvas, Matrix matrix) {
        HiTraceId id = L.beginSection(SAVE_LAYER);
        Utils.saveLayerCompat(canvas, rect, dstInPaint, SAVE_FLAGS);
        L.endSection(id);
        for (int i = 0; i < mask.getMasks().size(); i++) {
            Mask mask = this.mask.getMasks().get(i);
            BaseKeyframeAnimation<ShapeData, Path> maskAnimation = this.mask.getMaskAnimations().get(i);
            BaseKeyframeAnimation<Integer, Integer> opacityAnimation = this.mask.getOpacityAnimations().get(i);
            maskMode(canvas,matrix,mask,maskAnimation,opacityAnimation,i);
        }
        id = L.beginSection(RESTORE_LAYER);
        canvas.restore();
        L.endSection(id);
    }

    private boolean areAllMasksNone() {
        if (mask.getMaskAnimations().isEmpty()) {
            return false;
        }
        boolean areAllMasksNone = true;
        for (int i = 0; i < mask.getMasks().size(); i++) {
            if (mask.getMasks().get(i).getMaskMode() != Mask.MaskMode.MASK_MODE_NONE) {
                return false;
            }
        }
        return true;
    }

    private void applyAddMask(Canvas canvas, Matrix matrix, Mask mask,
        BaseKeyframeAnimation<ShapeData, Path> maskAnimation,  BaseKeyframeAnimation<Integer, Integer> opacityAnimation) {
        Path maskPath = maskAnimation.getValue();
        path.set(maskPath);
        path.transform(matrix);
        contentPaint.setAlpha((opacityAnimation.getValue()/100f));
        canvas.drawPath(path, contentPaint);
    }

    private void applyInvertedAddMask(Canvas canvas, Matrix matrix, Mask mask, 
		BaseKeyframeAnimation<ShapeData, Path> maskAnimation, BaseKeyframeAnimation<Integer, Integer> opacityAnimation) {
        Utils.saveLayerCompat(canvas, rect, contentPaint);
        canvas.drawRect(rect, contentPaint);
        Path maskPath = maskAnimation.getValue();
        path.set(maskPath);
        path.transform(matrix);
        contentPaint.setAlpha((opacityAnimation.getValue()/100f));
        canvas.drawPath(path, dstOutPaint);
        canvas.restore();
    }

    private void applySubtractMask(Canvas canvas, Matrix matrix, Mask mask,
        BaseKeyframeAnimation<ShapeData, Path> maskAnimation, BaseKeyframeAnimation<Integer, Integer> opacityAnimation) {
        Path maskPath = maskAnimation.getValue();
        path.set(maskPath);
        path.transform(matrix);
        canvas.drawPath(path, dstOutPaint);
    }

    private void applyInvertedSubtractMask(Canvas canvas, Matrix matrix,Mask mask, 
		BaseKeyframeAnimation<ShapeData, Path> maskAnimation, BaseKeyframeAnimation<Integer, Integer> opacityAnimation) {
        Utils.saveLayerCompat(canvas, rect, dstOutPaint);
        canvas.drawRect(rect, contentPaint);
        dstOutPaint.setAlpha((int) (opacityAnimation.getValue() /100f));
        Path maskPath = maskAnimation.getValue();
        path.set(maskPath);
        path.transform(matrix);
        canvas.drawPath(path, dstOutPaint);
        canvas.restore();
    }

    private void applyIntersectMask(Canvas canvas, Matrix matrix, Mask mask,
        BaseKeyframeAnimation<ShapeData, Path> maskAnimation, BaseKeyframeAnimation<Integer, Integer> opacityAnimation) {
        Utils.saveLayerCompat(canvas, rect, dstInPaint);
        Path maskPath = maskAnimation.getValue();
        path.set(maskPath);
        path.transform(matrix);
        contentPaint.setAlpha((opacityAnimation.getValue()/100f));
        canvas.drawPath(path, contentPaint);
        canvas.restore();
    }

    private void applyInvertedIntersectMask(Canvas canvas, Matrix matrix,
        Mask mask, BaseKeyframeAnimation<ShapeData, Path> maskAnimation, BaseKeyframeAnimation<Integer, Integer> opacityAnimation) {
        Utils.saveLayerCompat(canvas, rect, dstInPaint);
        canvas.drawRect(rect, contentPaint);
        dstOutPaint.setAlpha((opacityAnimation.getValue()/100f));
        Path maskPath = maskAnimation.getValue();
        path.set(maskPath);
        path.transform(matrix);
        canvas.drawPath(path, dstOutPaint);
        canvas.restore();
    }

    boolean hasMasksOnThisLayer() {
        return mask != null && !mask.getMaskAnimations().isEmpty();
    }

    private void setVisible(boolean visible) {
        if (visible != this.visible) {
            this.visible = visible;
            invalidateSelf();
        }
    }

    void setProgress(float progress) {
        // Time stretch should not be applied to the layer transform.
        transform.setProgress(progress);
        if (mask != null) {
            for (int i = 0; i < mask.getMaskAnimations().size(); i++) {
                mask.getMaskAnimations().get(i).setProgress(progress);
            }
        }
        if (layerModel.getTimeStretch() != 0) {
            progress /= layerModel.getTimeStretch();
        }
        if (inOutAnimation != null) {
            // Time stretch needs to be divided again for the inOutAnimation.
            inOutAnimation.setProgress(progress / layerModel.getTimeStretch());
        }
        if (matteLayer != null) {
            // The matte layer's time stretch is pre-calculated.
            float matteTimeStretch = matteLayer.layerModel.getTimeStretch();
            matteLayer.setProgress(progress * matteTimeStretch);
        }
        for (int i = 0; i < animations.size(); i++) {
            animations.get(i).setProgress(progress);
        }
    }

    private void buildParentLayerListIfNeeded() {
        if (parentLayers != null) {
            return;
        }
        if (parentLayer == null) {
            parentLayers = Collections.emptyList();
            return;
        }

        parentLayers = new ArrayList<>();
        BaseLayer layer = parentLayer;
        while (layer != null) {
            parentLayers.add(layer);
            layer = layer.parentLayer;
        }
    }

    @Override
    public String getName() {
        return layerModel.getName();
    }

    @Override
    public void setContents(List<Content> contentsBefore, List<Content> contentsAfter) {
        // Do nothing
    }

    @Override
    public void resolveKeyPath(KeyPath keyPath, int depth, List<KeyPath> accumulator, KeyPath currentPartialKeyPath) {
        if (!keyPath.matches(getName(), depth)) {
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
            resolveChildKeyPath(keyPath, newDepth, accumulator, currentPartialKeyPath);
        }
    }

    void resolveChildKeyPath(KeyPath keyPath, int depth, List<KeyPath> accumulator, KeyPath currentPartialKeyPath) {
    }

    @Override
    public <T> void addValueCallback(T property, @Nullable LottieValueCallback<T> callback) {
        transform.applyValueCallback(property, callback);
    }
}
