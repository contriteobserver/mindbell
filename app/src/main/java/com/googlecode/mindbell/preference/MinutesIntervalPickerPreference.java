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
import android.util.AttributeSet;

import com.googlecode.mindbell.util.TimeOfDay;

/**
 * Allows to pick an interval specified as hours:minutes with a TimePickerPreference.
 */
public class MinutesIntervalPickerPreference extends TimePickerPreference {

    public static final TimeOfDay MIN_INTERVAL = new TimeOfDay(0, 1);  // public for MindBellPreferences#isFrequencyDividesAnHour()

    public MinutesIntervalPickerPreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
    }

    public static TimeOfDay parseTimeOfDayFromSummary(String summary) {
        return new TimeOfDay(summary.split(" ")[0]);
    }

    @Override
    protected boolean isUse24HourView() {
        return true;
    }

    @Override
    protected String deriveSummary() {
        return deriveSummary(time, true);
    }

    public static String deriveSummary(TimeOfDay time, boolean isMinutesInterval) {
        String unit = (isMinutesInterval) ? "min" : "s";
        return time.getDisplayString() + " (" + time.getInterval() + " " + unit + ")";
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            TimeOfDay newTime = new TimeOfDay(picker.getCurrentHour(), picker.getCurrentMinute());
            if (newTime.getInterval() < MIN_INTERVAL.getInterval()) {
                newTime = MIN_INTERVAL;
            }
            String newTimeString = newTime.getPersistString();
            if (callChangeListener(newTimeString)) {
                persistString(newTimeString);
                setTime(newTime);
            }
        }
    }

}
