/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
package com.googlecode.mindbell.util;

import android.content.Context;

import com.googlecode.mindbell.accessors.AndroidPrefsAccessor;

import java.util.Calendar;
import java.util.Set;

/**
 * Represents an immutable time of day by storing only hour, minute and weekday.
 */
public class TimeOfDay {

    /**
     * For a hh:mm-TimeOfDay the hour value, for a mm:ss-TimeOfDay the minutes value.
     */
    private int hour;

    /**
     * For a hh:mm-TimeOfDay the minutes value, for a mm:ss-TimeOfDay the seconds value.
     */
    private int minute;

    private Integer second;

    private Integer millisecond;

    private Integer weekday; // null or 1-Calendar.SUNDAY ... 7-Calendar.SATURDAY

    /**
     * The current time of day, as provided by the Calendar.getInstance().
     */
    public TimeOfDay() {
        this(Calendar.getInstance());
    }

    /**
     * The time of day, as provided by the given Calendar.
     */
    public TimeOfDay(Calendar cal) {
        init(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), null, null, cal.get(Calendar.DAY_OF_WEEK));
    }

    /**
     * Checks values and initialize fields.
     */
    private void init(int hour, int minute, Integer second, Integer millisecond, Integer weekday) {
        if (hour > 23 || hour < 0) {
            throw new IllegalArgumentException("Hour must be between 0 and 23, but is " + hour);
        }
        if (minute > 59 || minute < 0) {
            throw new IllegalArgumentException("Minute must be between 0 and 59, but is " + minute);
        }
        if (weekday != null && (weekday > 7 || weekday < 1)) {
            throw new IllegalArgumentException("Weekday must be between 1 and 7, but is " + weekday);
        }
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.millisecond = millisecond;
        this.weekday = weekday;
    }

    /**
     * The time of day, as provided by hour, minute and weekday (which my be null).
     */
    public TimeOfDay(int hour, int minute, Integer weekday) {
        init(hour, minute, null, null, weekday);
    }

    /**
     * The time of day, as provided by hour, minute without a weekday.
     */
    public TimeOfDay(int hour, int minute) {
        init(hour, minute, null, null, null);
    }

    /**
     * The time of day, as provided by the given String with the format "hour[:minute]" without a weekday.
     */
    public TimeOfDay(String time) {
        String[] parts = time.split(":");
        switch (parts.length) {
            case 2:
                init(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), null, null, null);
                break;
            case 1:
                init(Integer.valueOf(parts[0]), 0, null, null, null);
                break;
            default:
                throw new IllegalArgumentException("Time <" + time + "> not formatted as hh[:mm]");
        }
    }

    /**
     * The time of day, as provides by milliseconds since 1970.
     */
    public TimeOfDay(long millisecondsSince1970) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millisecondsSince1970);
        init(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), cal.get(Calendar.MILLISECOND),
                cal.get(Calendar.DAY_OF_WEEK));
    }

    /**
     * The hh:mm-TimeOfDay, as provided by an interval given in milliseconds.
     */
    public static TimeOfDay fromMillisecondsInterval(long milliseconds) {
        return fromSecondsInterval((int) (milliseconds / AndroidPrefsAccessor.ONE_MINUTE_MILLIS));
    }

    /**
     * The hh:mm-TimeOfDay, as provided by an interval given in minute, or the mm:ss-TimeOfDay, as provided by an interval given in
     * seconds.
     */
    public static TimeOfDay fromSecondsInterval(int interval) {
        return new TimeOfDay(interval / 60, interval % 60);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TimeOfDay other = (TimeOfDay) obj;
        if (hour != other.hour) {
            return false;
        }
        if (minute != other.minute) {
            return false;
        }
        if (weekday == null) {
            if (other.weekday != null) {
                return false;
            }
        } else if (!weekday.equals(other.weekday)) {
            return false;
        }
        return true;
    }

    /**
     * Returns a String readily representing this TimeOfDay to be used for displaying (with am/pm if locale requires).
     */
    public String getDisplayString(Context context) {
        final boolean hasSeconds = second != null && millisecond != null;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, (hasSeconds) ? second : 0);
        cal.set(Calendar.MILLISECOND, (hasSeconds) ? millisecond : 0);
        return android.text.format.DateFormat.getTimeFormat(context).format(cal.getTime());

    }

    /**
     * Returns a String readily representing this TimeOfDay to be used for displaying with a context.
     */
    public String getDisplayString() {
        return getPersistString();
    }

    /**
     * Returns a String readily representing this TimeOfDay to be used for persisting.
     */
    public String getPersistString() {
        return String.format("%02d:%02d", hour, minute);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hour;
        result = prime * result + minute;
        result = prime * result + ((weekday == null) ? 0 : weekday.hashCode());
        return result;
    }

    /**
     * Returns true if the bell should ring at this TimeOfDay, so it must be in the active time interval and the weekday of it must
     * be activated. The method name carries the historical meaning.
     *
     * @return whether bell should ring
     */
    public boolean isDaytime(AndroidPrefsAccessor prefs) {
        return isDaytime(prefs.getDaytimeStart(), prefs.getDaytimeEnd(), prefs.getActiveOnDaysOfWeek());
    }

    /**
     * Returns true if the bell should ring at this TimeOfDay, so it must be in the active time interval and the weekday of it must
     * be activated. The method name carries the historical meaning.
     *
     * @return whether bell should ring
     */
    public boolean isDaytime(TimeOfDay tStart, TimeOfDay tEnd, Set<Integer> activeOnDaysOfWeek) {
        if (!isInInterval(tStart, tEnd)) {
            return false; // time is before or after active time interval
        }
        return isActiveOnThatDay(activeOnDaysOfWeek);
    }

    /**
     * Determine whether the present time is in the semi-open interval including start up to but excluding end. If start is after
     * end, the interval is understood to span midnight.
     *
     * @param start
     * @param end
     * @return
     */
    public boolean isInInterval(TimeOfDay start, TimeOfDay end) {
        if (this.isSameTime(start)) {
            return true; // same time means to be active the whole day
        }
        if (start.isBefore(end)) {
            return start.isBefore(this) && this.isBefore(end);
        } else { // spanning midnight
            return start.isBefore(this) || this.isBefore(end);
        }
    }

    /**
     * Returns true if this weekday is one on which the bell is active.
     *
     * @param activeOnDaysOfWeek
     * @return
     */
    public boolean isActiveOnThatDay(Set<Integer> activeOnDaysOfWeek) {
        boolean result = activeOnDaysOfWeek.contains(Integer.valueOf(weekday));
        return result;
    }

    /**
     * Return true if this time is the same than the other regardless of the weekday.
     *
     * @param other
     * @return
     */
    public boolean isSameTime(TimeOfDay other) {
        return hour == other.hour && minute == other.minute;
    }

    /**
     * Return true if this time is earlier than the other regardless of the weekday.
     *
     * @param other
     * @return
     */
    public boolean isBefore(TimeOfDay other) {
        return hour < other.hour || hour == other.hour && minute < other.minute;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "TimeOfDay [" + getLogString() + "]";
    }

    /**
     * Returns a String readily representing this TimeOfDay to be used for logging.
     */
    public String getLogString() {
        final boolean hasSeconds = second != null && millisecond != null;
        final boolean hasWeekday = weekday != null;
        if (hasSeconds && hasWeekday) {
            return String.format("%02d:%02d:%02d.%03d(%d)", hour, minute, second, millisecond, weekday);
        } else if (hasSeconds && !hasWeekday) {
            return String.format("%02d:%02d:%02d.%03d", hour, minute, second, millisecond);
        } else if (!hasSeconds && hasWeekday) {
            return String.format("%02d:%02d(%d)", hour, minute, weekday);
        } else { // !hasSeconds && !hasWeekday
            return String.format("%02d:%02d", hour, minute);
        }
    }

    /**
     * Returns the hour value.
     */
    public int getHour() {
        return hour;
    }

    /**
     * Returns the minute value.
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Returns the weekday value which may be null.
     */
    public Integer getWeekday() {
        return weekday;
    }

    /**
     * Returns the hour and minute value as minutes. If this is an hh:mm-TimeOfDay the result are minutes. If this is a
     * mm:ss-TimeOfDay the result are seconds. However, the interpretation depends on the caller.
     */
    public int getInterval() {
        return hour * 60 + minute;
    }

}
