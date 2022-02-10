# Introduction
Lottie-ohos: Lottie is a mobile library that parses Adobe After Effects animations exported as json and renders them natively on mobile.

# Usage Instructions
Declare the lottie animation view in layout as shown below  :
```
    <com.airbnb.lottie.LottieAnimationView
        ohos:id="$+id:animationView"
        ohos:width="match_parent"
        ohos:height="match_parent"
        />
```
For playing animation using json/zip file:
```
 LottieAnimationView lv = (LottieAnimationView)rootLayout.findComponentById(ResourceTable.Id_animationView);
 LottieAnimationViewData data = new LottieAnimationViewData();
 data.setFilename(string);
 data.autoPlay = true;
 data.setRepeatCount(repeatCount); // specify repetition count
 lv.setAnimationData(data);

```
where string is the json or zip file name which needs to be displayed.

For loading animation from URL :
```
 LottieAnimationView lv = (LottieAnimationView)rootLayout.findComponentById(ResourceTable.Id_animationView);
 LottieAnimationViewData data = new LottieAnimationViewData();
 data.setUrl(string);
 data.autoPlay = true;
 data.setRepeatCount(repeatCount); // specify repetition count
 lv.setAnimationData(data);

```
where string is the url path to the json file to be loaded.


# Installation tutorial

1) For using Lottie module in sample app, modify entry build.gradle as below :

        dependencies {
            implementation project(':lottie')
        }

2) For using Lottie in separate application make sure to add lottie.har in entry/libs folder and modify build.gradle as below to add dependencies :

        dependencies {
            implementation fileTree(dir: 'libs', include: [ '*.jar', ' *.har'])
        }

3) For using Lottie from a remote repository in separate application, add the below dependencies:

    	Modify entry build.gradle as below :
    	```
    	dependencies {
    	    implementation fileTree(dir: 'libs', include: ['*.har'])
    	    implementation 'io.openharmony.tpc.thirdlib:lottie-ohos:1.0.4'
    	}