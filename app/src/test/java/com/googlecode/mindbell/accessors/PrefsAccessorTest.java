/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2016 Uwe Damken
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

import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_NEGATIVE_PERIOD;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PrefsAccessorTest {

    @Test
    public void testGetVibrationPattern() {
        long[] ms = PrefsAccessor.getVibrationPattern("100:200:100:600");
        assertNotNull(ms);
        int i = 0;
        assertEquals(100, ms[i++]);
        assertEquals(200, ms[i++]);
        assertEquals(100, ms[i++]);
        assertEquals(600, ms[i++]);
    }

    @Test
    public void testDeriveNumberOfPeriods() {
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("x"));
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("1"));
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("15"));
        assertEquals(1, PrefsAccessor.deriveNumberOfPeriods("399"));
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("1,x"));
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("1,x"));
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("17, x"));
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("22, 12"));
        assertEquals(2, PrefsAccessor.deriveNumberOfPeriods("x, x"));
        assertEquals(3, PrefsAccessor.deriveNumberOfPeriods("x, x ,x"));
        assertEquals(3, PrefsAccessor.deriveNumberOfPeriods("x,x,x"));
    }

    @Test
    public void testDerivePatternOfPeriods() {
        assertEquals("x", PrefsAccessor.derivePatternOfPeriods(1));
        assertEquals("x, x", PrefsAccessor.derivePatternOfPeriods(2));
        assertEquals("x, x, x", PrefsAccessor.derivePatternOfPeriods(3));
    }

    @Test
    public void testDerivePeriodMillis() {
        // error minute defaults
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("0, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("0, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("-1, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("-1, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("y, x", 25, 1));
        assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, PrefsAccessor.derivePeriodMillis("y, x", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING, PrefsAccessor.derivePeriodMillis("1, 2", 25, 2));
        assertEquals(ONE_MINUTE_MILLIS_NEGATIVE_PERIOD, PrefsAccessor.derivePeriodMillis("1, 2, x", 2, 3));
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, PrefsAccessor.derivePeriodMillis("1, 2, x", 3, 3));
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT, PrefsAccessor.derivePeriodMillis("x, x", 1, 1));
        assertEquals(ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING, PrefsAccessor.derivePeriodMillis("x, x", 25, 3));
        // valid results
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("x, x", 2, 1));
        assertEquals(ONE_MINUTE_MILLIS, PrefsAccessor.derivePeriodMillis("x, x", 2, 2));
        // can we get an error minute value with a valid request? yes, if we meditate 60001 minutes in 60000 periods => unlikely
        {
            String patternOfPeriods = PrefsAccessor.derivePatternOfPeriods((int) ONE_MINUTE_MILLIS);
            long periodMillis =
                    PrefsAccessor.derivePeriodMillis(patternOfPeriods, (int) (ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION), 2);
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis);
        }
        {
            String patternOfPeriods = PrefsAccessor.derivePatternOfPeriods((int) (2 * ONE_MINUTE_MILLIS));
            long periodMillis =
                    PrefsAccessor.derivePeriodMillis(patternOfPeriods, (int) (2 * ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION),
                            2);
            assertEquals(ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION, periodMillis);
        }
    }

}
