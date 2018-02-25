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
package com.googlecode.mindbell.accessors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PrefsAccessorTest {

    @Test
    fun testGetVibrationPattern() {
        val ms = PrefsAccessor.getVibrationPattern("100:200:100:600")
        assertNotNull(ms)
        var i = 0
        assertEquals(100, ms[i++])
        assertEquals(200, ms[i++])
        assertEquals(100, ms[i++])
        assertEquals(600, ms[i++])
    }

    @Test
    fun testDeriveNumberOfPeriods() {
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("x").toLong())
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("1").toLong())
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("15").toLong())
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("399").toLong())
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("1,x").toLong())
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("1,x").toLong())
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("17, x").toLong())
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("22, 12").toLong())
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("x, x").toLong())
        assertEquals(3, PrefsAccessor.deriveNumberOfPeriods("x, x ,x").toLong())
        assertEquals(3, PrefsAccessor.deriveNumberOfPeriods("x,x,x").toLong())
    }

    @Test
    fun testDerivePatternOfPeriods() {
        assertEquals("x", PrefsAccessor.derivePatternOfPeriods(1))
        assertEquals("x, x", PrefsAccessor.derivePatternOfPeriods(2))
        assertEquals("x, x, x", PrefsAccessor.derivePatternOfPeriods(3))
    }

    @Test
    fun testDerivePeriodMillis() {
        // error minute defaults
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("1000, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("1000, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("0, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("0, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("-1, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("-1, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("y, x", 25, 1))
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("y, x", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING, PrefsAccessor.derivePeriodMillis("1, 2", 25, 2))
        assertEquals(ONE_MINUTE_MILLIS_NEGATIVE_PERIOD, PrefsAccessor.derivePeriodMillis("1, 2, x", 2, 3))
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, PrefsAccessor.derivePeriodMillis("1, 2, x", 3, 3))
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, PrefsAccessor.derivePeriodMillis("x, x", 1, 1))
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING, PrefsAccessor.derivePeriodMillis("x, x", 25, 3))
        // valid results
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("x, x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("x, x", 2, 2))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis(" x, x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis(" x, x", 2, 2))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis(" 1, x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis(" 1, x", 2, 2))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("1 , x", 2, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("1 , x", 2, 2))
        assertEquals(999 * ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("999 , x", 1000, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("999 , x", 1000, 2))
        assertEquals(4 * ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 2))
        assertEquals(11 * ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 3))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 4))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 5))
        assertEquals(4 * ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 1))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 2))
        assertEquals(10 * ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 3))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 4))
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 5))
        // can we get an error minute value with a valid request? yes, if we meditate 60001 minutes in 60000 periods => unlikely
        run {
            val patternOfPeriods = PrefsAccessor.derivePatternOfPeriods(ONE_MINUTE_MILLIS.toInt())
            val periodMillis = PrefsAccessor.derivePeriodMillis(patternOfPeriods, ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION.toInt(), 2)
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis)
        }
        run {
            val patternOfPeriods = PrefsAccessor.derivePatternOfPeriods((2 * ONE_MINUTE_MILLIS).toInt())
            val periodMillis = PrefsAccessor.derivePeriodMillis(patternOfPeriods, (2 * ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION).toInt(),
                    2)
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis)
        }
    }

}
