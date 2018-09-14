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
package com.googlecode.mindbell.util

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.util.*

class UtilsTest {

    @Test
    fun test_deriveOrderedEntrySummary_SundayFirst() {
        val values = HashSet(Arrays.asList("1", "3", "7"))
        val entries = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val entryValues = arrayOf("1", "2", "3", "4", "5", "6", "7")
        assertEquals("Sunday, Tuesday, Saturday", Utils.deriveOrderedEntrySummary(values, entries, entryValues))
    }

    @Test
    fun test_deriveOrderedEntrySummary_SundayLast() {
        val values = HashSet(Arrays.asList("1", "3", "7"))
        val entries = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val entryValues = arrayOf("2", "3", "4", "5", "6", "7", "1")
        assertEquals("Tuesday, Saturday, Sunday", Utils.deriveOrderedEntrySummary(values, entries, entryValues))
    }

    @Test
    fun test_deriveOrderedEntrySummary_SunFirst() {
        val values = HashSet(Arrays.asList("1", "3", "7"))
        val entries = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val entryValues = arrayOf("1", "2", "3", "4", "5", "6", "7")
        assertEquals("Sun, Tue, Sat", Utils.deriveOrderedEntrySummary(values, entries, entryValues))
    }

    @Test
    fun test_deriveOrderedEntrySummary_SunLast() {
        val values = HashSet(Arrays.asList("1", "3", "7"))
        val entries = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val entryValues = arrayOf("2", "3", "4", "5", "6", "7", "1")
        assertEquals("Tue, Sat, Sun", Utils.deriveOrderedEntrySummary(values, entries, entryValues))
    }

}
