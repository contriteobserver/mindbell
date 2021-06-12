/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2018 Uwe Damken
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

package com.googlecode.mindbell.preference

import android.content.Context
import android.util.AttributeSet

import com.googlecode.mindbell.util.TimeOfDay

/**
 * Allows to pick an interval specified as hours:minutes with a TimePickerPreference.
 */
class MinutesIntervalPickerPreference(ctxt: Context, attrs: AttributeSet) : TimePickerPreference(ctxt, attrs) {

    override val isUse24HourView: Boolean
        get() = true

    override fun deriveSummary(): String {
        return deriveSummary(time, true)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            @Suppress("DEPRECATION") // getCurrent*() deprecated now but not for older API levels < 23
            var newTime = TimeOfDay(picker!!.currentHour, picker!!.currentMinute)
            if (newTime.interval < MIN_INTERVAL.interval) {
                newTime = MIN_INTERVAL
            }
            val newTimeString = newTime.persistString
            if (callChangeListener(newTimeString)) {
                persistString(newTimeString)
                time = newTime
            }
        }
    }

    companion object {

        private val MIN_INTERVAL = TimeOfDay(0, 1)

        fun parseTimeOfDayFromSummary(summary: String): TimeOfDay {
            return TimeOfDay(summary.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
        }

        fun deriveSummary(time: TimeOfDay, isMinutesInterval: Boolean): String {
            val unit = if (isMinutesInterval) "min" else "s"
            return "${time.displayString} (${time.interval} $unit)"
        }
    }

}
