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
package com.airbnb.lottie.slice;

import com.airbnb.lottie.L;
import com.airbnb.lottie.LottieAnimationView;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;

import com.airbnb.lottie.ResourceTable;
import com.airbnb.lottie.value.LottieAnimationViewData;

public class AnimationSlice extends AbilitySlice {
    ComponentContainer rootLayout;
    private String KEY_JSON_STRING = "fileName";
    private String KEY_URL = "url";
    private String KEY_REPEAT_COUNT = "repeatCount";

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        String jsonString = null;
        String url = null;
        int repeatCount = -1;
        rootLayout = (ComponentContainer) LayoutScatter.getInstance(this)
                .parse(ResourceTable.Layout_animation_slice, null, false);
        if(intent!=null) {
            if(intent.hasParameter(KEY_REPEAT_COUNT)) {
                repeatCount = intent.getIntParam(KEY_REPEAT_COUNT, -1);
            }
            if(intent.hasParameter(KEY_JSON_STRING)) {
                jsonString = intent.getStringParam(KEY_JSON_STRING);
                initLottieViews(jsonString, KEY_JSON_STRING, repeatCount);
            } else if(intent.hasParameter(KEY_URL)) {
                url = intent.getStringParam(KEY_URL);
                initLottieViews(url, KEY_URL, repeatCount);
            }
        }
        super.setUIContent(rootLayout);
    }

    private void initLottieViews(String string, String bundleKey, int repeatCount) {
        L.setTraceEnabled(true);
        LottieAnimationView lv = (LottieAnimationView)rootLayout.findComponentById(ResourceTable.Id_animationView);
        lv.setContentPosition((float)50.0,(float)50.0);
        LottieAnimationViewData data = new LottieAnimationViewData();
        if(bundleKey.equals(KEY_JSON_STRING) && string!=null) {
            data.setFilename(string);
        }
        else {
            data.setUrl(string);
        }
        data.setRepeatCount(repeatCount);
        data.autoPlay = true;
        lv.setAnimationData(data);
    }
}
