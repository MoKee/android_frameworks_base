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
import android.content.res.Resources;
import android.telephony.MSimTelephonyManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.Iterator;

public final class PhoneStatusBarTransitions extends BarTransitions {
    private static final float ICON_ALPHA_WHEN_NOT_OPAQUE = 1;
    private static final float ICON_ALPHA_WHEN_LIGHTS_OUT_BATTERY_CLOCK = 0.5f;
    private static final float ICON_ALPHA_WHEN_LIGHTS_OUT_NON_BATTERY_CLOCK = 0;

    private final PhoneStatusBarView mView;
    private final float mIconAlphaWhenOpaque;

    private ArrayList<ImageView> mIcons = new ArrayList<ImageView>();
    private ArrayList<TextView> mTexts = new ArrayList<TextView>();

    private View mLeftSide, mStatusIcons, mSignalCluster, mClock, mCenterClock, mNetworkTraffic;
    private View mBattery, mDockBattery;
    private Animator mCurrentAnimation;

    public PhoneStatusBarTransitions(PhoneStatusBarView view) {
        super(view, R.drawable.status_background, R.color.status_bar_background_opaque,
                R.color.status_bar_background_semi_transparent);
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

    public boolean isOpaque(int mode) {
        return !(mode == MODE_SEMI_TRANSPARENT || mode == MODE_TRANSLUCENT);
    }

    @Override
    public void transitionTo(int mode, boolean animate) {
        if (mode == MODE_SEMI_TRANSPARENT || mode == MODE_TRANSLUCENT) {
            changeColorIconBackground(-3, -3);
        }
        super.transitionTo(mode, animate);
    }

    @Override
    protected void onTransition(int oldMode, int newMode, boolean animate) {
        super.onTransition(oldMode, newMode, animate);
        applyMode(newMode, animate);
    }

    public void addIcon(ImageView iv) {
        if (!mIcons.contains(iv)) {
            mIcons.add(iv);
        }
    }

    public void addText(TextView tv) {
        if (!mTexts.contains(tv)) {
            mTexts.add(tv);
        }
    }

    @Override
    public void changeColorIconBackground(int bg_color, int ic_color) {
        super.changeColorIconBackground(bg_color, ic_color);
        if (getMode() == MODE_SEMI_TRANSPARENT || getMode() == MODE_TRANSLUCENT) {
            return;
        }
        if (isBrightColor(bg_color)) {
            ic_color = Color.BLACK;
        }

        for (Iterator <ImageView> ivIterator = mIcons.iterator(); ivIterator.hasNext();) {
            ImageView icon = ivIterator.next();
            if (icon != null) {
                if (ic_color == -3) {
                    icon.clearColorFilter();
                } else {
                    icon.setColorFilter(ic_color, PorterDuff.Mode.SRC_ATOP);
                }
            } else {
                ivIterator.remove();
            }
        }

        for (Iterator <TextView> tvIterator = mTexts.iterator(); tvIterator.hasNext();) {
            TextView tv = tvIterator.next();
            if (tv != null) {
                if (ic_color == -3) {
                    tv.setTextColor(Color.WHITE);
                } else {
                    tv.setTextColor(ic_color);
                }
            } else {
                tvIterator.remove();
            }
        }
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
}
