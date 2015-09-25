/**
 * Copyright (c) 2014, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.policy.impl;

import static android.media.AudioAttributes.USAGE_ALARM;
import static android.media.AudioAttributes.USAGE_NOTIFICATION;
import static android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.media.AudioManager;
import android.media.AudioManagerInternal;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.util.Slog;

import com.android.internal.R;
import com.android.server.LocalServices;

import libcore.io.IoUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeHelper {
    private static final String TAG = "ZenModeHelper";

    private final Context mContext;
    private final AppOpsManager mAppOps;

    private int mZenMode;
    private ZenModeConfig mConfig;
    private AudioManagerInternal mAudioManager;
    private int mPreviousRingerMode = -1;
    private boolean mEffectsSuppressed;

    public ZenModeHelper(Context context) {
        mContext = context;
        mAppOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mAudioManager = LocalServices.getService(AudioManagerInternal.class);
        mConfig = readDefaultConfig(context.getResources());
    }

    public static ZenModeConfig readDefaultConfig(Resources resources) {
        XmlResourceParser parser = null;
        try {
            parser = resources.getXml(R.xml.default_zen_mode_config);
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                final ZenModeConfig config = ZenModeConfig.readXml(parser);
                if (config != null) return config;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error reading default zen mode config from resource", e);
        } finally {
            IoUtils.closeQuietly(parser);
        }
        return new ZenModeConfig();
    }

    public void setZenMode(int zenMode) {
        if (mZenMode == zenMode) return;
        mZenMode = zenMode;
        Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, mZenMode);
        applyZenToRingerMode();
        applyRestrictions();
    }

    private void applyZenToRingerMode() {
        if (mAudioManager == null) return;
        // force the ringer mode into compliance
        final int ringerModeInternal = mAudioManager.getRingerModeInternal();
        int newRingerModeInternal = ringerModeInternal;
        switch (mZenMode) {
            case Global.ZEN_MODE_NO_INTERRUPTIONS:
                if (ringerModeInternal != AudioManager.RINGER_MODE_SILENT) {
                    mPreviousRingerMode = ringerModeInternal;
                    newRingerModeInternal = AudioManager.RINGER_MODE_SILENT;
                }
                break;
            case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
            case Global.ZEN_MODE_OFF:
                if (ringerModeInternal == AudioManager.RINGER_MODE_SILENT) {
                    newRingerModeInternal = mPreviousRingerMode != -1 ? mPreviousRingerMode
                            : AudioManager.RINGER_MODE_NORMAL;
                    mPreviousRingerMode = -1;
                }
                break;
        }
        if (newRingerModeInternal != -1) {
            mAudioManager.setRingerModeInternal(newRingerModeInternal, TAG);
        }
    }

    private void applyRestrictions() {
        final boolean zen = mZenMode != Global.ZEN_MODE_OFF;

        // notification restrictions
        applyRestrictions(false, USAGE_NOTIFICATION);

        // call restrictions
        applyRestrictions(zen && !mConfig.allowCalls, USAGE_NOTIFICATION_RINGTONE);

        // alarm restrictions
        final boolean muteAlarms = mZenMode == Global.ZEN_MODE_NO_INTERRUPTIONS;
        applyRestrictions(muteAlarms, USAGE_ALARM);
    }

    private void applyRestrictions(boolean mute, int usage) {
        final String[] exceptionPackages = null; // none (for now)
        mAppOps.setRestriction(AppOpsManager.OP_VIBRATE, usage,
                mute ? AppOpsManager.MODE_IGNORED : AppOpsManager.MODE_ALLOWED,
                exceptionPackages);
        mAppOps.setRestriction(AppOpsManager.OP_PLAY_AUDIO, usage,
                mute ? AppOpsManager.MODE_IGNORED : AppOpsManager.MODE_ALLOWED,
                exceptionPackages);
    }

}
