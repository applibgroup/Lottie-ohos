package com.airbnb.lottie.parser;

import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.model.animatable.AnimatableGradientColorValue;
import com.airbnb.lottie.model.animatable.AnimatableIntegerValue;
import com.airbnb.lottie.model.animatable.AnimatablePointValue;
import com.airbnb.lottie.model.content.GradientFill;
import com.airbnb.lottie.model.content.GradientType;

import com.airbnb.lottie.value.Keyframe;
import ohos.agp.render.Path;

import java.io.IOException;
import java.util.Collections;

class GradientFillParser {
    private static final JsonReader.Options NAMES = JsonReader.Options.of("nm", "g", "o", "t", "s", "e", "r", "hd");

    private static final JsonReader.Options GRADIENT_NAMES = JsonReader.Options.of("p", "k");

    private GradientFillParser() {
    }

    private static GradientType getGradientType(GradientType gradientType, JsonReader reader) throws IOException {
        gradientType = reader.nextInt() == 1 ? GradientType.LINEAR : GradientType.RADIAL;
        return gradientType;
    }

    static GradientFill parse(JsonReader reader, LottieComposition composition) throws IOException {
        String name = null;
        AnimatableGradientColorValue color = null;
        AnimatableIntegerValue opacity = null;
        GradientType gradientType = null;
        AnimatablePointValue startPoint = null;
        AnimatablePointValue endPoint = null;
        Path.FillType fillType = Path.FillType.WINDING_ORDER;
        boolean hidden = false;

        while (reader.hasNext()) {
            switch (reader.selectName(NAMES)) {
                case 0:
                    name = reader.nextString();
                    break;
                case 1:
                    int points = -1;
                    reader.beginObject();
                    while (reader.hasNext()) {
                        switch (reader.selectName(GRADIENT_NAMES)) {
                            case 0:
                                points = reader.nextInt();
                                break;
                            case 1:
                                color = AnimatableValueParser.parseGradientColor(reader, composition, points);
                                break;
                            default:
                                reader.skipName();
                                reader.skipValue();
                        }
                    }
                    reader.endObject();
                    break;
                case 2:
                    opacity = AnimatableValueParser.parseInteger(reader, composition);
                    break;
                case 3:
                    gradientType = getGradientType(gradientType,reader);
                    break;
                case 4:
                    startPoint = AnimatableValueParser.parsePoint(reader, composition);
                    break;
                case 5:
                    endPoint = AnimatableValueParser.parsePoint(reader, composition);
                    break;
                case 6:
                    fillType = reader.nextInt() == 1 ? Path.FillType.WINDING_ORDER : Path.FillType.EVEN_ODD;
                    break;
                case 7:
                    hidden = reader.nextBoolean();
                    break;
                default:
                    reader.skipName();
                    reader.skipValue();
            }
        }

    // Telegram sometimes omits opacity.
        opacity = opacity == null ? new AnimatableIntegerValue(Collections.singletonList(new Keyframe<>(100))) : opacity;
        return new GradientFill(name, gradientType, fillType, color, opacity, startPoint, endPoint, null, null, hidden);
    }
}
