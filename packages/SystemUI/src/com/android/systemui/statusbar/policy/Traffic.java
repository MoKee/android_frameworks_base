/*
 * Copyright (C) 2006 The MoKee Open Source Project
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

package com.android.systemui.statusbar.policy;

import java.text.DecimalFormat;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class Traffic extends TextView {
	private boolean mAttached;
	private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			NetworkInfo netInfo = ((android.net.ConnectivityManager)Traffic.this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
			if ((netInfo != null) && (netInfo.isConnected()))
				mIsNetworkConnected = true;
			else
				mIsNetworkConnected = false;
			Traffic.this.updateNetworkSpeed();
		}
	};
	private Context mContext;
	private boolean mDisabled = true;
	private Handler mHandler;
	private boolean mIsNetworkConnected;
	private boolean mIsRequestHideByKeyguard;
	private long mLastTime;
	private SettingsObserver mNetworkSpeedObserver;
	private Runnable mNetworkSpeedRunnable = new Runnable() {
		public void run() {
			Traffic.this.updateNetworkSpeed();
		}
	};
	private int mNetworkUpdateInterval;
	private long mTotalBytes;

	public Traffic(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mContext = context;
	}

	private static String formatSpeed(Context context, long number) {
		String unit = "K/s";
		float speedValue = (float)number / 1024.0F;
		if (speedValue > 999.0F) {
			unit = "M/s";
			speedValue /= 1024.0F;
		}
		Object fullString[] = new Object[2];
		fullString[0] = Float.valueOf(speedValue);
		fullString[1] = unit;
		if (speedValue < 1.0F)
			return String.format("%.2f%s", fullString);
		if (speedValue < 10.0F)
			return String.format("%.1f%s", fullString);
		return String.format("%.0f%s", fullString);
	}

	private void updateNetworkSpeed() {
		if ((this.mDisabled) || (!this.mIsNetworkConnected) || (this.mIsRequestHideByKeyguard)) {
			setVisibility(8);
			this.mLastTime = 0L;
			this.mTotalBytes = 0L;
			return;
		}
		long lastTime = System.currentTimeMillis();
		long totalBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
		long speedValue = 0L;
		if ((mLastTime != 0L) && (lastTime > mLastTime) && (mTotalBytes != 0L) && (totalBytes != 0L) && (totalBytes > mTotalBytes))
			speedValue = 1000L * (totalBytes - mTotalBytes) / (lastTime - mLastTime);
		setText(formatSpeed(mContext, speedValue));
		setVisibility(0);
		mLastTime = lastTime;
		mTotalBytes = totalBytes;
		removeCallbacks(mNetworkSpeedRunnable);
		postDelayed(mNetworkSpeedRunnable, mNetworkUpdateInterval);
	}

	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!this.mAttached) {
			mAttached = true;
			mHandler = new Handler();
			mNetworkSpeedObserver = new SettingsObserver(mHandler);
			mContext.registerReceiver(mConnectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
			ContentResolver resolver = mContext.getContentResolver();
			resolver.registerContentObserver(android.provider.Settings.System.getUriFor("status_bar_show_network_speed"), true, mNetworkSpeedObserver);
			resolver.registerContentObserver(android.provider.Settings.System.getUriFor("status_bar_network_speed_interval"), true, mNetworkSpeedObserver);
			mNetworkSpeedObserver.onChange(true);
			setWidth((int)Math.ceil(getPaint().measureText("0.00K/s")));
		}
	}

	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (this.mAttached) {
			this.mAttached = false;
			this.mContext.unregisterReceiver(this.mConnectivityReceiver);
			this.mContext.getContentResolver().unregisterContentObserver(this.mNetworkSpeedObserver);
			removeCallbacks(this.mNetworkSpeedRunnable);
		}
	}

	class SettingsObserver extends ContentObserver {
		SettingsObserver(Handler handler) {
			super(handler);
		}

		public void onChange(boolean selfChange) {
			ContentResolver resolver = Traffic.this.mContext.getContentResolver();
			if (android.provider.Settings.System.getInt(resolver, "status_bar_network_speed_interval", 4000) == 0)
				mDisabled = true;
			else
				mDisabled = false;
			mNetworkUpdateInterval = Settings.System.getInt(resolver, "status_bar_network_speed_interval", 4000);
			Traffic.this.updateNetworkSpeed();
		}
	}
}

