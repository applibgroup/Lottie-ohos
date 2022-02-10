package com.airbnb.lottie;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Extend this class to replace animation text with custom text. This can be useful to handle
 * translations.
 * <p>
 * The only method you should have to override is {@link #getText(String)}.
 */
public class TextDelegate {

    private final Map<String, String> stringMap = new HashMap<>();

    @Nullable private final LottieAnimationView animationView;

    @Nullable private final LottieDrawable drawable;

    private boolean cacheText = true;

    /**
     * This normally needs to be able to invalidate the view/drawable but not for the test.
     */
    TextDelegate() {
        animationView = null;
        drawable = null;
    }

    public TextDelegate(LottieAnimationView animationView) {
        this.animationView = animationView;
        drawable = null;
    }

    public TextDelegate(LottieDrawable drawable) {
        this.drawable = drawable;
        animationView = null;
    }

    /**
     * Override this to replace the animation text with something dynamic. This can be used for
     * translations or custom data.
     * @param input text to replace
     * @return input
     */
    private String getText(String input) {
        return input;
    }

    /**
     * Update the text that will be rendered for the given input text.
     * @param input to update
     * @param output text to replace with imput given
     */
    public void setText(String input, String output) {
        stringMap.put(input, output);
        invalidate();
    }

    /**
     * Sets whether or not {@link TextDelegate} will cache (memoize) the results of getText.
     * If this isn't necessary then set it to false.
     * @param cacheText to cache
     */
    public void setCacheText(boolean cacheText) {
        this.cacheText = cacheText;
    }

    /**
     * Invalidates a cached string with the given input.
     * @param input Invalidates a cached string with the given input.
     */
    public void invalidateText(String input) {
        stringMap.remove(input);
        invalidate();
    }

    /**
     * Invalidates all cached strings
     */
    public void invalidateAllText() {
        stringMap.clear();
        invalidate();
    }

    public final String getTextInternal(String input) {
        if (cacheText && stringMap.containsKey(input)) {
            return stringMap.get(input);
        }
        String text = getText(input);
        if (cacheText) {
            stringMap.put(input, text);
        }
        return text;
    }

    private void invalidate() {
         if (animationView != null) {
             animationView.invalidate();
         }
        if (drawable != null) {
            drawable.invalidateSelf();
        }
    }
}
