/*
 * Copyright (C) 2013 MoKee OpenSource Project
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

package com.android.systemui;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.statusbar.phone.NavigationBarView;
import com.android.systemui.statusbar.phone.PanelBar;

import java.util.List;

public class TransparencyManager {

    public static final float KEYGUARD_ALPHA = 0.44f;

    private static final String TAG = TransparencyManager.class.getSimpleName();

    private NavigationBarView mNavbar;
    private PanelBar mStatusbar;

    private SomeInfo mNavbarInfo = new SomeInfo();
    private SomeInfo mStatusbarInfo = new SomeInfo();

    private final Context mContext;
    private ContentResolver resolver;

    private Handler mHandler = new Handler();

    private boolean mIsHomeShowing;
    private boolean mIsKeyguardShowing;
    private int mAlphaMode;

    private KeyguardManager mKeyguardManager;
    private ActivityManager mActivityManager;

    private class SomeInfo {
        float keyguardAlpha;
        float homeAlpha;
        boolean tempDisable;
    }

    private final Runnable updateTransparencyRunnable = new Runnable() {
        @Override
        public void run() {
            doTransparentUpdate();
        }
    };

    public TransparencyManager(Context context) {
        mContext = context;
        resolver = mContext.getContentResolver();

        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                update();
            }
        }, intentFilter);

        SettingsObserver settingsObserver = new SettingsObserver(new Handler());
        settingsObserver.observe();
    }

    public void update() {
        mHandler.removeCallbacks(updateTransparencyRunnable);
        mHandler.postDelayed(updateTransparencyRunnable, 100);
    }

    public void setNavbar(NavigationBarView n) {
        mNavbar = n;
    }

    public void setStatusbar(PanelBar s) {
        mStatusbar = s;
    }

    public void setTempDisableStatusbarState(boolean state) {
        mStatusbarInfo.tempDisable = state;
    }

    public void setTempNavbarState(boolean state) {
        mNavbarInfo.tempDisable = state;
    }

    private void doTransparentUpdate() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                mIsHomeShowing = isLauncherShowing();
                mIsKeyguardShowing = isKeyguardShowing();
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                // TODO animate alpha~
                if (mNavbar != null) {
                    if (mNavbarInfo.tempDisable) {
                        mNavbar.setBackgroundAlpha(1);
                        mNavbarInfo.tempDisable = false;
                    } else if (mIsKeyguardShowing && mIsHomeShowing || mIsKeyguardShowing && !mIsHomeShowing) {
                        if (mAlphaMode == 1) {
                            mNavbar.setBackgroundAlpha(mNavbarInfo.keyguardAlpha);
                        } else {
                            mNavbar.setBackgroundAlpha(1);
                        }
                    } else if (mIsHomeShowing && !mIsKeyguardShowing) {
                        mNavbar.setBackgroundAlpha(mNavbarInfo.homeAlpha);
                    } else {
                        mNavbar.setBackgroundAlpha(1);
                    }
                }
                if (mStatusbar != null) {
                    if (mStatusbarInfo.tempDisable) {
                        mStatusbar.setBackgroundAlpha(1);
                        mStatusbarInfo.tempDisable = false;
                    } else if (mIsKeyguardShowing && mIsHomeShowing || mIsKeyguardShowing && !mIsHomeShowing) {
                        if (mAlphaMode == 1) {
                            mStatusbar.setBackgroundAlpha(mStatusbarInfo.keyguardAlpha);
                        } else {
                            mStatusbar.setBackgroundAlpha(1);
                        }
                    } else if (mIsHomeShowing && !mIsKeyguardShowing) {
                        mStatusbar.setBackgroundAlpha(mStatusbarInfo.homeAlpha);
                    } else {
                        mStatusbar.setBackgroundAlpha(1);
                    }
                }
            }
        }.execute();
    }

    private boolean isLauncherShowing() {
        try {
            final List<ActivityManager.RecentTaskInfo> recentTasks =
                mActivityManager.getRecentTasksForUser(
                            1, ActivityManager.RECENT_WITH_EXCLUDED,
                            UserHandle.CURRENT.getIdentifier());
            if (recentTasks.size() > 0) {
                ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(0);
                Intent intent = new Intent(recentInfo.baseIntent);
                if (recentInfo.origActivity != null) {
                    intent.setComponent(recentInfo.origActivity);
                }
                ComponentName cn = intent.getComponent();
                boolean isRecentActivity = cn.getPackageName().equals("com.android.systemui") && cn.getClassName().equals("com.android.systemui.recent.RecentsActivity");
                if (isCurrentHomeActivity(intent.getComponent(), null) || isRecentActivity) {
                    return true;
                }
            }
        } catch(Exception ignore) {
        }
        return false;
    }

    private boolean isKeyguardShowing() {
        if (mKeyguardManager == null) {
            return false;
        }
        return mKeyguardManager.isKeyguardLocked();
    }

    private boolean isCurrentHomeActivity(ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .resolveActivityInfo(mContext.getPackageManager(), 0);
        }
        return homeInfo != null
                && homeInfo.packageName.equals(component.getPackageName())
                && homeInfo.name.equals(component.getClassName());
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.NAVIGATION_BAR_ALPHA), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_BAR_ALPHA), false,
                    this, UserHandle.USER_ALL);
            resolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.STATUS_NAV_BAR_ALPHA_MODE), false,
                    this, UserHandle.USER_ALL);
            updateSettings();
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    protected void updateSettings() {
        final float defaultAlpha = Float.valueOf(mContext.getResources().getInteger(R.integer.navigation_bar_transparency)) / 255;
        String alphas[];
        String settingValue = Settings.System.getStringForUser(resolver,
                Settings.System.NAVIGATION_BAR_ALPHA, UserHandle.USER_CURRENT);
        mAlphaMode = Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_NAV_BAR_ALPHA_MODE, 1, UserHandle.USER_CURRENT);

        //Log.e(TAG, "nav bar config: " + settingValue);
        if (settingValue == null) {
            mNavbarInfo.homeAlpha = defaultAlpha;
            mNavbarInfo.keyguardAlpha = KEYGUARD_ALPHA;
        } else {
            alphas = settingValue.split(";");
            if (alphas != null && alphas.length == 2) {
                mNavbarInfo.homeAlpha = Float.parseFloat(alphas[0]) / 255;
                mNavbarInfo.keyguardAlpha = Float.parseFloat(alphas[1]) / 255;
            }
        }

        settingValue = Settings.System.getStringForUser(resolver,
                Settings.System.STATUS_BAR_ALPHA, UserHandle.USER_CURRENT);
        //Log.e(TAG, "status bar config: " + settingValue);
        if (settingValue == null) {
            mStatusbarInfo.homeAlpha = defaultAlpha;
            mStatusbarInfo.keyguardAlpha = KEYGUARD_ALPHA;
        } else {
            alphas = settingValue.split(";");
            if (alphas != null && alphas.length == 2) {
                mStatusbarInfo.homeAlpha = Float.parseFloat(alphas[0]) / 255;
                mStatusbarInfo.keyguardAlpha = Float.parseFloat(alphas[1]) / 255;
            }
        }
        update();
    }
}
