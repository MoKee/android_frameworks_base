/*
 * Copyright (C) 2012-2018 The MoKee Open Source Project
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

package android.mokee.location;

import android.text.TextUtils;

public final class PhoneNumberOfflineGeocoder {

    private static String LIBNAME = "mokee-phonelocation";

    static {
        System.loadLibrary(LIBNAME);
    }

    public static String getPhoneCode(String number) {
        if (TextUtils.isEmpty(number)) return "";
        String mPhoneCode = getDescriptionForNumber(number.toString(), 0);
        return (TextUtils.isEmpty(mPhoneCode) ? "" : mPhoneCode);
    }

    public static String getPhoneLocation(String number) {
        if (TextUtils.isEmpty(number)) return "";
        String mPhoneLocation = getDescriptionForNumber(number.toString(), 1);
        return (TextUtils.isEmpty(mPhoneLocation) ? "" : mPhoneLocation);
    }

    private synchronized static String doGetDescriptionForNumber(String number) {
        return nativeGetDescriptionForNumber(number);
    }

    private static String getDescriptionForNumber(String number, int pos) {
        String s = doGetDescriptionForNumber(number.replaceAll("(?:-| )", ""));
        String[] ss = null;
        if (!TextUtils.isEmpty(s)) {
            ss = s.split(",");
            if (ss.length == 2) return ss[pos];
        }
        return null;
    }

    private static native String nativeGetDescriptionForNumber(String number);

}
