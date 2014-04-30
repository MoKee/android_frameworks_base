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

package org.mokee.services;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

/**
 * Performs a number of miscellaneous, non-system-critical actions
 * after the system has finished booting.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "MoKeeServicesBootReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {

        ContentResolver res = context.getContentResolver();
        // Start the powersaver service if enabled
        if (Settings.System.getIntForUser(res, Settings.System.POWER_SAVER_ENABLED, 1, UserHandle.USER_CURRENT_OR_SELF) != 0) {
            Intent powersaver = new Intent(context, org.mokee.services.powersaver.PowerSaverService.class);
            context.startService(powersaver);
        }

        // Start the quiethours service if enabled
        Intent quietHours = new Intent(context, org.mokee.services.quiethours.QuietHoursService.class);
        context.startService(quietHours);
    }
}
