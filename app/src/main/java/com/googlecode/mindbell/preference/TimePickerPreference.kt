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
import android.content.res.TypedArray
import android.preference.DialogPreference
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker

import com.googlecode.mindbell.util.TimeOfDay

open class TimePickerPreference(ctxt: Context, attrs: AttributeSet) : DialogPreference(ctxt, attrs) {

    var time = TimeOfDay(0, 0, null)
        set(newTime: TimeOfDay) {
            field = newTime
            summary = deriveSummary()
        }


    protected var picker: TimePicker? = null

    /**
     * Returns true if the picker should use 24 hour format.
     */
    protected open val isUse24HourView: Boolean
        get() = DateFormat.is24HourFormat(context)

    init {
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }

    override fun onCreateDialogView(): View {
        picker = TimePicker(context)
        picker!!.setIs24HourView(isUse24HourView)
        return picker!!
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)

        @Suppress("DEPRECATION") // setCurrent*() deprecated now but not for older API levels < 23
        picker!!.currentHour = time.hour
        @Suppress("DEPRECATION") // setCurrent*() deprecated now but not for older API levels < 23
        picker!!.currentMinute = time.minute
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            @Suppress("DEPRECATION") // getCurrent*() deprecated now but not for older API levels < 23
            val newTime = TimeOfDay(picker!!.currentHour, picker!!.currentMinute)
            val newTimeString = newTime.persistString
            if (callChangeListener(newTimeString)) {
                persistString(newTimeString)
                time = newTime
            }
        }
    }

    /**
     * Returns a summary string derived from the set time value.
     */
    protected open fun deriveSummary(): String {
        return time.getDisplayString(context)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        return a.getString(index)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        val newTimeString: String = if (restoreValue) {
            if (defaultValue == null) {
                getPersistedString("00:00")
            } else {
                getPersistedString(defaultValue.toString())
            }
        } else {
            defaultValue!!.toString()
        }
        time = TimeOfDay(newTimeString)
    }

}
