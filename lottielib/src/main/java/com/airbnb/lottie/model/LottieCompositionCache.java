package com.airbnb.lottie.model;

import com.airbnb.lottie.LottieComposition;

import ohos.utils.LruBuffer;
import org.jetbrains.annotations.Nullable;

public class LottieCompositionCache {

    private static final LottieCompositionCache INSTANCE = new LottieCompositionCache();

    public static LottieCompositionCache getInstance() {
        return INSTANCE;
    }

    private final LruBuffer<String, LottieComposition> cache = new LruBuffer<>(20);

    public LottieCompositionCache() {
    }

	@Nullable
    public LottieComposition get(@Nullable String cacheKey) {
        if (cacheKey == null) {
            return null;
        }
        return cache.get(cacheKey);
    }

    public void put(@Nullable String cacheKey, LottieComposition composition) {
        if (cacheKey == null) {
            return;
        }
        cache.put(cacheKey, composition);
    }

    public void clear() {
        cache.clear();
    }

    /**
     * Set the maximum number of compositions to keep cached in memory.
     * This must be > 0.
     * @param size in int
     */
    public void resize(int size) {
        cache.updateCapacity(size);
    }
}
