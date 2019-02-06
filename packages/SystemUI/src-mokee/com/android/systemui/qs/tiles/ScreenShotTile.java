/*
 * Copyright (C) 2016-2019 The MoKee Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.format.DateUtils;
import android.view.WindowManager;

import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.screenshot.TakeScreenshotService;

import org.mokee.internal.logging.MKMetricsLogger;

import javax.inject.Inject;

public class ScreenShotTile extends QSTileImpl<BooleanState> {

    @Inject
    public ScreenShotTile(QSHost host) {
        super(host);
    }

    @Override
    public void handleSetListening(boolean listening) {
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        mHost.collapsePanels();
        mHandler.postDelayed(() -> takeScreenshot(false), DateUtils.SECOND_IN_MILLIS);
    }

    @Override
    public void handleLongClick() {
        mHost.collapsePanels();
        mHandler.postDelayed(() -> takeScreenshot(true), DateUtils.SECOND_IN_MILLIS);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_screenshot_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_screenshot);
        state.label = mContext.getString(R.string.quick_settings_screenshot_label);
    }

    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = () -> {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                mContext.unbindService(mScreenshotConnection);
                mScreenshotConnection = null;
            }
        }
    };

    private void takeScreenshot(final boolean partial) {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            Intent intent = new Intent(mContext, TakeScreenshotService.class);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        msg.what = partial ? WindowManager.TAKE_SCREENSHOT_SELECTED_REGION
                                : WindowManager.TAKE_SCREENSHOT_FULLSCREEN;
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHandler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;

                        // Take the screenshot
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                            // Do nothing
                        }
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
            if (mContext.bindServiceAsUser(
                    intent, conn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
                mScreenshotConnection = conn;
                mHandler.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MKMetricsLogger.TILE_SCREENSHOT;
    }

}
