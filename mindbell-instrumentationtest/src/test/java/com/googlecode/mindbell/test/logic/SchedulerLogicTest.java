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
package com.googlecode.mindbell.test.logic;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Random;

import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.logic.SchedulerLogic;
import com.googlecode.mindbell.test.accessors.MockContextAccessor;
import com.googlecode.mindbell.test.accessors.MockPrefsAccessor;
import com.googlecode.mindbell.util.TimeOfDay;

import android.util.Log;
import junit.framework.TestCase;

/**
 * @author marc
 *
 */
public class SchedulerLogicTest extends TestCase {

    private PrefsAccessor getDayPrefs() {
        MockPrefsAccessor prefs = MockContextAccessor.getInstance().getPrefs();
        prefs.setDaytimeStart(new TimeOfDay(9, 0));
        prefs.setDaytimeEnd(new TimeOfDay(21, 0));
        prefs.setActiveOnDaysOfWeek(new HashSet<Integer>(Arrays.asList(new Integer[] { 2, 3, 4, 5, 6 })));
        return prefs;
    }

    // private PrefsAccessor getDayPrefsAllDays() {
    // MockPrefsAccessor dayPrefs = new MockPrefsAccessor();
    // dayPrefs.setDaytimeStart(new TimeOfDay(9, 0));
    // dayPrefs.setDaytimeEnd(new TimeOfDay(21, 0));
    // dayPrefs.setActiveOnDaysOfWeek(new HashSet<Integer>(Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 })));
    // return dayPrefs;
    // }

    private PrefsAccessor getNightPrefs() {
        MockPrefsAccessor prefs = MockContextAccessor.getInstance().getPrefs();
        prefs.setDaytimeStart(new TimeOfDay(13, 0));
        prefs.setDaytimeEnd(new TimeOfDay(2, 0));
        prefs.setActiveOnDaysOfWeek(new HashSet<Integer>(Arrays.asList(new Integer[] { 2, 3, 4, 5, 6 })));
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

    public void testRescheduleNotRandomized() {
        PrefsAccessor prefs = getDayPrefs();
        ((MockPrefsAccessor) prefs).setRandomize(false);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        long nowTimeMillis = getTimeMillis(5, 00, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertEquals(getTimeMillis(9, 0, Calendar.FRIDAY), targetTimeMillis);
        // Second reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertEquals(getTimeMillis(10, 0, Calendar.FRIDAY), targetTimeMillis);
        // Third reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertEquals(getTimeMillis(11, 0, Calendar.FRIDAY), targetTimeMillis);
    }

    public void testRescheduleNotRandomizedSeeded() {
        Random previousRandom = SchedulerLogic.random; // save for later restoration
        try {
            SchedulerLogic.random = new Random(456789253L);
            PrefsAccessor prefs = getDayPrefs();
            ((MockPrefsAccessor) prefs).setRandomize(false);
            // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
            long nowTimeMillis = getTimeMillis(5, 00, Calendar.FRIDAY);
            long differenceTimeMillis = nowTimeMillis - 1467349200000L; // difference to first used Calendar instance
            assertEquals("TimeOfDay [hour=5, minute=0, weekday=6]", (new TimeOfDay(nowTimeMillis)).toString());
            long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=9, minute=0, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467363600000L, targetTimeMillis - differenceTimeMillis);
            // Second reschedule
            nowTimeMillis = targetTimeMillis;
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=10, minute=0, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467363600000L + 1 * 3600000L, targetTimeMillis - differenceTimeMillis);
            // Third reschedule
            nowTimeMillis = targetTimeMillis;
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=11, minute=0, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467363600000L + 2 * 3600000L, targetTimeMillis - differenceTimeMillis);
            // Fourth reschedule
            nowTimeMillis = targetTimeMillis;
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=12, minute=0, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467363600000L + 3 * 3600000L, targetTimeMillis - differenceTimeMillis);
        } finally {
            SchedulerLogic.random = previousRandom; // restore unseeded generator
        }
    }

    public void testRescheduleRandomized() {
        PrefsAccessor prefs = getDayPrefs();
        ((MockPrefsAccessor) prefs).setRandomize(true);
        // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
        long nowTimeMillis = getTimeMillis(5, 00, Calendar.FRIDAY);
        int fails = 0;
        long targetTimeMillis;
        do {
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        } while (getTimeMillis(9, 0, Calendar.FRIDAY) == targetTimeMillis && ++fails < 10); // may fail randomly sometimes
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(getTimeMillis(9, 0, Calendar.FRIDAY) != targetTimeMillis);
        assertTrue(targetTimeMillis >= getTimeMillis(9, 00, Calendar.FRIDAY)); // min(randomizedInterval - meanInterval / 2)
        assertTrue(targetTimeMillis <= getTimeMillis(10, 00, Calendar.FRIDAY)); // max(randomizedInterval - meanInterval / 2)
        // Second reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(getTimeMillis(10, 0, Calendar.FRIDAY) != targetTimeMillis); // may fail randomly from time to time
        assertTrue(targetTimeMillis >= nowTimeMillis + prefs.getInterval() * 0.5); // * 0.5 = min(randomizedInterval)
        assertTrue(targetTimeMillis <= nowTimeMillis + prefs.getInterval() * 1.5); // * 1.5 = max(randomizedInterval)
        // Third reschedule
        nowTimeMillis = targetTimeMillis;
        targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(getTimeMillis(11, 0, Calendar.FRIDAY) != targetTimeMillis); // may fail randomly from time to time
        assertTrue(targetTimeMillis >= nowTimeMillis + prefs.getInterval() * 0.5); // * 0.5 = min(randomizedInterval)
        assertTrue(targetTimeMillis <= nowTimeMillis + prefs.getInterval() * 1.5); // * 1.5 = max(randomizedInterval)
    }

    public void testRescheduleRandomizedRepeated() {
        // Retest several times to reduce the risk that it works by chance
        for (int i = 0; i < 1000; i++) {
            testRescheduleRandomized();
        }
    }

    public void testRescheduleRandomizedSeeded() {
        Random previousRandom = SchedulerLogic.random; // save for later restoration
        try {
            SchedulerLogic.random = new Random(456789253L);
            PrefsAccessor prefs = getDayPrefs();
            ((MockPrefsAccessor) prefs).setRandomize(true);
            // First reschedule from nighttime (05:00, before 09:00) to beginning of daytime (09:00)
            long nowTimeMillis = getTimeMillis(5, 00, Calendar.FRIDAY);
            long differenceTimeMillis = nowTimeMillis - 1467349200000L; // difference to first used Calendar instance
            assertEquals("TimeOfDay [hour=5, minute=0, weekday=6]", (new TimeOfDay(nowTimeMillis)).toString());
            long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=9, minute=35, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467365711846L, targetTimeMillis - differenceTimeMillis);
            // Second reschedule
            nowTimeMillis = targetTimeMillis;
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=11, minute=2, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467370931607L, targetTimeMillis - differenceTimeMillis);
            // Third reschedule
            nowTimeMillis = targetTimeMillis;
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=11, minute=47, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467373635101L, targetTimeMillis - differenceTimeMillis);
            // Fourth reschedule
            nowTimeMillis = targetTimeMillis;
            targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
            Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals("TimeOfDay [hour=13, minute=13, weekday=6]", (new TimeOfDay(targetTimeMillis)).toString());
            assertEquals(1467378815198L, targetTimeMillis - differenceTimeMillis);
        } finally {
            SchedulerLogic.random = previousRandom; // restore unseeded generator
        }
    }

    public void testRescheduleYieldsDayFriday1() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayFriday2() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayFriday3() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 00, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayFriday4() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayFriday5() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayFriday6() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.FRIDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.FRIDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayMonday1() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayMonday2() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayMonday3() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 00, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.TUESDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayMonday4() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.TUESDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayMonday5() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDayMonday6() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.MONDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySaturday1() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySaturday2() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySaturday3() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 00, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySaturday4() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySaturday5() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySaturday6() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.SATURDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySunday1() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySunday2() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySunday3() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(23, 00, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySunday4() {
        PrefsAccessor prefs = getDayPrefs();
        long nowTimeMillis = getTimeMillis(20, 59, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySunday5() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(01, 00, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

    public void testRescheduleYieldsDaySunday6() {
        PrefsAccessor prefs = getNightPrefs();
        long nowTimeMillis = getTimeMillis(12, 00, Calendar.SUNDAY);
        long targetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs);
        Log.d(TAG, (new TimeOfDay(nowTimeMillis)).toString() + " -> " + (new TimeOfDay(targetTimeMillis)).toString());
        assertTrue(targetTimeMillis > nowTimeMillis);
        assertEquals(Calendar.MONDAY, getWeekday(targetTimeMillis));
        assertTrue(prefs.isDaytime(new TimeOfDay(targetTimeMillis)));
    }

}
