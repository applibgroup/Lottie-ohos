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
package com.airbnb.lottie.demo.slice;

import com.airbnb.lottie.demo.ResourceTable;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Button;

public class MainAbilitySlice extends AbilitySlice {
    private static final String KEY_URL = "url";

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);
        super.setUIContent(ResourceTable.Layout_ability_main);
        initViews();
    }

    public void initViews() {
        Button assetLoad = (Button) findComponentById(ResourceTable.Id_asset_load);
        assetLoad.setClickedListener(Component -> {
            present(new AssetLoadSlice(), new Intent());
        });
        Button assetLoadUrl = (Button) findComponentById(ResourceTable.Id_asset_load_url);
        assetLoadUrl.setClickedListener(Component -> {
            present(new UrlLoadSlice(), new Intent());
        });
    }

    @Override
    public void onActive() {
        super.onActive();
    }

    @Override
    public void onForeground(Intent intent) {
        super.onForeground(intent);
    }
}
