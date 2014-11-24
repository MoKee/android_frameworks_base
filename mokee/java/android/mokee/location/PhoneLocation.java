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

package android.mokee.location;

import android.text.TextUtils;

/**
* @hide
*/

public final class PhoneLocation {

    private static String LIBNAME = "mokee-phonelocation";

    static {
        System.loadLibrary(LIBNAME);
    }

    private static native String getPhoneNumberLocation(String number);

    private synchronized static String doGetLocationFromPhone(String number) {
        if (TextUtils.isEmpty(number)) return null;
        return getPhoneNumberLocation(number);
    }

    private static String getPosFromPhone(String number, int i) {
        String s = doGetLocationFromPhone(number);
        String[] ss = null;
        if (null != s) {
            ss = s.split(",");
            if (ss.length == 2) return ss[i];
        }
        return null;
    }

    public static String getCodeFromPhone(String number) {
        return getPosFromPhone(number, 0);
    }

    public static String getCityFromPhone(CharSequence number) {
        if (TextUtils.isEmpty(number)) return "";
        String phoneLocation = getPosFromPhone(number.toString().replaceAll("(?:-| )", ""), 1);
        return (TextUtils.isEmpty(phoneLocation) ? "" : phoneLocation);
    }
}
