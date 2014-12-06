/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.DataUsageGraph;
import com.android.systemui.statusbar.policy.NetworkController;

import android.text.format.Formatter;
/**
 * Layout for the data usage detail in quick settings.
 */
public class DataUsageDetailView extends LinearLayout {


    public DataUsageDetailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this, android.R.id.title, R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_text, R.dimen.qs_data_usage_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_carrier_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_info_top_text,
                R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_period_text, R.dimen.qs_data_usage_text_size);
        FontSizeUtils.updateFontSize(this, R.id.usage_info_bottom_text,
                R.dimen.qs_data_usage_text_size);
    }

    public void bind(NetworkController.DataUsageInfo info) {
        final Resources res = mContext.getResources();
        final int titleId;
        final long bytes;
        int usageColor = R.color.system_accent_color;
        final String top;
        String bottom = null;
        if (info.usageLevel < info.warningLevel) {
            // under warning, or no limit
            titleId = R.string.quick_settings_has_data_usage;
            bytes = info.usageLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_warning,
                    Formatter.formatFileSize(mContext,info.warningLevel));
	    if (info.limitLevel <= 0) 
	    bottom = res.getString(R.string.quick_settings_data_Package_Set);
	    else
	    bottom = res.getString(R.string.quick_settings_data_Package,
		    Formatter.formatFileSize(mContext,info.limitLevel));
        } else if (info.usageLevel <= info.limitLevel) {
            // over warning, under limit
            titleId = R.string.quick_settings_has_data_usage;
            bytes = info.usageLevel;
            top = res.getString(R.string.quick_settings_remaining_data_usage,
                    Formatter.formatFileSize(mContext,info.limitLevel - info.usageLevel));
            bottom = res.getString(R.string.quick_settings_data_Package,
                    Formatter.formatFileSize(mContext,info.limitLevel));
        } else {
            // over limit
            titleId = R.string.quick_settings_cellular_detail_over_limit;
            bytes = info.usageLevel - info.limitLevel;
            top = res.getString(R.string.quick_settings_cellular_detail_data_used,
                    Formatter.formatFileSize(mContext,info.usageLevel));
            bottom = res.getString(R.string.quick_settings_cellular_detail_data_limit,
                    Formatter.formatFileSize(mContext,info.limitLevel));
            usageColor = R.color.system_warning_color;
        }

        final TextView title = (TextView) findViewById(android.R.id.title);
        title.setText(titleId);
        final TextView usage = (TextView) findViewById(R.id.usage_text);
        usage.setText(Formatter.formatFileSize(mContext,bytes));
        usage.setTextColor(res.getColor(usageColor));
        final DataUsageGraph graph = (DataUsageGraph) findViewById(R.id.usage_graph);
        graph.setLevels(info.limitLevel, info.warningLevel, info.usageLevel);
        final TextView carrier = (TextView) findViewById(R.id.usage_carrier_text);
        carrier.setText(info.carrier);
        final TextView period = (TextView) findViewById(R.id.usage_period_text);
        period.setText(info.period);
        final TextView infoTop = (TextView) findViewById(R.id.usage_info_top_text);
        infoTop.setVisibility(top != null ? View.VISIBLE : View.GONE);
        infoTop.setText(top);
        final TextView infoBottom = (TextView) findViewById(R.id.usage_info_bottom_text);
        infoBottom.setVisibility(bottom != null ? View.VISIBLE : View.GONE);
        infoBottom.setText(bottom);
    }

}
