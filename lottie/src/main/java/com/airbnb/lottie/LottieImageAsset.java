package com.airbnb.lottie;

import ohos.media.image.PixelMap;

/**
 * Data class describing an image asset exported by bodymovin.
 */
public class LottieImageAsset {
    private final int width;

    private final int height;

    private final String id;

    private final String fileName;

    private final String dirName;

    /**
     * Pre-set a pixelmap for this asset
     */
    private PixelMap pixelmap;

    public LottieImageAsset(int width, int height, String id, String fileName, String dirName) {
        this.width = width;
        this.height = height;
        this.id = id;
        this.fileName = fileName;
        this.dirName = dirName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDirName() {
        return dirName;
    }

    /**
     * Returns the pixelmap that has been stored for this image asset if one was explicitly set.
     * @return (maxScaleX, maxScaleY)
     */
    public PixelMap getPixelmap() {
        return pixelmap;
    }

    public void setPixelMap(PixelMap pixelmap) {
        this.pixelmap = pixelmap;
    }

    /**
     * Returns whether this asset has an embedded pixelmap or whether the fileName is a base64 encoded bitmap.
     * @return  pixelmap
     */
    public boolean hasBitmap() {
        return pixelmap != null || (fileName.startsWith("data:") && fileName.indexOf("base64,") > -1);
    }

}
