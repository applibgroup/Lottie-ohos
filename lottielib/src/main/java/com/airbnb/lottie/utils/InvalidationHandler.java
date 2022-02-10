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

package com.airbnb.lottie.utils;

import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.eventhandler.InnerEvent;

import java.lang.ref.WeakReference;

/**
 * 功能描述
 */
class InvalidationHandler extends EventHandler {
    static final int MSG_TYPE_INVALIDATION = -1;

	private final WeakReference<LottieValueAnimator> mlottieRef;

	InvalidationHandler(final LottieValueAnimator gifDrawable) {
		super(EventRunner.getMainEventRunner());
		mlottieRef = new WeakReference<>(gifDrawable);
	}

	@Override
	protected void processEvent(InnerEvent event) {
		final LottieValueAnimator ltDrawable = mlottieRef.get();
		if (ltDrawable == null) {
			return;
		}
		if (event.eventId == MSG_TYPE_INVALIDATION) {
			ltDrawable.doFrame(System.nanoTime());
		}
	}
}
