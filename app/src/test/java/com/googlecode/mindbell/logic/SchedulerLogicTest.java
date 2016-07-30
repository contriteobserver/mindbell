/*******************************************************************************
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
package com.googlecode.mindbell.logic;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

import com.googlecode.mindbell.logic.SchedulerLogic;
import com.googlecode.mindbell.accessors.MockContextAccessor;
import com.googlecode.mindbell.accessors.MockPrefsAccessor;
import com.googlecode.mindbell.util.TimeOfDay;

import android.util.Log;
import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class SchedulerLogicTest {

    private MockPrefsAccessor getDayPrefs() {
        MockPrefsAccessor prefs = MockContextAccessor.getInstance().getPrefs();
        prefs.setDaytimeStart(new TimeOfDay(9, 0));
        prefs.setDaytimeEnd(new TimeOfDay(21, 0));
        prefs.setActiveOnDaysOfWeek(new HashSet<>(Arrays.asList(new Integer[]{2, 3, 4, 5, 6})));
        return prefs;
    }

    // private PrefsAccessor getDayPrefsAllDays() {
    // MockPrefsAccessor dayPrefs = new MockPrefsAccessor();
    // dayPrefs.setDaytimeStart(new TimeOfDay(9, 0));
    // dayPrefs.setDaytimeEnd(new TimeOfDay(21, 0));
    // dayPrefs.setActiveOnDaysOfWeek(new HashSet<Integer>(Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 })));
    // return dayPrefs;
    // }

    private MockPrefsAccessor getNightPrefs() {
        MockPrefsAccessor prefs = MockContextAccessor.getInstance().getPrefs();
        prefs.setDaytimeStart(new TimeOfDay(13, 0));
        prefs.setDaytimeEnd(new TimeOfDay(2, 0));
        prefs.setActiveOnDaysOfWeek(new HashSet<>(Arrays.asList(new Integer[]{2, 3, 4, 5, 6})));
        return prefs;
    }

    // private long getTimeMillis(int hour, int minute) {
    // Calendar cal = Calendar.getInstance();
    // cal.set(Calendar.HOUR_OF_DAY, hour);
    // cal.set(Calendar.MINUTE, minute);
    // cal.set(Calendar.SECOND, 0);
    // cal.set(Calendar.MILLISECOND, 0);
    // return cal.getTimeInMillis();
    // }

    private long getTimeMillis(int hour, int minute, int weekday) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, weekday);
        return cal.getTimeInMillis();
    }

    private int getWeekday(long timeMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    @Test
    public void testRescheduleNotRandomized() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(false);
        // Current time setting in the middle of the night (05:00)
        long nowTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 0, Calendar.FRIDAY), targetTimeMillis);
        // Second reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 0, Calendar.FRIDAY), targetTimeMillis);
        // Third reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(11, 0, Calendar.FRIDAY), targetTimeMillis);
    }

    @Test
    public void testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval5() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(false);
        prefs.setNormalize(5);
        prefs.setInterval(5 * 60000L);
        {
            // Current time setting in the middle of the evening (18:52)
            long targetTimeMillis = getTimeMillis(18, 52, Calendar.FRIDAY);
            Assert.assertEquals(getTimeMillis(18, 52, Calendar.FRIDAY), targetTimeMillis);
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(18, 55, Calendar.FRIDAY), targetTimeMillis);
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(19, 0, Calendar.FRIDAY), targetTimeMillis);
            // Third reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(19, 5, Calendar.FRIDAY), targetTimeMillis);
        }
    }

    @Test
    public void testRescheduleNotRandomizedNormalizedToFivePastFullHourInterval60() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(false);
        prefs.setNormalize(5);
        // Current time setting in the middle of the night (05:00)
        long targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY);
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 5, Calendar.FRIDAY), targetTimeMillis);
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 5, Calendar.FRIDAY), targetTimeMillis);
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(11, 5, Calendar.FRIDAY), targetTimeMillis);
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(12, 5, Calendar.FRIDAY), targetTimeMillis);
    }

    @Test
    public void testRescheduleNotRandomizedNormalizedToHalfPastFullHourInterval20() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(false);
        prefs.setNormalize(30);
        prefs.setInterval(20 * 60000L);
        // Current time setting in the middle of the night (05:00)
        long targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY);
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis);
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 50, Calendar.FRIDAY), targetTimeMillis);
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 10, Calendar.FRIDAY), targetTimeMillis);
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 30, Calendar.FRIDAY), targetTimeMillis);
        // Fifth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 50, Calendar.FRIDAY), targetTimeMillis);
    }

    @Test
    public void testRescheduleNotRandomizedNormalizedToQuarterPastFullHourInterval30() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(false);
        prefs.setNormalize(15);
        prefs.setInterval(30 * 60000L);
        {
            // Current time setting in the middle of the night (05:00)
            long targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY);
            Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis);
            // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(9, 15, Calendar.FRIDAY), targetTimeMillis);
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(9, 45, Calendar.FRIDAY), targetTimeMillis);
            // Third reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis);
            // Fourth reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis);
            // Fifth reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis);
        }
        {
            // Current time setting in the middle of the morning (10:14)
            long targetTimeMillis = getTimeMillis(10, 14, Calendar.FRIDAY);
            Assert.assertEquals(getTimeMillis(10, 14, Calendar.FRIDAY), targetTimeMillis);
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis);
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis);
        }
        {
            // Current time setting in the middle of the morning (10:15)
            long targetTimeMillis = getTimeMillis(10, 15, Calendar.FRIDAY);
            Assert.assertEquals(getTimeMillis(10, 15, Calendar.FRIDAY), targetTimeMillis);
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis);
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis);
        }
        {
            // Current time setting in the middle of the morning (10:16)
            long targetTimeMillis = getTimeMillis(10, 16, Calendar.FRIDAY);
            Assert.assertEquals(getTimeMillis(10, 16, Calendar.FRIDAY), targetTimeMillis);
            // First reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(10, 45, Calendar.FRIDAY), targetTimeMillis);
            // Second reschedule
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
            Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
            Assert.assertEquals(getTimeMillis(11, 15, Calendar.FRIDAY), targetTimeMillis);
        }
    }

    @Test
    public void testRescheduleNotRandomizedNormalizedToTenPastFullHourInterval20() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(false);
        prefs.setNormalize(10);
        prefs.setInterval(20 * 60000L);
        // Current time setting in the middle of the night (05:00)
        long targetTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY);
        Assert.assertEquals(getTimeMillis(5, 0, Calendar.FRIDAY), targetTimeMillis);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 10, Calendar.FRIDAY), targetTimeMillis);
        // Second reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 30, Calendar.FRIDAY), targetTimeMillis);
        // Third reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(9, 50, Calendar.FRIDAY), targetTimeMillis);
        // Fourth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 10, Calendar.FRIDAY), targetTimeMillis);
        // Fifth reschedule
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(targetTimeMillis, prefs);
        Log.d(TAG, "targetTimeMillis=" + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertEquals(getTimeMillis(10, 30, Calendar.FRIDAY), targetTimeMillis);
    }

    @Test
    public void testRescheduleRandomized() {
        MockPrefsAccessor prefs = getDayPrefs();
        prefs.setRandomize(true);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        long nowTimeMillis = getTimeMillis(5, 0, Calendar.FRIDAY);
        int fails = 0;
        long targetTimeMillis;
        do {
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        } while (getTimeMillis(9, 0, Calendar.FRIDAY) == targetTimeMillis && ++fails < 10); // may fail randomly sometimes
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(getTimeMillis(9, 0, Calendar.FRIDAY) != targetTimeMillis);
        Assert.assertTrue(targetTimeMillis >= getTimeMillis(9, 0, Calendar.FRIDAY)); // min(randomizedInterval - meanInterval / 2)
        Assert.assertTrue(targetTimeMillis <= getTimeMillis(10, 0, Calendar.FRIDAY)); // max(randomizedInterval - meanInterval / 2)
        // Second reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(getTimeMillis(10, 0, Calendar.FRIDAY) != targetTimeMillis); // may fail randomly from time to time
        Assert.assertTrue(targetTimeMillis >= nowTimeMillis + prefs.getInterval() * 0.5); // * 0.5 = min(randomizedInterval)
        Assert.assertTrue(targetTimeMillis <= nowTimeMillis + prefs.getInterval() * 1.5); // * 1.5 = max(randomizedInterval)
        // Third reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(getTimeMillis(11, 0, Calendar.FRIDAY) != targetTimeMillis); // may fail randomly from time to time
        Assert.assertTrue(targetTimeMillis >= nowTimeMillis + prefs.getInterval() * 0.5); // * 0.5 = min(randomizedInterval)
        Assert.assertTrue(targetTimeMillis <= nowTimeMillis + prefs.getInterval() * 1.5); // * 1.5 = max(randomizedInterval)
    }

    @Test
    public void testRescheduleRandomizedRepeated() {
        // Retest several times to reduce the risk that it works by chance
        for (int i = 0; i < 1000; i++) {
            testRescheduleRandomized();
        }
    }

    @Test
    public void testRescheduleYieldsDayFriday1() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayFriday2() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayFriday3() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 0, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayFriday4() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayFriday5() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayFriday6() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayMonday1() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayMonday2() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayMonday3() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 0, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.TUESDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayMonday4() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.TUESDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayMonday5() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDayMonday6() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySaturday1() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySaturday2() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySaturday3() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 0, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySaturday4() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySaturday5() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySaturday6() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySunday1() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySunday2() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySunday3() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 0, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySunday4() {
        MockPrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySunday5() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(1, 0, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    @Test
    public void testRescheduleYieldsDaySunday6() {
        MockPrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 0, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        Assert.assertTrue(targetTimeMillis > nowTimeMillis);
        Assert.assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        Assert.assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

}
