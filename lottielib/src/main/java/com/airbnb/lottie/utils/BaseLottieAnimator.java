package com.airbnb.lottie.utils;

import ohos.agp.animation.Animator;
import ohos.agp.animation.AnimatorValue;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class BaseLottieAnimator extends AnimatorValue {
    private final Set<ValueUpdateListener> updateListeners = new CopyOnWriteArraySet<>();

    private final Set<Animator.StateChangedListener> listeners = new CopyOnWriteArraySet<>();

    private final Set<Animator.LoopedListener> repeatListeners = new CopyOnWriteArraySet<>();

    public void addUpdateListener(ValueUpdateListener listener) {
        updateListeners.add(listener);
    }

    public void removeUpdateListener(ValueUpdateListener listener) {
        updateListeners.remove(listener);
    }

    public void removeAllUpdateListeners() {
        updateListeners.clear();
    }

    public void addListener(Animator.StateChangedListener listener) {
        listeners.add(listener);
    }

    public void removeListener(Animator.StateChangedListener listener) {
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        listeners.clear();
    }

    public void addRepeatListener(Animator.LoopedListener listener) {
        repeatListeners.add(listener);
    }

    public void removeRepeatListener(Animator.LoopedListener listener) {
        repeatListeners.remove(listener);
    }

    public void removeRepeatListeners() {
        repeatListeners.clear();
    }

    void notifyStart(boolean isReverse) {
        for (Animator.StateChangedListener listener : listeners) {
            listener.onStart(this);
        }
    }

    void notifyRepeat() {
        for (Animator.LoopedListener listener : repeatListeners) {
            listener.onRepeat(this);
        }
    }

    void notifyEnd(boolean isReverse) {
        for (Animator.StateChangedListener listener : listeners) {
            listener.onEnd(this);
        }
    }

    void notifyCancel() {
        for (Animator.StateChangedListener listener : listeners) {
            listener.onCancel(this);
        }
    }

    void notifyUpdate() {
        for (ValueUpdateListener listener : updateListeners) {
            listener.onUpdate(this, 0F);
        }
    }
}
