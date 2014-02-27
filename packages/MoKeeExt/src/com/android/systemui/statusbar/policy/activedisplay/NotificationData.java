/*
 * Copyright (C) 2012-2014 MoKee OpenSource Project
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

package com.android.systemui.statusbar.policy.activedisplay;

import android.graphics.Bitmap;

/**
 * The list of currently displaying notifications.
 */
public class NotificationData {
    public Bitmap iconApp;
    public Bitmap iconAppSmall;
    public CharSequence titleText;
    public CharSequence messageText;
    public CharSequence largeMessageText;
    public CharSequence infoText;
    public CharSequence subText;
    public CharSequence summaryText;
    public CharSequence tickerText;
    public int number;

    public CharSequence getLargeMessage() {
        return largeMessageText == null ? messageText : largeMessageText;
    }
}
