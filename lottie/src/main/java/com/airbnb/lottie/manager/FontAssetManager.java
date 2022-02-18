package com.airbnb.lottie.manager;

import com.airbnb.lottie.FontAssetDelegate;
import com.airbnb.lottie.L;
import com.airbnb.lottie.model.MutablePair;
import com.airbnb.lottie.utils.HMOSLogUtil;

import ohos.agp.text.Font;
import ohos.app.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FontAssetManager {
    private final MutablePair<String> tempPair = new MutablePair<>();

    /**
     * openharmony - Pair is (fontName, fontStyle)
     */
    private final Map<MutablePair<String>, Font> fontMap = new HashMap<>();

    /**
     * openharmony - Map of font families to their fonts. Necessary to create a font with a different style
     */
    private final Map<String, Font> fontFamilies = new HashMap<>();

    private FontAssetDelegate delegate;

    private String defaultFontFileExtension = ".ttf";

    public FontAssetManager(FontAssetDelegate delegate) {
        this.delegate = delegate;
    }

    public void setDelegate(FontAssetDelegate assetDelegate) {
        delegate = assetDelegate;
    }

    private static final int BUFFER_LENGTH = 8192;

    private static final String RAW_FILE_PATH = "entry_watch/resources/rawfile/";

    /**
     * font file name: digit condensed regular
     */
    public static final String DIGIT_REGULAR = "hw-digit-reg-LL.otf";

    /**
     * font file name: digit condensed medium
     */
    public static final String DIGIT_MEDIUM = "hw-digit-med-LL.otf";

    /**
     * font file name: robot condensed regular
     */
    public static final String ROBOT_CONDENSED_REGULAR = "sans-serif-condensed";

    /**
     * font file name: robot condensed medium
     */
    public static final String ROBOT_CONDENSED_MEDIUM = "sans-serif-condensed-medium";

    private static final Map<String, Font> FONT_MAP = new HashMap<>();

    /** Default error code */
    public static final int DEFAULT_ERROR = -1;

    /**
     * Sets the default file extension (include the `.`).
     * <p>
     * e.g. `.ttf` `.otf`
     * <p>
     * Defaults to `.ttf`
     */
    /*public void setDefaultFontFileExtension(String defaultFontFileExtension) {
        this.defaultFontFileExtension = defaultFontFileExtension;
    }

    public Font getTypeface(String fontFamily, String style) {
        tempPair.set(fontFamily, style);
        Font typeface = fontMap.get(tempPair);
        if (typeface != null) {
            return typeface;
        }
        Font typefaceWithDefaultStyle = getFontFamily(fontFamily);
        typeface = typefaceForStyle(typefaceWithDefaultStyle, style);
        fontMap.put(tempPair, typeface);
        return typeface;
    }

    private Font getFontFamily(String fontFamily) {
        Font defaultTypeface = fontFamilies.get(fontFamily);
        if (defaultTypeface != null) {
            return defaultTypeface;
        }

        Font typeface = null;
        if (delegate != null) {
            typeface = delegate.fetchFont(fontFamily);
        }

        if (delegate != null && typeface == null) {
            String path = delegate.getFontPath(fontFamily);
            if (path != null) {
                typeface = Typeface.createFromFile(path);
            }
        }

        if (typeface == null) {
            String path = "fonts/" + fontFamily + defaultFontFileExtension;
            typeface = Typeface.createFromFile(path);
        }

        fontFamilies.put(fontFamily, typeface);
        return typeface;
    }

    private Typeface typefaceForStyle(Typeface typeface, String style) {
        int styleInt = Typeface.NORMAL;
        boolean containsItalic = style.contains("Italic");
        boolean containsBold = style.contains("Bold");
        if (containsItalic && containsBold) {
            styleInt = Typeface.BOLD_ITALIC;
        } else if (containsItalic) {
            styleInt = Typeface.ITALIC;
        } else if (containsBold) {
            styleInt = Typeface.BOLD;
        }

        if (typeface.getStyle() == styleInt) {
            return typeface;
        }

        return Typeface.create(typeface, styleInt);
    }*/

    /**
     * get font by family name
     *
     * @param context context
     * @param familyName family name
     * @param style font style
     * @return font
     */
    public Font getTypeface(Context context, String familyName, int style) {
        tempPair.set(familyName, String.valueOf(style));
        Font typeface = fontMap.get(tempPair);
        if(null != typeface)
        {
            return typeface;
        }

        Font font = null;
        switch (familyName) {
            case DIGIT_MEDIUM:
            case DIGIT_REGULAR:
                font = loadFontFromFile(context, familyName, style).orElse(Font.DEFAULT);
                break;
            case ROBOT_CONDENSED_MEDIUM:
            case ROBOT_CONDENSED_REGULAR:
                font = new Font.Builder(familyName).setWeight(style).build();
                break;
            default:
                HMOSLogUtil.error(L.TAG, "getFont -> get unknown familyName");
        }
        if (font != null) {
            fontMap.put(tempPair, font);
            return font;
        }
        return Font.DEFAULT;
    }


    private static Optional<Font> loadFontFromFile(Context context, String familyName, int style) {
        if (context == null || isEmpty(familyName)) {
            HMOSLogUtil.error(L.TAG, "loadFontFromFile -> get null params");
            return Optional.empty();
        }
        String path = RAW_FILE_PATH + familyName;
        File file = new File(context.getApplicationContext().getDataDir(), familyName);
        if (file.exists()) {
            return Optional.of(new Font.Builder(file).build());
        }
        try (OutputStream outputStream = new FileOutputStream(file);
            InputStream inputStream = context.getResourceManager().getRawFileEntry(path).openRawFile()) {
            byte[] buffer = new byte[BUFFER_LENGTH];
            int bytesRead = inputStream.read(buffer, 0, BUFFER_LENGTH);
            while (bytesRead != DEFAULT_ERROR) {
                outputStream.write(buffer, 0, bytesRead);
                bytesRead = inputStream.read(buffer, 0, BUFFER_LENGTH);
            }
        } catch (FileNotFoundException exception) {
            HMOSLogUtil.error(L.TAG, "loadFontFromFile -> FileNotFoundException : "+exception.getLocalizedMessage());
        } catch (IOException exception) {
            HMOSLogUtil.error(L.TAG, "loadFontFromFile -> IOException : "+exception.getLocalizedMessage());
        }
        return Optional.of(new Font.Builder(file).setWeight(style).build());
    }

    /**
     * check if the input string is empty
     *
     * @param input the input strings
     * @return the input string is empty
     */
    public static boolean isEmpty(String... input) {
        if (input == null) {
            return true;
        }
        for (String oneItem : input) {
            if (isEmpty(oneItem)) {
                return true;
            }
        }
        return false;
    }
}
