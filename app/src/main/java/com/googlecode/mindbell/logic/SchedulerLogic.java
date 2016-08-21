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

import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.util.TimeOfDay;

import java.util.Calendar;
import java.util.Random;
import java.util.Set;

public class SchedulerLogic {

    /** Random for generation of randomized intervals */
    private static Random random = new Random();

    /**
     * Return next time to bell after the given "now".
     *
     * @param nowTimeMillis
     * @param prefs
     * @return
     */
    public static long getNextTargetTimeMillis(long nowTimeMillis, PrefsAccessor prefs) {
        final long meanInterval = prefs.getInterval();
        final boolean randomize = prefs.isRandomize();
        final int normalizeValue = prefs.getNormalize();
        final boolean normalize = prefs.isNormalize(normalizeValue);
        long randomizedInterval = randomize ? getRandomInterval(meanInterval) : meanInterval;
        long targetTimeMillis = nowTimeMillis + randomizedInterval;
        long normalizeMillis = normalizeValue * 60000L;
        targetTimeMillis = normalize(targetTimeMillis, meanInterval, normalize, normalizeMillis);
        if (!(new TimeOfDay(targetTimeMillis)).isDaytime(prefs)) { // inactive time?
            targetTimeMillis = getNextDaytimeStartInMillis(targetTimeMillis, prefs.getDaytimeStart(), prefs.getActiveOnDaysOfWeek()) // start of next day time millis
                    + (randomize ? randomizedInterval - meanInterval / 2 : 0) // if requested randomize but never before start of day
                    + (normalize ? normalizeMillis : 0); // if requested normalize to minute of first ring a day
        }
        return targetTimeMillis;
    }

    /**
     * Compute a random value following a Gaussian distribution around the given mean. The value is guaranteed not to fall below
     * 0.5 * mean and not above 1.5 * mean.
     *
     * @param mean
     * @return
     */
    private static long getRandomInterval(long mean) {
        long value = (long) (mean * (1.0 + 0.3 * random.nextGaussian()));
        if (value < mean / 2) {
            value = mean / 2;
        }
        if (value > 3 * mean / 2) {
            value = 3 * mean / 2;
        }
        return value;
    }

    /**
     * If normalize is requested, return the given timeMillis normalized to full intervals from the first ring in an hour on the
     * minute firstRingMinutes, otherwise return the given timeMillis.
     *
     * @param timeMillis
     * @param interval
     * @param normalize
     * @param normalizeValue
     * @return
     */
    private static long normalize(long timeMillis, long interval, boolean normalize, long normalizeMillis) {
        if (!normalize) {
            return timeMillis;
        }
        long hourMillis = (timeMillis / 3600000L) * 3600000L; // milliseconds of all whole hours
        long minuteMillis = timeMillis - hourMillis; // milliseconds of remaining minutes
        minuteMillis = Math.round((minuteMillis - normalizeMillis) / interval) * interval + normalizeMillis;
        return hourMillis + minuteMillis;
    }

    /**
     * Return next time to bell at daytime after the given "now" which is lying in the nighttime.
     */
    public static long getNextDaytimeStartInMillis(long nightTimeMillis, TimeOfDay tStart, Set<Integer> activeOnDaysOfWeek) {
        Calendar morning = Calendar.getInstance();
        morning.setTimeInMillis(nightTimeMillis);
        morning.set(Calendar.HOUR_OF_DAY, tStart.hour);
        morning.set(Calendar.MINUTE, tStart.minute);
        morning.set(Calendar.SECOND, 0);
        morning.set(Calendar.MILLISECOND, 0);
        if (morning.getTimeInMillis() <= nightTimeMillis) { // today's start time has already passed
            morning.add(Calendar.DATE, 1); // therefore go to morning of next day
        }
        while (!(new TimeOfDay(morning)).isActiveOnThatDay(activeOnDaysOfWeek)) { // inactive on that day?
            morning.add(Calendar.DATE, 1); // therefore go to morning of next day
        }
        return morning.getTimeInMillis();
    }

}
