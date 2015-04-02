/*
 * Copyright (C) 2014-2015 The MoKee OpenSource Project
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

package org.mokee.services.assist.receiver;

import org.mokee.services.assist.utils.RSAEncryption;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.Log;

/**
 * Performs a number of miscellaneous, non-system-critical actions after the
 * system has finished booting.
 */
public class AssistReceiver extends BroadcastReceiver {

    private static final String TAG = AssistReceiver.class.getName();
    private static final String ACTION_PREFIX = "com.mokee.assist.action.";

    // Receiver from MoKeeAssist
    private static final String ACTION_REBOOT = ACTION_PREFIX + "reboot";
    private static final String ACTION_REBOOT_RECOVERY = ACTION_PREFIX + "reboot.recovery";
    private static final String ACTION_REBOOT_BOOTLOADER = ACTION_PREFIX + "reboot.bootloader";
    private static final String ACTION_POWEROFF = ACTION_PREFIX + "poweroff";
    private static final String ACTION_LOCKSCREEN = ACTION_PREFIX + "lockscreen";
    
    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String signature = extras.getString("signature");
        try {
            if (!RSAEncryption.verify(signature)) {
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        if (action.equals(ACTION_REBOOT)) {
            PowerManager mPowerManager = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mPowerManager.reboot("");
        } else if (action.equals(ACTION_REBOOT_RECOVERY)) {
            PowerManager mPowerManager = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mPowerManager.reboot("recovery");
        } else if (action.equals(ACTION_REBOOT_BOOTLOADER)) {
            PowerManager mPowerManager = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mPowerManager.reboot("bootloader");
        } else if (action.equals(ACTION_POWEROFF)) {
            Intent command = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
            intent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivityAsUser(command, UserHandle.CURRENT);
        } else if (action.equals(ACTION_LOCKSCREEN)) {
            PowerManager mPowerManager = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
    }
}
