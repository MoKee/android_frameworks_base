/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util.cm;

import android.content.Context;
import android.os.UserHandle;
import android.provider.Settings;

import java.util.Calendar;

public class QuietHoursUtils {

    public static boolean inQuietHours(Context context, String option) {
        if (Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_FORCED, 0, UserHandle.USER_CURRENT_OR_SELF) != 0) {
            // If Quiet hours is forced return immediately
            return true;
        }

        if (Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_WAITED, 0, UserHandle.USER_CURRENT_OR_SELF) != 0) {
            // If Quiet hours is waited return immediately
            return false;
        }

        // Check if we are in timed Quiet hours mode
        boolean quietHoursEnabled = Settings.System.getIntForUser(context.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0, UserHandle.USER_CURRENT_OR_SELF) != 0;
        boolean quietHoursOption = Settings.System.getIntForUser(context.getContentResolver(),
                option, 0, UserHandle.USER_CURRENT_OR_SELF) != 0;

        if (quietHoursEnabled && quietHoursOption) {
            int quietHoursStart = Settings.System.getIntForUser(context.getContentResolver(),
                    Settings.System.QUIET_HOURS_START, 0, UserHandle.USER_CURRENT_OR_SELF);
            int quietHoursEnd = Settings.System.getIntForUser(context.getContentResolver(),
                    Settings.System.QUIET_HOURS_END, 0, UserHandle.USER_CURRENT_OR_SELF);
            return inQuietHours(quietHoursStart, quietHoursEnd);
        }

        return false;
    }

    public static boolean inQuietHours(int quietHoursStart, int quietHoursEnd) {
        if (quietHoursStart != quietHoursEnd) {
            // Get the date in "quiet hours" format.
            Calendar cal = Calendar.getInstance();
            int minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);

            if (quietHoursEnd < quietHoursStart) {
                // Starts at night, ends in the morning.
                return (minutes > quietHoursStart) || (minutes < quietHoursEnd);
            } else {
                // Starts in the morning, ends at night.
                return (minutes > quietHoursStart) && (minutes < quietHoursEnd);
            }
        }
        return false;
    }
}
