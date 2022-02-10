package com.airbnb.lottie;

/**
 * @see LottieCompositionFactory
 * @see LottieResult
 */
@Deprecated
public interface OnCompositionLoadedListener {
    /**
     * Composition will be null if there was an error loading it. Check logcat for more details.
     * @param composition of json
     */
    void onCompositionLoaded(LottieComposition composition);
}
