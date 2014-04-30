/*
 * Copyright (C) 2014 The MoKee OpenSource Project
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

package org.mokee.services.powersaver;

import android.content.Context;
import android.location.LocationManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.mokee.services.powersaver.Utils;

public class CpuGovernorToggle extends PowerSaverToggle {

    private static final String TAG = "PowerSaverService_CpuGovernorToggle";

    public CpuGovernorToggle(Context context) {
        super(context);
    }

    protected boolean isEnabled() {
        return Settings.System.getIntForUser(mContext.getContentResolver(), Settings.System.POWER_SAVER_CPU_GOVERNOR, 1, UserHandle.USER_CURRENT_OR_SELF) != 0;
    }

    protected boolean doScreenOnAction() {
        return mDoAction;
    }

    protected boolean doScreenOffAction() {
        if (needSwtich()) {
            mDoAction = true;
        } else {
            mDoAction = false;
        }
        return mDoAction;
    }

    private boolean needSwtich() {
        String defGov = Settings.System.getStringForUser(mContext.getContentResolver(), Settings.System.POWER_SAVER_CPU_GOVERNOR_DEFAULT, UserHandle.USER_CURRENT_OR_SELF);
        String remGov = Utils.getRecommendGovernor(mContext);
        if (TextUtils.isEmpty(remGov) || TextUtils.isEmpty(defGov))
            return false;
        return !defGov.equals(remGov);
    }

    protected Runnable getScreenOffAction() {
        return new Runnable() {
            @Override
            public void run() {
                String remGov = Utils.getRecommendGovernor(mContext);
                Utils.fileWriteOneLine(Utils.GOV_FILE, remGov);
                Log.d(TAG, "cpu = " + remGov);
            }
        };
    }

    protected Runnable getScreenOnAction() {
        return new Runnable() {
            @Override
            public void run() {
                String defGov = Settings.System.getStringForUser(mContext.getContentResolver(), Settings.System.POWER_SAVER_CPU_GOVERNOR_DEFAULT, UserHandle.USER_CURRENT_OR_SELF);
                Utils.fileWriteOneLine(Utils.GOV_FILE, defGov);
                Log.d(TAG, "cpu = " + defGov);
            }
        };
    }
}
