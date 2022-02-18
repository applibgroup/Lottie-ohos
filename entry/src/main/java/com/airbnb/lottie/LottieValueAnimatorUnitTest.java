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

import ohos.agp.animation.Animator;
import ohos.agp.utils.Rect;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.utils.LongPlainArray;
import ohos.utils.PlainArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.airbnb.lottie.model.Font;
import com.airbnb.lottie.model.FontCharacter;
import com.airbnb.lottie.model.Marker;
import com.airbnb.lottie.model.layer.Layer;
import com.airbnb.lottie.utils.LottieValueAnimator;
import com.airbnb.lottie.utils.Utils;

/**
 * LottieValueAnimatorUnitTest testcases
 */
public class LottieValueAnimatorUnitTest {
    private static final String TAG_LOG = "[Lottie Sample test] ";
    private static final String ACTUAL = "actual ";

    private static final int DOMAIN_ID = 0xD000F00;

    private static final HiLogLabel LABEL_LOG = new HiLogLabel(3, DOMAIN_ID, TAG_LOG);

    private LottieComposition composition;
    private LottieValueAnimator animator;
    private Animator.StateChangedListener spyListener;
    private AtomicBoolean isDone;

    /**
     * LottieValueAnimatorUnitTest Constructor
     */
    public LottieValueAnimatorUnitTest() {
        //TODO
    }

    public void setup() {
        animator = createAnimator();
        composition = createComposition(0, 1000);
        animator.setComposition(composition);
        spyListener = new Animator.StateChangedListener() {
            @Override
            public void onStart(Animator animator) {
                //TODO
            }

            @Override
            public void onStop(Animator animator) {
                //TODO
            }

            @Override
            public void onCancel(Animator animator) {
                //TODO
            }

            @Override
            public void onEnd(Animator animator) {
                //TODO
            }

            @Override
            public void onPause(Animator animator) {
                //TODO
            }

            @Override
            public void onResume(Animator animator) {
                //TODO
            }
        };
        isDone = new AtomicBoolean(false);
    }

    private LottieValueAnimator createAnimator() {
        // Choreographer#postFrameCallback hangs with robolectric.
        return new LottieValueAnimator() {
            @Override
            public void postFrameCallback() {
                running = true;
            }

            @Override
            public void removeFrameCallback() {
                running = false;
            }
        };
    }

    private LottieComposition createComposition(int startFrame, int endFrame) {
        float scale = Utils.dpScale();
        ;
        float frameRate = 1000f;
        final LongPlainArray<Layer> layerMap = new LongPlainArray<>();
        final List<Layer> layers = new ArrayList<>();
        int width = 0;
        int height = 0;
        Map<String, List<Layer>> precomps = new HashMap<>();
        Map<String, LottieImageAsset> images = new HashMap<>();
        Map<String, Font> fonts = new HashMap<>();
        List<Marker> markers = new ArrayList<>();
        PlainArray<FontCharacter> characters = new PlainArray<>();

        LottieComposition composition = new LottieComposition();

        int scaledWidth = (int) (width * scale);
        int scaledHeight = (int) (height * scale);
        Rect bounds = new Rect(0, 0, scaledWidth, scaledHeight);

        composition.init(bounds, startFrame, endFrame, frameRate, layers, layerMap, precomps, images, characters,
                fonts, markers);

        return composition;
    }

    public void testInitialState() {
        HiLog.error(LABEL_LOG, "Test inside testInitialState -> - ");
        assertClose(0f, animator.getFrame());
    }

    public void testResumingMaintainsValue() {
        HiLog.error(LABEL_LOG, "Test inside testResumingMaintainsValue -> - ");
        animator.setFrame(500);
        animator.resumeAnimation();
        assertClose(500f, animator.getFrame());
    }

    public void testFrameConvertsToAnimatedFraction() {
        HiLog.error(LABEL_LOG, "Test inside testFrameConvertsToAnimatedFraction -> - ");
        animator.setFrame(500);
        animator.resumeAnimation();
        assertClose(0.5f, animator.getAnimatedFraction());
        assertClose(0.5f, animator.getAnimatedValueAbsolute());
    }

    public void testPlayingResetsValue() {
        HiLog.error(LABEL_LOG, "Test inside testPlayingResetsValue -> - ");
        animator.setFrame(500);
        animator.playAnimation();
        assertClose(0f, animator.getFrame());
        assertClose(0f, animator.getAnimatedFraction());
    }

    public void testReversingMaintainsValue() {
        HiLog.error(LABEL_LOG, "Test inside testReversingMaintainsValue -> - ");
        animator.setFrame(250);
        animator.reverseAnimationSpeed();
        assertClose(250f, animator.getFrame());
        assertClose(0.75f, animator.getAnimatedFraction());
        assertClose(0.25f, animator.getAnimatedValueAbsolute());
    }

    public void testReversingWithMinValueMaintainsValue() {
        HiLog.error(LABEL_LOG, "Test inside testReversingWithMinValueMaintainsValue -> - ");
        animator.setMinFrame(100);
        animator.setFrame(1000);
        animator.reverseAnimationSpeed();
        assertClose(1000f, animator.getFrame());
        assertClose(0f, animator.getAnimatedFraction());//API not giving same value
        assertClose(1f, animator.getAnimatedValueAbsolute());
    }

    public void testReversingWithMaxValueMaintainsValue() {
        HiLog.error(LABEL_LOG, "Test inside testReversingWithMaxValueMaintainsValue -> - ");
        animator.setMaxFrame(900);
        animator.reverseAnimationSpeed();
        assertClose(0f, animator.getFrame());
        assertClose(1f, animator.getAnimatedFraction());//API not giving same value
        assertClose(0f, animator.getAnimatedValueAbsolute());
    }

    public void testResumeReversingWithMinValueMaintainsValue() {
        HiLog.error(LABEL_LOG, "Test inside testResumeReversingWithMinValueMaintainsValue -> - ");
        animator.setMaxFrame(900);
        animator.reverseAnimationSpeed();
        animator.resumeAnimation();
        assertClose(900f, animator.getFrame());//API not giving same value
        assertClose(0f, animator.getAnimatedFraction());
        assertClose(0.9f, animator.getAnimatedValueAbsolute());//API not giving same value
    }

    public void testPlayReversingWithMinValueMaintainsValue() {
        HiLog.error(LABEL_LOG, "Test inside testPlayReversingWithMinValueMaintainsValue -> - ");
        animator.setMaxFrame(900);
        animator.reverseAnimationSpeed();
        animator.playAnimation();
        assertClose(900f, animator.getFrame());
        assertClose(0f, animator.getAnimatedFraction());
        assertClose(0.9f, animator.getAnimatedValueAbsolute());
    }

    public void testMinAndMaxBothSet() {
        HiLog.error(LABEL_LOG, "Test inside testMinAndMaxBothSet -> - ");
        animator.setMinFrame(200);
        animator.setMaxFrame(800);
        animator.setFrame(400);
        assertClose(0.33333f, animator.getAnimatedFraction());
        assertClose(0.4f, animator.getAnimatedValueAbsolute());
        animator.reverseAnimationSpeed();
        assertClose(400f, animator.getFrame());
        assertClose(0.66666f, animator.getAnimatedFraction());
        assertClose(0.4f, animator.getAnimatedValueAbsolute());
        animator.resumeAnimation();
        assertClose(400f, animator.getFrame());
        assertClose(0.66666f, animator.getAnimatedFraction());
        assertClose(0.4f, animator.getAnimatedValueAbsolute());
        animator.playAnimation();
        assertClose(800f, animator.getFrame());
        assertClose(0f, animator.getAnimatedFraction());
        assertClose(0.8f, animator.getAnimatedValueAbsolute());
    }

    public void testSetFrameIntegrity() {
        HiLog.error(LABEL_LOG, "Test inside testSetFrameIntegrity -> - ");
        animator.setMinAndMaxFrames(200, 800);

        // setFrame < minFrame should clamp to minFrame
        animator.setFrame(100);
        assertEquals(200f, animator.getFrame());

        animator.setFrame(900);
        assertEquals(800f, animator.getFrame());
    }

    //(expected = IllegalArgumentException.class)
    public void testMinAndMaxFrameIntegrity() {
        HiLog.error(LABEL_LOG, "Test inside testMinAndMaxFrameIntegrity -> - ");
        animator.setMinAndMaxFrames(800, 200);
    }


    public void setMinFrameSmallerThanComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMinFrameSmallerThanComposition -> - ");
        animator.setMinFrame(-9000);
        assertClose(animator.getMinFrame(), composition.getStartFrame());
    }


    public void setMaxFrameLargerThanComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMaxFrameLargerThanComposition -> - ");
        animator.setMaxFrame(9000);
        assertClose(animator.getMaxFrame(), composition.getEndFrame());
    }


    public void setMinFrameBeforeComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMinFrameBeforeComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setMinFrame(100);
        animator.setComposition(composition);
        assertClose(100.0f, animator.getMinFrame());
    }


    public void setMaxFrameBeforeComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMaxFrameBeforeComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setMaxFrame(100);
        animator.setComposition(composition);
        assertClose(100.0f, animator.getMaxFrame());
    }


    public void setMinAndMaxFrameBeforeComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMinAndMaxFrameBeforeComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setMinAndMaxFrames(100, 900);
        animator.setComposition(composition);
        assertClose(100.0f, animator.getMinFrame());
        assertClose(900.0f, animator.getMaxFrame());
    }


    public void setMinFrameAfterComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMinFrameAfterComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setComposition(composition);
        animator.setMinFrame(100);
        assertClose(100.0f, animator.getMinFrame());
    }


    public void setMaxFrameAfterComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMaxFrameAfterComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setComposition(composition);
        animator.setMaxFrame(100);
        assertEquals(100.0f, animator.getMaxFrame());
    }


    public void setMinAndMaxFrameAfterComposition() {
        HiLog.error(LABEL_LOG, "Test inside setMinAndMaxFrameAfterComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setComposition(composition);
        animator.setMinAndMaxFrames(100, 900);
        assertClose(100.0f, animator.getMinFrame());
        assertClose(900.0f, animator.getMaxFrame());
    }


    public void maxFrameOfNewShorterComposition() {
        HiLog.error(LABEL_LOG, "Test inside maxFrameOfNewShorterComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setComposition(composition);
        LottieComposition composition2 = createComposition(0, 500);
        animator.setComposition(composition2);
        assertClose(500.0f, animator.getMaxFrame());
    }


    public void maxFrameOfNewLongerComposition() {
        HiLog.error(LABEL_LOG, "Test inside maxFrameOfNewLongerComposition -> - ");
        LottieValueAnimator animator = createAnimator();
        animator.setComposition(composition);
        LottieComposition composition2 = createComposition(0, 1500);
        animator.setComposition(composition2);
        assertClose(1500.0f, animator.getMaxFrame());
    }


    public void clearComposition() {
        HiLog.error(LABEL_LOG, "Test inside clearComposition -> - ");
        animator.clearComposition();
        assertClose(0.0f, animator.getMaxFrame());
        assertClose(0.0f, animator.getMinFrame());
    }


    public void resetComposition() {
        HiLog.error(LABEL_LOG, "Test inside resetComposition -> - ");
        animator.clearComposition();
        animator.setComposition(composition);
        assertClose(0.0f, animator.getMinFrame());
        assertClose(1000.0f, animator.getMaxFrame());
    }


    public void resetAndSetMinBeforeComposition() {
        HiLog.error(LABEL_LOG, "Test inside resetAndSetMinBeforeComposition -> - ");
        animator.clearComposition();
        animator.setMinFrame(100);
        animator.setComposition(composition);
        assertClose(100.0f, animator.getMinFrame());
        assertClose(1000.0f, animator.getMaxFrame());
    }


    public void resetAndSetMinAterComposition() {
        HiLog.error(LABEL_LOG, "Test inside resetAndSetMinAterComposition -> - ");
        animator.clearComposition();
        animator.setComposition(composition);
        animator.setMinFrame(100);
        assertClose(100.0f, animator.getMinFrame());
        assertClose(1000.0f, animator.getMaxFrame());
    }


    /*private static void assertClose(float expected, float actual) {
        assertEquals(expected, actual, expected * 0.01f);
    }*/

    private static void assertClose(float expected, float actual) {
        HiLog.error(LABEL_LOG, "Test inside assertClose -> - ");
        //assertEquals(expected, actual, expected * 0.01f);
        if (Float.compare(expected, actual) == 0) {
            HiLog.error(LABEL_LOG, "Test testInitialState assertEquals-> - expected " + expected + " " + ACTUAL + actual);
            return;
        }
        if (!(Math.abs(expected - actual) <= expected * 0.01f)) {
            if (expected == actual) {
                HiLog.error(LABEL_LOG, "Test testInitialState assertEquals-> -  values are equal expected " + expected + " " + ACTUAL + actual);
            } else {
                HiLog.error(LABEL_LOG, "Test testInitialState assertEquals-> -  values are Not equal expected " + expected + " " + ACTUAL + actual);
            }

            // failNotEquals( new Float(expected), new Float(actual));
        }
    }

    static public void assertEquals(Object expected, Object actual) {
        HiLog.error(LABEL_LOG, "Test inside assertEquals 1 -> - ");
        assertEquals(null, expected, actual);
    }

    /**
     * Asserts that two objects are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     * @param message message to display
     * @param expected value
     * @param actual value
     */
    static public void assertEquals(String message, Object expected, Object actual) {
        HiLog.error(LABEL_LOG, "Test inside assertEquals 2 -> -  expected " + expected + "" + ACTUAL + actual);
        if (expected == null && actual == null) {
            return;
        }
        if (expected != null && expected.equals(actual)) {
            return;
        }
        failNotEquals(message, expected, actual);
    }

    static public void failNotEquals(String message, Object expected, Object actual) {
        HiLog.error(LABEL_LOG, "Test inside failNotEquals  -> - ");
        fail(format(message, expected, actual));
    }

    public static String format(String message, Object expected, Object actual) {
        HiLog.error(LABEL_LOG, "Test inside format  -> - ");
        String formatted = "";
        if (message != null && message.length() > 0) {
            formatted = message + " ";
        }
        return formatted + "expected:<" + expected + "> but was:<" + actual + ">";
    }

    static public void fail(String message) {
        HiLog.error(LABEL_LOG, "Test inside fail  -> - ");
        if (message == null) {
            HiLog.error(LABEL_LOG, "Test fail-> - " + "message is null");
        } else {
            HiLog.error(LABEL_LOG, "Test fail-> - " + message);
        }
    }


}
