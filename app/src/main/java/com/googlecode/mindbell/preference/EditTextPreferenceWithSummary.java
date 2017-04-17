/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2017 Uwe Damken
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

package com.googlecode.mindbell.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * If specifying android:summary="%s" in xml for an EditTextPreference the summary is never set. According to my tests this isn't
 * even implemented in API level 24. This implementation sets the text straight into the summary without respecting the %s. That's
 * specified in the XML only for documentation. Maybe it will work some time.
 */
public class EditTextPreferenceWithSummary extends EditTextPreference {

    private String defaultValue = "?";

    public EditTextPreferenceWithSummary(Context context) {
        super(context);
    }

    public EditTextPreferenceWithSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            if ("defaultValue".equals(attrs.getAttributeName(i))) {
                defaultValue = attrs.getAttributeValue(i);
                if (defaultValue.startsWith("@")) { // ressource id?
                    defaultValue = context.getText(Integer.valueOf(defaultValue.substring(1))).toString();
                }
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextPreferenceWithSummary(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EditTextPreferenceWithSummary(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setText(final String value) {
        String newValue = (value == null || value.isEmpty()) ? defaultValue : value;
        super.setText(newValue);
        setSummary(newValue);
    }

}
