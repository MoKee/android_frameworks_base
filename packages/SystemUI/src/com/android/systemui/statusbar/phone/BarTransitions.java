/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.android.systemui.R;

public class BarTransitions {
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_COLORS = false;

    public static final boolean HIGH_END = ActivityManager.isHighEndGfx();

    public static final int MODE_OPAQUE = 0;
    public static final int MODE_SEMI_TRANSPARENT = 1;
    public static final int MODE_TRANSLUCENT = 2;
    public static final int MODE_LIGHTS_OUT = 3;
    public static final int MODE_TRANSPARENT = 4;

    public static final int LIGHTS_IN_DURATION = 250;
    public static final int LIGHTS_OUT_DURATION = 750;
    public static final int BACKGROUND_DURATION = 200;

    private static int mDSBDuration;

    private final String mTag;
    private final View mView;
    private final BarBackgroundDrawable mBarBackground;

    private int mMode;

    public BarTransitions(View view, BarBackgroundDrawable barBackground) {
        mTag = "BarTransitions." + view.getClass().getSimpleName();
        mView = view;
        mBarBackground = barBackground;
        if (HIGH_END) {
            mView.setBackground(mBarBackground);
        }
    }

    protected void setGradientResourceId(int gradientResourceId) {
        mBarBackground.setGradientResourceId(mView.getContext().getResources(),
                gradientResourceId);
    }

    public void updateResources(Resources res) {
        mBarBackground.updateResources(res);
    }

    public int getMode() {
        return mMode;
    }

    public void transitionTo(int mode, boolean animate) {
        // low-end devices do not support translucent modes, fallback to opaque
        if (!HIGH_END && (mode == MODE_SEMI_TRANSPARENT || mode == MODE_TRANSLUCENT)) {
            mode = MODE_OPAQUE;
        }
        if (mMode == mode) return;
        int oldMode = mMode;
        mMode = mode;
        if (DEBUG) Log.d(mTag, String.format("%s -> %s animate=%s",
                modeToString(oldMode), modeToString(mode),  animate));
        onTransition(oldMode, mMode, animate);
    }

    protected void onTransition(int oldMode, int newMode, boolean animate) {
        if (HIGH_END) {
            applyModeBackground(oldMode, newMode, animate);
        }
    }

    protected void applyModeBackground(int oldMode, int newMode, boolean animate) {
        if (DEBUG) Log.d(mTag, String.format("applyModeBackground oldMode=%s newMode=%s animate=%s",
                modeToString(oldMode), modeToString(newMode), animate));
        mBarBackground.applyMode(newMode, animate);
    }

    public static String modeToString(int mode) {
        if (mode == MODE_OPAQUE) return "MODE_OPAQUE";
        if (mode == MODE_SEMI_TRANSPARENT) return "MODE_SEMI_TRANSPARENT";
        if (mode == MODE_TRANSLUCENT) return "MODE_TRANSLUCENT";
        if (mode == MODE_LIGHTS_OUT) return "MODE_LIGHTS_OUT";
        if (mode == MODE_TRANSPARENT) return "MODE_TRANSPARENT";
        if (DEBUG && mode == -1) return "-1";
        throw new IllegalArgumentException("Unknown mode " + mode);
    }

    public void finishAnimations() {
        mBarBackground.finishAnimating();
    }

    public void setContentVisible(boolean visible) {
        // for subclasses
    }

    public void applyTransparent(boolean sticky) {
        // for subclasses
    }

    protected static class BarBackgroundDrawable extends Drawable
            implements Animator.AnimatorListener, ValueAnimator.AnimatorUpdateListener {
        private final int mOpaqueColorResId;
        private final int mSemiTransparentColorResId;
        private int mGradientResId;

        private final Handler mHandler;
        private final Runnable mInvalidateSelf = new Runnable() {

            @Override
            public void run() {
                invalidateSelf();
            }

        };

        private int mOpaque = 0;
        private int mSemiTransparent = 0;
        private Drawable mGradient = null;

        private int mCurrentMode = -1;
        private int mCurrentColor = 0;
        private Animator mColorAnimator = null;
        private int mCurrentGradientAlpha = 0;
        private Animator mGradientAlphaAnimator = null;

        public BarBackgroundDrawable(final Context context, final int opaqueColorResId,
                final int semiTransparentColorResId, final int gradientResId) {
            mHandler = new Handler();

            mOpaqueColorResId = opaqueColorResId;
            mSemiTransparentColorResId = semiTransparentColorResId;
            mGradientResId = gradientResId;

            final Resources res = context.getResources();
            mDSBDuration = res.getInteger(R.integer.dsb_transition_duration);
            updateResources(res);
        }

        @Override
        public final void draw(final Canvas canvas) {
            final int currentColor = mCurrentColor;
            if (Color.alpha(currentColor) > 0) {
                canvas.drawColor(currentColor);
            }

            final int currentGradientAlpha = mCurrentGradientAlpha;
            if (currentGradientAlpha > 0) {
                mGradient.setAlpha(currentGradientAlpha);
                mGradient.draw(canvas);
            }
        }

        public void setGradientResourceId(Resources res, int gradientResourceId) {
            mGradient = res.getDrawable(gradientResourceId);
            mGradientResId = gradientResourceId;
        }

        @Override
        public final int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public final void setAlpha(final int alpha) {
            // noop
        }

        @Override
        public final void setColorFilter(final ColorFilter cf) {
            // noop
        }

        @Override
        public final void onAnimationCancel(final Animator animation) {
            // offload to the animation ending handler as both are the same for our needs
            onAnimationEnd(animation);
        }

        @Override
        public final void onAnimationEnd(final Animator animation) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (animation.equals(mColorAnimator)) {
                        mColorAnimator = null; // abandon the bugger
                        setCurrentColor(getTargetColor()); // force the good value
                    } else if (animation.equals(mGradientAlphaAnimator)) {
                        mGradientAlphaAnimator = null; // abandon the bugger
                        setCurrentGradientAlpha(getTargetGradientAlpha()); // force the good value
                    } else {
                        // some poor zombie animation stopped
                        return;
                    }

                    // ensure the good value is shown to the user
                    mHandler.removeCallbacks(mInvalidateSelf);
                    mHandler.post(mInvalidateSelf);
                }

            });
        }

        @Override
        public final void onAnimationRepeat(final Animator animation) {
            // noop
        }

        @Override
        public final void onAnimationStart(final Animator animation) {
            // noop
        }

        @Override
        public final void onAnimationUpdate(final ValueAnimator animation) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    final Object value = animation.getAnimatedValue();

                    if (animation.equals(mColorAnimator)) {
                        setCurrentColor((int) (Integer) value);
                    } else if (animation.equals(mGradientAlphaAnimator)) {
                        setCurrentGradientAlpha((int) (Integer) value);
                    } else {
                        // tell the zombie animation to stop
                        // TODO animation.cancel();
                        return;
                    }

                    mHandler.removeCallbacks(mInvalidateSelf);
                    mHandler.post(mInvalidateSelf);
                }

            });
        }

        @Override
        protected final void onBoundsChange(final Rect bounds) {
            super.onBoundsChange(bounds);
            mGradient.setBounds(bounds);
        }

        public final synchronized void updateResources(final Resources res)  {
            if (DEBUG_COLORS) {
                mOpaque = 0xff0000ff;
                mSemiTransparent = 0x7f0000ff;
            } else {
                mOpaque = res.getColor(mOpaqueColorResId);
                mSemiTransparent = res.getColor(mSemiTransparentColorResId);
            }

            // without holding on to the bounds, they will be reset and the gradient won't be drawn
            final Rect bounds = mGradient == null ? new Rect() : mGradient.getBounds();
            mGradient = res.getDrawable(mGradientResId);
            mGradient.setBounds(bounds);

            setCurrentColor(getTargetColor());
            setCurrentGradientAlpha(getTargetGradientAlpha());

            mHandler.removeCallbacks(mInvalidateSelf);
            mHandler.postDelayed(mInvalidateSelf, 50);
        }

        protected int getColorOpaque() {
            return mOpaque;
        }

        protected int getColorSemiTransparent() {
            return mSemiTransparent;
        }

        protected int getGradientAlphaOpaque() {
            return 0;
        }

        protected int getGradientAlphaSemiTransparent() {
            return 0;
        }

        private final int getTargetColor() {
            return getTargetColor(mCurrentMode);
        }

        private final int getTargetColor(final int mode) {
            switch (mode) {
            case MODE_TRANSPARENT:
            case MODE_TRANSLUCENT:
                return 0;
            case MODE_SEMI_TRANSPARENT:
                return getColorSemiTransparent();
            default:
                return getColorOpaque();
            }
        }

        private final int getTargetGradientAlpha() {
            return getTargetGradientAlpha(mCurrentMode);
        }

        private final int getTargetGradientAlpha(final int mode) {
            switch (mode) {
            case MODE_TRANSPARENT:
                return 0;
            case MODE_TRANSLUCENT:
                return 0xff;
            case MODE_SEMI_TRANSPARENT:
                return getGradientAlphaSemiTransparent();
            default:
                return getGradientAlphaOpaque();
            }
        }

        protected final Animator setColorAnimator(final Animator animation) {
            if (mColorAnimator != null) {
                // TODO mColorAnimator.cancel();
            }
            mColorAnimator = animation;
            return animation; // return value for chaining calls
        }

        protected final Animator setGradientAlphaAnimator(final Animator animation) {
            if (mGradientAlphaAnimator != null) {
                // TODO mGradientAlphaAnimator.cancel();
            }
            mGradientAlphaAnimator = animation;
            return animation; // return value for chaining calls
        }

        protected final void setCurrentColor(final int color) {
            mCurrentColor = color;
        }

        protected final void setCurrentGradientAlpha(final int alpha) {
            mCurrentGradientAlpha = alpha;
        }

        public final synchronized void applyMode(final int mode, final boolean animate) {
            mCurrentMode = mode;

            if (animate) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        final Animator anim = generateAnimator(mode);
                        if (anim != null) {
                            anim.start();
                        }
                    }

                });
            } else {
                final int targetColor = getTargetColor(mode);
                final int targetGradientAlpha = getTargetGradientAlpha(mode);

                if (targetColor != mCurrentColor ||
                        targetGradientAlpha != mCurrentGradientAlpha) {
                    setCurrentColor(targetColor);
                    setCurrentGradientAlpha(targetGradientAlpha);

                    mHandler.removeCallbacks(mInvalidateSelf);
                    mHandler.postDelayed(mInvalidateSelf, 50);
                }
            }
        }

        public final void finishAnimating() {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    setColorAnimator(null);
                    setGradientAlphaAnimator(null);

                    final int targetColor = getTargetColor();
                    final int targetGradientAlpha = getTargetGradientAlpha();

                    if (targetColor != mCurrentColor ||
                            targetGradientAlpha != mCurrentGradientAlpha) {
                        setCurrentColor(targetColor);
                        setCurrentGradientAlpha(targetGradientAlpha);

                        mHandler.removeCallbacks(mInvalidateSelf);
                        mHandler.postDelayed(mInvalidateSelf, 50);
                    }
                }

            });
        }

        protected final Animator generateAnimator() {
            return generateAnimator(mCurrentMode);
        }

        protected final Animator generateAnimator(final int targetMode) {
            final int targetColor = getTargetColor(targetMode);
            final int targetGradientAlpha = getTargetGradientAlpha(targetMode);

            if (targetColor == mCurrentColor && targetGradientAlpha == mCurrentGradientAlpha) {
                // no values are changing - nothing to do
                return null;
            }

            final ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(),
                    mCurrentColor, targetColor).setDuration(mDSBDuration);
            final ValueAnimator gradientAlphaAnimator = ValueAnimator.ofInt(
                    mCurrentGradientAlpha, targetGradientAlpha).setDuration(mDSBDuration);

            colorAnimator.addUpdateListener(this);
            gradientAlphaAnimator.addUpdateListener(this);

            if (targetColor == mCurrentColor || colorAnimator == null) {
                // color value is not changing - only gradient alpha is changing
                return setGradientAlphaAnimator(gradientAlphaAnimator);
            }

            if (targetGradientAlpha == mCurrentGradientAlpha || gradientAlphaAnimator == null) {
                // gradient alpha is not changing - only color value is changing
                return setColorAnimator(colorAnimator);
            }

            // both values are changing - play them together
            final AnimatorSet set = new AnimatorSet().setDuration(mDSBDuration);
            set.playTogether(setColorAnimator(colorAnimator),
                    setGradientAlphaAnimator(gradientAlphaAnimator));
            set.addListener(this);
            return set;
        }
    }
}
