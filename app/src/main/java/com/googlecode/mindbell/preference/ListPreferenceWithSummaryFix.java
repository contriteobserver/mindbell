/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
 *     Copyright (C) 2014-2016 Uwe Damken
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
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * If specifying android:summary="%s" in xml for a ListPreference the summary is set at first time and when checking or unchecking a
 * CheckBoxPreference but not when choosing a different entry from the list. According to my tests this seems to be fixed in API
 * level 19. I have taken this approach (http://stackoverflow.com/a/7018053) and reduced it to call notifyChanged() in every call of
 * setValue(), but only for API level < 19 as it might produce some overhead to call value changed events if value has not changed.
 */
public class ListPreferenceWithSummaryFix extends ListPreference {

    public ListPreferenceWithSummaryFix(Context context) {
        super(context);
    }

    public ListPreferenceWithSummaryFix(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreferenceWithSummaryFix(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ListPreferenceWithSummaryFix(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setValue(final String value) {
        super.setValue(value);
        if (Build.VERSION.SDK_INT < 19) {
            notifyChanged(); // avoid calling this twice for newer Android versions
        }
    }

}
