package com.airbnb.lottie;

import ohos.agp.render.BlendMode;
import ohos.agp.render.Canvas;
import ohos.agp.render.ColorFilter;

/**
 * A color filter with a predefined transfer mode that applies the specified color on top of the
 * original color. As there are many other transfer modes, please take a look at the definition
 * of PorterDuff.Mode.SRC_ATOP to find one that suits your needs.
 * This site has a great explanation of Porter/Duff compositing algebra as well as a visual
 * representation of many of the transfer modes:
 * http://ssp.impulsetrain.com/porterduff.html
 */
@SuppressWarnings("WeakerAccess") public class SimpleColorFilter extends ColorFilter {
  public SimpleColorFilter(int color) {
    super(color, BlendMode.SRC_ATOP);
  }
}
