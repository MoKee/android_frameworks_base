/*
 * Copyright (C) 2012 - 2015 The MoKee OpenSource Project
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

package android.mokee.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.DecimalFormat;
import java.util.Locale;

/**
* @hide
*/

public class MoKeeUtils {

    private static String LIBNAME = "mokeeutils";

    static {
        System.loadLibrary(LIBNAME);
    }

    public static native boolean isSupportLanguage(boolean excludeTW);

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static boolean isApkInstalledAndEnabled(String packagename, Context context) {
        int state;
        try {
            context.getPackageManager().getPackageInfo(packagename, 0);
            state = context.getPackageManager().getApplicationEnabledSetting(packagename);
        } catch (NameNotFoundException e) {
            return false;
        }
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ? true : false;
    }

    public static boolean isApkInstalled(String packagename, Context context) {
        try {
            context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public static boolean isSystemApp(String packagename, Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static String formatFileSize(long size) {
        DecimalFormat df = new DecimalFormat("#0.00");
        String fileSizeString = "";
        if (size < 1024) {
            fileSizeString = df.format((double) size) + "B";
        } else if (size < 1048576) {
            fileSizeString = df.format((double) size / 1024) + "K";
        } else if (size < 1073741824) {
            fileSizeString = df.format((double) size / 1048576) + "M";
        } else {
            fileSizeString = df.format((double) size / 1073741824) + "G";
        }
        return fileSizeString;
    }

}
