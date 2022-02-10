package com.airbnb.lottie;

import org.jetbrains.annotations.Nullable;

public interface LottieOnCompositionLoadedListener {
  void onCompositionLoaded(@Nullable LottieComposition composition);
}
