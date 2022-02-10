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

import java.util.concurrent.TimeUnit;


class RenderTask extends SafeRunnable {

	RenderTask(LottieValueAnimator ltAnimator) {
		super(ltAnimator);
	}

	@Override
	public void doFrame() {
		final long invalidationDelay = 16;
		if (invalidationDelay >= 0) {
			lt.mExecutor.remove(this);
			lt.mRenderTaskSchedule = lt.mExecutor.schedule(this, invalidationDelay,
				TimeUnit.MILLISECONDS);
			lt.mInvalidationHandler.sendEvent(-1,0);
		}
	}
}
