/*
 * Copyright (C) 2013 ParanoidAndroid Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.TextUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.util.SpnOverride;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PiePolicy {

    public int lowBatteryLevel;
    public int criticalBatteryLevel;

    private Context mContext;
    private int mBatteryLevel;
    private boolean mTelephony;
    private boolean isCN;

    private OnClockChangedListener mClockChangedListener;

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            mBatteryLevel = intent.getIntExtra("level", 0);
            isCN = mContext.getResources().getConfiguration().locale.getCountry().equals("CN") || mContext.getResources().getConfiguration().locale.getCountry().equals("TW");
        }
    };

    private final BroadcastReceiver mClockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mClockChangedListener.onChange(getSimpleTime());
        }
    };

    public interface OnClockChangedListener {
        public abstract void onChange(String s);
    }

    public PiePolicy(Context context) {
        mContext = context;
        mContext.registerReceiver(mBatteryReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        mContext.registerReceiver(mClockReceiver, filter);
        lowBatteryLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lowBatteryWarningLevel);
        criticalBatteryLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);
        mTelephony = mContext.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        isCN = mContext.getResources().getConfiguration().locale.getCountry().equals("CN") || mContext.getResources().getConfiguration().locale.getCountry().equals("TW");
    }

    public void setOnClockChangedListener(OnClockChangedListener l) {
        mClockChangedListener = l;
    }

    public boolean supportsTelephony() {
        return mTelephony;
    }

    public void destroy() {
        if (mClockReceiver != null)
            mContext.unregisterReceiver(mClockReceiver);
        if (mBatteryReceiver != null)
            mContext.unregisterReceiver(mBatteryReceiver);
    }

    public String getWifiSsid(NetworkController networkController) {
        String ssid = mContext.getString(R.string.quick_settings_wifi_not_connected);
        ConnectivityManager connManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) mContext
                    .getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            ssid = networkController.huntForSsid(connectionInfo);
        }
        return ssid.toUpperCase();
    }

    public String getNetworkProvider() {
        String operatorName = Settings.System.getStringForUser(mContext.getContentResolver(), Settings.System.CUSTOM_CARRIER_LABEL, UserHandle.USER_CURRENT);
        if(TextUtils.isEmpty(operatorName)) {
		operatorName = mContext.getString(R.string.quick_settings_wifi_no_network);
		TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            if(isCN) {
                String operator = telephonyManager.getNetworkOperator();
                SpnOverride mSpnOverride = new SpnOverride();
                operatorName = mSpnOverride.getSpn(operator);
                if(operatorName == null) {
                    operatorName = telephonyManager.getSimOperatorName();
                }
            } else {
                operatorName = telephonyManager.getNetworkOperatorName();
                if(operatorName == null) {
                    operatorName = telephonyManager.getSimOperatorName();
                }
            }
        }
        return operatorName.toUpperCase();
    }

    public String getSimpleDate() {
        String dateFormat = mContext.getString(R.string.pie_date_format);
        String date = String.valueOf(DateFormat.format(dateFormat, new Date()));
        return date.toUpperCase();
    }

    public boolean is24Hours() {
        return DateFormat.is24HourFormat(mContext);
    }

    public String getSimpleTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                mContext.getString(is24Hours() ? R.string.pie_hour_format_24 :
                        R.string.pie_hour_format_12));
        String amPm = sdf.format(new Date());
        return amPm.toUpperCase();
    }

    public String getAmPm() {
        String amPm = "";
        if(!is24Hours()) {
        	if(isCN) {
        		Calendar inDate = Calendar.getInstance();
        		amPm = DateUtils.getAMPMCNString(inDate.get(Calendar.HOUR), inDate.get(Calendar.AM_PM));
        	} else {
        		SimpleDateFormat sdf = new SimpleDateFormat(mContext.getString(R.string.pie_am_pm));
        		amPm = sdf.format(new Date()).toUpperCase();
        	}
        }
        return amPm;
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public String getBatteryLevelReadable() {
        return mContext.getString(R.string.battery_low_percent_format, mBatteryLevel)
                .toUpperCase();
    }
}
