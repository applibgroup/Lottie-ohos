package com.airbnb.lottie.model;

public class Font {

    private final String family;

    private final String name;

    private final String style;

    private final float ascent;

    public Font(String family, String name, String style, float ascent) {
        this.family = family;
        this.name = name;
        this.style = style;
        this.ascent = ascent;
    }

    public String getFamily() {
        return family;
    }

    public String getName() {
        return name;
    }

    public String getStyle() {
        return style;
    }

    float getAscent() {
        return ascent;
    }
}
