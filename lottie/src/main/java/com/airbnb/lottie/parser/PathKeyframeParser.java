package com.airbnb.lottie.parser;

import com.airbnb.lottie.animation.keyframe.PathKeyframe;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.Keyframe;
import ohos.agp.utils.Point;

import java.io.IOException;

class PathKeyframeParser {

    private PathKeyframeParser() {
    }

    static PathKeyframe parse(JsonReader reader, LottieComposition composition) throws IOException {
        boolean animated = reader.peek() == JsonReader.Token.BEGIN_OBJECT;
        Keyframe<Point> keyframe = KeyframeParser.parse(reader, composition, Utils.dpScale(), PathParser.INSTANCE, animated, false);

        return new PathKeyframe(composition, keyframe);
    }
}
