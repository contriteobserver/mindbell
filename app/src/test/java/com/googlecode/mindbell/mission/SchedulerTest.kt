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
import com.googlecode.mindbell.util.TimeOfDay
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class SchedulerTest {

    @Before
    fun setUp() = MockKAnnotations.init(this)

    private val dayPrefs: Prefs
        get() {
            val prefs = mockk<Prefs>()
            every { prefs.daytimeStart } returns TimeOfDay(9, 0)
            every { prefs.daytimeEnd } returns TimeOfDay(21, 0)
            every { prefs.activeOnDaysOfWeek } returns HashSet(Arrays.asList("2", "3", "4", "5", "6"))
            every { prefs.isRandomize } returns true
            every { prefs.normalize } returns -1
            every { prefs.isNormalize } returns false
            every { prefs.interval } returns 60 * ONE_MINUTE_MILLIS
            return prefs
        }

    private val nightPrefs: Prefs
        get() {
            val prefs = mockk<Prefs>()
            every { prefs.daytimeStart } returns TimeOfDay(13, 0)
            every { prefs.daytimeEnd } returns TimeOfDay(2, 0)
            every { prefs.activeOnDaysOfWeek } returns HashSet(Arrays.asList("2", "3", "4", "5", "6"))
            every { prefs.isRandomize } returns true
            every { prefs.normalize } returns -1
            every { prefs.isNormalize } returns false
            every { prefs.interval } returns 60 * ONE_MINUTE_MILLIS
            return prefs
        }

    @Test
    fun testRescheduleNotRandomized1() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        // Current time setting in the middle of the night (05:00)
        var nowTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        var targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 0, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 0, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(11, 0, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomized2() {
        val prefs = dayPrefs
        every { prefs.daytimeStart } returns TimeOfDay(11, 11)
        every { prefs.daytimeEnd } returns TimeOfDay(11, 30)
        every { prefs.isRandomize } returns false
        // Current time setting short before the alarm should go off
        var nowTimeMillis = getTimeMillis(10, 0, Calendar.FRIDAY)
        var targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        // First reschedule from nighttime (10:00, before 11:11) to beginning of daytime (11:11)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(11, 11, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomized3() {
        val prefs = dayPrefs
        every { prefs.daytimeStart } returns TimeOfDay(11, 11)
        every { prefs.daytimeEnd } returns TimeOfDay(11, 30)
        every { prefs.isRandomize } returns false
        // Current time setting *very* short before the alarm should go off
        var nowTimeMillis = getTimeMillis(11, 0, Calendar.FRIDAY)
        var targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        // First reschedule from nighttime (11:00, before 11:11) to beginning of daytime (11:11)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(11, 11, Calendar.FRIDAY), targetTimeMillis)
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
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(8, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(9, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(21, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(20, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(21, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(21, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(9, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(23, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
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
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(1, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(2, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(13, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(12, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(13, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.FRIDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(13, 0, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
        run {
            val now = getTimeMillis(23, 59, Calendar.FRIDAY)
            val nextDaytimeStart = TimeOfDay(Scheduler.getNextDayNightChangeInMillis(now, prefs))
            assertEquals(2, nextDaytimeStart.hour)
            assertEquals(0, nextDaytimeStart.minute)
            assertEquals(Calendar.SATURDAY, nextDaytimeStart.weekday!!.toInt())
        }
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval5() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 5
        every { prefs.isNormalize } returns true
        every { prefs.interval } returns 5 * ONE_MINUTE_MILLIS
        run {
            // Current time setting in the middle of the evening (18:52)
            var targetTimeMillis = getTimeMillis(18, 52, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(18, 52, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(18, 55, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(19, 0, Calendar.FRIDAY), targetTimeMillis)
            // Third reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(19, 5, Calendar.FRIDAY), targetTimeMillis)
        }
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval60() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 5
        every { prefs.isNormalize } returns true
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 5, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 5, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(11, 5, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(12, 5, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval120() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 5
        every { prefs.isNormalize } returns true
        every { prefs.interval } returns 120 * ONE_MINUTE_MILLIS
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 5, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(11, 5, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(13, 5, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(15, 5, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToHalfPastFullHourInterval20() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 30
        every { prefs.isNormalize } returns true
        every { prefs.interval } returns 20 * ONE_MINUTE_MILLIS
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 50, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 10, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 30, Calendar.FRIDAY), targetTimeMillis)
        // Fifth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 50, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToHalfPastFullHourInterval180() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 30
        every { prefs.isNormalize } returns true
        every { prefs.interval } returns 180 * ONE_MINUTE_MILLIS
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(12, 30, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(15, 30, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(18, 30, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToQuarterPastFullHourInterval30() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 15
        every { prefs.isNormalize } returns true
        every { prefs.interval } returns 30 * ONE_MINUTE_MILLIS
        run {
            // Current time setting in the middle of the night (05:00)
            var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(9, 15, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(9, 45, Calendar.FRIDAY), targetTimeMillis)
            // Third reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis)
            // Fourth reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
            // Fifth reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis)
        }
        run {
            // Current time setting in the middle of the morning (10:14)
            var targetTimeMillis = getTimeMillis(10, 14, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(10, 14, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
        }
        run {
            // Current time setting in the middle of the morning (10:15)
            var targetTimeMillis = getTimeMillis(10, 15, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis)
        }
        run {
            // Current time setting in the middle of the morning (10:16)
            var targetTimeMillis = getTimeMillis(10, 16, Calendar.FRIDAY)
            Assert.assertEquals(getTimeMillis(10, 16, Calendar.FRIDAY), targetTimeMillis)
            // First reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis)
            // Second reschedule
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
            println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis)
        }
    }

    @Test
    fun testRescheduleNotRandomizedNormalizedToTenPastFullHourInterval20() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns false
        every { prefs.normalize } returns 10
        every { prefs.isNormalize } returns true
        every { prefs.interval } returns 20 * ONE_MINUTE_MILLIS
        // Current time setting in the middle of the night (05:00)
        var targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis)
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 10, Calendar.FRIDAY), targetTimeMillis)
        // Second reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis)
        // Third reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(9, 50, Calendar.FRIDAY), targetTimeMillis)
        // Fourth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 10, Calendar.FRIDAY), targetTimeMillis)
        // Fifth reschedule
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(targetTimeMillis, prefs)
        println("targetTimeMillis=${TimeOfDay(targetTimeMillis)}")
        Assert.assertEquals(getTimeMillis(10, 30, Calendar.FRIDAY), targetTimeMillis)
    }

    @Test
    fun testRescheduleRandomizedRepeated() {
        // Retest several times to reduce the risk that it works by chance
        for (i in 0..999) {
            testRescheduleRandomized()
        }
    }

    private fun testRescheduleRandomized() {
        val prefs = dayPrefs
        every { prefs.isRandomize } returns true
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        var nowTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY)
        var fails = 0
        var targetTimeMillis: Long
        do {
            targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        } while (getTimeMillis(9, 0, Calendar.FRIDAY) == targetTimeMillis && ++fails < 10) // may fail randomly sometimes
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(getTimeMillis(9, 0, Calendar.FRIDAY) != targetTimeMillis)
        Assert.assertTrue(targetTimeMillis >= getTimeMillis(9, 0, Calendar.FRIDAY)) // min(randomizedInterval - meanInterval / 2)
        Assert.assertTrue(targetTimeMillis <= getTimeMillis(10, 0, Calendar.FRIDAY)) // max(randomizedInterval - meanInterval / 2)
        // Second reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(getTimeMillis(10, 0, Calendar.FRIDAY) != targetTimeMillis) // may fail randomly from time to time
        Assert.assertTrue(targetTimeMillis >= nowTimeMillis + prefs.interval * 0.5) // * 0.5 = min(randomizedInterval)
        Assert.assertTrue(targetTimeMillis <= nowTimeMillis + prefs.interval * 1.5) // * 1.5 = max(randomizedInterval)
        // Third reschedule
        nowTimeMillis = targetTimeMillis
        targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(getTimeMillis(11, 0, Calendar.FRIDAY) != targetTimeMillis) // may fail randomly from time to time
        Assert.assertTrue(targetTimeMillis >= nowTimeMillis + prefs.interval * 0.5) // * 0.5 = min(randomizedInterval)
        Assert.assertTrue(targetTimeMillis <= nowTimeMillis + prefs.interval * 1.5) // * 1.5 = max(randomizedInterval)
    }

    @Test
    fun testRescheduleYieldsDayFriday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.FRIDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
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
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.FRIDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.FRIDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.FRIDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayFriday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.FRIDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.FRIDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.MONDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.MONDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.MONDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.TUESDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.MONDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.TUESDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.MONDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDayMonday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.MONDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SATURDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.SATURDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.SATURDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.SATURDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(0, 30, Calendar.SATURDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.SATURDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySaturday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SATURDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday1() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SUNDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday2() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.SUNDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday3() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(23, 0, Calendar.SUNDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday4() {
        val prefs = dayPrefs
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.SUNDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday5() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(1, 0, Calendar.SUNDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test
    fun testRescheduleYieldsDaySunday6() {
        val prefs = nightPrefs
        val nowTimeMillis = getTimeMillis(12, 0, Calendar.SUNDAY)
        val targetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        println("${TimeOfDay(nowTimeMillis)} -> ${TimeOfDay(targetTimeMillis)}")
        Assert.assertTrue(targetTimeMillis > nowTimeMillis)
        Assert.assertEquals(Calendar.MONDAY.toLong(), getWeekday(targetTimeMillis).toLong())
        Assert.assertTrue(TimeOfDay(targetTimeMillis).isDaytime(prefs))
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_getNextDaytimeStartInMillis_EmptyActiveOnDaysOfWeek() {
        val prefs = dayPrefs
        every { prefs.activeOnDaysOfWeek } returns HashSet()
        val nowTimeMillis = getTimeMillis(20, 59, Calendar.SUNDAY)
        Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
    }

}
