package com.airbnb.lottie;

import com.airbnb.lottie.manager.FontAssetManager;
import com.airbnb.lottie.manager.ImageAssetManager;
import com.airbnb.lottie.model.KeyPath;
import com.airbnb.lottie.model.Marker;
import com.airbnb.lottie.model.layer.CompositionLayer;
import com.airbnb.lottie.parser.LayerParser;
import com.airbnb.lottie.utils.HMOSLogUtil;
import com.airbnb.lottie.utils.LottieValueAnimator;
import com.airbnb.lottie.utils.MiscUtils;
import com.airbnb.lottie.value.LottieFrameInfo;
import com.airbnb.lottie.value.LottieValueCallback;
import com.airbnb.lottie.value.SimpleLottieValueCallback;

import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorValue;
import ohos.agp.components.Component;
import ohos.agp.components.Image;
import ohos.agp.components.element.*;
import ohos.agp.render.Canvas;
import ohos.agp.render.ColorFilter;
import ohos.agp.text.Font;
import ohos.agp.utils.Matrix;
import ohos.agp.utils.Rect;
import ohos.media.image.PixelMap;
import ohos.media.image.common.PixelFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This can be used to show an lottie animation in any place that would normally take a drawable.
 *
 * @see <a href="http://airbnb.io/lottie">Full Documentation</a>
 */
public class LottieDrawable extends ElementContainer {
    private static final String TAG = LottieDrawable.class.getSimpleName();
    private static final String MARKERWITHNAME = "Cannot find marker with name ";

    private interface LazyCompositionTask {
        void run(LottieComposition composition);
    }

    private final Matrix matrix = new Matrix();

    private LottieComposition composition;

    private final LottieValueAnimator animator = new LottieValueAnimator();

    private float scale = 3f;

    private boolean systemAnimationsEnabled = true;
    private boolean ignoreSystemAnimationsDisabled = false;

    private boolean safeMode = false;

    private final ArrayList<LazyCompositionTask> lazyCompositionTasks = new ArrayList<>();

    private LottieAnimationView cbImage = null;

    private final AnimatorValue.ValueUpdateListener progressUpdateListener = new AnimatorValue.ValueUpdateListener() {
        @Override
        public void onUpdate(AnimatorValue var1, float var2) {
            if (compositionLayer != null) {
                compositionLayer.setProgress(animator.getAnimatedValueAbsolute());
            }
        }
    };
    @Nullable
    private Image.ScaleMode scaleType;
    @Nullable
    private ImageAssetManager imageAssetManager;
    @Nullable
    private String imageAssetsFolder;
    @Nullable
    private ImageAssetDelegate imageAssetDelegate;
    @Nullable
    private FontAssetManager fontAssetManager;
    @Nullable
    FontAssetDelegate fontAssetDelegate;
    @Nullable
    TextDelegate textDelegate;
    @Nullable
    private boolean enableMergePaths;
    @Nullable
    private CompositionLayer compositionLayer;

    private int alpha = 255;

    private boolean performanceTrackingEnabled;
    private boolean outlineMasksAndMattes;

    private boolean isApplyingOpacityToLayersEnabled;

    private boolean isExtraScaleEnabled = true;

    /**
     * True if the drawable has not been drawn since the last invalidateSelf.
     * We can do this to prevent things like bounds from getting recalculated
     * many times.
     */
    private boolean isDirty = false;

    //@IntDef( {RESTART, REVERSE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RepeatMode { }

    /**
     * When the animation reaches the end and <code>repeatCount</code> is INFINITE
     * or a positive value, the animation restarts from the beginning.
     */
    public static final int RESTART = 1;//ValueAnimator.RESTART;

    /**
     * When the animation reaches the end and <code>repeatCount</code> is INFINITE
     * or a positive value, the animation reverses direction on every iteration.
     */
    public static final int REVERSE = 2;//ValueAnimator.REVERSE;

    /**
     * This value used used with the {@link #setRepeatCount(int)} property to repeat
     * the animation indefinitely.
     */
    public static final int INFINITE = AnimatorValue.INFINITE;

    public LottieDrawable() {
        animator.addUpdateListener(progressUpdateListener);
    }

    /**
     * Returns whether or not any layers in this composition has masks.
     * @return Returns whether or not any layers in this composition has masks
     */
    public boolean hasMasks() {
        return compositionLayer != null && compositionLayer.hasMasks();
    }

    /**
     * Returns whether or not any layers in this composition has a matte layer.
     * @return Returns whether or not any layers in this composition has a matte layer.
     */
    public boolean hasMatte() {
        return compositionLayer != null && compositionLayer.hasMatte();
    }

    private final CopyOnWriteArrayList<CallbackWeakReference> mCallbacks = new CopyOnWriteArrayList<>();


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
     * @param imageAssetsFolder If your images are located in src/main/assets/airbnb_loader/
     */
    public void setImagesAssetsFolder( String imageAssetsFolder) {
        this.imageAssetsFolder = imageAssetsFolder;
    }

    /**
     * Returns current imageAssetsFolder
     * @return imageAssetsFolder
     */
    @Nullable
    public String getImageAssetsFolder() {
        return imageAssetsFolder;
    }

    /**
     * Create a composition with {@link LottieCompositionFactory}
     * @param composition Create a composition
     * @return True if the composition is different from the previously set composition, false otherwise.
     */
    public boolean setComposition(LottieComposition composition) {
        if (this.composition == composition) {
            return false;
        }

        isDirty = false;
        clearComposition();
        this.composition = composition;
        buildCompositionLayer();
        animator.setComposition(composition);
        setProgress(animator.getAnimatedFraction());
        setScale(scale);

        // We copy the tasks to a new ArrayList so that if this method is called from multiple threads,
        // then there won't be two iterators iterating and removing at the same time.
        Iterator<LazyCompositionTask> it = new ArrayList<>(lazyCompositionTasks).iterator();
        while (it.hasNext()) {
            LazyCompositionTask t = it.next();
            if (t != null) {
                t.run(composition);
            }
            it.remove();
        }
        lazyCompositionTasks.clear();

        composition.setPerfTrackingEnabled(performanceTrackingEnabled);

        // Ensure that ImageView updates the drawable width/height so it can
        // properly calculate its drawable matrix.
        LottieAnimationView callback = getLottieCallback();
        if (callback instanceof Image) {
            ((Image) callback).setImageElement(null);
            ((Image) callback).setImageElement(this);
        }

        return true;
    }

    private LottieAnimationView getLottieCallback() {
        return cbImage;
    }

    public void setPerformanceTrackingEnabled(boolean enabled) {
        performanceTrackingEnabled = enabled;
        if (composition != null) {
            composition.setPerfTrackingEnabled(enabled);
        }
    }

    /**
     * Enable this to debug slow animations by outlining masks and mattes. The performance overhead of the masks and mattes will
     * be proportional to the surface area of all of the masks/mattes combined.
     * <p>
     * DO NOT leave this enabled in production.
     * @param outline boolean
     */
    public void setOutlineMasksAndMattes(boolean outline) {
        if (outlineMasksAndMattes == outline) {
            return;
        }
        outlineMasksAndMattes = outline;
        if (compositionLayer != null) {
            compositionLayer.setOutlineMasksAndMattes(outline);
        }
    }

    public final void setCallback(LottieAnimationView cb)
    {
        cbImage = cb;
    }

    /**
     * if composition is not null returns composition.getPerfTracker() else null
     * @return if composition is not null returns composition.getPerfTracker() else null
     */
    @Nullable
    public PerformanceTracker getPerformanceTracker() {
        if (composition != null) {
            return composition.getPerfTracker();
        }
        return null;
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
     * @see LottieAnimationView#setRenderMode(RenderMode)
     * @param isApplyingOpacityToLayersEnabled boolean value
     */
    public void setApplyingOpacityToLayersEnabled(boolean isApplyingOpacityToLayersEnabled) {
        this.isApplyingOpacityToLayersEnabled = isApplyingOpacityToLayersEnabled;
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
     * @see #drawWithNewAspectRatio(Canvas)
     */
    /*public void disableExtraScaleModeInFitXY() {
        isExtraScaleEnabled = false;
    }*/

    public boolean isApplyingOpacityToLayersEnabled() {
        return isApplyingOpacityToLayersEnabled;
    }

    private void buildCompositionLayer() {
        compositionLayer = new CompositionLayer(this, LayerParser.parse(composition), composition.getLayers(),
            composition);
        if (outlineMasksAndMattes) {
            compositionLayer.setOutlineMasksAndMattes(true);
        }
    }


    public void clearComposition() {
        if (animator.isRunning()) {
            animator.cancel();
        }
        composition = null;
        compositionLayer = null;
        imageAssetManager = null;
        animator.clearComposition();
        invalidateSelf();
    }

    /**
     * If you are experiencing a device specific crash that happens during drawing, you can set this to true
     * for those devices. If set to true, draw will be wrapped with a try/catch which will cause Lottie to
     * render an empty frame rather than crash your app.
     * <p>
     * Ideally, you will never need this and the vast majority of apps and animations won't. However, you may use
     * this for very specific cases if absolutely necessary.
     * @param safeMode boolean value
     */
    public void setSafeMode(boolean safeMode) {
        this.safeMode = safeMode;
    }

    public void invalidateSelf() {
        if (isDirty) {
            return;
        }
        isDirty = true;
        final Component callback = getLottieCallback();
        if (callback != null) {
            callback.invalidate();
        }
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    /*@Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        HMOSLogUtil.warn(L.TAG, "Use addColorFilter instead.");
    }*/

    public int getOpacity() {
        return PixelFormat.ARGB_8888.getValue();
    }

    @Override
    public void drawToCanvas(@NotNull Canvas canvas) {
        isDirty = false;
//        HiTraceId id = L.beginSection("Drawable#draw");

        if (safeMode) {
            try {
                drawInternal(canvas);
            } catch (Throwable e) {
                HMOSLogUtil.error(L.TAG, "Lottie crashed in draw!", e);
            }
        } else {
            drawInternal(canvas);
        }

//        L.endSection(id);
    }

    private void drawInternal(@NotNull Canvas canvas) {
        //TODO : FITXY support not provided in HMOS
        if (Image.ScaleMode.CLIP_CENTER == scaleType) {
            drawWithNewAspectRatio(canvas);
        } else {
            drawWithOriginalAspectRatio(canvas);
        }
    }

    private boolean boundsMatchesCompositionAspectRatio() {
        LottieComposition composition = this.composition;
        if (composition == null || getBounds().isEmpty()) {
            return true;
        }
        return aspectRatio(getBounds()) == aspectRatio(composition.getBounds());
    }

    private float aspectRatio(Rect rect) {
        return rect.getWidth() / (float) rect.getHeight();
    }

    public void stop() {
        endAnimation();
    }

    public boolean isRunning() {
        return isAnimating();
    }

    /**
     * Plays the animation from the beginning. If speed is < 0, it will start at the end
     * and play towards the beginning
     */
    
    public void playAnimation() {
        if (compositionLayer == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    playAnimation();
                }
            });
            return;
        }

        if (animationsEnabled() || getRepeatCount() == 0) {
            animator.playAnimation();
        }
        if (!animationsEnabled()) {
            setFrame((int) (getSpeed() < 0 ? getMinFrame() : getMaxFrame()));
            animator.endAnimation();
        }
    }

    
    public void endAnimation() {
        lazyCompositionTasks.clear();
        animator.endAnimation();
    }

    /**
     * Continues playing the animation from its current position. If speed < 0, it will play backwards
     * from the current position.
     */
    
    public void resumeAnimation() {
        if (compositionLayer == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    resumeAnimation();
                }
            });
            return;
        }

        if (animationsEnabled() || getRepeatCount() == 0) {
            animator.resumeAnimation();
        }
        if (!animationsEnabled()) {
            setFrame((int) (getSpeed() < 0 ? getMinFrame() : getMaxFrame()));
            animator.endAnimation();
        }
    }

    /**
     * Sets the minimum frame that the animation will start from when playing or looping.
     * @param minFrame to set
     */
    public void setMinFrame(final int minFrame) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinFrame(minFrame);
                }
            });
            return;
        }
        animator.setMinFrame(minFrame);
    }

    /**
     * Returns the minimum frame set by {@link #setMinFrame(int)} or {@link #setMinProgress(float)}
     * @return Returns the minimum frame set
     */
    public float getMinFrame() {
        return animator.getMinFrame();
    }

    /**
     * Sets the minimum progress that the animation will start from when playing or looping.
     * @param minProgress Sets the minimum progress
     */
    public void setMinProgress(final float minProgress) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinProgress(minProgress);
                }
            });
            return;
        }
        setMinFrame((int) MiscUtils.lerp(composition.getStartFrame(), composition.getEndFrame(), minProgress));
    }

    /**
     * Sets the maximum frame that the animation will end at when playing or looping.
     * @param maxFrame max frame to set
     */
    public void setMaxFrame(final int maxFrame) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMaxFrame(maxFrame);
                }
            });
            return;
        }
        animator.setMaxFrame(maxFrame + 0.99f);
    }

    /**
     * Returns the maximum frame set by {@link #setMaxFrame(int)} or {@link #setMaxProgress(float)}
     * @return Returns the maximum frame
     */
    public float getMaxFrame() {
        return animator.getMaxFrame();
    }

    /**
     * Sets the maximum progress that the animation will end at when playing or looping.
     * @param maxProgress progress to set
     */
    public void setMaxProgress(final float maxProgress) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMaxProgress(maxProgress);
                }
            });
            return;
        }
        setMaxFrame((int) MiscUtils.lerp(composition.getStartFrame(), composition.getEndFrame(), maxProgress));
    }

    /**
     * Sets the minimum frame to the start time of the specified marker.
     * @param markerName name
     * @throws IllegalArgumentException if the marker is not found.
     */
    public void setMinFrame(final String markerName) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinFrame(markerName);
                }
            });
            return;
        }
        Marker marker = composition.getMarker(markerName);
        if (marker == null) {
            throw new IllegalArgumentException(MARKERWITHNAME + markerName + ".");
        }
        setMinFrame((int) marker.startFrame);
    }

    /**
     * Sets the maximum frame to the start time + duration of the specified marker.
     * @param markerName name
     * @throws IllegalArgumentException if the marker is not found.
     */
    public void setMaxFrame(final String markerName) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMaxFrame(markerName);
                }
            });
            return;
        }
        Marker marker = composition.getMarker(markerName);
        if (marker == null) {
            throw new IllegalArgumentException(MARKERWITHNAME + markerName + ".");
        }
        setMaxFrame((int) (marker.startFrame + marker.durationFrames));
    }

    /**
     * Sets the minimum and maximum frame to the start time and start time + duration
     * of the specified marker.
     * @param markerName name
     * @throws IllegalArgumentException if the marker is not found.
     */
    public void setMinAndMaxFrame(final String markerName) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinAndMaxFrame(markerName);
                }
            });
            return;
        }
        Marker marker = composition.getMarker(markerName);
        if (marker == null) {
            throw new IllegalArgumentException(MARKERWITHNAME + markerName + ".");
        }
        int startFrame = (int) marker.startFrame;
        setMinAndMaxFrame(startFrame, startFrame + (int) marker.durationFrames);
    }

    /**
     * Sets the minimum and maximum frame to the start marker start and the maximum frame to the end marker start.
     * playEndMarkerStartFrame determines whether or not to play the frame that the end marker is on. If the end marker
     * represents the end of the section that you want, it should be true. If the marker represents the beginning of the
     * next section, it should be false.
     * @param startMarkerName name
     * @param playEndMarkerStartFrame end marker start
     * @param endMarkerName name
     * @throws IllegalArgumentException if either marker is not found.
     */
    public void setMinAndMaxFrame(final String startMarkerName, final String endMarkerName,
        final boolean playEndMarkerStartFrame) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinAndMaxFrame(startMarkerName, endMarkerName, playEndMarkerStartFrame);
                }
            });
            return;
        }
        Marker startMarker = composition.getMarker(startMarkerName);
        if (startMarker == null) {
            throw new IllegalArgumentException(MARKERWITHNAME + startMarkerName + ".");
        }
        int startFrame = (int) startMarker.startFrame;

        final Marker endMarker = composition.getMarker(endMarkerName);
        if (endMarker == null) {
            throw new IllegalArgumentException(MARKERWITHNAME + endMarkerName + ".");
        }
        int endFrame = (int) (endMarker.startFrame + (playEndMarkerStartFrame ? 1f : 0f));

        setMinAndMaxFrame(startFrame, endFrame);
    }

    /**
     * set Minimun And Maximum Frame
     * @see #setMinFrame(int)
     * @see #setMaxFrame(int)
     * @param maxFrame to set
     * @param minFrame to set
     */
    public void setMinAndMaxFrame(final int minFrame, final int maxFrame) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinAndMaxFrame(minFrame, maxFrame);
                }
            });
            return;
        }
        // Adding 0.99 ensures that the maxFrame itself gets played.
        animator.setMinAndMaxFrames(minFrame, maxFrame + 0.99f);
    }

    /**
     * Set Minimum and Maximum progress to set
     * @see #setMinProgress(float)
     * @see #setMaxProgress(float)
     * @param maxProgress progress to set
     * @param minProgress progress to set
     */
    public void setMinAndMaxProgress(final float minProgress,
        final float maxProgress) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setMinAndMaxProgress(minProgress, maxProgress);
                }
            });
            return;
        }

        setMinAndMaxFrame((int) MiscUtils.lerp(composition.getStartFrame(), composition.getEndFrame(), minProgress),
            (int) MiscUtils.lerp(composition.getStartFrame(), composition.getEndFrame(), maxProgress));
    }

    /**
     * Reverses the current animation speed. This does NOT play the animation.
     *
     * @see #setSpeed(float)
     * @see #playAnimation()
     * @see #resumeAnimation()
     */
    public void reverseAnimationSpeed() {
        animator.reverseAnimationSpeed();
    }

    /**
     * Sets the playback speed. If speed < 0, the animation will play backwards.
     * @param speed playback speed
     */
    public void setSpeed(float speed) {
        animator.setSpeed(speed);
    }

    /**
     * Returns the current playback speed. This will be < 0 if the animation is playing backwards.
     * @return current speed
     */
    public float getSpeed() {
        return animator.getSpeed();
    }

    public void addAnimatorUpdateListener(AnimatorValue.ValueUpdateListener updateListener) {
        animator.addUpdateListener(updateListener);
    }

    public void removeAnimatorUpdateListener(AnimatorValue.ValueUpdateListener updateListener) {
        animator.removeUpdateListener(updateListener);
    }

    public void removeAllUpdateListeners() {
        animator.removeAllUpdateListeners();
        animator.addUpdateListener(progressUpdateListener);
    }

    public void addAnimatorListener(Animator.StateChangedListener listener) {
        animator.addListener(listener);
    }

    public void removeAnimatorListener(Animator.StateChangedListener listener) {
        animator.removeListener(listener);
    }

    public void removeAllAnimatorListeners() {
        animator.removeAllListeners();
    }

    public void addAnimatorPauseListener(Animator.StateChangedListener listener) {
        animator.addListener(listener);
    }

    public void removeAnimatorPauseListener(Animator.StateChangedListener listener) {
        animator.removeListener(listener);
    }

    public void addRepeatListener(Animator.LoopedListener listener) {
        animator.addRepeatListener(listener);
    }

    public void removeRepeatListener(Animator.LoopedListener listener) {
        animator.removeRepeatListener(listener);
    }

    public void removeRepeatListeners() {
        animator.removeRepeatListeners();
    }

    /**
     * Sets the progress to the specified frame.
     * If the composition isn't set yet, the progress will be set to the frame when
     * it is.
     * @param frame Sets the progress to the specified frame.
     */
    public void setFrame(final int frame) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setFrame(frame);
                }
            });
            return;
        }

        animator.setFrame(frame);
    }

    /**
     * Get the currently rendered frame.
     * @return Get the currently rendered frame.
     */
    public int getFrame() {
        return (int) animator.getFrame();
    }

    public void setProgress(final float progress) {
        if (composition == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    setProgress(progress);
                }
            });
            return;
        }
        //HiTraceId id = L.beginSection("Drawable#setProgress");
        animator.setFrame(MiscUtils.lerp(composition.getStartFrame(), composition.getEndFrame(), progress));
        //L.endSection(id);
    }

    /**
     * Set number of loops to the animator
     * @see #setRepeatCount(int)
     * @param loop in int
     */
    @Deprecated
    public void loop(boolean loop) {
        animator.setLoopedCount(loop ? AnimatorValue.INFINITE : 0);
    }

    /**
     * Defines what this animation should do when it reaches the end. This
     * setting is applied only when the repeat count is either greater than
     * 0 or {@link #INFINITE}. Defaults to {@link #RESTART}.
     *
     * @param mode {@link #RESTART} or {@link #REVERSE}
     */
    /*public void setRepeatMode(@RepeatMode int mode) {
        animator.setRepeatMode(mode);
    }*/

    /**
     * Repeatmode not supported in HMOS as of now (29-6-2020)
     * Defines what this animation should do when it reaches the end.
     *
     * @return either one of {@link #REVERSE} or {@link #RESTART}
     */
    /*@RepeatMode
    public int getRepeatMode() {
        return animator.getRepeatMode();
    }*/

    /**
     * Sets how many times the animation should be repeated. If the repeat
     * count is 0, the animation is never repeated. If the repeat count is
     * greater than 0 or {@link #INFINITE}, the repeat mode will be taken
     * into account. The repeat count is 0 by default.
     *
     * @param count the number of times the animation should be repeated
     */
    public void setRepeatCount(int count) {
        animator.setLoopedCount(count);
    }

    /**
     * Defines how many times the animation should repeat. The default value
     * is 0.
     *
     * @return the number of times the animation should repeat, or {@link #INFINITE}
     */
    public int getRepeatCount() {
        return animator.getLoopedCount();
    }

    public boolean isLooping() {
        return animator.getLoopedCount() == AnimatorValue.INFINITE;
    }

    public boolean isAnimating() {
        // On some versions of Harmoney, this is called from the LottieAnimationView constructor, before animator was created.
        if (animator == null) {
            return false;
        }
        return animator.isRunning();
    }

    private boolean animationsEnabled() {
        return systemAnimationsEnabled || ignoreSystemAnimationsDisabled;
    }

    void setSystemAnimationsAreEnabled(Boolean areEnabled) {
        systemAnimationsEnabled = areEnabled;
    }

    // </editor-fold>
    /**
     * Allows ignoring system animations settings, therefore allowing animations to run even if they are disabled.
     * <p>
     * Defaults to false.
     *
     * @param ignore if true animations will run even when they are disabled in the system settings.
     */
    public void setIgnoreDisabledSystemAnimations(boolean ignore) {
        ignoreSystemAnimationsDisabled = ignore;
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
     * @param scale to set
     */
    public void setScale(float scale) {
        this.scale = scale;
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
     * @param assetDelegate animation asset
     */
    public void setImageAssetDelegate(ImageAssetDelegate assetDelegate) {
        imageAssetDelegate = assetDelegate;
        if (imageAssetManager != null) {
            imageAssetManager.setDelegate(assetDelegate);
        }
    }

    /**
     * Use this to manually set fonts.
     * @param assetDelegate to set fonts
     */
    public void setFontAssetDelegate(FontAssetDelegate assetDelegate) {
        fontAssetDelegate = assetDelegate;
        if (fontAssetManager != null) {
            fontAssetManager.setDelegate(assetDelegate);
        }
    }

    public void setTextDelegate(TextDelegate textDelegate) {
        this.textDelegate = textDelegate;
    }

    @Nullable
    public TextDelegate getTextDelegate() {
        return textDelegate;
    }

    public boolean useTextGlyphs() {
        return textDelegate == null && composition.getCharacters().size() > 0;
    }

    public float getScale() {
        return scale;
    }

    public LottieComposition getComposition() {
        return composition;
    }


    public void cancelAnimation() {
        lazyCompositionTasks.clear();
        animator.cancel();
    }

    public void pauseAnimation() {
        lazyCompositionTasks.clear();
        animator.pauseAnimation();
    }

    //@FloatRange(from = 0f, to = 1f)
    public float getProgress() {
        return animator.getAnimatedValueAbsolute();
    }

    public int getIntrinsicWidth() {
        return composition == null ? -1 : (int) (composition.getBounds().getWidth() * getScale());
    }

    public int getIntrinsicHeight() {
        return composition == null ? -1 : (int) (composition.getBounds().getHeight() * getScale());
    }

    /**
     * Takes a {@link KeyPath}, potentially with wildcards or globstars and resolve it to a list of
     * zero or more actual {@link KeyPath Keypaths} that exist in the current animation.
     * <p>
     * If you want to set value callbacks for any of these values, it is recommend to use the
     * returned {@link KeyPath} objects because they will be internally resolved to their content
     * and won't trigger a tree walk of the animation contents when applied.
     * @param keyPath that exist in the current animation
     * @return  list of keypath
     */
    public List<KeyPath> resolveKeyPath(KeyPath keyPath) {
        if (compositionLayer == null) {
            HMOSLogUtil.warn(L.TAG, "Cannot resolve KeyPath. Composition is not set yet.");
            return Collections.emptyList();
        }
        List<KeyPath> keyPaths = new ArrayList<>();
        compositionLayer.resolveKeyPath(keyPath, 0, keyPaths, new KeyPath());
        return keyPaths;
    }

    /**
     * Add an property callback for the specified {@link KeyPath}. This {@link KeyPath} can resolve
     * to multiple contents. In that case, the callbacks's value will apply to all of them.
     * <p>
     * Internally, this will check if the {@link KeyPath} has already been resolved with
     * {@link #resolveKeyPath(KeyPath)} and will resolve it if it hasn't.
     * @param keyPath keypath
     * @param callback of LottieValueCallback<T>
     * @param property to set
     */
    public <T> void addValueCallback(final KeyPath keyPath, final T property, final LottieValueCallback<T> callback) {
        if (compositionLayer == null) {
            lazyCompositionTasks.add(new LazyCompositionTask() {
                @Override
                public void run(LottieComposition composition) {
                    addValueCallback(keyPath, property, callback);
                }
            });
            return;
        }
        boolean invalidate;
        if (keyPath == KeyPath.COMPOSITION) {
            compositionLayer.addValueCallback(property, callback);
            invalidate = true;
        } else
        if (keyPath.getResolvedElement() != null) {
            keyPath.getResolvedElement().addValueCallback(property, callback);
            invalidate = true;
        } else {
            List<KeyPath> elements = resolveKeyPath(keyPath);

            for (int i = 0; i < elements.size(); i++) {
                elements.get(i).getResolvedElement().addValueCallback(property, callback);
            }
            invalidate = !elements.isEmpty();
        }
        if (invalidate) {
            invalidateSelf();
            if (property == LottieProperty.TIME_REMAP) {
                // Time remapping values are read in setProgress. In order for the new value
                // to apply, we have to re-set the progress with the current progress so that the
                // time remapping can be reapplied.
                setProgress(getProgress());
            }
        }
    }

    /**
     * Overload of {@link #addValueCallback(KeyPath, Object, LottieValueCallback)} that takes an interface. This allows you to use a single abstract
     * method code block in Kotlin such as:
     * drawable.addValueCallback(yourKeyPath, LottieProperty.COLOR) { yourColor }
     * @param keyPath keypath
     * @param callback of LottieValueCallback<T>
     * @param property to set
     */
    public <T> void addValueCallback(KeyPath keyPath, T property, final SimpleLottieValueCallback<T> callback) {
        addValueCallback(keyPath, property, new LottieValueCallback<T>() {
            @Override
            public T getValue(LottieFrameInfo<T> frameInfo) {
                return callback.getValue(frameInfo);
            }
        });
    }

    /**
     * Allows you to modify or clear a bitmap that was loaded for an image either automatically
     * through {@link #setImagesAssetsFolder(String)} or with an {@link ImageAssetDelegate}.
     * @param id of bitmap
     * @param pixelMap to update
     * @return the previous Bitmap or null.
     */
    @Nullable
    public PixelMap updatePixelmap(String id,  PixelMap pixelMap) {
        ImageAssetManager bm = getImageAssetManager();
        if (bm == null) {
            HMOSLogUtil.warn(L.TAG, "Cannot update bitmap. Most likely the drawable is not added to a View "
                + "which prevents Lottie from getting a Context.");
            return null;
        }
        PixelMap ret = bm.updateBitmap(id, pixelMap);
        invalidateSelf();
        return ret;
    }

    @Nullable
    public PixelMap getImageAsset(String id) {
        ImageAssetManager bm = getImageAssetManager();
        if (bm != null) {
            return bm.bitmapForId(id);
        }
        return null;
    }

    private ImageAssetManager getImageAssetManager() {

        if (imageAssetManager != null && !imageAssetManager.hasSameContext(getLottieCallback().getContext())) {
            imageAssetManager = null;
        }

        if (imageAssetManager == null) {
            imageAssetManager = new ImageAssetManager(getLottieCallback().getContext(), imageAssetsFolder, imageAssetDelegate,
                composition.getImages());
        }

        return imageAssetManager;
    }

    @Nullable
    public Font getTypeface(String fontFamily, String style) {
        FontAssetManager assetManager = getFontAssetManager();
        if (assetManager != null) {
            return assetManager.getTypeface(getLottieCallback().getContext(),fontFamily, Integer.valueOf(style));
        }
        return null;
    }

    private FontAssetManager getFontAssetManager() {
        if (fontAssetManager == null) {
            fontAssetManager = new FontAssetManager(fontAssetDelegate);
        }

        return fontAssetManager;
    }

    /**
     * These Drawable.Callback methods proxy the calls so that this is the drawable that is
     * actually invalidated, not a child one which will not pass the view's validateDrawable check.
     */
    /*@Override
    public void invalidateDrawable(Element who) {
        Object callback = getCallback();
        if (callback == null) {
            return;
        }
        if(callback instanceof Component)
            ((Component)callback).invalidate();
    }

    @Override
    public void scheduleDrawable(Element who, Runnable what, long when) {
        Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.scheduleDrawable(this, what, when);
    }

    @Override
    public void unscheduleDrawable(Element who, Runnable what) {
        Callback callback = getCallback();
        if (callback == null) {
            return;
        }
        callback.unscheduleDrawable(this, what);
    }*/
    void setScaleMode(Image.ScaleMode scaleType) {
        this.scaleType = scaleType;
    }

    /**
     * If the composition is larger than the canvas, we have to use a different method to scale it up.
     * See the comments in {@link #drawToCanvas(Canvas)}  for more info.
     * @param canvas to draw
     * @return minimum of (maxScaleX, maxScaleY)
     */
    private float getMaxScale(Canvas canvas) {
        float maxScaleX = 0;
        float maxScaleY = 0;
        if (canvas.getLocalClipBounds() != null) {
            maxScaleX = canvas.getLocalClipBounds().getWidth() / (float) composition.getBounds().getWidth();
            maxScaleY = canvas.getLocalClipBounds().getHeight() / (float) composition.getBounds().getHeight();
        }
        return Math.min(maxScaleX, maxScaleY);
    }

    private void drawWithNewAspectRatio(Canvas canvas) {
        if (compositionLayer == null) {
            return;
        }

        int saveCount = -1;
        Rect bounds = getBounds();
        // In fitXY mode, the scale doesn't take effect.
        float scaleX = bounds.getWidth() / (float) composition.getBounds().getWidth();
        float scaleY = bounds.getHeight() / (float) composition.getBounds().getHeight();

        if (isExtraScaleEnabled) {
            float maxScale = Math.min(scaleX, scaleY);
            float extraScale = 1f;
            if (maxScale < 1f) {
                extraScale = extraScale / maxScale;
                scaleX = scaleX / extraScale;
                scaleY = scaleY / extraScale;
            }

            if (extraScale > 1) {
                //TODO - HMOS interface doesn't return value for save operation
                saveCount = canvas.save();
                float halfWidth = bounds.getWidth() / 2f;
                float halfHeight = bounds.getHeight() / 2f;
                float scaledHalfWidth = halfWidth * maxScale;
                float scaledHalfHeight = halfHeight * maxScale;

                canvas.translate(halfWidth - scaledHalfWidth, halfHeight - scaledHalfHeight);
                canvas.scale(extraScale, extraScale, scaledHalfWidth, scaledHalfHeight);
            }
        }

        matrix.reset();
        matrix.scale(scaleX, scaleY);
        compositionLayer.draw(canvas, matrix, alpha);

        if (saveCount > 0) {
            canvas.restoreToCount(saveCount);
        }
    }

    private void drawWithOriginalAspectRatio(Canvas canvas) {
        if (compositionLayer == null) {
            return;
        }

        float scale = this.scale;
        float extraScale = 1f;
        float maxScale = getMaxScale(canvas);
        if (scale > maxScale && maxScale > 0) {
            scale = maxScale;
            extraScale = this.scale / scale;
        }

        int saveCount = -1;
        if (extraScale > 1) {
            // This is a bit tricky...
            // We can't draw on a canvas larger than ViewConfiguration.get(context).getScaledMaximumDrawingCacheSize()
            // which works out to be roughly the size of the screen because Harmoney can't generate a
            // bitmap large enough to render to.
            // As a result, we cap the scale such that it will never be wider/taller than the screen
            // and then only render in the top left corner of the canvas. We then use extraScale
            // to scale up the rest of the scale. However, since we rendered the animation to the top
            // left corner, we need to scale up and translate the canvas to zoom in on the top left
            // corner.
            saveCount = canvas.save();
            float halfWidth = composition.getBounds().getWidth() / 2f;
            float halfHeight = composition.getBounds().getHeight() / 2f;
            float scaledHalfWidth = halfWidth * scale;
            float scaledHalfHeight = halfHeight * scale;

            canvas.translate(getScale() * halfWidth - scaledHalfWidth, getScale() * halfHeight - scaledHalfHeight);
            canvas.scale(extraScale, extraScale, scaledHalfWidth, scaledHalfHeight);
        }

        matrix.reset();
        matrix.scale(scale, scale);
        compositionLayer.draw(canvas, matrix, alpha);

        if (saveCount > 0) {
            canvas.restoreToCount(saveCount);
        }
    }


    static final class CallbackWeakReference extends WeakReference<ElementCallback> {
        CallbackWeakReference(final ElementCallback r) {
            super(r);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            return get() == ((CallbackWeakReference) o).get();
        }

        @Override
        public int hashCode() {
            final ElementCallback callback = get();
            return callback != null ? callback.hashCode() : 0;
        }
    }
}
