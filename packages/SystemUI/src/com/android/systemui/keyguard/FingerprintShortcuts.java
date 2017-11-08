/*
 * Copyright (C) 2017 The MoKee Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.keyguard;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import mokee.providers.MKSettings;

class FingerprintShortcuts {

    private static final String TAG = "FingerprintShortcuts";

    static void launchAppByFinger(Context context, int fingerId) {
        final SparseArray<String> shortcuts = getShortcuts(context);
        final String target = shortcuts.get(fingerId);
        if (target == null) {
            return;
        }

        final String[] cmp = target.split("/");
        if (cmp.length != 2) {
            return;
        }

        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(cmp[0], cmp[1]);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch fingerprint shortcut", e);
        }
    }

    static SparseArray<String> getShortcuts(Context context) {
        final SparseArray<String> shortcuts = new SparseArray<>();

        final String shortcutMappings = MKSettings.System.getString(
                context.getContentResolver(),
                MKSettings.System.FINGERPRINT_SHORTCUTS);

        if (TextUtils.isEmpty(shortcutMappings)) {
            return shortcuts;
        }

        for (String line : shortcutMappings.split("\n")) {
            final String[] mapping = line.split(":");
            final int fingerId = Integer.parseInt(mapping[0]);
            shortcuts.put(fingerId, mapping[1]);
        }

        return shortcuts;
    }

}
