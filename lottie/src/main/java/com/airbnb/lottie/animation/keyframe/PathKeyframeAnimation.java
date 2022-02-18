package com.airbnb.lottie.animation.keyframe;

import com.airbnb.lottie.value.Keyframe;
import ohos.agp.render.Path;
import ohos.agp.render.PathMeasure;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.Point;

import java.util.List;

public class PathKeyframeAnimation extends KeyframeAnimation<Point> {
    private final Point point = new Point();
    private final float[] pos = new float[2];
    private final Matrix matrix = new Matrix();
    private PathKeyframe pathMeasureKeyframe;
    private PathMeasure pathMeasure ;

    public PathKeyframeAnimation(List<? extends Keyframe<Point>> keyframes) {
        super(keyframes);
    }

    @Override
    Point getValue(Keyframe<Point> keyframe, float keyframeProgress) {
        PathKeyframe pathKeyframe = (PathKeyframe) keyframe;
        Path path = pathKeyframe.getPath();
        if (path == null) {
            return keyframe.startValue;
        }

        if (valueCallback != null) {
            Point value = valueCallback.getValueInternal(pathKeyframe.startFrame, pathKeyframe.endFrame,
                pathKeyframe.startValue, pathKeyframe.endValue, getLinearCurrentKeyframeProgress(), 
				keyframeProgress, getProgress());
            if (value != null) {
                return value;
            }
        }
        pathMeasure = new PathMeasure(path, false);
        if (pathMeasureKeyframe != pathKeyframe) {
            pathMeasureKeyframe = pathKeyframe;
        }
        matrix.reset();
        pathMeasure.getMatrix(keyframeProgress * pathMeasure.getLength(), matrix, 1);
        point.modify(matrix.getTranslateX(), matrix.getTranslateY());
        return point;
    }
}