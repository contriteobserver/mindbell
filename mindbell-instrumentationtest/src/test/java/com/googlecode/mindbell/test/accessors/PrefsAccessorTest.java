/*******************************************************************************
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
 *******************************************************************************/
package com.googlecode.mindbell.test.accessors;

import com.googlecode.mindbell.accessors.PrefsAccessor;

import junit.framework.TestCase;

public class PrefsAccessorTest extends TestCase {

    public void testGetVibrationPattern() {
        PrefsAccessor prefs = MockContextAccessor.getInstance().getPrefs();
        long[] ms = prefs.getVibrationPattern();
        int i = 0;
        assertEquals(100, ms[i++]);
        assertEquals(200, ms[i++]);
        assertEquals(100, ms[i++]);
        assertEquals(600, ms[i++]);
    }

}
