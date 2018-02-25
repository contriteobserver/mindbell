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
package com.googlecode.mindbell.logic

import com.googlecode.mindbell.accessors.PrefsAccessor
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.ONE_MINUTE_MILLIS
import com.googlecode.mindbell.util.TimeOfDay
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.runners.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class SchedulerLogicTest {

    private val dayPrefs: PrefsAccessor
        get() {
            val prefs = mock<PrefsAccessor>(PrefsAccessor::class.java)
            `when`(prefs.daytimeStart).thenReturn(TimeOfDay(9, 0))
            `when`(prefs.daytimeEnd).thenReturn(TimeOfDay(21, 0))
            `when`(prefs.activeOnDaysOfWeek).thenReturn(HashSet(Arrays.asList(*arrayOf(2, 3, 4, 5, 6))))
            `when`(prefs.isRandomize).thenReturn(true)
            `when`(prefs.normalize).thenReturn(-1)
            `when`(prefs.interval).thenReturn(60 * ONE_MINUTE_MILLIS)
            return prefs
        }

    private val nightPrefs: PrefsAccessor
        get() {
            val prefs = mock<PrefsAccessor>(PrefsAccessor::class.java)
            `when`(prefs.daytimeStart).thenReturn(TimeOfDay(13, 0))
            `when`(prefs.daytimeEnd).thenReturn(TimeOfDay(2, 0))
            `when`(prefs.activeOnDaysOfWeek).thenReturn(HashSet(Arrays.asList(*arrayOf(2, 3, 4, 5, 6))))
            `when`(prefs.isRandomize).thenReturn(true)
            `when`(prefs.normalize).thenReturn(-1)
            `when`(prefs.interval).thenReturn(60 * ONE_MINUTE_MILLIS)
            return prefs
        }

    @Test
    fun testRescheduleNotRandomized() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        // Current time setting in the middle of the night (05:00)
        var nowTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        var targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 0, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 0, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(11, 0, Calendar.FRIDAY), targetTimeMillis)
    }

    private fun getTimeMillis(hour: Int, minute: Int, weekday: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_WEEK, weekday)
        return cal.timeInMillis
    }

    @Test
    fun test_getNextDayNightChangeInMillis_forDayPrefs() {
        val prefs = dayPrefs
        run {
            val now = getTimeMillis(0, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(8, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(9, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(21, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(20, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(21, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(21, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(23, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
    }

    @Test
    fun test_getNextDayNightChangeInMillis_forNightPrefs() {
        val prefs = nightPrefs
        run {
            val now = getTimeMillis(0, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(1, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(2, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(13, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(12, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(13, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(13, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(23, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(SchedulerLogic.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval5() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(5)
        `when`(prefs.interval).thenReturn(5 * ONE_MINUTE_MILLIS)
        run {
            // Current time setting in the middle of the evening (18:52)
            var targetTimeMillis = getTimeMillis(18, 52, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(18, 52, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(18, 55, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(19, 0, Calendar.FRIDAY), targetTimeMillis)
            // Third reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(19, 5, Calendar.FRIDAY), targetTimeMillis)
        }
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval60() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(5)
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 5, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 5, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(11, 5, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(12, 5, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval120() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(5)
        `when`(prefs.interval).thenReturn(120 * ONE_MINUTE_MILLIS)
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 5, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(11, 5, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(13, 5, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(15, 5, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToHalfPastFullHourInterval20() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(30)
        `when`(prefs.interval).thenReturn(20 * ONE_MINUTE_MILLIS)
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 50, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 10, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 30, Calendar.FRIDAY), targetTimeMillis)
        // Fifth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 50, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToHalfPastFullHourInterval180() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(30)
        `when`(prefs.interval).thenReturn(180 * ONE_MINUTE_MILLIS)
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(12, 30, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(15, 30, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(18, 30, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToQuarterPastFullHourInterval30() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(15)
        `when`(prefs.interval).thenReturn(30 * ONE_MINUTE_MILLIS)
        run {
            // Current time setting in the middle of the night (05:00)
            var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(9, 15, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(9, 45, Calendar.FRIDAY), targetTimeMillis)
            // Third reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis)
            // Fourth reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
            // Fifth reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis)
        }
        run {
            // Current time setting in the middle of the morning (10:14)
            var targetTimeMillis = getTimeMillis(10, 14, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(10, 14, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
        }
        run {
            // Current time setting in the middle of the morning (10:15)
            var targetTimeMillis = getTimeMillis(10, 15, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis)
        }
        run {
            // Current time setting in the middle of the morning (10:16)
            var targetTimeMillis = getTimeMillis(10, 16, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(10, 16, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis)
        }
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToTenPastFullHourInterval20() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(false)
        `when`(prefs.normalize).thenReturn(10)
        `when`(prefs.interval).thenReturn(20 * ONE_MINUTE_MILLIS)
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 10, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(9, 50, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 10, Calendar.FRIDAY), targetTimeMillis)
        // Fifth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=" + TimeOfDay(targetTimeMillis).toString())
        Assert.assertEquals(getTimeMillis(10, 30, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleRandomizedRepeated() {
        // Retest several times to reduce the risk that it works by chance
        for (i in 0..999) {
            testRescheduleRandomized()
        }
    }

    @Test
    fun testRescheduleRandomized() {
        val prefs = dayPrefs
        `when`(prefs.isRandomize).thenReturn(true)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        var nowTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        var fails = 0
        var targetTimeMillis: Long
        do {
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        } while (getTimeMillis(9, 0, Calendar.FRIDAY) == targetTimeMillis && ++fails < 10) // may fail randomly sometimes
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(getTimeMillis(9, 0, Calendar.FRIDAY) != targetTimeMillis)
        Assert.assertTrue(targetTimeMillis >= getTimeMillis(9, 0, Calendar.FRIDAY)) // min(randomizedInterval - meanInterval / 2)
        Assert.assertTrue(targetTimeMillis <= getTimeMillis(10, 0, Calendar.FRIDAY)) // max(randomizedInterval - meanInterval / 2)
        // Second reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(getTimeMillis(10, 0, Calendar.FRIDAY) != targetTimeMillis) // may fail randomly from time to time
        Assert.assertTrue(targetTimeMillis >= nowTimeMillis + prefs.interval * 0.5) // * 0.5 = min(randomizedInterval)
        Assert.assertTrue(targetTimeMillis <= nowTimeMillis + prefs.interval * 1.5) // * 1.5 = max(randomizedInterval)
        // Third reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(getTimeMillis(11, 0, Calendar.FRIDAY) != targetTimeMillis) // may fail randomly from time to time
        Assert.assertTrue(targetTimeMillis >= nowTimeMillis + prefs.interval * 0.5) // * 0.5 = min(randomizedInterval)
        Assert.assertTrue(targetTimeMillis <= nowTimeMillis + prefs.interval * 1.5) // * 1.5 = max(randomizedInterval)
    }

    @Test
    fun testRescheduleYieldsDayFriday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.FRIDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    private fun getWeekday(timeMillis: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMillis
        return cal.get(Calendar.DAY_OF_WEEK)
    }

    @Test
    fun testRescheduleYieldsDayFriday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.FRIDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.FRIDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.FRIDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.FRIDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.FRIDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.MONDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.MONDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.MONDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.TUESDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.MONDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.TUESDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.MONDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.MONDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SATURDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.SATURDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.SATURDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.SATURDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(0, 30, Calendar.SATURDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.SATURDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SATURDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println(TimeOfDay(nowTimeMillis).toString() + " -> " + TimeOfDay(targetTimeMillis).toString())
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_getNextDaytimeStartInMillis_EmptyActiveOnDaysOfWeek() {
        val prefs = dayPrefs
        `when`(prefs.activeOnDaysOfWeek).thenReturn(HashSet())
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.SUNDAY)
        val targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
    }

}
