package com.airbnb.lottie.parser;

import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.model.CubicCurveData;
import com.airbnb.lottie.model.content.ShapeData;
import com.airbnb.lottie.utils.MiscUtils;
import ohos.agp.utils.Point;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShapeDataParser implements ValueParser<ShapeData> {
    public static final ShapeDataParser INSTANCE = new ShapeDataParser();

    private static final JsonReader.Options NAMES = JsonReader.Options.of("c", "v", "i", "o");

    private ShapeDataParser() {
    }

    @Override
    public ShapeData parse(JsonReader reader, float scale) throws IOException {
        // Sometimes the points data is in a array of length 1. Sometimes the data is at the top
        // level.
        if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
            reader.beginArray();
        }

        boolean closed = false;
        List<Point> pointsArray = null;
        List<Point> inTangents = null;
        List<Point> outTangents = null;
        reader.beginObject();

        while (reader.hasNext()) {
            switch (reader.selectName(NAMES)) {
                case 0:
                    closed = reader.nextBoolean();
                    break;
                case 1:
                    pointsArray = JsonUtils.jsonToPoints(reader, scale);
                    break;
                case 2:
                    inTangents = JsonUtils.jsonToPoints(reader, scale);
                    break;
                case 3:
                    outTangents = JsonUtils.jsonToPoints(reader, scale);
                    break;
                default:
                    reader.skipName();
                    reader.skipValue();
            }
        }

        reader.endObject();

        if (reader.peek() == JsonReader.Token.END_ARRAY) {
            reader.endArray();
        }

        if (pointsArray == null || inTangents == null || outTangents == null) {
            throw new IllegalArgumentException("Shape data was missing information.");
        }

        if (pointsArray.isEmpty()) {
            return new ShapeData(new Point(), false, Collections.<CubicCurveData>emptyList());
        }

        int length = pointsArray.size();
        Point vertex = pointsArray.get(0);
        Point initialPoint = vertex;
        List<CubicCurveData> curves = new ArrayList<>(length);

        for (int i = 1; i < length; i++) {
            vertex = pointsArray.get(i);
            Point previousVertex = pointsArray.get(i - 1);
            Point cp1 = outTangents.get(i - 1);
            Point cp2 = inTangents.get(i);
            Point shapeCp1 = MiscUtils.addPoints(previousVertex, cp1);
            Point shapeCp2 = MiscUtils.addPoints(vertex, cp2);
            curves.add(new CubicCurveData(shapeCp1, shapeCp2, vertex));
        }

        if (closed) {
            vertex = pointsArray.get(0);
            Point previousVertex = pointsArray.get(length - 1);
            Point cp1 = outTangents.get(length - 1);
            Point cp2 = inTangents.get(0);

            Point shapeCp1 = MiscUtils.addPoints(previousVertex, cp1);
            Point shapeCp2 = MiscUtils.addPoints(vertex, cp2);

            curves.add(new CubicCurveData(shapeCp1, shapeCp2, vertex));
        }
        return new ShapeData(initialPoint, closed, curves);
    }
}
