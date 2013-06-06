/*
 * Copyright (C) 2013 The MoKee OpenSource Project
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

package com.android.systemui.statusbar.toggle;

import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class TogglePowerButtonListener implements View.OnTouchListener {

    private int mInjectKeyCode;
    private long deayed;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                deayed = SystemClock.uptimeMillis();
                mHandler.postDelayed(mPowerMenuRunnable, 200);
                break;
            case MotionEvent.ACTION_UP:
                deayed = SystemClock.uptimeMillis() - deayed;
                if (deayed < 200) {
                    mHandler.removeCallbacks(mPowerMenuRunnable);
                    injectKeyDelayed(KeyEvent.KEYCODE_POWER, 10);
                }
                break;
        }
        return true;
    }

    private void injectKeyDelayed(int keycode, int event) {
        mInjectKeyCode = keycode;
        mHandler.removeCallbacks(mInjectKeyDownRunnable);
        mHandler.removeCallbacks(mInjectKeyUpRunnable);
        mHandler.post(mInjectKeyDownRunnable);
        mHandler.postDelayed(mInjectKeyUpRunnable, event);
    }

    final Runnable mInjectKeyDownRunnable = new Runnable() {
        public void run() {
            final KeyEvent ev = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_DOWN, mInjectKeyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD,
                    0,
                    KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                    InputDevice.SOURCE_KEYBOARD);
            InputManager.getInstance().injectInputEvent(ev,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    };

    final Runnable mInjectKeyUpRunnable = new Runnable() {
        public void run() {
            final KeyEvent ev = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    KeyEvent.ACTION_UP, mInjectKeyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                    KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
                    InputDevice.SOURCE_KEYBOARD);
            InputManager.getInstance().injectInputEvent(ev,
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    };

    final Runnable mPowerMenuRunnable = new Runnable() {
        public void run() {
            injectKeyDelayed(KeyEvent.KEYCODE_POWER, 1000);
        }
    };

    private static Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            }
        }
    };
}
