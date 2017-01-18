/*
 * Copyright (C) 2014-2017 The MoKee Open Source Project
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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.mokee.utils.MoKeeUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import mokee.providers.MKSettings;

import com.android.systemui.R;
import com.android.systemui.utils.NetworkTrafficSpan;

/*
 *
 * Seeing how an Integer object in java requires at least 16 Bytes, it seemed awfully wasteful
 * to only use it for a single boolean. 32-bits is plenty of room for what we need it to do.
 *
 */
public class NetworkTraffic extends TextView {

    private ConnectivityManager mConnectivityService;

    public static final int MASK_UP = 0x00000001; // Least valuable bit
    public static final int MASK_DOWN = 0x00000002; // Second least valuable bit

    private static final int KILOBYTE = 1024;

    private static DecimalFormat decimalFormat = new DecimalFormat("##0.#");
    static {
        decimalFormat.setMaximumIntegerDigits(4);
        decimalFormat.setMaximumFractionDigits(1);
    }

    private int mState = 0;
    private boolean mAttached;
    private boolean mVpnConnected;
    private long totalRxBytes;
    private long totalTxBytes;
    private long lastUpdateTime;
    private int txtSizeSingle;
    private int txtSizeMulti;
    private int KB = KILOBYTE;
    private int MB = KB * KB;
    private int GB = MB * KB;
    private String mUp = " \u25B2";
    private String mDown = " \u25BC";
    private boolean shouldHide;

    private Handler mTrafficHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            long timeDelta = SystemClock.elapsedRealtime() - lastUpdateTime;

            if (timeDelta < getInterval(mState) * .95) {
                if (msg.what != 1) {
                    // we just updated the view, nothing further to do
                    return;
                }
                if (timeDelta < 1) {
                    // Can't div by 0 so make sure the value displayed is minimal
                    timeDelta = Long.MAX_VALUE;
                }
            }
            lastUpdateTime = SystemClock.elapsedRealtime();

            // Calculate the data rate from the change in total bytes and time
            long newTotalRxBytes = TrafficStats.getTotalRxBytes();
            long newTotalTxBytes = TrafficStats.getTotalTxBytes();
            long rxData = newTotalRxBytes - totalRxBytes;
            long txData = newTotalTxBytes - totalTxBytes;

            if (mVpnConnected) {
                rxData = rxData / 2;
                txData = txData / 2;
            }

            // If bit/s convert from Bytes to bits
            String symbol = "B/s";

            // Get information for uplink ready so the line return can be added
            String output = "";
            if (isSet(mState, MASK_UP)) {
                output = formatOutput(timeDelta, txData, symbol);
                output += mUp;
                shouldHide = txData == 0;
            }

            // Ensure text size is where it needs to be
            int textSize;
            if (isSet(mState, MASK_UP + MASK_DOWN)) {
                output += "\n";
                textSize = txtSizeMulti;
                shouldHide = rxData == 0 && txData == 0;
            } else {
                textSize = txtSizeSingle;
            }

            // Add information for downlink if it's called for
            if (isSet(mState, MASK_DOWN)) {
                output += formatOutput(timeDelta, rxData, symbol);
                output += mDown;
                shouldHide = rxData == 0;
            }

            // Update view if there's anything new to show
            if (!output.contentEquals(getText()) && !shouldHide) {
                if (textSize == txtSizeMulti) {
                    int upIndex = output.indexOf(mUp);
                    int downIndex = output.indexOf(mDown);
                    int lineIndex = output.indexOf("\n");
                    Spannable spannable = new SpannableString(output);
                    spannable.setSpan(new AbsoluteSizeSpan(txtSizeMulti), 0, upIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new AbsoluteSizeSpan((int) (txtSizeMulti * 0.7)), upIndex,
                            lineIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new NetworkTrafficSpan(-0.25), upIndex, lineIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new AbsoluteSizeSpan(txtSizeMulti), lineIndex, downIndex,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new AbsoluteSizeSpan((int) (txtSizeMulti * 0.7)), downIndex,
                            output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new NetworkTrafficSpan(0.25), downIndex,
                            output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    setText(spannable);
                } else {
                    int index = output.indexOf(isSet(mState, MASK_DOWN) ? mDown : mUp);
                    Spannable spannable = new SpannableString(output);
                    spannable.setSpan(new AbsoluteSizeSpan(txtSizeSingle), 0, index,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new AbsoluteSizeSpan((int) (txtSizeMulti * 0.9)), index,
                            output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    spannable.setSpan(new NetworkTrafficSpan(0.25), index,
                            output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    setText(spannable);
                }
            }
            setVisibility(shouldHide || mState == 0 ? View.GONE : View.VISIBLE);
            // Post delayed message to refresh in ~1000ms
            totalRxBytes = newTotalRxBytes;
            totalTxBytes = newTotalTxBytes;
            clearHandlerCallbacks();
            mTrafficHandler.postDelayed(mRunnable, getInterval(mState));
        }

        private String formatOutput(long timeDelta, long data, String symbol) {
            long speed = (long) (data / (timeDelta / 1000F));
            if (speed < KB) {
                return decimalFormat.format(speed) + symbol;
            } else if (speed < MB) {
                return decimalFormat.format(speed / (float) KB) + 'K' + symbol;
            } else if (speed < GB) {
                return decimalFormat.format(speed / (float) MB) + 'M' + symbol;
            }
            return decimalFormat.format(speed / (float) GB) + 'G' + symbol;
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mTrafficHandler.sendEmptyMessage(0);
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            Uri uri = MKSettings.System.getUriFor(MKSettings.System.STATUS_BAR_NETWORK_TRAFFIC_STYLE);
            resolver.registerContentObserver(uri, false, this, UserHandle.USER_ALL);
        }

        /*
         * @hide
         */
        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    /*
     * @hide
     */
    public NetworkTraffic(Context context) {
        this(context, null);
    }

    /*
     * @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /*
     * @hide
     */
    public NetworkTraffic(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final Resources resources = getResources();
        mConnectivityService = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        txtSizeSingle = resources.getDimensionPixelSize(R.dimen.net_traffic_single_text_size);
        txtSizeMulti = resources.getDimensionPixelSize(R.dimen.net_traffic_multi_text_size);
        Handler mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
        }
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                updateSettings();
            }
        }
    };

    public void updateSettings() {
        mState = MKSettings.System.getIntForUser(mContext.getContentResolver(),
                MKSettings.System.STATUS_BAR_NETWORK_TRAFFIC_STYLE, 3 , UserHandle.USER_CURRENT);

        if (isSet(mState, MASK_UP) || isSet(mState, MASK_DOWN)) {
            if (MoKeeUtils.isOnline(mContext)) {
                if (mAttached) {
                    NetworkInfo vpnInfo = mConnectivityService.getNetworkInfo(ConnectivityManager.TYPE_VPN);
                    if (vpnInfo != null && vpnInfo.isConnected()) {
                        mVpnConnected = true;
                    } else {
                        mVpnConnected = false;
                    }
                    totalRxBytes = TrafficStats.getTotalRxBytes();
                    totalTxBytes = TrafficStats.getTotalTxBytes();
                    lastUpdateTime = SystemClock.elapsedRealtime();
                    mTrafficHandler.sendEmptyMessage(1);
                }
                return;
            }
        } else {
            setVisibility(View.GONE);
            clearHandlerCallbacks();
        }
    }

    private static boolean isSet(int intState, int intMask) {
        return (intState & intMask) == intMask;
    }

    private static int getInterval(int intState) {
        int intInterval = intState >>> 16;
        return (intInterval >= 250 && intInterval <= 32750) ? intInterval : 1000;
    }

    private void clearHandlerCallbacks() {
        mTrafficHandler.removeCallbacks(mRunnable);
        mTrafficHandler.removeMessages(0);
        mTrafficHandler.removeMessages(1);
    }

}
