/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
package com.googlecode.mindbell.util

import android.content.Context
import com.googlecode.mindbell.accessors.PrefsAccessor
import java.util.*

/**
 * Represents an immutable time of day by storing only hour, minute and weekday.
 */
class TimeOfDay {

    /**
     * For a hh:mm-TimeOfDay the hour value, for a mm:ss-TimeOfDay the minutes value.
     */
    /**
     * Returns the hour value.
     */
    var hour: Int = 0
        private set

    /**
     * For a hh:mm-TimeOfDay the minutes value, for a mm:ss-TimeOfDay the seconds value.
     */
    /**
     * Returns the minute value.
     */
    var minute: Int = 0
        private set

    private var second: Int? = null

    private var millisecond: Int? = null

    /**
     * Returns the weekday value which may be null.
     */
    var weekday: Int? = null
        private set // null or 1-Calendar.SUNDAY ... 7-Calendar.SATURDAY

    /**
     * Returns a String readily representing this TimeOfDay to be used for displaying with a context.
     */
    val displayString: String
        get() = persistString

    /**
     * Returns a String readily representing this TimeOfDay to be used for persisting.
     */
    val persistString: String
        get() = String.format("%02d:%02d", hour, minute)

    /**
     * Returns a String readily representing this TimeOfDay to be used for logging.
     */
    // !hasSeconds && !hasWeekday
    val logString: String
        get() {
            val hasSeconds = second != null && millisecond != null
            val hasWeekday = weekday != null
            return if (hasSeconds && hasWeekday) {
                String.format("%02d:%02d:%02d.%03d(%d)", hour, minute, second, millisecond, weekday)
            } else if (hasSeconds && !hasWeekday) {
                String.format("%02d:%02d:%02d.%03d", hour, minute, second, millisecond)
            } else if (!hasSeconds && hasWeekday) {
                String.format("%02d:%02d(%d)", hour, minute, weekday)
            } else {
                String.format("%02d:%02d", hour, minute)
            }
        }

    /**
     * Returns the hour and minute value as minutes. If this is an hh:mm-TimeOfDay the result are minutes. If this is a
     * mm:ss-TimeOfDay the result are seconds. However, the interpretation depends on the caller.
     */
    val interval: Int
        get() = hour * 60 + minute

    /**
     * The time of day, as provided by the given Calendar.
     */
    @JvmOverloads constructor(cal: Calendar = Calendar.getInstance()) {
        init(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), null, null, cal.get(Calendar.DAY_OF_WEEK))
    }

    /**
     * Checks values and initialize fields.
     */
    private fun init(hour: Int, minute: Int, second: Int?, millisecond: Int?, weekday: Int?) {
        if (hour > 23 || hour < 0) {
            throw IllegalArgumentException("Hour must be between 0 and 23, but is " + hour)
        }
        if (minute > 59 || minute < 0) {
            throw IllegalArgumentException("Minute must be between 0 and 59, but is " + minute)
        }
        if (weekday != null && (weekday > 7 || weekday < 1)) {
            throw IllegalArgumentException("Weekday must be between 1 and 7, but is " + weekday)
        }
        this.hour = hour
        this.minute = minute
        this.second = second
        this.millisecond = millisecond
        this.weekday = weekday
    }

    /**
     * The time of day, as provided by hour, minute and weekday (which my be null).
     */
    constructor(hour: Int, minute: Int, weekday: Int?) {
        init(hour, minute, null, null, weekday)
    }

    /**
     * The time of day, as provided by hour, minute without a weekday.
     */
    constructor(hour: Int, minute: Int) {
        init(hour, minute, null, null, null)
    }

    /**
     * The time of day, as provided by the given String with the format "hour[:minute]" without a weekday.
     */
    constructor(time: String) {
        val parts = time.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        when (parts.size) {
            2 -> init(Integer.valueOf(parts[0])!!, Integer.valueOf(parts[1])!!, null, null, null)
            1 -> init(Integer.valueOf(parts[0])!!, 0, null, null, null)
            else -> throw IllegalArgumentException("Time <$time> not formatted as hh[:mm]")
        }
    }

    /**
     * The time of day, as provides by milliseconds since 1970.
     */
    constructor(millisecondsSince1970: Long) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millisecondsSince1970
        init(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND),
                cal.get(Calendar.DAY_OF_WEEK))
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as TimeOfDay?
        if (hour != other!!.hour) {
            return false
        }
        if (minute != other.minute) {
            return false
        }
        if (weekday == null) {
            if (other.weekday != null) {
                return false
            }
        } else if (weekday != other.weekday) {
            return false
        }
        return true
    }

    /**
     * Returns a String readily representing this TimeOfDay to be used for displaying (with am/pm if locale requires).
     */
    fun getDisplayString(context: Context): String {
        val hasSeconds = second != null && millisecond != null
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, if (hasSeconds) second!! else 0)
        cal.set(Calendar.MILLISECOND, if (hasSeconds) millisecond!! else 0)
        return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + hour
        result = prime * result + minute
        result = prime * result + if (weekday == null) 0 else weekday!!.hashCode()
        return result
    }

    /**
     * Returns true if the bell should ring at this TimeOfDay, so it must be in the active time interval and the weekday of it must
     * be activated. The method name carries the historical meaning.
     *
     * @return whether bell should ring
     */
    fun isDaytime(prefs: PrefsAccessor): Boolean {
        return isDaytime(prefs.daytimeStart, prefs.daytimeEnd, prefs.activeOnDaysOfWeek)
    }

    /**
     * Returns true if the bell should ring at this time of day, so it must be in the active time interval and the weekday of the
     * begin of the time interval must be activated. For a night interval it is not correct to look at the weekday of this time of
     * day because this wouldn't keep the interval together. Supposed Friday is activated but Saturday is not and the interval
     * ranges  from Fri 13:00 to Sat 02:00. The reminder is expected to be active the whole time interval, not only till midnight.
     *
     * The method name carries the historical meaning.
     *
     * @return whether bell should ring
     */
    fun isDaytime(start: TimeOfDay, end: TimeOfDay, activeOnDaysOfWeek: Set<Int>): Boolean {
        if (!isInInterval(start, end)) {
            return false // time is before or after active time range
        }
        if (start.isBefore(end)) { // day ?
            return isActiveOnThatDay(activeOnDaysOfWeek) // this time of day and the range start are on the same day
        }
        if (isBefore(end)) { // after midnight?
            val weekdayAtStart = if (weekday == 1) 7 else weekday!! - 1
            return isActiveOnThatDay(weekdayAtStart, activeOnDaysOfWeek)
        } else {
            return isActiveOnThatDay(activeOnDaysOfWeek)
        }
    }

    /**
     * Determine whether the present time is in the semi-open interval including start up to but excluding end. If start is after
     * end, the interval is understood to span midnight.
     *
     * @param start
     * @param end
     * @return
     */
    fun isInInterval(start: TimeOfDay, end: TimeOfDay): Boolean {
        if (this.isSameTime(start)) {
            return true // same time means to be active the whole day
        }
        return if (start.isBefore(end)) {
            start.isBefore(this) && this.isBefore(end)
        } else { // spanning midnight
            start.isBefore(this) || this.isBefore(end)
        }
    }

    /**
     * Return true if this time is earlier than the other regardless of the weekday.
     *
     * @param other
     * @return
     */
    fun isBefore(other: TimeOfDay): Boolean {
        return hour < other.hour || hour == other.hour && minute < other.minute
    }

    /**
     * Returns true if this weekday is one on which the bell is active.
     *
     * @param activeOnDaysOfWeek
     * @return
     */
    fun isActiveOnThatDay(activeOnDaysOfWeek: Set<Int>): Boolean {
        return isActiveOnThatDay(weekday!!, activeOnDaysOfWeek)
    }

    /**
     * Return true if this time is the same than the other regardless of the weekday.
     *
     * @param other
     * @return
     */
    fun isSameTime(other: TimeOfDay): Boolean {
        return hour == other.hour && minute == other.minute
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    override fun toString(): String {
        return "TimeOfDay [$logString]"
    }

    companion object {

        /**
         * The hh:mm-TimeOfDay, as provided by an interval given in milliseconds.
         */
        fun fromMillisecondsInterval(milliseconds: Long): TimeOfDay {
            return fromSecondsInterval((milliseconds / PrefsAccessor.ONE_MINUTE_MILLIS).toInt())
        }

        /**
         * The hh:mm-TimeOfDay, as provided by an interval given in minute, or the mm:ss-TimeOfDay, as provided by an interval given in
         * seconds.
         */
        fun fromSecondsInterval(interval: Int): TimeOfDay {
            return TimeOfDay(interval / 60, interval % 60)
        }

        /**
         * Returns true if this weekday is one on which the bell is active.
         *
         * @param weekday
         * @param activeOnDaysOfWeek
         * @return
         */
        fun isActiveOnThatDay(weekday: Int, activeOnDaysOfWeek: Set<Int>): Boolean {
            return activeOnDaysOfWeek.contains(Integer.valueOf(weekday))
        }
    }

}
/**
 * The current time of day, as provided by the Calendar.getInstance().
 */
