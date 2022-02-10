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

import java.util.ArrayList;

import com.airbnb.lottie.demo.ResourceTable;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.ComponentContainer;
import ohos.agp.components.LayoutScatter;
import ohos.agp.components.ListContainer;
import ohos.agp.components.TextField;


public class UrlLoadSlice extends AbilitySlice {
    ListContainer listContainer;
    JsonItemProvider jsonItemProvider;
    private ArrayList<String> urlData;
    ComponentContainer rootLayout;
    TextField repeatCount;
    private String KEY_URL = "url";
    private String KEY_REPEAT_COUNT = "repeatCount";

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        rootLayout = (ComponentContainer) LayoutScatter.getInstance(this)
                .parse(ResourceTable.Layout_list_container_layout, null, false);
        listContainer = (ListContainer) rootLayout.findComponentById(ResourceTable.Id_list_container);
        repeatCount = (TextField) rootLayout.findComponentById(ResourceTable.Id_repeatCount);
        listContainer.setItemClickedListener(new ListContainer.ItemClickedListener() {
            @Override
            public void onItemClicked(ListContainer listContainer, Component component, int i, long l) {
                Intent intent = new Intent();
                intent.setParam(KEY_URL, urlData.get(i));
                if(!(repeatCount.getText().isEmpty())) {
                    intent.setParam(KEY_REPEAT_COUNT, Integer.parseInt(repeatCount.getText()));
                }
                present(new AnimationSlice(), intent);
            }
        });
        initUrl();
        jsonItemProvider = new JsonItemProvider(this, urlData);
        listContainer.setItemProvider(jsonItemProvider);
        super.setUIContent(rootLayout);
    }

    @Override
    protected void onActive() {
        super.onActive();
        repeatCount.setText("");
    }

    private void initUrl() {
        urlData = new ArrayList<>();
        urlData.add("https://assets3.lottiefiles.com/packages/lf20_3JZqqW.json");
        urlData.add("https://assets7.lottiefiles.com/packages/lf20_90ZmDV.json");
        urlData.add("https://assets1.lottiefiles.com/datafiles/Q8Sma9sElMkfAOy/data.json");
        urlData.add("https://assets9.lottiefiles.com/datafiles/aTul2rjDNYuKZkj/data.json");
        urlData.add("https://assets9.lottiefiles.com/packages/lf20_sGOHY4.json");
    }
}
