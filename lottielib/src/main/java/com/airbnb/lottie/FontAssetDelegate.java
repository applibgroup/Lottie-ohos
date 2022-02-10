package com.airbnb.lottie;

import ohos.agp.text.Font;

/**
 * Delegate to handle the loading of fonts that are not packaged in the assets of your app or don't
 * have the same file name.
 *
 * @see LottieDrawable#setFontAssetDelegate(FontAssetDelegate)
 */
@SuppressWarnings({"unused", "WeakerAccess"}) public class FontAssetDelegate {

    /**
     * verride this if you want to return a Typeface from a font family.
     * @param fontFamily takes the fontFamily
     * @return Font.DEFAULT
     */
    public Font fetchFont(String fontFamily) {
        return Font.DEFAULT;
    }

    /**
     * Override this if you want to specify the asset path for a given font family.
     * @param fontFamily takes the fontFamily
     * @return null
     */
    public String getFontPath(String fontFamily) {
        return null;
    }
}
