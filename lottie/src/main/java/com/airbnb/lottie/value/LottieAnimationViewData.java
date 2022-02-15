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
package com.airbnb.lottie.value;

public class LottieAnimationViewData {
    private boolean hasRawRes;
    private int resId;
    private boolean hasFileName;
    private String fileName;
    private boolean hasUrl;
    private String url;
    public boolean loop;
    public boolean autoPlay;
    public int repeatMode; //1 for restart, 2 for reverse
    public int repeatCount = -1;
    public int renderMode; //0-Automatic, 1-Hardware, 2-software
    //TODO: Capture all the features and behaviors from lottie and add here
    //as OHOS platform matures all this content should come from xml like attr.xml

    public LottieAnimationViewData(){
        //TODO

    };

    public void setFilename(String name){
        this.hasFileName = true;
        this.fileName = name;
    }

    public void setUrl(String url){
        this.hasUrl = true;
        this.url = url;
    }

    public void setResId(int id){
        this.hasRawRes = true;
        this.resId = id;
    }

    public void setRepeatCount(int count){
        this.repeatCount = count;
    }

    public int getRepeatCount(){
        return this.repeatCount;
    }

    public String getFileName(){
        return this.fileName;
    }

    public String getUrl(){
        return this.url;
    }

    public int getResId(){
        return this.resId;
    }
    public boolean isHasFileName(){
        return this.hasFileName;
    }

    public boolean isHasRawRes() {
        return this.hasRawRes;
    }

    public boolean isHasUrl(){
        return this.hasUrl;
    }
}
