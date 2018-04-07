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

import com.googlecode.mindbell.Prefs
import junit.framework.Assert.*
import org.junit.Test
import java.util.*

class TimeOfDayTest {

    @Test
    fun testBefore1() {
        val t1 = TimeOfDay(9, 1)
        val t2 = TimeOfDay(9, 2)
        assertTrue(t1.isBefore(t2))
        assertFalse(t2.isBefore(t1))
    }

    @Test
    fun testBefore2() {
        val t1 = TimeOfDay(8, 5)
        val t2 = TimeOfDay(9, 2)
        assertTrue(t1.isBefore(t2))
        assertFalse(t2.isBefore(t1))
    }

    @Test
    fun testCalendarConstructor1() {
        val hour = 13
        val minute = 17
        val weekday = 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testCalendarConstructor2() {
        val hour = 13
        val minute = 17
        val weekday = 2
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testCalendarConstructor3() {
        val hour = 13
        val minute = 17
        val weekday = 3
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testCalendarConstructor4() {
        val hour = 13
        val minute = 17
        val weekday = 4
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testCalendarConstructor5() {
        val hour = 13
        val minute = 17
        val weekday = 5
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testCalendarConstructor6() {
        val hour = 13
        val minute = 17
        val weekday = 6
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testCalendarConstructor7() {
        val hour = 13
        val minute = 17
        val weekday = 7
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testGetters1() {
        val hour = 1
        val minute = 2
        val t = TimeOfDay(hour, minute)
        assertEquals(hour, t.hour)
        assertEquals(minute, t.minute)
        assertNull(t.weekday)
    }

    @Test
    fun testGetters2() {
        val hour = 1
        val minute = 2
        val weekday = 5
        val t = TimeOfDay(hour, minute, weekday)
        assertEquals(hour, t.hour)
        assertEquals(minute, t.minute)
        assertEquals(weekday, t.weekday!!.toInt())
    }

    @Test
    fun testIdentity1() {
        val time = TimeOfDay(9, 15)
        assertEquals(TimeOfDay(9, 15), time)
    }

    @Test
    fun testIdentity2() {
        val time = TimeOfDay(9, 15, 1)
        assertEquals(TimeOfDay(9, 15, 1), time)
    }

    @Test
    fun testIdentity3() {
        val time = TimeOfDay(9, 15, 1)
        assertFalse(time == TimeOfDay(9, 15, 2))
    }

    @Test
    fun testInterval1() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(21, 0)
        val t = TimeOfDay(12, 15)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInterval10() {
        val start = TimeOfDay(23, 0, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(4, 59, null)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInterval2() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(9, 0)
        val t = TimeOfDay(9, 0)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInterval3() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(9, 1)
        val t = TimeOfDay(9, 0)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInterval4() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(9, 1)
        val t = TimeOfDay(9, 1)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun testInterval5() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(9, 59)
        val t = TimeOfDay(10, 37)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun testInterval6() {
        val start = TimeOfDay(9, 0, 2)
        val end = TimeOfDay(21, 0, 1)
        val t = TimeOfDay(12, 15, 3)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInterval7() {
        val start = TimeOfDay(9, 0, 5)
        val end = TimeOfDay(9, 59, 6)
        val t = TimeOfDay(10, 37, 7)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun testInterval8() {
        val start = TimeOfDay(13, 0, null)
        val end = TimeOfDay(2, 0, null)
        val t = TimeOfDay(13, 0, 7)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInterval9() {
        val start = TimeOfDay(23, 0, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(23, 0, null)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun testInvalid1() {
        try {
            TimeOfDay(25, 0)
        } catch (iae: IllegalArgumentException) {
            // expected
            return
        }

        fail("Should have thrown an IllegalArgumentException")
    }

    @Test
    fun testInvalid2() {
        try {
            TimeOfDay(-1, 0)
        } catch (iae: IllegalArgumentException) {
            // expected
            return
        }

        fail("Should have thrown an IllegalArgumentException")
    }

    @Test
    fun testInvalid3() {
        try {
            TimeOfDay(5, 60)
        } catch (iae: IllegalArgumentException) {
            // expected
            return
        }

        fail("Should have thrown an IllegalArgumentException")
    }

    @Test
    fun testInvalid4() {
        try {
            TimeOfDay(5, -1)
        } catch (iae: IllegalArgumentException) {
            // expected
            return
        }

        fail("Should have thrown an IllegalArgumentException")
    }

    @Test
    fun testInvalid5() {
        try {
            TimeOfDay(5, 5, 0)
        } catch (iae: IllegalArgumentException) {
            // expected
            return
        }

        fail("Should have thrown an IllegalArgumentException")
    }

    @Test
    fun testInvalid6() {
        try {
            TimeOfDay(6, 6, 8)
        } catch (iae: IllegalArgumentException) {
            // expected
            return
        }

        fail("Should have thrown an IllegalArgumentException")
    }

    @Test
    fun testIsSameTime1() {
        val time = TimeOfDay(9, 15)
        assertTrue(time.isSameTime(TimeOfDay(9, 15)))
    }

    @Test
    fun testIsSameTime2() {
        val time = TimeOfDay(9, 15, 1)
        assertTrue(time.isSameTime(TimeOfDay(9, 15, 2)))
    }

    @Test
    fun testIsSameTime3() {
        val time = TimeOfDay(9, 15, null)
        assertTrue(time.isSameTime(TimeOfDay(9, 15, 2)))
    }

    @Test
    fun testMillisecondConstructor1() {
        val hour = 13
        val minute = 17
        val weekday = 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testMillisecondConstructor2() {
        val hour = 13
        val minute = 17
        val weekday = 2
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testMillisecondConstructor3() {
        val hour = 13
        val minute = 17
        val weekday = 3
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testMillisecondConstructor4() {
        val hour = 13
        val minute = 17
        val weekday = 4
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testMillisecondConstructor5() {
        val hour = 13
        val minute = 17
        val weekday = 5
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testMillisecondConstructor6() {
        val hour = 13
        val minute = 17
        val weekday = 6
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun testMillisecondConstructor7() {
        val hour = 13
        val minute = 17
        val weekday = 7
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        val t = TimeOfDay(cal.timeInMillis)
        assertEquals(TimeOfDay(hour, minute, weekday), t)
    }

    @Test
    fun test_isInInterval_NightInterval1() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(1, 1)
        val t = TimeOfDay(9, 1)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval2() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(1, 1)
        val t = TimeOfDay(0, 1)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval3() {
        val start = TimeOfDay(9, 0)
        val end = TimeOfDay(1, 1)
        val t = TimeOfDay(1, 1)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval4() {
        val start = TimeOfDay(23, 59)
        val end = TimeOfDay(0, 0)
        val t = TimeOfDay(9, 1)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval5() {
        val start = TimeOfDay(23, 59)
        val end = TimeOfDay(0, 1)
        val t = TimeOfDay(0, 0)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval6() {
        val start = TimeOfDay(23, 59, 4)
        val end = TimeOfDay(0, 1, 2)
        val t = TimeOfDay(0, 0, 1)
        assertTrue(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval7() {
        val start = TimeOfDay(9, 0, 7)
        val end = TimeOfDay(1, 1, 1)
        val t = TimeOfDay(1, 1, 5)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval8() {
        val start = TimeOfDay(23, 0, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(5, 0, null)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun test_isInInterval_NightInterval9() {
        val start = TimeOfDay(22, 59, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(5, 0, null)
        assertFalse(t.isInInterval(start, end))
    }

    @Test
    fun test_isDaytime_NightInterval1() {
        val start = TimeOfDay(22, 59, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(22, 59, Calendar.FRIDAY)
        assertTrue(t.isDaytime(start, end, HashSet(Arrays.asList(Calendar.FRIDAY))))
    }

    @Test
    fun test_isDaytime_NightInterval2() {
        val start = TimeOfDay(22, 59, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(23, 59, Calendar.FRIDAY)
        assertTrue(t.isDaytime(start, end, HashSet(Arrays.asList(Calendar.FRIDAY))))
    }

    @Test
    fun test_isDaytime_NightInterval3() {
        val start = TimeOfDay(22, 59, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(0, 0, Calendar.SATURDAY)
        assertTrue(t.isDaytime(start, end, HashSet(Arrays.asList(Calendar.FRIDAY))))
    }

    @Test
    fun test_isDaytime_NightInterval4() {
        val start = TimeOfDay(22, 59, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(4, 59, Calendar.SATURDAY)
        assertTrue(t.isDaytime(start, end, HashSet(Arrays.asList(Calendar.FRIDAY))))
    }

    @Test
    fun test_isDaytime_NightInterval5() {
        val start = TimeOfDay(22, 59, null)
        val end = TimeOfDay(5, 0, null)
        val t = TimeOfDay(5, 0, Calendar.SATURDAY)
        assertFalse(t.isDaytime(start, end, HashSet(Arrays.asList(Calendar.FRIDAY))))
    }

    @Test
    fun testStringConstructorOldValues() {
        for (hh in 0..23) {
            val time = TimeOfDay(hh.toString())
            assertEquals(hh, time.hour)
            assertEquals(0, time.minute)
            assertNull(time.weekday)
        }
    }

    @Test
    fun testStringConstructorNewValues() {
        for (hh in 0..23) {
            for (mm in 0..59) {
                val time = TimeOfDay(String.format("%02d:%02d", hh, mm))
                assertEquals(hh, time.hour)
                assertEquals(mm, time.minute)
                assertNull(time.weekday)
            }
        }
    }

    @Test
    fun testFromMillisecondsInterval() {
        for (min in 0 until 24 * 60 - 1) {
            val time = TimeOfDay.fromMillisecondsInterval(min * Prefs.ONE_MINUTE_MILLIS)
            assertEquals(min, time.interval)
        }
    }

    @Test
    fun testfromSecondsInterval() {
        for (s in 0 until 24 * 60 - 1) {
            val time = TimeOfDay.fromSecondsInterval(s)
            assertEquals(s, time.interval)
        }
    }

}
