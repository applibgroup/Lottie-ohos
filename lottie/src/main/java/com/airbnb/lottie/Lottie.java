package com.airbnb.lottie;


import org.jetbrains.annotations.NotNull;

/**
 * Class for initializing the library with custom config
 */
public class Lottie {

  private Lottie() {
  }

  /**
   * Initialize Lottie with global configuration.
   *
   * @see LottieConfig.Builder
   * @param lottieConfig config
   */
  public static void initialize(@NotNull final LottieConfig lottieConfig) {
    L.setFetcher(lottieConfig.networkFetcher);
    L.setCacheProvider(lottieConfig.cacheProvider);
    L.setTraceEnabled(lottieConfig.enableSystraceMarkers);
  }
}