/*
 * Copyright (C) 2015 The MoKee OpenSource Project
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

package com.android.server;

import com.mokee.mms.utils.CaptchasUtils;

import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.widget.Toast;

import com.android.internal.R;

public class SmsCaptchasReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) return;
        Object[] pdus = (Object[]) extras.get("pdus");
        SmsMessage message[] = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; i++) {
            message[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
        }
        String captchas = getCaptchas(message[0].getMessageBody().toString());
        if (!TextUtils.isEmpty(captchas)) {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setText(captchas);
            Toast.makeText(context, String.format(context.getString(R.string.captchas_has_copied), captchas), Toast.LENGTH_SHORT).show();
        }
    }

    public static String getCaptchas(String messageBody) {
        if (!CaptchasUtils.isChineseContains(messageBody) && CaptchasUtils.isCaptchasEnMessage(messageBody)) {
            return CaptchasUtils.tryToGetEnCaptchas(messageBody);
        } else if (CaptchasUtils.isCaptchasMessage(messageBody)) {
            return CaptchasUtils.tryToGetCnCaptchas(messageBody);
        }
        return "";
    }
}
