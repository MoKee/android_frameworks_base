/*
 * Copyright (C) 2015 The MoKee OpenSource Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <cutils/properties.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

JNIEXPORT jboolean JNICALL isSupportLanguage (JNIEnv* env, jclass thiz, jboolean excludeTW) {
    char language[PROPERTY_VALUE_MAX];
    char country[PROPERTY_VALUE_MAX];
    property_get("persist.sys.language", language, "");
    property_get("persist.sys.country", country, "");

    if (excludeTW) {
        return (strstr(language, "zh") && !strstr(country, "TW")) ? JNI_TRUE : JNI_FALSE;
    } else {
        return strstr(language, "zh") ? JNI_TRUE : JNI_FALSE;
    }
}
