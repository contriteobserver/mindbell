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
package com.googlecode.mindbell.mission

import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_NEGATIVE_PERIOD
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrefsTest {

    @Test
    fun testGetVibrationPattern() {
        val ms = Prefs.getVibrationPattern("100:200:100:600")
        assertNotNull(ms)
        var i = 0
        assertEquals(100, ms[i++])
        assertEquals(200, ms[i++])
        assertEquals(100, ms[i++])
        assertEquals(600, ms[i++])
    }

    @Test
    fun testDeriveNumberOfPeriods() {
        assertEquals(1, Prefs.deriveNumberOfPeriods("x").toLong())
        assertEquals(1, Prefs.deriveNumberOfPeriods("1").toLong())
        assertEquals(1, Prefs.deriveNumberOfPeriods("15").toLong())
        assertEquals(1, Prefs.deriveNumberOfPeriods("399").toLong())
        assertEquals(2, Prefs.deriveNumberOfPeriods("1,x").toLong())
        assertEquals(2, Prefs.deriveNumberOfPeriods("1,x").toLong())
        assertEquals(2, Prefs.deriveNumberOfPeriods("17, x").toLong())
        assertEquals(2, Prefs.deriveNumberOfPeriods("22, 12").toLong())
        assertEquals(2, Prefs.deriveNumberOfPeriods("x, x").toLong())
        assertEquals(3, Prefs.deriveNumberOfPeriods("x, x ,x").toLong())
        assertEquals(3, Prefs.deriveNumberOfPeriods("x,x,x").toLong())
    }

    @Test
    fun testDerivePatternOfPeriods() {
        assertEquals("x", Prefs.derivePatternOfPeriods(1))
        assertEquals("x, x", Prefs.derivePatternOfPeriods(2))
        assertEquals("x, x, x", Prefs.derivePatternOfPeriods(3))
    }

    @Test
    fun testDerivePeriodMillis() {
        // error minute defaults
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("1000, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("1000, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("0, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("0, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("-1, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("-1, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("y, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, Prefs.derivePeriodMillis("y, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING, Prefs.derivePeriodMillis("1, 2", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_NEGATIVE_PERIOD, Prefs.derivePeriodMillis("1, 2, x", 2, 3))
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, Prefs.derivePeriodMillis("1, 2, x", 3, 3))
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, Prefs.derivePeriodMillis("x, x", 1, 1))
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING, Prefs.derivePeriodMillis("x, x", 25, 3))
        // valid results
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("x, x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("x, x", 2, 2))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis(" x, x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis(" x, x", 2, 2))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis(" 1, x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis(" 1, x", 2, 2))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("1 , x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("1 , x", 2, 2))
        assertEquals(999 * ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("999 , x", 1000, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("999 , x", 1000, 2))
        assertEquals(4 * ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 11, x, x", 18, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 11, x, x", 18, 2))
        assertEquals(11 * ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 11, x, x", 18, 3))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 11, x, x", 18, 4))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 11, x, x", 18, 5))
        assertEquals(4 * ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 10, x, x", 17, 1))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 10, x, x", 17, 2))
        assertEquals(10 * ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 10, x, x", 17, 3))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 10, x, x", 17, 4))
        assertEquals(ONE_MINUTE_MILLIS, Prefs.derivePeriodMillis("4, x, 10, x, x", 17, 5))
        // can we get an error minute value with a valid request? yes, if we meditate 60001 minutes in 60000 periods => unlikely
        run {
            val patternOfPeriods = Prefs.derivePatternOfPeriods(ONE_MINUTE_MILLIS.toInt())
            val periodMillis = Prefs.derivePeriodMillis(patternOfPeriods, ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION.toInt(), 2)
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis)
        }
        run {
            val patternOfPeriods = Prefs.derivePatternOfPeriods((2 * ONE_MINUTE_MILLIS).toInt())
            val periodMillis = Prefs.derivePeriodMillis(patternOfPeriods, (2 * ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION).toInt(),
                    2)
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis)
        }
    }

}
