/*
 * Copyright (C) 2017 The MoKee Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import mokee.hardware.MKHardwareManager;
import mokee.providers.MKSettings;

public class KeyDisablerTile extends QSTile<QSTile.BooleanState> {

    private static final String TAG = "KeyDisablerTile";

    private final BroadcastReceiver screenOffListener =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    restoreKey();
                }
            };

    public KeyDisablerTile(Host host) {
        super(host);
        // I'm SystemUI! No need to unregister! lol
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(screenOffListener, filter);
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
    protected void setListening(boolean listening) {
    }

    private boolean isNavigationBarShown() {
        return MKSettings.Global.getInt(mContext.getContentResolver(),
                MKSettings.Global.DEV_FORCE_SHOW_NAVBAR, 0) != 0;
    }

    @Override
    protected void handleClick() {
        MKHardwareManager hardware = MKHardwareManager.getInstance(mContext);
        if (isNavigationBarShown()) {
            mState.value = false; // always off if using nav bar
            Log.d(TAG, "Tile is clicked, but navbar is shown, keep it off");
        } else {
            mState.value = !hardware.get(MKHardwareManager.FEATURE_KEY_DISABLE);
            MetricsLogger.action(mContext, getMetricsCategory(), mState.value);
            hardware.set(MKHardwareManager.FEATURE_KEY_DISABLE, mState.value);
            Log.d(TAG, "Tile is clicked, set to " + String.valueOf(mState.value));
        }
        refreshState(mState.value);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        MKHardwareManager hardware = MKHardwareManager.getInstance(mContext);
        if (isNavigationBarShown()) {
            state.value = false; // always off if using nav bar
            state.icon = ResourceIcon.get(R.drawable.ic_navkey_invalid);
        } else {
            state.value = arg instanceof Boolean ? (Boolean) arg
                    : hardware.get(MKHardwareManager.FEATURE_KEY_DISABLE);
            state.icon = ResourceIcon.get(state.value
                    ? R.drawable.ic_navkey_off
                    : R.drawable.ic_navkey_on);
        }
        state.label = mContext.getString(R.string.key_disabler);
        state.contentDescription = state.label;
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();
    }

    private void restoreKey() {
        if (isNavigationBarShown()) {
            return;
        }

        MKHardwareManager hardware = MKHardwareManager.getInstance(mContext);
        mState.value = false;
        hardware.set(MKHardwareManager.FEATURE_KEY_DISABLE, mState.value);
        refreshState(mState.value);
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent("org.mokee.mkparts.BUTTON_SETTINGS");
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
