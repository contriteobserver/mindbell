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
import java.util.*

object SchedulerLogic {

    /**
     * Random for generation of randomized intervals
     */
    private val random = Random()

    /**
     * Return next time to bell after the given "now".
     *
     * @param nowTimeMillis
     * @param prefs
     * @return
     */
    fun getNextTargetTimeMillis(nowTimeMillis: Long, prefs: PrefsAccessor): Long {
        val meanInterval = prefs.interval
        val randomize = prefs.isRandomize
        val normalizeValue = prefs.normalize
        val normalize = PrefsAccessor.isNormalize(normalizeValue)
        val randomizedInterval = if (randomize) getRandomInterval(meanInterval) else meanInterval
        var targetTimeMillis = nowTimeMillis + randomizedInterval
        val normalizeMillis = normalizeValue * ONE_MINUTE_MILLIS
        targetTimeMillis = normalize(targetTimeMillis, meanInterval, normalize, normalizeMillis)
        if (!TimeOfDay(targetTimeMillis).isDaytime(prefs)) { // inactive time?
            targetTimeMillis = (getNextDaytimeStartInMillis(targetTimeMillis, prefs.daytimeStart, prefs.activeOnDaysOfWeek)
                    // start of next day time millis
                    + (if (randomize) randomizedInterval - meanInterval / 2 else 0)
                    // if requested randomize but never before start of day
                    + if (normalize) normalizeMillis else 0) // if requested normalize to minute of first ring a day
        }
        return targetTimeMillis
    }

    /**
     * Compute a random value following a Gaussian distribution around the given mean. The value is guaranteed not to fall below 0.5
     * * mean and not above 1.5 * mean.
     *
     * @param mean
     * @return
     */
    private fun getRandomInterval(mean: Long): Long {
        var value = (mean * (1.0 + 0.3 * random.nextGaussian())).toLong()
        if (value < mean / 2) {
            value = mean / 2
        }
        if (value > 3 * mean / 2) {
            value = 3 * mean / 2
        }
        return value
    }

    /**
     * If normalize is requested, return the given timeMillis normalized to full intervals from the first ring in an hour on the
     * minute firstRingMinutes, otherwise return the given timeMillis.
     *
     * @param timeMillis
     * @param interval
     * @param normalize
     * @param normalizeMillis
     * @return
     */
    private fun normalize(timeMillis: Long, interval: Long, normalize: Boolean, normalizeMillis: Long): Long {
        if (!normalize) {
            return timeMillis
        }
        val hourMillis = timeMillis / 3600000L * 3600000L // milliseconds of all whole hours
        var minuteMillis = timeMillis - hourMillis // milliseconds of remaining minutes
        minuteMillis = Math.round(((minuteMillis - normalizeMillis) / interval).toFloat()) * interval + normalizeMillis
        return hourMillis + minuteMillis
    }

    /**
     * Return time millis of next time when an active daytime start is gets reached after referenceTimeMillis.
     */
    fun getNextDaytimeStartInMillis(referenceTimeMillis: Long, start: TimeOfDay, activeOnDaysOfWeek: Set<Int>): Long {
        if (activeOnDaysOfWeek.isEmpty()) {
            throw IllegalArgumentException("Empty activeOnDaysOfWeek would result in an endless loop, prefs checks bypassed?")
        }
        val morning = getNextCalendarAtTimeOfDay(referenceTimeMillis, start)
        while (!TimeOfDay(morning).isActiveOnThatDay(activeOnDaysOfWeek)) { // inactive on that day?
            morning.add(Calendar.DATE, 1) // therefore go to morning of next day
        }
        return morning.timeInMillis
    }

    /**
     * Return the next instant (as Calendar) in the future with hour and minute of the given time.
     */
    private fun getNextCalendarAtTimeOfDay(referenceTimeMillis: Long, time: TimeOfDay): Calendar {
        val result = Calendar.getInstance()
        result.timeInMillis = referenceTimeMillis
        result.set(Calendar.HOUR_OF_DAY, time.hour)
        result.set(Calendar.MINUTE, time.minute)
        result.set(Calendar.SECOND, 0)
        result.set(Calendar.MILLISECOND, 0)
        if (result.timeInMillis <= referenceTimeMillis) { // time has already passed
            result.add(Calendar.DATE, 1) // therefore go to next day
        }
        return result
    }

    /**
     * Return next time of a potential change from daytime to nighttime or vice versa, ignoring whether days are active or not.
     * This can be either start or end time but not midnight because the weekday of the start time determines whether reminder is
     * active in the range.
     */
    fun getNextDayNightChangeInMillis(referenceTimeMillis: Long, prefs: PrefsAccessor): Long {
        val nextStart = getNextCalendarAtTimeOfDay(referenceTimeMillis, prefs.daytimeStart).timeInMillis
        val nextEnd = getNextCalendarAtTimeOfDay(referenceTimeMillis, prefs.daytimeEnd).timeInMillis
        return Math.min(nextStart, nextEnd)
    }

}
