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

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import com.googlecode.mindbell.util.TimeOfDay;

public class TimePickerPreference extends DialogPreference {

    protected TimeOfDay time = new TimeOfDay(0, 0, null);

    protected TimePicker picker = null;

    public TimePickerPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(isUse24HourView());
        return (picker);
    }

    /**
     * Returns true if the picker should use 24 hour format.
     */
    protected boolean isUse24HourView() {
        return DateFormat.is24HourFormat(getContext());
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);

        picker.setCurrentHour(time.getHour());
        picker.setCurrentMinute(time.getMinute());
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            TimeOfDay newTime = new TimeOfDay(picker.getCurrentHour(), picker.getCurrentMinute());
            String newTimeString = newTime.getPersistString();
            if (callChangeListener(newTimeString)) {
                persistString(newTimeString);
                setTime(newTime);
            }
        }
    }

    /**
     * Returns a summary string derived from the set time value.
     */
    protected String deriveSummary() {
        return time.getDisplayString(getContext());
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String newTimeString = null;
        if (restoreValue) {
            if (defaultValue == null) {
                newTimeString = getPersistedString("00:00");
            } else {
                newTimeString = getPersistedString(defaultValue.toString());
            }
        } else {
            newTimeString = defaultValue.toString();
        }
        setTime(new TimeOfDay(newTimeString));
    }

    public TimeOfDay getTime() {
        return time;
    }

    public void setTime(TimeOfDay newTime) {
        time = newTime;
        setSummary(deriveSummary());
    }

}
