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
package com.googlecode.mindbell.accessors;

import org.junit.Test;

import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.ONE_MINUTE_MILLIS;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.ONE_MINUTE_MILLIS_NEGATIVE_PERIOD;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PrefsAccessorTest {

    @Test
    public void testGetVibrationPattern() {
        long[] ms = AndroidPrefsAccessor.getVibrationPattern("100:200:100:600");
        assertNotNull(ms);
        int i = 0;
        assertEquals(100, ms[i++]);
        assertEquals(200, ms[i++]);
        assertEquals(100, ms[i++]);
        assertEquals(600, ms[i++]);
    }

    @Test
    public void testDeriveNumberOfPeriods() {
        assertEquals(1, AndroidPrefsAccessor.deriveNumberOfPeriods("x"));
        assertEquals(1, AndroidPrefsAccessor.deriveNumberOfPeriods("1"));
        assertEquals(1, AndroidPrefsAccessor.deriveNumberOfPeriods("15"));
        assertEquals(1, AndroidPrefsAccessor.deriveNumberOfPeriods("399"));
        assertEquals(2, AndroidPrefsAccessor.deriveNumberOfPeriods("1,x"));
        assertEquals(2, AndroidPrefsAccessor.deriveNumberOfPeriods("1,x"));
        assertEquals(2, AndroidPrefsAccessor.deriveNumberOfPeriods("17, x"));
        assertEquals(2, AndroidPrefsAccessor.deriveNumberOfPeriods("22, 12"));
        assertEquals(2, AndroidPrefsAccessor.deriveNumberOfPeriods("x, x"));
        assertEquals(3, AndroidPrefsAccessor.deriveNumberOfPeriods("x, x ,x"));
        assertEquals(3, AndroidPrefsAccessor.deriveNumberOfPeriods("x,x,x"));
    }

    @Test
    public void testDerivePatternOfPeriods() {
        assertEquals("x", AndroidPrefsAccessor.derivePatternOfPeriods(1));
        assertEquals("x, x", AndroidPrefsAccessor.derivePatternOfPeriods(2));
        assertEquals("x, x, x", AndroidPrefsAccessor.derivePatternOfPeriods(3));
    }

    @Test
    public void testDerivePeriodMillis() {
        // error minute defaults
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("1000, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("1000, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("0, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("0, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("-1, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("-1, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("y, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, AndroidPrefsAccessor.derivePeriodMillis("y, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING, AndroidPrefsAccessor.derivePeriodMillis("1, 2", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_NEGATIVE_PERIOD, AndroidPrefsAccessor.derivePeriodMillis("1, 2, x", 2, 3));
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, AndroidPrefsAccessor.derivePeriodMillis("1, 2, x", 3, 3));
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, AndroidPrefsAccessor.derivePeriodMillis("x, x", 1, 1));
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING, AndroidPrefsAccessor.derivePeriodMillis("x, x", 25, 3));
        // valid results
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("x, x", 2, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("x, x", 2, 2));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis(" x, x", 2, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis(" x, x", 2, 2));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis(" 1, x", 2, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis(" 1, x", 2, 2));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("1 , x", 2, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("1 , x", 2, 2));
        assertEquals(999 * ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("999 , x", 1000, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("999 , x", 1000, 2));
        assertEquals(4 * ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 2));
        assertEquals(11 * ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 3));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 4));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 11, x, x", 18, 5));
        assertEquals(4 * ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 1));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 2));
        assertEquals(10 * ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 3));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 4));
        assertEquals(ONE_MINUTE_MILLIS, AndroidPrefsAccessor.derivePeriodMillis("4, x, 10, x, x", 17, 5));
        // can we get an error minute value with a valid request? yes, if we meditate 60001 minutes in 60000 periods => unlikely
        {
            String patternOfPeriods = AndroidPrefsAccessor.derivePatternOfPeriods((int) ONE_MINUTE_MILLIS);
            long periodMillis = AndroidPrefsAccessor.derivePeriodMillis(patternOfPeriods,
                    (int) (ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION), 2);
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis);
        }
        {
            String patternOfPeriods = AndroidPrefsAccessor.derivePatternOfPeriods((int) (2 * ONE_MINUTE_MILLIS));
            long periodMillis = AndroidPrefsAccessor.derivePeriodMillis(patternOfPeriods,
                    (int) (2 * ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION),
                            2);
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis);
        }
    }

}
