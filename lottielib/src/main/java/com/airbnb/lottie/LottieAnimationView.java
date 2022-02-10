package com.airbnb.lottie;

import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.parser.moshi.JsonReader;
import com.airbnb.lottie.utils.HMOSLogUtil;
import com.airbnb.lottie.value.SimpleLottieValueCallback;
import com.airbnb.lottie.utils.Utils;
import com.airbnb.lottie.value.LottieAnimationViewData;
import com.airbnb.lottie.value.LottieFrameInfo;
import com.airbnb.lottie.value.LottieValueCallback;

import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorValue;
import ohos.agp.components.AttrSet;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.element.Element;
import ohos.agp.components.element.PixelMapElement;
import ohos.agp.render.Canvas;
import ohos.agp.render.ColorFilter;
import ohos.agp.utils.Color;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This view will load, deserialize, and display an After Effects animation exported with
 * bodymovin (https://github.com/bodymovin/bodymovin).
 * <p>
 * You may set the animation in one of two ways:
 * 1) Attrs: {@link R.styleable#LottieAnimationView_lottie_fileName}
 * 2) Programmatically:
 * {@link #setAnimation(String)}
 * {@link #setAnimation(JsonReader, String)}
 * {@link #setAnimationFromJson(String, String)}
 * {@link #setAnimationFromUrl(String)}
 * {@link #setComposition(LottieComposition)}
 * <p>
 * You can set a default cache strategy with {@link R.attr#lottie_cacheStrategy}.
 * <p>
 * You can manually set the progress of the animation with {@link #setProgress(float)} or
 * {@link R.attr#lottie_progress}
 *
 * @see <a href="http://airbnb.io/lottie">Full Documentation</a>
 */
public class LottieAnimationView extends Image implements Component.BindStateChangedListener,
        Component.ComponentStateChangedListener, Component.DrawTask {

    private static final String TAG = LottieAnimationView.class.getSimpleName();

    private static final LottieListener<Throwable> DEFAULT_FAILURE_LISTENER = throwable -> {
        // By default, fail silently for network errors.
        if (Utils.isNetworkException(throwable)) {
            HMOSLogUtil.error(L.TAG, "Unable to load composition.", throwable);
            return;
        }
        throw new IllegalStateException("Unable to parse composition", throwable);
    };

    private final LottieListener<LottieComposition> loadedListener = composition -> setComposition(composition);

    private final LottieListener<Throwable> wrappedFailureListener = new LottieListener<Throwable>() {
        @Override
        public void onResult(Throwable result) {
            if (fallbackResource != 0) {
                // Element drawable = getContext().getResourceManager().getDrawable(fallbackResource);
                //From context get ResouceMangager
                ohos.global.resource.ResourceManager rm = getContext().getResourceManager();
                //Pass resrouceId and get Inputstream
                try {
                    InputStream is = (InputStream) rm.getResource(fallbackResource);
                    //Create options to decode bitmap
                    ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
                    Utils.resetOptions(decodingOpts);
                    PixelMap pixelmap = Utils.decodePixelMap(is, decodingOpts, null);
                    setImageElement(new PixelMapElement(pixelmap));
                } catch (IOException | NotExistException e) {
                    HMOSLogUtil.error(L.TAG, "Exception :" + e.getLocalizedMessage());
                }
            }
            LottieListener<Throwable> l = failureListener == null ? DEFAULT_FAILURE_LISTENER : failureListener;
            l.onResult(result);
        }
    };


    private LottieListener<Throwable> failureListener;


    private int fallbackResource = 0;

    private final LottieDrawable lottieDrawable = new LottieDrawable();

    private boolean isInitialized;

    private String animationName;

    private int animationResId;

    private boolean playAnimationWhenShown = false;

    private boolean wasAnimatingWhenNotShown = false;

    private boolean wasAnimatingWhenDetached = false;
    /**
     * When we set a new composition, we set LottieDrawable to null then back again so that ImageView re-checks its bounds.
     * However, this causes the drawable to get unscheduled briefly. Normally, we would pause the animation but in this case, we don't want to.
     */
    private boolean ignoreUnschedule = false;

    private boolean autoPlay = false;

    private boolean cacheComposition = true;

    private RenderMode renderMode = RenderMode.AUTOMATIC;

    private final Set<LottieOnCompositionLoadedListener> lottieOnCompositionLoadedListeners = new HashSet<>();

    /**
     * Prevents a StackOverflowException on 4.4 in which getDrawingCache() calls buildDrawingCache().
     * This isn't a great solution but it works and has very little performance overhead.
     * At some point in the future, the original goal of falling back to hardware rendering when
     * the animation is set to software rendering but it is too large to fit in a software bitmap
     * should be reevaluated.
     */
    private int buildDrawingCacheDepth = 0;


    private LottieTask<LottieComposition> compositionTask;

    /**
     * Can be null because it is created async
     */

    private LottieComposition composition;

    private LottieAnimationViewData animationData;

    public LottieAnimationView(Context context) {
        super(context);
        init(null);
        setBindStateChangedListener(this);
    }

    public LottieAnimationView(Context context, AttrSet attrs) {
        super(context, attrs);
        //init(attrs);
        setBindStateChangedListener(this);
    }

    public LottieAnimationView(Context context, AttrSet attrs, String styleName) {
        super(context, attrs, styleName);
        //init(attrs);
        setBindStateChangedListener(this);
    }

    //TODO : TypedArray usage need to implement in HMOS
    private void init(LottieAnimationViewData attrs) {
        HMOSLogUtil.error(L.TAG, "Enter init");
        if (attrs.isHasRawRes() && attrs.isHasFileName()) {
            throw new IllegalArgumentException("rawRes and fileName cannot be used at " +
                    "the same time. Please use only one at once.");
        } else if (attrs.isHasRawRes()) {
            setAnimation(attrs.getResId());
        } else if (attrs.isHasFileName()) {
            setAnimation(attrs.getFileName());
        } else if (attrs.isHasUrl()) {
            setAnimationFromUrl(attrs.getUrl());
        } else {
            HMOSLogUtil.error(L.TAG, "No RawRes, No FileName & no URL");
        }

        //setFallbackResource(ta.getResourceId(R.styleable.LottieAnimationView_lottie_fallbackRes, 0)); //TODO: all commented code to be ported

        if (attrs.autoPlay) {
            wasAnimatingWhenDetached = true;
            autoPlay = true;
        }

        if (attrs.loop) {
            lottieDrawable.setRepeatCount(LottieDrawable.INFINITE);
        }
        setRepeatCount(attrs.getRepeatCount());
        /*if (ta.hasValue(R.styleable.LottieAnimationView_lottie_speed)) {
            setSpeed(ta.getFloat(R.styleable.LottieAnimationView_lottie_speed, 1f));
        }*/
        setSpeed(1f);

        setProgress(0/*ta.getFloat(R.styleable.LottieAnimationView_lottie_progress, 0)*/);
        SimpleColorFilter filter = new SimpleColorFilter(Color.TRANSPARENT.getValue());
        KeyPath keyPath = new KeyPath("**");
        LottieValueCallback<ColorFilter> callback = new LottieValueCallback<>(filter);
        addValueCallback(keyPath, LottieProperty.COLOR_FILTER, callback);
        HMOSLogUtil.error(L.TAG, "addValueCallback done");
        lottieDrawable.setScale(1f);

        int renderModeOrdinal = RenderMode.AUTOMATIC.ordinal();
        if (renderModeOrdinal >= RenderMode.values().length) {
            renderModeOrdinal = RenderMode.AUTOMATIC.ordinal();
        }
        setRenderMode(RenderMode.values()[renderModeOrdinal]);
        HMOSLogUtil.error(L.TAG, "set render modes done");

        if (getScaleMode() != null) {
            lottieDrawable.setScaleMode(getScaleMode());
        }

        lottieDrawable.setSystemAnimationsAreEnabled(Utils.getAnimationScale(getContext()) != 0f);
        isInitialized = true;
        addDrawTask(this);
        HMOSLogUtil.error(L.TAG, "init end");
    }

    @Override
    public void setPixelMap(int resId) {
        cancelLoaderTask();
        super.setPixelMap(resId);
    }

    @Override
    public void setImageElement(Element drawable) {
        cancelLoaderTask();
        super.setImageElement(drawable);
    }

    @Override
    public void setPixelMap(PixelMap pm) {
        cancelLoaderTask();
        super.setPixelMap(pm);
    }

   /* @Override public void unscheduleDrawable(Drawable who) {
        if (!ignoreUnschedule && who == lottieDrawable && lottieDrawable.isAnimating()) {
            pauseAnimation();
        } else if (!ignoreUnschedule && who instanceof LottieDrawable && ((LottieDrawable) who).isAnimating()) {
            ((LottieDrawable) who).pauseAnimation();
        }
        super.unscheduleDrawable(who);
    }*/

    @Override
    public void invalidate() {
        super.invalidate();
    }


    /**
     * If set to true, all future compositions that are set will be cached so that they don't need to be parsed
     * next time they are loaded. This won't apply to compositions that have already been loaded.
     * <p>
     * Defaults to true.
     * <p>
     * {@link R.attr#lottie_cacheComposition}
     *
     * @param cacheComposition takes cacheComposition of type boolean
     */
    public void setCacheComposition(boolean cacheComposition) {
        this.cacheComposition = cacheComposition;
    }

    /**
     * Sets the animation from a file in the raw directory.
     * This will load and deserialize the file asynchronously.
     *
     * @param rawRes takes rawRes to parse the json and set task.
     */
    public void setAnimation(final int rawRes) {
        animationResId = rawRes;
        animationName = null;
        setCompositionTask(fromRawRes(rawRes));
    }

    private LottieTask<LottieComposition> fromRawRes( final int rawRes) {
            return new LottieTask<>(new Callable<LottieResult<LottieComposition>>() {
                @Override public LottieResult<LottieComposition> call() throws Exception {
                    return cacheComposition
                            ? LottieCompositionFactory.fromRawResSync(getContext(), rawRes) : LottieCompositionFactory.fromRawResSync(getContext(), rawRes, null);
                }
            }, true);
        }

    public void setAnimation(final String assetName) {
        animationName = assetName;
        animationResId = 0;
        setCompositionTask(fromAssets(assetName));
    }

    private LottieTask<LottieComposition> fromAssets(final String assetName) {
            return new LottieTask<>(new Callable<LottieResult<LottieComposition>>() {
                @Override public LottieResult<LottieComposition> call() throws Exception {
                    return cacheComposition ?
                            LottieCompositionFactory.fromAssetSync(getContext(), assetName) : LottieCompositionFactory.fromAssetSync(getContext(), assetName, null);
                }
            }, true);
    }

    /**
     * set Animation from json String.
     *
     * @param jsonString takes json in string format
     * @see #setAnimationFromJson(String, String)
     */
    @Deprecated
    public void setAnimationFromJson(String jsonString) {
        setAnimationFromJson(jsonString, null);
    }

    /**
     * Sets the animation from json string. This is the ideal API to use when loading an animation
     * over the network because you can use the raw response body here and a conversion to a
     * JSONObject never has to be done.
     *
     * @param jsonString json in string format
     * @param cacheKey   cachekey
     */
    public void setAnimationFromJson(String jsonString, String cacheKey) {
        setAnimation(new ByteArrayInputStream(jsonString.getBytes()), cacheKey);
    }

    /**
     * Sets the animation from an arbitrary InputStream.
     * This will load and deserialize the file asynchronously.
     * <p>
     * This is particularly useful for animations loaded from the network. You can fetch the
     * bodymovin json from the network and pass it directly here.
     *
     * @param stream   Sets the animation from an arbitrary InputStream
     * @param cacheKey cachkey to cache the json
     */
    public void setAnimation(InputStream stream, String cacheKey) {
        setCompositionTask(LottieCompositionFactory.fromJsonInputStream(stream, cacheKey));
    }

    /**
     * Load a lottie animation from a url. The url can be a json file or a zip file. Use a zip file if you have images. Simply zip them together and lottie
     * will unzip and link the images automatically.
     * <p>
     * Under the hood, Lottie uses Java HttpURLConnection because it doesn't require any transitive networking dependencies. It will download the file
     * to the application cache under a temporary name. If the file successfully parses to a composition, it will rename the temporary file to one that
     * can be accessed immediately for subsequent requests. If the file does not parse to a composition, the temporary file will be deleted.
     *
     * @param url url of type json or zip file
     */
    public void setAnimationFromUrl(String url) {
        LottieTask<LottieComposition> task = cacheComposition
                ? LottieCompositionFactory.fromUrl(getContext(), url)
                : LottieCompositionFactory.fromUrl(getContext(), url, null);
        setCompositionTask(task);
    }

    /**
     * Load a lottie animation from a url. The url can be a json file or a zip file. Use a zip file if you have images. Simply zip them together and lottie
     * will unzip and link the images automatically.
     * <p>
     * Under the hood, Lottie uses Java HttpURLConnection because it doesn't require any transitive networking dependencies. It will download the file
     * to the application cache under a temporary name. If the file successfully parses to a composition, it will rename the temporary file to one that
     * can be accessed immediately for subsequent requests. If the file does not parse to a composition, the temporary file will be deleted.
     *
     * @param url      url of type json or zip file
     * @param cacheKey cachkey to cache the json
     */
    public void setAnimationFromUrl(String url, String cacheKey) {
        LottieTask<LottieComposition> task = LottieCompositionFactory.fromUrl(getContext(), url, cacheKey);
        setCompositionTask(task);
    }

    /**
     * Set a default failure listener that will be called if any of the setAnimation APIs fail for any reason.
     * This can be used to replace the default behavior.
     * <p>
     * The default behavior will log any network errors and rethrow all other exceptions.
     * <p>
     * If you are loading an animation from the network, errors may occur if your user has no internet.
     * You can use this listener to retry the download or you can have it default to an error drawable
     * with {@link #setFallbackResource(int)}.
     * <p>
     * Unless you are using {@link #setAnimationFromUrl(String)}, errors are unexpected.
     * <p>
     * Set the listener to null to revert to the default behavior.
     *
     * @param failureListener failure listener if any of the setAnimation APIs fail for any reason
     */
    public void setFailureListener(LottieListener<Throwable> failureListener) {
        this.failureListener = failureListener;
    }

    /**
     * Set a drawable that will be rendered if the LottieComposition fails to load for any reason.
     * Unless you are using {@link #setAnimationFromUrl(String)}, this is an unexpected error and
     * you should handle it with {@link #setFailureListener(LottieListener)}.
     * <p>
     * If this is a network animation, you may use this to show an error to the user or
     * you can use a failure listener to retry the download.
     *
     * @param fallbackResource takes fallbackResource.
     */
    public void setFallbackResource(int fallbackResource) {
        this.fallbackResource = fallbackResource;
    }

    private void setCompositionTask(LottieTask<LottieComposition> compositionTask) {
        clearComposition();
        cancelLoaderTask();
        this.compositionTask = compositionTask.addListener(loadedListener).addFailureListener(wrappedFailureListener);
    }

    private void cancelLoaderTask() {
        if (compositionTask != null) {
            compositionTask.removeListener(loadedListener);
            compositionTask.removeFailureListener(wrappedFailureListener);
        }
    }

    /**
     * Sets a composition.
     * You can set a default cache strategy if this view was inflated with xml by
     * using {@link R.attr#lottie_cacheStrategy}.
     *
     * @param composition composition to set
     */
    public void setComposition(LottieComposition composition) {
        if (L.DBG) {
            HMOSLogUtil.info(TAG, "Set Composition \n" + composition);
        }
        lottieDrawable.setCallback(this);

        this.composition = composition;
        ignoreUnschedule = true;
        boolean isNewComposition = lottieDrawable.setComposition(composition);
        ignoreUnschedule = false;
        // enableOrDisableHardwareLayer();
        if (getForegroundElement() == lottieDrawable && !isNewComposition) {
            // We can avoid re-setting the drawable, and invalidating the view, since the composition
            // hasn't changed.
            return;
        }

        // This is needed to makes sure that the animation is properly played/paused for the current visibility state.
        // It is possible that the drawable had a lazy composition task to play the animation but this view subsequently
        // became invisible. Comment this out and run the espresso tests to see a failing test.
        onComponentStateChanged(this, getVisibility());

//        requestLayout();

        for (LottieOnCompositionLoadedListener lottieOnCompositionLoadedListener : lottieOnCompositionLoadedListeners) {
            lottieOnCompositionLoadedListener.onCompositionLoaded(composition);
        }

    }

    /**
     * Get the composition.
     *
     * @return retuens composition.
     */
    public LottieComposition getComposition() {
        return composition;
    }

    /**
     * Returns whether or not any layers in this composition has masks.
     *
     * @return Returns whether or not any layers in this composition has masks
     */
    public boolean hasMasks() {
        return lottieDrawable.hasMasks();
    }

    /**
     * Returns whether or not any layers in this composition has a matte layer.
     *
     * @return Returns whether or not any layers in this composition has a matte layer
     */
    public boolean hasMatte() {
        return lottieDrawable.hasMatte();
    }

    /**
     * Plays the animation from the beginning. If speed is < 0, it will start at the end
     * and play towards the beginning
     */

    public void playAnimation() {
        if (isFocused()) {
            lottieDrawable.playAnimation();
            // enableOrDisableHardwareLayer();
        } else {
            playAnimationWhenShown = true;
        }
    }

    /**
     * Continues playing the animation from its current position. If speed < 0, it will play backwards
     * from the current position.
     */
    public void resumeAnimation() {
        if (isFocused()) {
            lottieDrawable.resumeAnimation();
            // enableOrDisableHardwareLayer();
        } else {
            playAnimationWhenShown = false;
            wasAnimatingWhenNotShown = true;
        }
    }

    /**
     * Sets the minimum frame that the animation will start from when playing or looping.
     *
     * @param startFrame Sets the minimum frame that the animation will start from when playing or looping
     */
    public void setMinFrame(int startFrame) {
        lottieDrawable.setMinFrame(startFrame);
    }

    /**
     * Returns the minimum frame set by {@link #setMinFrame(int)} or {@link #setMinProgress(float)}
     *
     * @return Returns the minimum frame.
     */
    public float getMinFrame() {
        return lottieDrawable.getMinFrame();
    }

    /**
     * Sets the minimum progress that the animation will start from when playing or looping.
     *
     * @param startProgress Sets the minimum progress that the animation will start from when playing or looping
     */
    public void setMinProgress(float startProgress) {
        lottieDrawable.setMinProgress(startProgress);
    }

    /**
     * Sets the maximum frame that the animation will end at when playing or looping.
     * @param endFrame max frame to set.
     */
    public void setMaxFrame(int endFrame) {
        lottieDrawable.setMaxFrame(endFrame);
    }

    /**
     * Returns the maximum frame set by {@link #setMaxFrame(int)} or {@link #setMaxProgress(float)}
     *
     * @return Returns the maximum frame set by {@link #setMaxFrame(int)} or {@link #setMaxProgress(float)}
     */
    public float getMaxFrame() {
        return lottieDrawable.getMaxFrame();
    }

    /**
     * Sets the maximum progress that the animation will end at when playing or looping.
     *
     * @param endProgress Sets the maximum progress that the animation will end at when playing or looping.
     */
    public void setMaxProgress(float endProgress) {
        lottieDrawable.setMaxProgress(endProgress);
    }

    /**
     * Sets the minimum frame to the start time of the specified marker.
     *
     * @param markerName Sets the minimum frame to the start time of the specified marker.
     * @throws IllegalArgumentException if the marker is not found.
     */
    public void setMinFrame(String markerName) {
        lottieDrawable.setMinFrame(markerName);
    }

    /**
     * Sets the maximum frame to the start time + duration of the specified marker.
     *
     * @param markerName Sets the maximum frame to the start time + duration of the specified marker.
     * @throws IllegalArgumentException if the marker is not found.
     */
    public void setMaxFrame(String markerName) {
        lottieDrawable.setMaxFrame(markerName);
    }

    /**
     * Sets the minimum and maximum frame to the start time and start time + duration
     * of the specified marker.
     *
     * @param markerName Sets the minimum and maximum frame
     * @throws IllegalArgumentException if the marker is not found.
     */
    public void setMinAndMaxFrame(String markerName) {
        lottieDrawable.setMinAndMaxFrame(markerName);
    }

    /**
     * Sets the minimum and maximum frame to the start marker start and the maximum frame to the end marker start.
     * playEndMarkerStartFrame determines whether or not to play the frame that the end marker is on. If the end marker
     * represents the end of the section that you want, it should be true. If the marker represents the beginning of the
     * next section, it should be false.
     *
     * @param endMarkerName           sets end marker name
     * @param playEndMarkerStartFrame playEndMarkerStartFrame determines whether or not to play the frame
     * @param startMarkerName         sets start marker name
     * @throws IllegalArgumentException if either marker is not found.
     */
    public void setMinAndMaxFrame(final String startMarkerName, final String endMarkerName,final boolean playEndMarkerStartFrame) {
        lottieDrawable.setMinAndMaxFrame(startMarkerName, endMarkerName, playEndMarkerStartFrame);
    }

    /**
     * Set Minimun and Maximun  frames.
     *
     * @param minFrame  Min frame in integer
     * @param maxFrame  Max frame in interger
     */
    public void setMinAndMaxFrame(int minFrame, int maxFrame) {
        lottieDrawable.setMinAndMaxFrame(minFrame, maxFrame);
    }

    /**
     * Set Minimun and Maximun  progress
     * @param maxProgress Max progress in float
     * @param minProgress Min progress in float.
     *
     */
    public void setMinAndMaxProgress(float minProgress,float maxProgress) {
        lottieDrawable.setMinAndMaxProgress(minProgress, maxProgress);
    }

    /**
     * Reverses the current animation speed. This does NOT play the animation.
     */
    public void reverseAnimationSpeed() {
        lottieDrawable.reverseAnimationSpeed();
    }

    /**
     * Sets the playback speed. If speed < 0, the animation will play backwards.
     *
     * @param speed pass speed to set in float.
     */
    public void setSpeed(float speed) {
        lottieDrawable.setSpeed(speed);
    }

    /**
     * Returns the current playback speed. This will be < 0 if the animation is playing backwards.
     *
     * @return It returns the current speed.
     */
    public float getSpeed() {
        return lottieDrawable.getSpeed();
    }

    public void addAnimatorUpdateListener(AnimatorValue.ValueUpdateListener updateListener) {
        lottieDrawable.addAnimatorUpdateListener(updateListener);
    }

    public void removeUpdateListener(AnimatorValue.ValueUpdateListener updateListener) {
        lottieDrawable.removeAnimatorUpdateListener(updateListener);
    }

    public void removeAllUpdateListeners() {
        lottieDrawable.removeAllUpdateListeners();
    }

    public void addAnimatorListener(Animator.StateChangedListener listener) {
        lottieDrawable.addAnimatorListener(listener);
    }

    public void removeAnimatorListener(Animator.StateChangedListener listener) {
        lottieDrawable.removeAnimatorListener(listener);
    }

    /**
     * Clears the Animation Listeners
     */
    public void removeAllAnimatorListeners() {
        lottieDrawable.removeAllAnimatorListeners();
    }

    public void addAnimatorPauseListener(Animator.StateChangedListener listener) {
        lottieDrawable.addAnimatorPauseListener(listener);
    }

    public void removeAnimatorPauseListener(Animator.StateChangedListener listener) {
        lottieDrawable.removeAnimatorPauseListener(listener);
    }

    /**
     * Sets how many times the animation should be repeated. If the repeat
     * count is 0, the animation is never repeated. If the repeat count is
     * greater than 0 or {@link LottieDrawable#INFINITE}, the repeat mode will be taken
     * into account. The repeat count is 0 by default.
     *
     * @param loop the number of times the animation should be repeated
     */
    @Deprecated
    public void loop(boolean loop) {
        lottieDrawable.setRepeatCount(loop ? AnimatorValue.INFINITE : 0);
    }

    /**
     * Defines what this animation should do when it reaches the end. This
     * setting is applied only when the repeat count is either greater than
     * 0 or {@link LottieDrawable#INFINITE}. Defaults to {@link LottieDrawable#RESTART}.
     *
     * @param mode {@link LottieDrawable#RESTART} or {@link LottieDrawable#REVERSE}
     */
    /*public void setRepeatMode(@LottieDrawable.RepeatMode int mode) {
        lottieHMOSDrawable.setRepeatMode(mode);
    }

    *//**
     * Defines what this animation should do when it reaches the end.
     *
     * @return either one of {@link LottieDrawable#REVERSE} or {@link LottieDrawable#RESTART}
     *//*
    @LottieDrawable.RepeatMode
    public int getRepeatMode() {
        return lottieHMOSDrawable.getRepeatMode();
    }*/

    /**
     * Sets how many times the animation should be repeated. If the repeat
     * count is 0, the animation is never repeated. If the repeat count is
     * greater than 0 or {@link LottieDrawable#INFINITE}, the repeat mode will be taken
     * into account. The repeat count is 0 by default.
     *
     * @param count the number of times the animation should be repeated
     */
    public void setRepeatCount(int count) {
        lottieDrawable.setRepeatCount(count);
    }

    /**
     * Defines how many times the animation should repeat. The default value
     * is 0.
     *
     * @return the number of times the animation should repeat, or {@link LottieDrawable#INFINITE}
     */
    public int getRepeatCount() {
        return lottieDrawable.getRepeatCount();
    }

    /**
     * Check is Animation getting played.
     *
     * @return tue id playing else false.
     */
    public boolean isAnimating() {
        return lottieDrawable.isAnimating();
    }

    /**
     * If you use image assets, you must explicitly specify the folder in assets/ in which they are
     * located because bodymovin uses the name filenames across all compositions (img_#).
     * Do NOT rename the images themselves.
     * <p>
     * If your images are located in src/main/assets/airbnb_loader/ then call
     * `setImageAssetsFolder("airbnb_loader/");`.
     * <p>
     * Be wary if you are using many images, however. Lottie is designed to work with vector shapes
     * from After Effects. If your images look like they could be represented with vector shapes,
     * see if it is possible to convert them to shape layers and re-export your animation. Check
     * the documentation at http://airbnb.io/lottie for more information about importing shapes from
     * Sketch or Illustrator to avoid this.
     *
     * @param imageAssetsFolder pass exact name as asset folder
     */
    public void setImageAssetsFolder(String imageAssetsFolder) {
        lottieDrawable.setImagesAssetsFolder(imageAssetsFolder);
    }

    /**
     * Returns the image assets folder name
     *
     * @return Returns the image assets folder name
     */
    public String getImageAssetsFolder() {
        return lottieDrawable.getImageAssetsFolder();
    }

    /**
     * Allows you to modify or clear a bitmap that was loaded for an image either automatically
     * through {@link #setImageAssetsFolder(String)} or with an {@link ImageAssetDelegate}.
     *
     * @param bitmap to update with
     * @param id     of the bitmap
     * @return the previous Bitmap or null.
     */

    public PixelMap updateBitmap(String id, PixelMap bitmap) {
        return lottieDrawable.updatePixelmap(id, bitmap);
    }

    /**
     * Use this if you can't bundle images with your app. This may be useful if you download the
     * animations from the network or have the images saved to an SD Card. In that case, Lottie
     * will defer the loading of the bitmap to this delegate.
     * <p>
     * Be wary if you are using many images, however. Lottie is designed to work with vector shapes
     * from After Effects. If your images look like they could be represented with vector shapes,
     * see if it is possible to convert them to shape layers and re-export your animation. Check
     * the documentation at http://airbnb.io/lottie for more information about importing shapes from
     * Sketch or Illustrator to avoid this.
     * @param assetDelegate name to pass
     */
    public void setImageAssetDelegate(ImageAssetDelegate assetDelegate) {
        lottieDrawable.setImageAssetDelegate(assetDelegate);
    }

    /**
     * Use this to manually set fonts.
     * @param assetDelegate manually set fonts
     */
    public void setFontAssetDelegate(FontAssetDelegate assetDelegate) {
        lottieDrawable.setFontAssetDelegate(assetDelegate);
    }

    /**
     * Set this to replace animation text with custom text at runtime
     * @param textDelegate name to set
     */
    public void setTextDelegate(TextDelegate textDelegate) {
        lottieDrawable.setTextDelegate(textDelegate);
    }

    /**
     * Takes a {@link KeyPath}, potentially with wildcards or globstars and resolve it to a list of
     * zero or more actual {@link KeyPath Keypaths} that exist in the current animation.
     * <p>
     * If you want to set value callbacks for any of these values, it is recommended to use the
     * returned {@link KeyPath} objects because they will be internally resolved to their content
     * and won't trigger a tree walk of the animation contents when applied.
     * @param keyPath Takes a keypath
     * @return {@link KeyPath} objects
     */
    public List<KeyPath> resolveKeyPath(KeyPath keyPath) {
        return lottieDrawable.resolveKeyPath(keyPath);
    }

    /**
     * Add a property callback for the specified {@link KeyPath}. This {@link KeyPath} can resolve
     * to multiple contents. In that case, the callback's value will apply to all of them.
     * <p>
     * Internally, this will check if the {@link KeyPath} has already been resolved with
     * {@link #resolveKeyPath(KeyPath)} and will resolve it if it hasn't.
     * @param keyPath Add a property callback for the specified keypath
     * @param callback of LottieValueCallback
     * @param property to add
     */
    public <T> void addValueCallback(KeyPath keyPath, T property, LottieValueCallback<T> callback) {
        lottieDrawable.addValueCallback(keyPath, property, callback);
    }

    /**
     * Overload of {@link #addValueCallback(KeyPath, Object, LottieValueCallback)} that takes an interface. This allows you to use a single abstract
     * method code block in Kotlin such as:
     * animationView.addValueCallback(yourKeyPath, LottieProperty.COLOR) { yourColor }
     * @param keyPath Add a property callback for the specified keypath
     * @param callback of LottieValueCallback
     * @param property to add
     * @param <T> generic type
     */
    public <T> void addValueCallback(KeyPath keyPath, T property, final SimpleLottieValueCallback<T> callback) {
        lottieDrawable.addValueCallback(keyPath, property, new LottieValueCallback<T>() {
            @Override
            public T getValue(LottieFrameInfo<T> frameInfo) {
                return callback.getValue(frameInfo);
            }
        });
    }

    /**
     * Set the scale on the current composition. The only cost of this function is re-rendering the
     * current frame so you may call it frequent to scale something up or down.
     * <p>
     * The smaller the animation is, the better the performance will be. You may find that scaling an
     * animation down then rendering it in a larger ImageView and letting ImageView scale it back up
     * with a scaleType such as centerInside will yield better performance with little perceivable
     * quality loss.
     * <p>
     * You can also use a fixed view width/height in conjunction with the normal ImageView
     * scaleTypes centerCrop and centerInside.
     *
     * @param scale Set the scale on the current composition
     */
    public void setScale(float scale) {
        lottieDrawable.setScale(scale);
        if (getForegroundElement() == lottieDrawable) {
            setLottieDrawable();
        }
    }

    /**
     * Get the scale of current composition.
     *
     * @return the current scale
     */
    public float getLottieScale() {
        return lottieDrawable.getScale();
    }


    public void cancelAnimation() {
        wasAnimatingWhenDetached = false;
        wasAnimatingWhenNotShown = false;
        playAnimationWhenShown = false;
        lottieDrawable.cancelAnimation();
        // enableOrDisableHardwareLayer();
    }


    public void pauseAnimation() {
        autoPlay = false;
        wasAnimatingWhenDetached = false;
        wasAnimatingWhenNotShown = false;
        playAnimationWhenShown = false;
        lottieDrawable.pauseAnimation();
        // enableOrDisableHardwareLayer();
    }

    /**
     * Sets the progress to the specified frame.
     * If the composition isn't set yet, the progress will be set to the frame when
     * it is.
     * @param frame to set in int
     */
    public void setFrame(int frame) {
        lottieDrawable.setFrame(frame);
    }

    /**
     * Get the currently rendered frame.
     * @return current frame
     */
    public int getFrame() {
        return lottieDrawable.getFrame();
    }

    public void setProgress(float progress) {
        lottieDrawable.setProgress(progress);
    }

    //@FloatRange(from = 0.0f, to = 1.0f)
    public float getProgress() {
        return lottieDrawable.getProgress();
    }

    public long getDuration() {
        return composition != null ? (long) composition.getDuration() : 0;
    }

    public void setPerformanceTrackingEnabled(boolean enabled) {
        lottieDrawable.setPerformanceTrackingEnabled(enabled);
    }


    public PerformanceTracker getPerformanceTracker() {
        return lottieDrawable.getPerformanceTracker();
    }

    private void clearComposition() {
        composition = null;
        lottieDrawable.clearComposition();
    }

    /**
     * If you are experiencing a device specific crash that happens during drawing, you can set this to true
     * for those devices. If set to true, draw will be wrapped with a try/catch which will cause Lottie to
     * render an empty frame rather than crash your app.
     * <p>
     * Ideally, you will never need this and the vast majority of apps and animations won't. However, you may use
     * this for very specific cases if absolutely necessary.
     * <p>
     * There is no XML attr for this because it should be set programmatically and only for specific devices that
     * are known to be problematic.
     * @param safeMode set to true, draw will be wrapped with a try/catch
     */
    public void setSafeMode(boolean safeMode) {
        lottieDrawable.setSafeMode(safeMode);
    }

    //TODO - Forces the drawing cache to be built if the drawing cache is invalid.
    // Currently HMOS doesn't support it
    /**
     * If rendering via software, Harmoney will fail to generate a bitmap if the view is too large. Rather than displaying
     * nothing, fallback on hardware acceleration which may incur a performance hit.
     *
     * @see #setRenderMode(RenderMode)
     * @see LottieDrawable#draw(Canvas)
     */
    /*@Override
    public void buildDrawingCache(boolean autoScale) {
        HiTraceId id = L.beginSection("buildDrawingCache");
        buildDrawingCacheDepth++;
        super.buildDrawingCache(autoScale);
        if (buildDrawingCacheDepth == 1 && getWidth() > 0 && getHeight() > 0 && getLayerType() == LAYER_TYPE_SOFTWARE
            && getDrawingCache(autoScale) == null) {
            setRenderMode(HARDWARE);
        }
        buildDrawingCacheDepth--;
        L.endSection(id);
    }*/

    /**
     * Call this to set whether or not to render with hardware or software acceleration.
     * Lottie defaults to Automatic which will use hardware acceleration unless:
     * 1) There are dash paths and the device is pre-Pie.
     * 2) There are more than 4 masks and mattes and the device is pre-Pie.
     * Hardware acceleration is generally faster for those devices unless
     * there are many large mattes and masks in which case there is a ton
     * of GPU uploadTexture thrashing which makes it much slower.
     * <p>
     * In most cases, hardware rendering will be faster, even if you have mattes and masks.
     * However, if you have multiple mattes and masks (especially large ones) then you
     * should test both render modes. You should also test on pre-Pie and Pie+ devices
     * because the underlying rendering enginge changed significantly.
     * @param renderMode set rendermode
     */
    public void setRenderMode(RenderMode renderMode) {
        this.renderMode = renderMode;
        // enableOrDisableHardwareLayer();
    }

    /**
     * Sets whether to apply opacity to the each layer instead of shape.
     * <p>
     * Opacity is normally applied directly to a shape. In cases where translucent shapes overlap, applying opacity to a layer will be more accurate
     * at the expense of performance.
     * <p>
     * The default value is false.
     * <p>
     * Note: This process is very expensive. The performance impact will be reduced when hardware acceleration is enabled.
     *
     * @see #setRenderMode(RenderMode)
     * @param isApplyingOpacityToLayersEnabled Sets whether to apply opacity to the each layer instead of shape.
     */
    public void setApplyingOpacityToLayersEnabled(boolean isApplyingOpacityToLayersEnabled) {
        lottieDrawable.setApplyingOpacityToLayersEnabled(isApplyingOpacityToLayersEnabled);
    }
    //TODO : scaleType FitXY mode is not supported in HMOS

    /**
     * Disable the extraScale mode in {@link #drawToCanvas(Canvas)} function when scaleType is FitXY. It doesn't affect the rendering with other scaleTypes.
     *
     * <p>When there are 2 animation layout side by side, the default extra scale mode might leave 1 pixel not drawn between 2 animation, and
     * disabling the extraScale mode can fix this problem</p>
     *
     * <b>Attention:</b> Disable the extra scale mode can downgrade the performance and may lead to larger memory footprint. Please only disable this
     * mode when using animation with a reasonable dimension (smaller than screen size).
     *
     * @see LottieDrawable#drawWithNewAspectRatio(Canvas)
     */
    /*public void disableExtraScaleModeInFitXY() {
        lottieHMOSDrawable.disableExtraScaleModeInFitXY();
    }*/
    @Override
    public void onComponentStateChanged(Component component, int i) {
        if (!isInitialized) {
            return;
        }
        if (isFocused()) {
            if (wasAnimatingWhenNotShown) {
                resumeAnimation();
            } else if (playAnimationWhenShown) {
                playAnimation();
            }
            wasAnimatingWhenNotShown = false;
            playAnimationWhenShown = false;
        } else {
            if (isAnimating()) {
                pauseAnimation();
                wasAnimatingWhenNotShown = true;
            }
        }
    }

    @Override
    public void onComponentBoundToWindow(Component component) {
        if (autoPlay || wasAnimatingWhenDetached) {
            playAnimation();
            // Autoplay from xml should only apply once.
            autoPlay = false;
            wasAnimatingWhenDetached = false;
        }
    }

    @Override
    public void onComponentUnboundFromWindow(Component component) {
        if (isAnimating()) {
            cancelAnimation();
            wasAnimatingWhenDetached = true;
        }
    }

    public void setAnimationData(LottieAnimationViewData data) {
        animationData = data;
        init(animationData);
    }

    public String toString() {
        return "LottieAnimationView{";
    }

    @Override
    public void onDraw(Component component, Canvas canvas) {
        lottieDrawable.drawToCanvas(canvas);
        //lottieDrawable.invalidateSelf();
        //addDrawTask(this);
    }

    private void setLottieDrawable() {
        boolean wasAnimating = isAnimating();
        // Set the drawable to null first because the underlying LottieDrawable's intrinsic bounds can change
        // if the composition changes.
        setImageElement(null);
        setImageElement(lottieDrawable);
        if (wasAnimating) {
            // This is necessary because lottieDrawable will get unscheduled and canceled when the drawable is set to null.
            lottieDrawable.resumeAnimation();
        }
    }

}
