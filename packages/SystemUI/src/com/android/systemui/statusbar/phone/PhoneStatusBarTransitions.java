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
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.MSimTelephonyManager;
import android.view.View;

import com.android.systemui.R;

public final class PhoneStatusBarTransitions extends BarTransitions {
    private static final float ICON_ALPHA_WHEN_NOT_OPAQUE = 1;
    private static final float ICON_ALPHA_WHEN_LIGHTS_OUT_BATTERY_CLOCK = 0.5f;
    private static final float ICON_ALPHA_WHEN_LIGHTS_OUT_NON_BATTERY_CLOCK = 0;

    private final PhoneStatusBarView mView;
    private final float mIconAlphaWhenOpaque;

    private View mLeftSide, mStatusIcons, mSignalCluster, mClock, mCenterClock, mNetworkTraffic;
    private View mBattery, mDockBattery;
    private Animator mCurrentAnimation;

    public PhoneStatusBarTransitions(PhoneStatusBarView view) {
        super(view, new PhoneStatusBarBackgroundDrawable(view.getContext()));
        mView = view;
        final Resources res = mView.getContext().getResources();
        mIconAlphaWhenOpaque = res.getFraction(R.dimen.status_bar_icon_drawing_alpha, 1, 1);
    }

    public void init() {
        mLeftSide = mView.findViewById(R.id.notification_icon_area);
        mStatusIcons = mView.findViewById(R.id.statusIcons);
        if (MSimTelephonyManager.getDefault().isMultiSimEnabled()) {
            mSignalCluster = mView.findViewById(R.id.msim_signal_cluster);
        } else {
            mSignalCluster = mView.findViewById(R.id.signal_cluster);
        }
        mBattery = mView.findViewById(R.id.battery);
        mDockBattery = mView.findViewById(R.id.dock_battery);
        mNetworkTraffic = mView.findViewById(R.id.network_traffic);
        mClock = mView.findViewById(R.id.clock);
        mCenterClock = mView.findViewById(R.id.center_clock);
        applyModeBackground(-1, getMode(), false /*animate*/);
        applyMode(getMode(), false /*animate*/);
    }

    public ObjectAnimator animateTransitionTo(View v, float toAlpha) {
        return ObjectAnimator.ofFloat(v, "alpha", v.getAlpha(), toAlpha);
    }

    private float getNonBatteryClockAlphaFor(int mode) {
        return mode == MODE_LIGHTS_OUT ? ICON_ALPHA_WHEN_LIGHTS_OUT_NON_BATTERY_CLOCK
                : !isOpaque(mode) ? ICON_ALPHA_WHEN_NOT_OPAQUE
                : mIconAlphaWhenOpaque;
    }

    private float getBatteryClockAlpha(int mode) {
        return mode == MODE_LIGHTS_OUT ? ICON_ALPHA_WHEN_LIGHTS_OUT_BATTERY_CLOCK
                : getNonBatteryClockAlphaFor(mode);
    }

    private boolean isOpaque(int mode) {
        return !(mode == MODE_SEMI_TRANSPARENT || mode == MODE_TRANSLUCENT);
    }

    @Override
    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate);
    }

    private void applyMode(int mode, boolean animate) {
        if (mLeftSide == null) return; // pre-init
        float newAlpha = getNonBatteryClockAlphaFor(mode);
        float newAlphaBC = getBatteryClockAlpha(mode);
        if (mCurrentAnimation != null) {
            mCurrentAnimation.cancel();
        }
        if (animate) {
            AnimatorSet anims = new AnimatorSet();
            anims.playTogether(
                    animateTransitionTo(mLeftSide, newAlpha),
                    animateTransitionTo(mStatusIcons, newAlpha),
                    animateTransitionTo(mSignalCluster, newAlpha),
                    animateTransitionTo(mNetworkTraffic, newAlpha),
                    animateTransitionTo(mDockBattery, newAlphaBC),
                    animateTransitionTo(mBattery, newAlphaBC),
                    animateTransitionTo(mClock, newAlphaBC),
                    animateTransitionTo(mCenterClock, newAlphaBC)
                    );
            if (mode == MODE_LIGHTS_OUT) {
                anims.setDuration(LIGHTS_OUT_DURATION);
            }
            anims.start();
            mCurrentAnimation = anims;
        } else {
            mLeftSide.setAlpha(newAlpha);
            mStatusIcons.setAlpha(newAlpha);
            mSignalCluster.setAlpha(newAlpha);
            mNetworkTraffic.setAlpha(newAlpha);
            mDockBattery.setAlpha(newAlphaBC);
            mBattery.setAlpha(newAlphaBC);
            mClock.setAlpha(newAlphaBC);
            mCenterClock.setAlpha(newAlphaBC);
        }
    }

    protected static class PhoneStatusBarBackgroundDrawable
            extends BarTransitions.BarBackgroundDrawable {
        private final Context mContext;

        private int mOverrideColor = 0;
        private int mOverrideGradientAlpha = 0;

        public PhoneStatusBarBackgroundDrawable(final Context context) {
            super(context,
                    R.color.status_bar_background_opaque,
                    R.color.status_bar_background_semi_transparent,
                    R.drawable.status_background);

            mContext = context;

            final GradientObserver obs = new GradientObserver(this, new Handler());
            (mContext.getContentResolver()).registerContentObserver(
                    GradientObserver.DYNAMIC_SYSTEM_BARS_GRADIENT_URI,
                    false, obs, UserHandle.USER_ALL);

            mOverrideGradientAlpha = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.DYNAMIC_SYSTEM_BARS_GRADIENT_STATE, 0) == 1 ?
                            0xff : 0;

            BarBackgroundUpdater.addListener(new BarBackgroundUpdater.UpdateListener(this) {

                @Override
                public Animator onUpdateStatusBarColor(final int previousColor, final int color) {
                    mOverrideColor = color;
                    return generateAnimator();
                }

            });
            BarBackgroundUpdater.init(context);
        }

        @Override
        protected int getColorOpaque() {
            return mOverrideColor == 0 ? super.getColorOpaque() : mOverrideColor;
        }

        @Override
        protected int getColorSemiTransparent() {
            return mOverrideColor == 0 ? super.getColorSemiTransparent() :
                    (mOverrideColor & 0x00ffffff | 0x7f000000);
        }

        @Override
        protected int getGradientAlphaOpaque() {
            return mOverrideGradientAlpha;
        }

        @Override
        protected int getGradientAlphaSemiTransparent() {
            return mOverrideGradientAlpha & 0x7f;
        }

        public void setOverrideGradientAlpha(final int alpha) {
            mOverrideGradientAlpha = alpha;
            generateAnimator().start();
        }
    }

    private static final class GradientObserver extends ContentObserver {
        private static final Uri DYNAMIC_SYSTEM_BARS_GRADIENT_URI = Settings.System.getUriFor(
                Settings.System.DYNAMIC_SYSTEM_BARS_GRADIENT_STATE);

        private final PhoneStatusBarBackgroundDrawable mDrawable;

        private GradientObserver(final PhoneStatusBarBackgroundDrawable drawable,
                final Handler handler) {
            super(handler);
            mDrawable = drawable;
        }

        @Override
        public void onChange(final boolean selfChange) {
            mDrawable.setOverrideGradientAlpha(Settings.System.getInt(
                    mDrawable.mContext.getContentResolver(),
                    Settings.System.DYNAMIC_SYSTEM_BARS_GRADIENT_STATE, 0) == 1 ? 0xff : 0);
        }
    }
}
