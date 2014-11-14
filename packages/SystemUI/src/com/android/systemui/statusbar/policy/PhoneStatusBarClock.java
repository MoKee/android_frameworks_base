/*
 * Copyright (C) 2014 ParanoidAndroid Project
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

package com.android.systemui.statusbar.policy;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.BarBackgroundUpdater;

/**
 * Digital clock exclusively for the phone status bar.
 */
public class PhoneStatusBarClock extends Clock {
    private final int mDSBDuration;

    public PhoneStatusBarClock(final Context context) {
        this(context, null);
    }

    public PhoneStatusBarClock(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhoneStatusBarClock(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);

        mDSBDuration = context.getResources().getInteger(R.integer.dsb_transition_duration);
        BarBackgroundUpdater.addListener(new BarBackgroundUpdater.UpdateListener(this) {

            @Override
            public ObjectAnimator onUpdateStatusBarIconColor(final int previousIconColor,
                    final int iconColor) {
                final int currentColor = getTextColors().getDefaultColor();
                // TODO use the resource value instead for themeability
                final int targetColor = iconColor == 0 ? 0xffffffff : iconColor;
                return ObjectAnimator.ofObject(PhoneStatusBarClock.this, "textColor",
                        new ArgbEvaluator(), currentColor, targetColor).setDuration(mDSBDuration);
            }

        });
    }

}
