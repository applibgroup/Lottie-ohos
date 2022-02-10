package com.airbnb.lottie.manager;

import com.airbnb.lottie.ImageAssetDelegate;
import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieImageAsset;
import com.airbnb.lottie.utils.HMOSLogUtil;

import ohos.app.Context;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.PixelFormat;
import ohos.media.image.common.Rect;
import ohos.media.image.common.Size;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ImageAssetManager {
    private static final Object bitmapHashLock = new Object();

    private final Context context;

    private String imagesFolder;

    @Nullable private ImageAssetDelegate delegate;

    private final Map<String, LottieImageAsset> imageAssets;

    public ImageAssetManager(Context context, String imagesFolder, ImageAssetDelegate delegate,
        Map<String, LottieImageAsset> imageAssets) {
        this.imagesFolder = imagesFolder;
        if (!(imagesFolder == null || imagesFolder.length() == 0)
            && this.imagesFolder.charAt(this.imagesFolder.length() - 1) != '/') {
            this.imagesFolder += '/';
        }

        this.context = context;
        this.imageAssets = imageAssets;
        setDelegate(delegate);
    }

    public void setDelegate(@Nullable ImageAssetDelegate assetDelegate) {
        delegate = assetDelegate;
    }

    /**
     * Returns the previously set bitmap or null.
     * @param id bitmap id
     * @param bitmap previously set bitmap
     * @return Returns the previously set bitmap or null
     */
    @Nullable public PixelMap updateBitmap(String id, @Nullable PixelMap bitmap) {
        if (bitmap == null) {
            LottieImageAsset asset = imageAssets.get(id);
            PixelMap ret = asset.getPixelmap();
            asset.setPixelMap(null);
            return ret;
        }
        PixelMap prevBitmap = imageAssets.get(id).getPixelmap();
        putPixelMap(id, bitmap);
        return prevBitmap;
    }

    @Nullable public PixelMap bitmapForId(String id) {
        LottieImageAsset asset = imageAssets.get(id);
        if (asset == null) {
            return null;
        }
        PixelMap pixelmap = asset.getPixelmap();
        if (pixelmap != null) {
            return pixelmap;
        }

        if (delegate != null) {
            pixelmap = delegate.fetchPixelmap(asset);
            if (pixelmap != null) {
                putPixelMap(id, pixelmap);
            }
            return pixelmap;
        }

        String filename = asset.getFileName();
        ImageSource.SourceOptions srcOpts = new ImageSource.SourceOptions();
        // ImageSource.DecodingOptions opts = new ImageSource.DecodingOptions();
        // opts.inScaled = true;
        // opts.inDensity = 160;

        java.util.Base64.Decoder decoder = java.util.Base64.getDecoder();
        if (filename.startsWith("data:") && filename.indexOf("base64,") > 0) {
            // Contents look like a base64 data URI, with the format data:image/png;base64,<data>.
            byte[] data;
            try {
                data = decoder.decode(filename.substring(filename.indexOf(',') + 1));
            } catch (IllegalArgumentException e) {
                HMOSLogUtil.error(L.TAG, "data URL did not have correct base64 format.", e);
                return null;
            }
            ImageSource imageSource = ImageSource.create(data, srcOpts);
            ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
            decodingOpts.desiredSize = new Size(0, 0);
            decodingOpts.desiredRegion = new Rect(0, 0, 0, 0);
            decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;//RGBA_8888;
            PixelMap pixelMap = imageSource.createPixelmap(decodingOpts);
            // bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            return putPixelMap(id, pixelmap);
        }

        InputStream is;
        try {
            if (imagesFolder == null || imagesFolder.length() == 0) {
                throw new IllegalStateException("You must set an images folder before loading an image."
                    + " Set it with LottieComposition#setImagesFolder or LottieDrawable#setImagesFolder");
            }
            // is = context.getResourceManager().getAssetManager().getAsset(imagesFolder + filename);
            is = context.getResourceManager().getRawFileEntry(imagesFolder + filename).openRawFile();
        } catch (IOException e) {
            HMOSLogUtil.warn(L.TAG, "Unable to open asset -" + e.getLocalizedMessage());
            return null;
        }
        ImageSource imageSource = ImageSource.create(is, srcOpts);
        ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
        decodingOpts.desiredSize = new Size(0, 0);
        decodingOpts.desiredRegion = new Rect(0, 0, 0, 0);
        decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;//RGBA_8888;
        PixelMap pixelMap = imageSource.createPixelmap(decodingOpts);
        return putPixelMap(id, pixelmap);
    }

    public boolean hasSameContext(Context context) {
        return context == null && this.context == null || this.context.equals(context);
    }

    private PixelMap putPixelMap(String key, @Nullable PixelMap pixelmap) {
        synchronized (bitmapHashLock) {
            imageAssets.get(key).setPixelMap(pixelmap);
            return pixelmap;
        }
    }
}
