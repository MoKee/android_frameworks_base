/**
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.server.fingerprint;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.View.OnTouchListener;
import android.view.View;
import android.provider.Settings;
import android.widget.ImageView;
import android.view.MotionEvent;
import android.util.Slog;

import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;

import android.content.res.Resources;

import java.io.PrintWriter;

import vendor.xiaomi.hardware.fingerprintextension.V1_0.IXiaomiFingerprint;

import android.os.ServiceManager;

public class FacolaView extends ImageView implements OnTouchListener {
    private final int mX, mY, mW, mH;
    private final Paint mPaintFingerprint = new Paint();
    private final Paint mPaintShow = new Paint();
    private IXiaomiFingerprint mXiaomiFingerprint = null;
    private boolean mInsideCircle = false;
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();

    private final Handler mMainHandler;
    private boolean visible = false;

    // Settings that should be changed when toggling animations
    private static final String[] TOGGLE_ANIMATION_TARGETS = {
        Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.ANIMATOR_DURATION_SCALE
    };
    private static final String ANIMATION_ON_VALUE = "1";
    private static final String ANIMATION_OFF_VALUE = "0";

    private final WindowManager mWM;
    private final DisplayManager mDisplayManager;
    private final Context mContext;

    FacolaView(Context context) {
        super(context);

        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());

        String[] location = android.os.SystemProperties.get("persist.vendor.sys.fp.fod.location.X_Y", "").split(",");
        String[] size = android.os.SystemProperties.get("persist.vendor.sys.fp.fod.size.width_height", "").split(",");
        if(size.length == 2 && location.length == 2) {
            mX = Integer.parseInt(location[0]);
            mY = Integer.parseInt(location[1]);
            mW = Integer.parseInt(size[0]);
            mH = Integer.parseInt(size[1]);
        } else {
            mX = -1;
            mY = -1;
            mW = -1;
            mH = -1;
        }

        mPaintFingerprint.setAntiAlias(true);
        mPaintFingerprint.setColor(Color.GREEN);

        mPaintShow.setAntiAlias(true);
        mPaintShow.setColor(Color.argb(0x18, 0x00, 0xff, 0x00));
        setOnTouchListener(this);
        mWM = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

        this.setBackground(context.getDrawable(com.android.internal.R.drawable.fingerprint_underscreen));

        Slog.d("Mi9-FP", "Created facola...");
        try {
            if(mW != -1)
                mXiaomiFingerprint = IXiaomiFingerprint.getService();
        } catch(Exception e) {
            Slog.d("Mi9-FP", "Failed getting xiaomi fingerprint service", e);
        }

        mDisplayManager = context.getSystemService(DisplayManager.class);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Slog.d("Mi9-FP", "Drawing at " + mX + ", " + mY + ", " + mW + ", " + mH);
        //TODO w!=h?
        if(mInsideCircle) {
            canvas.drawCircle(mW/2, mH/2, (float) (mW/2.0f), this.mPaintFingerprint);
            try {
                int nitValue = 3;
                setDim(true);
                if(mXiaomiFingerprint != null) {
                    try {
                        mXiaomiFingerprint.extCmd(0xa, nitValue);
                    } catch(Exception e) {}
                }
            } catch(Exception e) {
                Slog.d("Mi9-FP", "Failed calling xiaomi fp extcmd");
            }
        } else {
            canvas.drawCircle(mW/2, mH/2, (float) (mW/2.0f), this.mPaintShow);
            try {
                if(mXiaomiFingerprint != null)
                    mXiaomiFingerprint.extCmd(0xa, 0);
            } catch(Exception e) {
                Slog.d("Mi9-FP", "Failed calling xiaomi fp extcmd");
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);

        boolean newInside = (x > 0 && x < mW) && (y > 0 && y < mW);
        if(event.getAction() == MotionEvent.ACTION_UP) {
            newInside = false;
            setDim(false);
        }

        Slog.d("Mi9-FP", "Got action " + event.getAction() + ", x = " + x + ", y = " + y + ", inside = " + mInsideCircle + "/" + newInside);
        if(newInside == mInsideCircle) return mInsideCircle;
        mInsideCircle = newInside;

        invalidate();

        if(!mInsideCircle)
            return false;

        return true;
    }

    public void show() {
        if(mX == -1 || mY == -1 || mW == -1 || mH == -1) return;

        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            Settings.Global.putString(mContext.getContentResolver(), animationPreference, ANIMATION_OFF_VALUE);
        }

        try {
            PrintWriter writer = new PrintWriter("/sys/devices/virtual/touch/tp_dev/fod_status", "UTF-8");
            writer.println("1");
            writer.close();
        } catch(Exception e) {
            Slog.d("Mi9-FP", "Failed setting fod status for touchscreen");
        }

        mParams.x = mX;
        mParams.y = mY;

        mParams.height = mW;
        mParams.width = mH;
        mParams.format = PixelFormat.TRANSLUCENT;

        mParams.type = WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY;
        mParams.setTitle("Fingerprint on display");
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
            WindowManager.LayoutParams.FLAG_DIM_BEHIND |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mParams.dimAmount = .1f;

        mParams.packageName = "android";

        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mWM.addView(this, mParams);
        visible = true;

    }

    public void hide() {
        if(mX == -1 || mY == -1 || mW == -1 || mH == -1) return;

        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            Settings.Global.putString(mContext.getContentResolver(), animationPreference, ANIMATION_ON_VALUE);
        }

        try {
            if(mXiaomiFingerprint != null)
                mXiaomiFingerprint.extCmd(0xa, 0);
        } catch(Exception e) {
            Slog.d("Mi9-FP", "Failed calling xiaomi fp extcmd");
        }
        try {
            PrintWriter writer = new PrintWriter("/sys/devices/virtual/touch/tp_dev/fod_status", "UTF-8");
            writer.println("0");
            writer.close();
        } catch(Exception e) {
            Slog.d("Mi9-FP", "Failed setting fod status for touchscreen");
        }

        Slog.d("Mi9-FP", "Removed facola");
        mWM.removeView(this);
        visible = false;
        setDim(false);
    }

    private void setDim(boolean dim) {
        int curBrightness = Settings.System.getInt(getContext().getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 100);
        float dimAmount = (float) curBrightness / 255.0f;
        dimAmount = 0.80f - dimAmount;

        if (dimAmount < 0) {
            dimAmount = 0f;
        }

        if (dim) {
            mParams.dimAmount = dimAmount;
            mWM.updateViewLayout(this, mParams);
            mDisplayManager.setTemporaryBrightness(255);
        } else {
            mParams.dimAmount = .0f;
            mWM.updateViewLayout(this, mParams);
            mDisplayManager.setTemporaryBrightness(curBrightness);
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, curBrightness, UserHandle.USER_CURRENT);
        }
    }
}
