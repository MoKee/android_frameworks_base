package com.android.systemui.quicksettings;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;

public class SleepScreenTile extends QuickSettingsTile {

    private PowerManager pm;

    public SleepScreenTile(Context context, QuickSettingsController qsc) {
        super(context, qsc);
        pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mOnClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                pm.goToSleep(SystemClock.uptimeMillis());
            }
        };
        mOnLongClick = new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                triggerVirtualKeypress(KeyEvent.KEYCODE_POWER, true);
                return true;
            }
        };
    }

    @Override
    void onPostCreate() {
        updateTile();
        super.onPostCreate();
    }

    @Override
    public void updateResources() {
        updateTile();
        super.updateResources();
    }

    private synchronized void updateTile() {
        mDrawable = R.drawable.ic_qs_power;
        mLabel = mContext.getString(R.string.quick_settings_power);
    }

    private void triggerVirtualKeypress(final int keyCode, final boolean longPress) {
        new Thread(new Runnable() {
            public void run() {
                InputManager im = InputManager.getInstance();
                KeyEvent keyEvent;
                if (longPress) {
                    keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                    keyEvent.changeFlags(keyEvent, KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_LONG_PRESS);
                } else {
                    keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                    keyEvent.changeFlags(keyEvent, KeyEvent.FLAG_FROM_SYSTEM);
                }
                im.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT);
            }
        }).start();
    }

}
