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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;

import java.lang.Math;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BarBackgroundUpdater {
    private final static boolean DEBUG_ALL = false;
    private final static String LOG_TAG = BarBackgroundUpdater.class.getSimpleName();

    private final static boolean DEBUG_COLOR_CHANGE = DEBUG_ALL || false;
    private final static boolean DEBUG_EXCESSIVE_DELAY = DEBUG_ALL || false;
    private final static boolean DEBUG_FLOOD_ALL_DELAY = DEBUG_ALL || false;

    private static long sMinDelay = 450; // time to enforce between the screenshots

    private static boolean PAUSED = true;

    private final static BroadcastReceiver RECEIVER = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized(BarBackgroundUpdater.class) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    pause();
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    resume();
                }
            }
        }

    };

    private final static Thread THREAD = new Thread(new Runnable() {

        @Override
        public void run() {
            while (true) {
                final long now = System.currentTimeMillis();

                if (PAUSED) {
                    // we have been told to do nothing; wait for notify to continue
                    synchronized (BarBackgroundUpdater.class) {
                        try {
                            BarBackgroundUpdater.class.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    continue;
                }

                if (mStatusEnabled || mNavigationEnabled) {
                    final Context context = mContext;

                    if (context == null) {
                        // we haven't been initiated yet; retry in a bit

                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            return;
                        }

                        continue;
                    }

                    final WindowManager wm =
                        (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

                    final int rotation = wm.getDefaultDisplay().getRotation();
                    final boolean isLandscape = rotation == Surface.ROTATION_90 ||
                        rotation == Surface.ROTATION_270;

                    final Resources r = context.getResources();
                    final int statusBarHeight = r.getDimensionPixelSize(
                        r.getIdentifier("status_bar_height", "dimen", "android"));
                    final int navigationBarHeight = r.getDimensionPixelSize(
                        r.getIdentifier("navigation_bar_height" + (isLandscape ?
                            "_landscape" : ""), "dimen", "android"));

                    if (navigationBarHeight <= 0 && mNavigationEnabled) {
                        // the navigation bar height is not positive - no dynamic navigation bar
                        Settings.System.putInt(context.getContentResolver(),
                            Settings.System.DYNAMIC_NAVIGATION_BAR_STATE, 0);

                        // configuration has changed - abort and retry in a bit
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            return;
                        }

                        continue;
                    }

                    final int[] colors = BarBackgroundUpdaterNative.getColors(rotation,
                            statusBarHeight, navigationBarHeight,
                            2 + (isLandscape ? navigationBarHeight : 0));

                    if (mStatusEnabled) {
                        final int statusBarOverrideColor = mStatusFilterEnabled ?
                                filter(colors[0], -10) : colors[0];
                        updateStatusBarColor(statusBarOverrideColor);

                        // magic from http://www.w3.org/TR/AERT#color-contrast
                        final float statusBarBrightness =
                                (0.299f * Color.red(statusBarOverrideColor) +
                                0.587f * Color.green(statusBarOverrideColor) +
                                0.114f * Color.blue(statusBarOverrideColor)) / 255;
                        final boolean isStatusBarConsistent = colors[1] == 1;
                        updateStatusBarIconColor(statusBarBrightness > 0.7f &&
                                isStatusBarConsistent ? 0x95000000 : 0xFFFFFFFF);
                    } else {
                        // dynamic status bar is disabled
                        updateStatusBarColor(0);
                        updateStatusBarIconColor(0);
                    }

                    if (mNavigationEnabled) {
                        final int navigationBarOverrideColor = colors[2];
                        updateNavigationBarColor(navigationBarOverrideColor);

                        // magic from http://www.w3.org/TR/AERT#color-contrast
                        final float navigationBarBrightness =
                                (0.299f * Color.red(navigationBarOverrideColor) +
                                0.587f * Color.green(navigationBarOverrideColor) +
                                0.114f * Color.blue(navigationBarOverrideColor)) / 255;
                        final boolean isNavigationBarConsistent = colors[3] == 1;
                        updateNavigationBarIconColor(navigationBarBrightness > 0.7f &&
                                isNavigationBarConsistent ? 0x95000000 : 0xFFFFFFFF);
                    } else {
                        // dynamic navigation bar is disabled
                        updateNavigationBarColor(0);
                        updateNavigationBarIconColor(0);
                    }
                } else {
                    // we are disabled completely - shush
                    updateStatusBarColor(0);
                    updateStatusBarIconColor(0);
                    updateNavigationBarColor(0);
                    updateNavigationBarIconColor(0);
                }

                // start queued animators
                if (!mQueuedAnimators.isEmpty()) {
                    final AnimatorSet animSet = new AnimatorSet();

                    animSet.playTogether(mQueuedAnimators);
                    mQueuedAnimators.clear();

                    mHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            animSet.start();
                        }

                    });
                }

                // do a quick cleanup of the listener list
                synchronized(BarBackgroundUpdater.class) {
                    final ArrayList<UpdateListener> removables = new ArrayList<UpdateListener>();

                    for (final UpdateListener listener : mListeners) {
                        if (listener.shouldGc()) {
                            removables.add(listener);
                        }
                    }

                    for (final UpdateListener removable : removables) {
                        mListeners.remove(removable);
                    }
                }

                final long delta = System.currentTimeMillis() - now;
                final long delay = Math.max(sMinDelay, delta * 2);

                if (DEBUG_FLOOD_ALL_DELAY || (DEBUG_EXCESSIVE_DELAY && delay > sMinDelay)) {
                    Log.d(LOG_TAG, "delta=" + Long.toString(delta) + "ms " +
                            "delay=" + Long.toString(delay) + "ms");
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

    });

    static {
        THREAD.setPriority(4);
        THREAD.start();
    }

    private static boolean mStatusEnabled = false;
    private static boolean mStatusFilterEnabled = false;
    private static int mPreviousStatusBarOverrideColor = 0;
    private static int mStatusBarOverrideColor = 0;
    private static int mPreviousStatusBarIconOverrideColor = 0;
    private static int mStatusBarIconOverrideColor = 0;

    private static boolean mNavigationEnabled = false;
    private static int mPreviousNavigationBarOverrideColor = 0;
    private static int mNavigationBarOverrideColor = 0;
    private static int mPreviousNavigationBarIconOverrideColor = 0;
    private static int mNavigationBarIconOverrideColor = 0;

    private static final ArrayList<UpdateListener> mListeners = new ArrayList<UpdateListener>();
    private static final ArrayList<Animator> mQueuedAnimators = new ArrayList<Animator>();
    private static Handler mHandler = null;
    private static Context mContext = null;
    private static SettingsObserver mObserver = null;

    private BarBackgroundUpdater() {
    }

    private static void anim(final Animator animator) {
        if (animator != null && mHandler != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    animator.start();
                }

            });
        }
    }

    private synchronized static void setPauseState(final boolean isPaused) {
        PAUSED = isPaused;
        if (!isPaused) {
            // the thread should be notified to resume
            BarBackgroundUpdater.class.notify();
        }
    }

    private static void pause() {
        setPauseState(true);
    }

    private static void resume() {
        setPauseState(false);
    }

    public synchronized static void init(final Context context) {
        if (mContext != null) {
            mContext.unregisterReceiver(RECEIVER);

            if (mObserver != null) {
                mContext.getContentResolver().unregisterContentObserver(mObserver);
            }
        }

        mHandler = new Handler();
        mContext = context;

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(RECEIVER, filter);

        if (mObserver == null) {
            mObserver = new SettingsObserver(new Handler());
        }

        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.DYNAMIC_STATUS_BAR_STATE),
                false, mObserver, UserHandle.USER_ALL);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.DYNAMIC_NAVIGATION_BAR_STATE),
                false, mObserver, UserHandle.USER_ALL);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.DYNAMIC_STATUS_BAR_FILTER_STATE),
                false, mObserver, UserHandle.USER_ALL);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.EXPERIMENTAL_DSB_FREQUENCY),
                false, mObserver, UserHandle.USER_ALL);

        mStatusEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.DYNAMIC_STATUS_BAR_STATE, 0, UserHandle.USER_CURRENT) == 1;
        mNavigationEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.DYNAMIC_NAVIGATION_BAR_STATE, 0, UserHandle.USER_CURRENT) == 1;
        mStatusFilterEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.DYNAMIC_STATUS_BAR_FILTER_STATE, 0, UserHandle.USER_CURRENT) == 1;

        final int freq = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.EXPERIMENTAL_DSB_FREQUENCY, 2, UserHandle.USER_CURRENT);
        if (freq == 0) {
            // approx 1 fps
            sMinDelay = 950;
        } else if (freq < 0) {
            // approx (-freq)^-1 fps
            sMinDelay = Math.abs(1000 * (-freq) - 50);
        } else {
            // approx freq fps
            sMinDelay = Math.max(0, Math.round(1000 / freq) - 50);
        }

        final Display d = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        final Point sizePoint = new Point();
        d.getRealSize(sizePoint);
        BarBackgroundUpdaterNative.setScreenSize(d.getRotation(), sizePoint.x, sizePoint.y);

        resume();
    }

    public synchronized static void addListener(final UpdateListener... listeners) {
        for (final UpdateListener listener : listeners) {
            if (listener == null) {
                continue;
            }

            anim(listener.onUpdateStatusBarColor(mPreviousStatusBarOverrideColor,
                    mStatusBarOverrideColor));

            anim(listener.onUpdateStatusBarIconColor(mPreviousStatusBarIconOverrideColor,
                    mStatusBarIconOverrideColor));

            anim(listener.onUpdateNavigationBarColor(mPreviousNavigationBarOverrideColor,
                    mNavigationBarOverrideColor));

            anim(listener.onUpdateNavigationBarIconColor(mPreviousNavigationBarIconOverrideColor,
                    mNavigationBarIconOverrideColor));

            boolean shouldAdd = true;

            for (final UpdateListener existingListener : mListeners) {
                if (existingListener == listener) {
                    shouldAdd = false;
                }
            }

            if (shouldAdd) {
                mListeners.add(listener);
            }
        }
    }

    private static int filter(final int original, final float diff) {
        final int red = (int) (Color.red(original) + diff);
        final int green = (int) (Color.green(original) + diff);
        final int blue = (int) (Color.blue(original) + diff);

        return Color.argb(
                Color.alpha(original),
                red > 0 ?
                        red < 255 ?
                                red :
                                255 :
                        0,
                green > 0 ?
                        green < 255 ?
                                green :
                                255 :
                        0,
                blue > 0 ?
                        blue < 255 ?
                                blue :
                                255 :
                        0
        );
    }

    private static int getPixel(final Bitmap bitmap, final int x, final int y) {
        if (bitmap == null) {
            // just silently ignore this
            return Color.BLACK;
        }

        if (x == 0) {
            Log.w(LOG_TAG, "getPixel for x=0 is not allowed; returning a black pixel");
            return Color.BLACK;
        }

        if (y == 0) {
            Log.w(LOG_TAG, "getPixel for y=0 is not allowed; returning a black pixel");
            return Color.BLACK;
        }

        return bitmap.getPixel(x > 0 ? x : bitmap.getWidth() + x,
            y > 0 ? y : bitmap.getHeight() + y);
    }

    public synchronized static void updateStatusBarColor(final int newColor) {
        if (mStatusBarOverrideColor == newColor) {
            return;
        }

        mPreviousStatusBarOverrideColor = mStatusBarOverrideColor;
        mStatusBarOverrideColor = newColor;

        if (DEBUG_COLOR_CHANGE) {
            Log.d(LOG_TAG, "statusBarOverrideColor=" + (newColor == 0 ? "none" :
                    "0x" + Integer.toHexString(newColor)));
        }

        for (final UpdateListener listener : mListeners) {
            final Animator anim = listener.onUpdateStatusBarColor(
                    mPreviousStatusBarOverrideColor, mStatusBarOverrideColor);
            if (anim != null) {
                mQueuedAnimators.add(anim);
            }
        }
    }

    public synchronized static void updateStatusBarIconColor(final int newColor) {
        if (mStatusBarIconOverrideColor == newColor) {
            return;
        }

        mPreviousStatusBarIconOverrideColor = mStatusBarIconOverrideColor;
        mStatusBarIconOverrideColor = newColor;

        if (DEBUG_COLOR_CHANGE) {
            Log.d(LOG_TAG, "statusBarIconOverrideColor=" + (newColor == 0 ? "none" :
                    "0x" + Integer.toHexString(newColor)));
        }

        for (final UpdateListener listener : mListeners) {
            final Animator anim = listener.onUpdateStatusBarIconColor(
                    mPreviousStatusBarIconOverrideColor, mStatusBarIconOverrideColor);
            if (anim != null) {
                mQueuedAnimators.add(anim);
            }
        }
    }

    public synchronized static void updateNavigationBarColor(final int newColor) {
        if (mNavigationBarOverrideColor == newColor) {
            return;
        }

        mPreviousNavigationBarOverrideColor = mNavigationBarOverrideColor;
        mNavigationBarOverrideColor = newColor;

        if (DEBUG_COLOR_CHANGE) {
            Log.d(LOG_TAG, "navigationBarOverrideColor=" + (newColor == 0 ? "none" :
                    "0x" + Integer.toHexString(newColor)));
        }

        for (final UpdateListener listener : mListeners) {
            final Animator anim = listener.onUpdateNavigationBarColor(
                    mPreviousNavigationBarOverrideColor, mNavigationBarOverrideColor);
            if (anim != null) {
                mQueuedAnimators.add(anim);
            }
        }
    }

    public synchronized static void updateNavigationBarIconColor(final int newColor) {
        if (mNavigationBarIconOverrideColor == newColor) {
            return;
        }

        mPreviousNavigationBarIconOverrideColor = mNavigationBarIconOverrideColor;
        mNavigationBarIconOverrideColor = newColor;

        if (DEBUG_COLOR_CHANGE) {
            Log.d(LOG_TAG, "navigationBarIconOverrideColor=" + (newColor == 0 ? "none" :
                    "0x" + Integer.toHexString(newColor)));
        }

        for (final UpdateListener listener : mListeners) {
            final Animator anim = listener.onUpdateNavigationBarIconColor(
                    mPreviousNavigationBarIconOverrideColor, mNavigationBarIconOverrideColor);
            if (anim != null) {
                mQueuedAnimators.add(anim);
            }
        }
    }

    public static class UpdateListener {
        private final WeakReference<Object> mRef;

        public UpdateListener(final Object ref) {
            mRef = new WeakReference<Object>(ref);
        }

        public final boolean shouldGc() {
            return mRef.get() == null;
        }

        public Animator onUpdateStatusBarColor(final int previousColor, final int color) {
            return null;
        }

        public Animator onUpdateStatusBarIconColor(final int previousIconColor,
                final int iconColor) {
            return null;
        }

        public Animator onUpdateNavigationBarColor(final int previousColor,
                final int color) {
            return null;
        }

        public Animator onUpdateNavigationBarIconColor(final int previousIconColor,
                final int iconColor) {
            return null;
        }
    }

    private static final class SettingsObserver extends ContentObserver {
        private SettingsObserver(final Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(final boolean selfChange) {
            mStatusEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.DYNAMIC_STATUS_BAR_STATE, 0, UserHandle.USER_CURRENT) == 1;
            mNavigationEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.DYNAMIC_NAVIGATION_BAR_STATE, 0, UserHandle.USER_CURRENT) == 1;
            mStatusFilterEnabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.DYNAMIC_STATUS_BAR_FILTER_STATE, 0,
                    UserHandle.USER_CURRENT) == 1;

            final int freq = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.EXPERIMENTAL_DSB_FREQUENCY, 2, UserHandle.USER_CURRENT);
            if (freq == 0) {
                // approx 1 fps
                sMinDelay = 950;
            } else if (freq < 0) {
                // approx (-freq)^-1 fps
                sMinDelay = Math.abs(1000 * (-freq) - 50);
            } else {
                // approx freq fps
                sMinDelay = Math.max(0, Math.round(1000 / freq) - 50);
            }
        }
    }

}
