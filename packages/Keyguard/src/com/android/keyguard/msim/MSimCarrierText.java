/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 * Not a Contribution.
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.IccCardConstants.State;
import com.android.internal.telephony.MSimConstants;
import com.android.internal.widget.LockPatternUtils;

import android.telephony.MSimTelephonyManager;

import static android.telephony.TelephonyManager.SIM_STATE_ABSENT;
import static android.telephony.TelephonyManager.SIM_STATE_READY;

public class MSimCarrierText extends CarrierText {
    private static final String TAG = "MSimCarrierText";
    private CharSequence []mPlmn;
    private CharSequence []mSpn;
    private State []mSimState;
    private MSimTelephonyManager mtm;

    private KeyguardUpdateMonitorCallback mMSimCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onRefreshCarrierInfo(CharSequence plmn, CharSequence spn, int sub) {
            mPlmn[sub] = plmn;
            mSpn[sub] = spn;
            updateCarrierText(mSimState, mPlmn, mSpn);
        }

        @Override
        public void onSimStateChanged(IccCardConstants.State simState, int sub) {
            mSimState[sub] = simState;
            updateCarrierText(mSimState, mPlmn, mSpn);
        }
    };

    private void initialize() {
        mtm = MSimTelephonyManager.getDefault();
        int numPhones = mtm.getPhoneCount();
        mPlmn = new CharSequence[numPhones];
        mSpn = new CharSequence[numPhones];
        mSimState = new State[numPhones];
    }

    public MSimCarrierText(Context context) {
        this(context, null);
    }

    public MSimCarrierText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    protected void updateCarrierText(State []simState, CharSequence []plmn, CharSequence []spn) {
        CharSequence text = "";
        int mSub1Status = mtm.getSimState(MSimConstants.SUB1);
        int mSub2Status = mtm.getSimState(MSimConstants.SUB2);
        if (mSub1Status == SIM_STATE_ABSENT && mSub2Status == SIM_STATE_ABSENT
                || mSub1Status == SIM_STATE_READY && mSub2Status == SIM_STATE_READY) {
            for (int i = 0; i < simState.length; i++) {
                CharSequence displayText = getCarrierTextForSimState(simState[i], plmn[i], spn[i]);
                if (mContext.getResources().getBoolean(R.bool.kg_use_all_caps)) {
                    displayText = (displayText != null ? displayText.toString().toUpperCase() : "");
                }
                text = (TextUtils.isEmpty(text)
                        ? displayText
                        : getContext().getString(R.string.msim_carrier_text_format, text, displayText));
            }
        } else {
            CharSequence displayText;
            if (mSub1Status != SIM_STATE_ABSENT) {
                displayText = getCarrierTextForSimState(simState[MSimConstants.SUB1], plmn[MSimConstants.SUB1], spn[MSimConstants.SUB1]);
            } else {
                displayText = getCarrierTextForSimState(simState[MSimConstants.SUB2], plmn[MSimConstants.SUB2], spn[MSimConstants.SUB2]);
            }
            text = displayText;
        }

        Log.d(TAG, "updateCarrierText: text = " + text);
        setText(text);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mMSimCallback);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mMSimCallback);
    }
}

