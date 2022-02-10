package com.airbnb.lottie;

import ohos.media.image.PixelMap;
import org.jetbrains.annotations.Nullable;

/**
 * Delegate to handle the loading of bitmaps that are not packaged in the assets of your app.
 *
 * @see LottieDrawable#setImageAssetDelegate(ImageAssetDelegate)
 */
public interface ImageAssetDelegate {

    /**
     * Interface added for HOSP
     * @param asset takes asset of type LottieImageAsset.
     * @return .
     */
    @Nullable PixelMap fetchPixelmap(LottieImageAsset asset);
}
