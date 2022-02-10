package com.airbnb.lottie.utils;

import com.airbnb.lottie.LottieComposition;

import ohos.agp.animation.AnimatorValue;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This is a slightly modified {@link AnimatorValue} that allows us to update start and end values
 * easily optimizing for the fact that we know that it's a value animator with 2 floats.
 */
public class LottieValueAnimator extends BaseLottieAnimator {

    private float speed = 1f;

    private boolean speedReversedForRepeatMode = false;

    private long lastFrameTimeNs = 0;

    private float frame = 0;

    private int repeatCount = 0;

    private float minFrame = Integer.MIN_VALUE;

    private float maxFrame = Integer.MAX_VALUE;

    private LottieComposition composition;

    protected boolean running = false;

    public LottieValueAnimator() {
        mInvalidationHandler = new InvalidationHandler(this);
    }

    private final RenderTask mRenderTask = new RenderTask(this);
    final ScheduledThreadPoolExecutor mExecutor = LottieDelayExecutor.getInstance();
    ScheduledFuture<?> mRenderTaskSchedule;
    final InvalidationHandler mInvalidationHandler;

    /**
     * Returns a float representing the current value of the animation from 0 to 1
     * regardless of the animation speed, direction, or min and max frames.
     * @return float representing the current value of the animation from 0 to 1
     */
    public Object getAnimatedValue() {
        return getAnimatedValueAbsolute();
    }

    /**
     * Returns the current value of the animation from 0 to 1 regardless
     * of the animation speed, direction, or min and max frames.
     * @return 0 if composition null else return the current value of animation from 0 to 1.
     */
    //FloatRange(from = 0f, to = 1f)
    public float getAnimatedValueAbsolute() {
        if (composition == null) {
            return 0;
        }
        return (frame - composition.getStartFrame()) / (composition.getEndFrame() - composition.getStartFrame());

    }

    /**
     * Returns the current value of the currently playing animation taking into
     * account direction, min and max frames.
     * @return 0 if composition null else return the current value
     */
    //FloatRange(from = 0f, to = 1f)
    public float getAnimatedFraction() {
        if (composition == null) {
            return 0;
        }
        if (isReversed()) {
            return (getMaxFrame() - frame) / (getMaxFrame() - getMinFrame());
        } else {
            return (frame - getMinFrame()) / (getMaxFrame() - getMinFrame());
        }
    }

    @Override
    public long getDuration() {
        return composition == null ? 0 : (long) composition.getDuration();
    }

    public float getFrame() {
        return frame;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void doFrame(long frameTimeNanos) {
        postFrameCallback();
        /*if (composition == null || !isRunning()) {
            return;
        }*/
        //HiTraceId traceid = L.beginSection("LottieValueAnimator#doFrame");
        long now = frameTimeNanos;
        long timeSinceFrame = lastFrameTimeNs == 0 ? 0 : now - lastFrameTimeNs;
        float frameDuration = getFrameDurationNs();
        float dFrames = timeSinceFrame / frameDuration;

        frame += isReversed() ? -dFrames : dFrames;
        boolean ended = !MiscUtils.contains(frame, getMinFrame(), getMaxFrame());
        frame = MiscUtils.clamp(frame, getMinFrame(), getMaxFrame());

        lastFrameTimeNs = now;

        notifyUpdate();
        if (ended) {
            if (getLoopedCount() != INFINITE && repeatCount >= getLoopedCount()) {
                frame = speed < 0 ? getMinFrame() : getMaxFrame();
                removeFrameCallback();
                notifyEnd(isReversed());
            } else {
                notifyRepeat();
                repeatCount++;
                /*if (getRepeatMode() == REVERSE) {
                    speedReversedForRepeatMode = !speedReversedForRepeatMode;
                    reverseAnimationSpeed();
                } else {
                    frame = isReversed() ? getMaxFrame() : getMinFrame();
                }*/
                frame = isReversed() ? getMaxFrame() : getMinFrame();
                lastFrameTimeNs = now;
            }
        }
        verifyFrame();
       // L.endSection(traceid);
    }

    private float getFrameDurationNs() {
        if (composition == null) {
            return Float.MAX_VALUE;
        }
        return Utils.SECOND_IN_NANOS / composition.getFrameRate() / Math.abs(speed);
    }

    public void clearComposition() {
        composition = null;
        minFrame = Integer.MIN_VALUE;
        maxFrame = Integer.MAX_VALUE;
    }

    public void setComposition(LottieComposition composition) {
        // Because the initial composition is loaded async, the first min/max frame may be set
        boolean keepMinAndMaxFrames = this.composition == null;
        this.composition = composition;

        if (keepMinAndMaxFrames) {
            setMinAndMaxFrames((int) Math.max(minFrame, composition.getStartFrame()),
                (int) Math.min(maxFrame, composition.getEndFrame()));
        } else {
            setMinAndMaxFrames((int) composition.getStartFrame(), (int) composition.getEndFrame());
        }
        float frame = this.frame;
        this.frame = 0f;
        setFrame((int) frame);
        mRenderTaskSchedule = mExecutor.schedule(mRenderTask, 240, TimeUnit.MILLISECONDS);
        notifyUpdate();
    }

    public void setFrame(float frame) {
        if (this.frame == frame) {
            return;
        }
        this.frame = MiscUtils.clamp(frame, getMinFrame(), getMaxFrame());
        lastFrameTimeNs = 0;
        notifyUpdate();
    }

    public void setMinFrame(int minFrame) {
        setMinAndMaxFrames(minFrame, (int) maxFrame);
    }

    public void setMaxFrame(float maxFrame) {
        setMinAndMaxFrames(minFrame, maxFrame);
    }

    public void setMinAndMaxFrames(float minFrame, float maxFrame) {
        if (minFrame > maxFrame) {
            throw new IllegalArgumentException(
                String.format("minFrame (%s) must be <= maxFrame (%s)", minFrame, maxFrame));
        }
        float compositionMinFrame = composition == null ? -Float.MAX_VALUE : composition.getStartFrame();
        float compositionMaxFrame = composition == null ? Float.MAX_VALUE : composition.getEndFrame();
        this.minFrame = MiscUtils.clamp(minFrame, compositionMinFrame, compositionMaxFrame);
        this.maxFrame = MiscUtils.clamp(maxFrame, compositionMinFrame, compositionMaxFrame);
        setFrame((int) MiscUtils.clamp(frame, minFrame, maxFrame));
    }

    public void reverseAnimationSpeed() {
        setSpeed(-getSpeed());
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Returns the current speed. This will be affected by repeat mode REVERSE.
     * @return the speed
     */
    public float getSpeed() {
        return speed;
    }

    //Repeat mode not supported in HMOS
    /*@Override
    public void setRepeatMode(int value) {
        super.setRepeatMode(value);
        if (value != REVERSE && speedReversedForRepeatMode) {
            speedReversedForRepeatMode = false;
            reverseAnimationSpeed();
        }
    }*/

    //MainThread
    public void playAnimation() {
        running = true;
        notifyStart(isReversed());
        setFrame((int) (isReversed() ? getMaxFrame() : getMinFrame()));
        lastFrameTimeNs = 0;
        repeatCount = 0;
        postFrameCallback();
    }

    //MainThread
    public void endAnimation() {
        removeFrameCallback();
        notifyEnd(isReversed());
    }

    //MainThread
    public void pauseAnimation() {
        removeFrameCallback();
    }

    //MainThread
    public void resumeAnimation() {
        running = true;
        postFrameCallback();
        lastFrameTimeNs = 0;
        if (isReversed() && getFrame() == getMinFrame()) {
            frame = getMaxFrame();
        } else if (!isReversed() && getFrame() == getMaxFrame()) {
            frame = getMinFrame();
        }
    }

    //MainThread
    @Override
    public void cancel() {
        notifyCancel();
        removeFrameCallback();
    }

    private boolean isReversed() {
        return getSpeed() < 0;
    }

    public float getMinFrame() {
        if (composition == null) {
            return 0;
        }
        return minFrame == Integer.MIN_VALUE ? composition.getStartFrame() : minFrame;
    }

    public float getMaxFrame() {
        if (composition == null) {
            return 0;
        }
        return maxFrame == Integer.MAX_VALUE ? composition.getEndFrame() : maxFrame;
    }

    protected void postFrameCallback() {
        if (isRunning()) {
            removeFrameCallback(false);
            //start();
            //TODO - support for Choreographer
            // Choreographer.getInstance().postFrameCallback(this);

        }
    }

    //MainThread
    protected void removeFrameCallback() {
        removeFrameCallback(true);
    }

    //MainThread
    protected void removeFrameCallback(boolean stopRunning) {

        //stop();
        if (stopRunning) {
            running = false;
        }
    }

    private void verifyFrame() {
        if (composition == null) {
            return;
        }
        if (frame < minFrame || frame > maxFrame) {
            throw new IllegalStateException(
                String.format("Frame must be [%f,%f]. It is %f", minFrame, maxFrame, frame));
        }
    }

}
