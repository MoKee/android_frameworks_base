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

package android.mokee.lunar;

import android.content.res.Resources;

import com.android.internal.R;
import com.mokee.cloud.calendar.ChineseCalendarUtils;

public class SolarFestival {

    public static String getSolarFestivalInfo(int currentMonth, int currentDayForMonth) {
        String[] array = Resources.getSystem().getStringArray(
                com.android.internal.R.array.solar_festival);
        return ChineseCalendarUtils.getSolarFestivalInfoFromArray(currentMonth, currentDayForMonth,
                array);
    }
}
