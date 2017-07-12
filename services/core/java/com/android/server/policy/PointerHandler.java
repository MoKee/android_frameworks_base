/*
 * Copyright (C) 2017, ParanoidAndroid Project
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

package com.android.server.policy;

import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy.PointerEventListener;

public class PointerHandler implements PointerEventListener {

    private static final String TAG = PointerHandler.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final int SWIPE_THRESHOLD = 175;

    private boolean mScreenTouched;
    private int mDownY;

    private ThreeFingerListener mListener;

    @Override
    public void onPointerEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                mDownY = (int) event.getY();
            case MotionEvent.ACTION_MOVE:
                mScreenTouched = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (pointerCount == 3 && event.getY() - mDownY > SWIPE_THRESHOLD) {
                    if (mListener != null) {
                        mListener.onThreeFingersSwipe();
                    }
                }
                // fall through
            default:
                mScreenTouched = false;
                break;
        }
        if (DEBUG) {
            Log.d(TAG, "Screen touched= " + mScreenTouched);
        }
    }

    public void setListener(ThreeFingerListener listener) {
        mListener = listener;
    }

    public boolean isScreenTouched() {
        if (DEBUG) {
            Log.d(TAG, "isScreenTouched: mScreenTouched= " + mScreenTouched);
        }
        return mScreenTouched;
    }

    public interface ThreeFingerListener {
        public abstract void onThreeFingersSwipe();
    }
}
