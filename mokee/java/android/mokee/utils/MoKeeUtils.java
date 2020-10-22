/*
 * Copyright (C) 2012-2020 The MoKee Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.text.TextUtils;

import com.mokee.os.Build;

import java.util.Locale;

public class MoKeeUtils {

    private MoKeeUtils() {}

    public static boolean isSupportLanguage(boolean excludeSAR) {
        Locale locale = Locale.getDefault();
        if (locale.getLanguage().startsWith(Locale.CHINESE.getLanguage())) {
            if (excludeSAR) {
                return locale.getCountry().equals("CN");
            } else {
                return !locale.getCountry().equals("SG");
            }
        } else {
            return false;
        }
    }

    public static boolean isOnline(@NonNull Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
            return (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
        }
        return false;
    }

    public static boolean isApkInstalledAndEnabled(@NonNull Context context, @Nullable String packageName) {
        int state;
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            state = context.getPackageManager().getApplicationEnabledSetting(packageName);
        } catch (NameNotFoundException e) {
            return false;
        }
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER;
    }

    public static boolean isApkInstalled(@NonNull Context context, @Nullable String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public static boolean isSystemApp(@NonNull Context context, @Nullable String packageName) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static boolean isPremiumVersion() {
        return TextUtils.equals(Build.RELEASE_TYPE.toLowerCase(Locale.ENGLISH), "premium");
    }

}
