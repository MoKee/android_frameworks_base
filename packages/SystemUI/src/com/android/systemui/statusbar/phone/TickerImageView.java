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

package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;

import com.android.systemui.R;

import java.util.ArrayList;

public class TickerImageView extends ImageSwitcher {
    private final Handler mHandler;
    private final int mDSBDuration;
    private int mPreviousOverrideIconColor = 0;
    private int mOverrideIconColor = 0;

    public TickerImageView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mHandler = new Handler();
        mDSBDuration = context.getResources().getInteger(R.integer.dsb_transition_duration);
        BarBackgroundUpdater.addListener(new BarBackgroundUpdater.UpdateListener(this) {

            @Override
            public AnimatorSet onUpdateStatusBarIconColor(final int previousIconColor,
                    final int iconColor) {
                mPreviousOverrideIconColor = previousIconColor;
                mOverrideIconColor = iconColor;

                final ArrayList<Animator> anims = new ArrayList<Animator>();

                final int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final ImageView iv = (ImageView) getChildAt(i);
                    if (iv != null) {
                        if (mOverrideIconColor == 0) {
                            mHandler.post(new Runnable() {

                                @Override
                                public void run() {
                                    iv.setColorFilter(null);
                                }

                            });
                        } else {
                            anims.add(ObjectAnimator.ofObject(iv, "colorFilter",
                                    new ArgbEvaluator(), mPreviousOverrideIconColor,
                                    mOverrideIconColor).setDuration(mDSBDuration));
                        }
                    }
                }

                if (anims.isEmpty()) {
                    return null;
                } else {
                    final AnimatorSet animSet = new AnimatorSet();
                    animSet.playTogether(anims);
                    return animSet;
                }
            }

        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addView(final View child, final int index, final ViewGroup.LayoutParams params) {
        if (child instanceof ImageView) {
            if (mOverrideIconColor == 0) {
                ((ImageView) child).setColorFilter(null);
            } else {
                ((ImageView) child).setColorFilter(mOverrideIconColor,
                        PorterDuff.Mode.MULTIPLY);
            }
        }

        super.addView(child, index, params);
    }

}
