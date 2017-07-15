/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import mokee.hardware.MKHardwareManager;

public class KeyDisablerTile extends QSTile<QSTile.BooleanState> {

    public KeyDisablerTile(Host host) {
        super(host);
    }

    @Override
    public boolean isAvailable() {
        return MKHardwareManager.getInstance(mContext)
                .isSupported(MKHardwareManager.FEATURE_KEY_DISABLE);
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.key_disabler);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_KEY_DISABLER;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        MKHardwareManager hardware = MKHardwareManager.getInstance(mContext);
        mState.value = !hardware.get(MKHardwareManager.FEATURE_KEY_DISABLE);
        MetricsLogger.action(mContext, getMetricsCategory(), mState.value);
        hardware.set(MKHardwareManager.FEATURE_KEY_DISABLE, mState.value);
        refreshState(mState.value);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        MKHardwareManager hardware = MKHardwareManager.getInstance(mContext);
        state.value = arg instanceof Boolean ? (Boolean) arg
                : hardware.get(MKHardwareManager.FEATURE_KEY_DISABLE);
        state.label = mContext.getString(R.string.key_disabler);
        state.contentDescription = state.label;
        state.icon = ResourceIcon.get(state.value ? R.drawable.ic_data_saver
                : R.drawable.ic_data_saver_off);
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();
    }

    @Override
    protected void setListening(boolean listening) {
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_key_disabler_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_key_disabler_changed_off);
        }
    }
}
