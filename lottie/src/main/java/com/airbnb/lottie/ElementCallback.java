/*
 * Copyright (C) 2021 Huawei Device Co., Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airbnb.lottie;

import ohos.agp.components.element.Element;

/**
 * Implement this interface if you want to create an animated drawable that
 * extends {@link Element}.
 * Upon retrieving a drawable, use
 * to supply your implementation of the interface to the drawable; it uses
 * this interface to schedule and execute animation changes.
 */
public interface ElementCallback {
    /**
     * Called when the drawable needs to be redrawn.  A view at this point
     * should invalidate itself (or at least the part of itself where the
     * drawable appears).
     *
     * @param who The drawable that is requesting the update.
     */
    void invalidateDrawable(Element who);

    /**
     * A Drawable can call this to schedule the next frame of its
     * animation.  An implementation can generally simply call
     * Runnable, Object, long) with
     * the parameters <var>(what, who, when)</var> to perform the
     * scheduling.
     *
     * @param who The drawable being scheduled.
     * @param what The action to execute.
     * @param when The time (in milliseconds) to run.  The timebase is
     */
    void scheduleDrawable(Element who, Runnable what, long when);

    /**
     * A Drawable can call this to unschedule an action previously
     * scheduled with {@link #scheduleDrawable}.  An implementation can
     * generally simply call
     * {removeCallbacks(Runnable, Object)} with
     * the parameters <var>(what, who)</var> to unschedule the drawable.
     *
     * @param who The drawable being unscheduled.
     * @param what The action being unscheduled.
     */
    void unscheduleDrawable(Element who, Runnable what);
}
