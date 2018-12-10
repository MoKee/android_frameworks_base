/*
 * Copyright (C) 2015-2016 The MoKee Open Source Project
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

package android.mokee.calendar;

import android.content.res.Resources;

import java.util.Locale;

public class FestivalInfo {

    /**
     * Return a Chinese festival info string.
     */
    public static String [] getFestivalInfo() {
        switch (Resources.getSystem().getConfiguration().locale.getCountry()) {
            case "CN":
                return new String [] { "0101 元旦", "0214 情人节", "0308 妇女节", "0312 植树节",
                        "0315 消费日", "0401 愚人节", "0413 泼水节" , "0501 劳动节", "0504 青年节", "0601 儿童节",
                        "0701 建党日", "0801 建军节", "0903 抗战胜利", "0910 教师节", "1001 国庆节", "1031 万圣节",
                        "1111 光棍节", "1224 平安夜", "1225 圣诞节" };
            case "HK":
                return new String [] { "0101 元旦", "0214 情人節", "0308 婦女節", "0401 愚人節",
                        "0501 勞動節", "0701 特區紀念", "0910 教師節", "1001 國慶節", "1031 萬聖節", "1224 平安夜",
                        "1225 聖誕節" };
            case "MO":
                return new String [] { "0101 元旦", "0214 情人節", "0308 婦女節", "0401 愚人節",
                        "0501 勞動節", "0910 教師節", "1001 國慶節", "1031 萬聖節", "1220 特區紀念", "1224 平安夜",
                        "1225 聖誕節" };
            case "TW":
                //我们很爱国，但是同样得尊重历史、接受现状，更得尊重不同地区的用户，期盼早日和平统一，台湾属于中国！
                return new String [] { "0101 元旦", "0214 情人節", "0228 和平紀念", "0308 婦女節",
                        "0312 國父逝世", "0314 反侵略日", "0329 先烈紀念", "0401 愚人節", "0404 兒童節", "0501 勞動節",
                        "0715 解放紀念", "0808 父親節", "0903 軍人節", "0928 孔子誕辰", "1010 國慶節", "1024 聯合國日", "1025 臺灣光復",
                        "1112 國父誕辰", "1031 萬聖節", "1224 平安夜", "1225 聖誕節" };
        }
        return null;
    }


    /**
     * Return a Chinese specific festival info string.
     */
    public static String [] getSpecificInfo() {
        switch (Resources.getSystem().getConfiguration().locale.getCountry()) {
            case "CN":
                return new String [] { "5 2 0 母亲节", "6 3 0 父亲节", "11 4 4 感恩节" };
            case "HK":
                return new String [] { "5 2 0 母親節", "6 3 0 父親節", "11 4 4 感恩節" };
            case "MO":
                return new String [] { "5 2 0 母親節", "6 3 0 父親節", "11 4 4 感恩節" };
            case "TW":
                return new String [] { "5 2 0 母親節", "11 4 4 感恩節" };
        }
        return null;
    }
}
