/*
 * Copyright (C) 2015 The MoKee OpenSource Project
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

package com.android.internal.widget;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureOverlayView;
import android.graphics.Color;
import android.util.AttributeSet;

public class LockGestureView extends GestureOverlayView implements GestureOverlayView.OnGesturingListener,
            GestureOverlayView.OnGesturePerformedListener {
    private static final int CORRECT_COLOR = Color.LTGRAY;
    private static final int WRONG_COLOR = Color.RED;

    private DisplayMode mGestureDisplayMode = DisplayMode.Correct;
    private boolean mInStealthMode = false;

    private OnLockGestureListener mOnGestureListener;

    @Override
    public void onGesturePerformed(GestureOverlayView gestureOverlayView, Gesture gesture) {
        notifyGestureDetected(gesture);
    }

    @Override
    public void onGesturingStarted(GestureOverlayView gestureOverlayView) {
        notifyGestureStart();
    }

    @Override
    public void onGesturingEnded(GestureOverlayView gestureOverlayView) {
    }

    /**
     * The call back interface for detecting gestures entered by the user.
     */
    public static interface OnLockGestureListener {

        /**
         * A new gesture has begun.
         */
        void onGestureStart();

        /**
         * The gesture was cleared.
         */
        void onGestureCleared();

        /**
         * A gesture was detected from the user.
         * @param gesture The gesture.
         */
        void onGestureDetected(Gesture gesture);
    }

    /**
     * How to display the current pattern.
     */
    public enum DisplayMode {

        /**
         * The pattern drawn is correct (i.e draw it in a friendly color)
         */
        Correct,

        /**
         * The pattern is wrong (i.e draw a foreboding color)
         */
        Wrong
    }

    public LockGestureView(Context context) {
        this(context, null);
    }

    public LockGestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setGestureVisible(true);
        addOnGesturingListener(this);
        addOnGesturePerformedListener(this);
        setGestureColor(CORRECT_COLOR);
        mClearPerformedGesture = false;
    }

    /**
     * @return Whether the view is in stealth mode.
     */
    public boolean isInStealthMode() {
        return mInStealthMode;
    }

    /**
     * Set whether the view is in stealth mode.  If true, there will be no
     * visible feedback as the user enters the gesture.
     *
     * @param inStealthMode Whether in stealth mode.
     */
    public void setInStealthMode(boolean inStealthMode) {
        mInStealthMode = inStealthMode;
        setGestureVisible(!inStealthMode);
    }

    /**
     * Set the display mode of the current pattern.  This can be useful, for
     * instance, after detecting a pattern to tell this view whether change the
     * in progress result to correct or wrong.
     * @param displayMode The display mode.
     */
    public void setDisplayMode(DisplayMode displayMode) {
        mGestureDisplayMode = displayMode;
        switch (displayMode) {
            case Correct:
                setGestureColor(CORRECT_COLOR);
                break;
            case Wrong:
                setGestureColor(WRONG_COLOR);
                break;
        }

        invalidate();
    }

    /**
     * Disable input (for instance when displaying a message that will
     * timeout so user doesn't get view into messy state).
     */
    public void disableInput() {
        mInputEnabled = false;
    }

    /**
     * Enable input.
     */
    public void enableInput() {
        mInputEnabled = true;
    }

    /**
     * Clear the gesture.
     */
    public void clearGesture() {
        resetGesture();
    }

    /**
     * Reset all pattern state.
     */
    private void resetGesture() {
        mGestureDisplayMode = DisplayMode.Correct;
        clear(false);
        invalidate();
    }

    /**
     * Set the call back for gesture detection.
     * @param onGestureListener The call back.
     */
    public void setOnGestureListener(
            OnLockGestureListener onGestureListener) {
        mOnGestureListener = onGestureListener;
    }

    private void notifyGestureStart() {
        if (mOnGestureListener != null)
            mOnGestureListener.onGestureStart();
    }

    private void notifyGestureCleared() {
        if (mOnGestureListener != null)
            mOnGestureListener.onGestureCleared();
    }

    private void notifyGestureDetected(Gesture gesture) {
        if (mOnGestureListener != null)
            mOnGestureListener.onGestureDetected(gesture);
    }
}
