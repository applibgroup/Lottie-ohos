package com.airbnb.lottie;

import com.airbnb.lottie.value.ScaleXY;
import com.airbnb.lottie.value.LottieValueCallback;
import ohos.agp.render.BlendMode;
import ohos.agp.render.ColorFilter;
import ohos.agp.utils.Color;
import ohos.agp.utils.Point;

/**
 * Property values are the same type as the generic type of their corresponding
 * {@link LottieValueCallback}. With this, we can use generics to maintain type safety
 * of the callbacks.
 * <p>
 * Supported properties:
 * Transform:
 * {@link #TRANSFORM_ANCHOR_POINT}
 * {@link #TRANSFORM_POSITION}
 * {@link #TRANSFORM_OPACITY}
 * {@link #TRANSFORM_SCALE}
 * {@link #TRANSFORM_ROTATION}
 * {@link #TRANSFORM_SKEW}
 * {@link #TRANSFORM_SKEW_ANGLE}
 * <p>
 * Fill:
 * {@link #COLOR} (non-gradient)
 * {@link #OPACITY}
 * {@link #COLOR_FILTER}
 * <p>
 * Stroke:
 * {@link #COLOR} (non-gradient)
 * {@link #STROKE_WIDTH}
 * {@link #OPACITY}
 * {@link #COLOR_FILTER}
 * <p>
 * Ellipse:
 * {@link #POSITION}
 * {@link #ELLIPSE_SIZE}
 * <p>
 * Polystar:
 * {@link #POLYSTAR_POINTS}
 * {@link #POLYSTAR_ROTATION}
 * {@link #POSITION}
 * {@link #POLYSTAR_INNER_RADIUS} (star)
 * {@link #POLYSTAR_OUTER_RADIUS}
 * {@link #POLYSTAR_INNER_ROUNDEDNESS} (star)
 * {@link #POLYSTAR_OUTER_ROUNDEDNESS}
 * <p>
 * Repeater:
 * All transform properties
 * {@link #REPEATER_COPIES}
 * {@link #REPEATER_OFFSET}
 * {@link #TRANSFORM_ROTATION}
 * {@link #TRANSFORM_START_OPACITY}
 * {@link #TRANSFORM_END_OPACITY}
 * <p>
 * Layers:
 * All transform properties
 * {@link #TIME_REMAP} (composition layers only)
 */
public interface LottieProperty {
    /**
     * ColorInt
     **/
    Integer COLOR = 1;
    Integer STROKE_COLOR = 2;
    /**
     * Opacity value are 0-100 to match after effects
     **/
    Integer TRANSFORM_OPACITY = 3;
    /**
     * [0,100]
     */
    Integer OPACITY = 4;
    /**
     * In Px
     */
    Point TRANSFORM_ANCHOR_POINT = new Point();
    /**
     * In Px
     */
    Point TRANSFORM_POSITION = new Point();
    /**
     * When split dimensions is enabled. In Px
     */
    Float TRANSFORM_POSITION_X = 15f;
    /**
     * When split dimensions is enabled. In Px
     */
    Float TRANSFORM_POSITION_Y = 16f;
    /**
     * In Px
     */
    Point ELLIPSE_SIZE = new Point();
    /**
     * In Px
     */
    Point RECTANGLE_SIZE = new Point();
    /**
     * In degrees
     */
    Float CORNER_RADIUS = 0f;
    /**
     * In Px
     */
    Point POSITION = new Point();
    ScaleXY TRANSFORM_SCALE = new ScaleXY();
    /**
     * In degrees
     */
    Float TRANSFORM_ROTATION = 1f;
    /**
     * 0-85
     */
    Float TRANSFORM_SKEW = 0f;
    /**
     * In degrees
     */
    Float TRANSFORM_SKEW_ANGLE = 0f;
    /**
     * In Px
     */
    Float STROKE_WIDTH = 2f;
    Float TEXT_TRACKING = 3f;
    Float REPEATER_COPIES = 4f;
    Float REPEATER_OFFSET = 5f;
    Float POLYSTAR_POINTS = 6f;
    /**
     * In degrees
     */
    Float POLYSTAR_ROTATION = 7f;
    /**
     * In Px
     */
    Float POLYSTAR_INNER_RADIUS = 8f;
    /**
     * In Px
     */
    Float POLYSTAR_OUTER_RADIUS = 9f;
    /**
     * [0,100]
     */
    Float POLYSTAR_INNER_ROUNDEDNESS = 10f;
    /**
     * [0,100]
     */
    Float POLYSTAR_OUTER_ROUNDEDNESS = 11f;
    /**
     * [0,100]
     */
    Float TRANSFORM_START_OPACITY = 12f;
    /**
     * [0,100]
     */
    Float TRANSFORM_END_OPACITY = 12.1f;
    /**
     * The time value in seconds
     */
    Float TIME_REMAP = 13f;
    /**
     * In Dp
     */
    Float TEXT_SIZE = 14f;

    ColorFilter COLOR_FILTER = new ColorFilter(Color.TRANSPARENT.getValue(), BlendMode.SRC_ATOP);

    Integer[] GRADIENT_COLOR = new Integer[0];
}