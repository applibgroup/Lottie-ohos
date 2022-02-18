package com.airbnb.lottie.model.layer;

import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.animation.LPaint;
import com.airbnb.lottie.animation.keyframe.BaseKeyframeAnimation;
import com.airbnb.lottie.animation.keyframe.ValueCallbackKeyframeAnimation;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.LottieDrawable;

import ohos.agp.render.Canvas;
import ohos.agp.render.ColorFilter;
import ohos.agp.render.Paint;
import ohos.agp.render.PixelMapHolder;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.RectFloat;
import ohos.media.image.PixelMap;

public class ImageLayer extends BaseLayer {

    private final Paint lPaint = new LPaint(true) {
        {
            setFilterBitmap(true);
        }
    };

    private final RectFloat src = new RectFloat();

    private final RectFloat dst = new RectFloat();

    private BaseKeyframeAnimation<ColorFilter, ColorFilter> colorFilterAnimation;

    ImageLayer(LottieDrawable lottieDrawable, Layer layerModel) {
        super(lottieDrawable, layerModel);
    }

    @Override
    void drawLayer(Canvas canvas, Matrix parentMatrix, float parentAlpha) {
        PixelMap pixelmap = getPixelmap();
        if (pixelmap == null) {
            return;
        }
        float density = Utils.dpScale();

        // Transparency range is 0 - 1, not 0-255 (0xFF)
        float alphaValue = parentAlpha / 255f;
        lPaint.setAlpha(alphaValue);
        if (colorFilterAnimation != null) {
            lPaint.setColorFilter(colorFilterAnimation.getValue());
        }
        canvas.save();
        canvas.concat(parentMatrix);
        src.modify(0, 0, pixelmap.getImageInfo().size.width, pixelmap.getImageInfo().size.height);
        dst.modify(0, 0, (int) (pixelmap.getImageInfo().size.width * density),
            (int) (pixelmap.getImageInfo().size.height * density));
        canvas.drawPixelMapHolderRect(new PixelMapHolder(pixelmap), src, dst, lPaint);
        canvas.restore();
    }

    @Override
    public void getBounds(RectFloat outBounds, Matrix parentMatrix, boolean applyParents) {
        super.getBounds(outBounds, parentMatrix, applyParents);
        PixelMap pixelmap = getPixelmap();
        if (pixelmap != null) {
            outBounds.modify(0, 0, pixelmap.getImageInfo().size.width * Utils.dpScale(),
                pixelmap.getImageInfo().size.height * Utils.dpScale());
            boundsMatrix.mapRect(outBounds);
        }
    }

    private PixelMap getPixelmap() {
        String refId = layerModel.getRefId();
        return lottieDrawable.getImageAsset(refId);
    }

    @Override
    public <T> void addValueCallback(T property, LottieValueCallback<T> callback) {
        super.addValueCallback(property, callback);
        if (property == LottieProperty.COLOR_FILTER) {
            if (callback == null) {
                colorFilterAnimation = null;
            } else {
                colorFilterAnimation = new ValueCallbackKeyframeAnimation<>(
                    (LottieValueCallback<ColorFilter>) callback);
            }
        }
    }
}
