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

#ifndef __ANDROID_JNI_H__
#define __ANDROID_JNI_H__

/**
 * This define the reg class for jni call
 */
#define JNIREG_CLASS "android/mokee/utils/MoKeeUtils"

JNIEXPORT jboolean JNICALL isSupportLanguage (JNIEnv* env, jclass thiz, jboolean excludeTW);

/**
 * Table of methods associated with a single class.
 */
static JNINativeMethod gMethods[] = {
    {
        "isSupportLanguage", "(Z)Z",
        (void*) isSupportLanguage
    },
    /* <<----Functions for sync end--------------------------------- */
};
#endif
