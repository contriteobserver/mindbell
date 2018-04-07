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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.util.Log
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_IS_RESCHEDULING
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_MEDITATION_PERIOD
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_NOW_TIME_MILLIS
import com.googlecode.mindbell.mission.Prefs.Companion.SCHEDULER_REQUEST_CODE
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import com.googlecode.mindbell.service.InterruptService
import com.googlecode.mindbell.util.AlarmManagerCompat
import com.googlecode.mindbell.util.TimeOfDay
import java.util.*

/**
 * This class manages the schedule of interrupt actions.
 */
class Scheduler private constructor(val context: Context) {

    private var notifier = Notifier.getInstance(context)

    /**
     * Send a newly created intent to InterruptService to update notification and setup a new bell schedule for reminder, if
     * requested cancel and newly setup alarms to update notification status depending on day-night mode.
     */
    fun updateBellScheduleForReminder(renewDayNightAlarm: Boolean) {
        Log.d(TAG, "Update bell schedule for reminder requested, renewDayNightAlarm=$renewDayNightAlarm")
        if (renewDayNightAlarm) {
            notifier.scheduleRefreshDayNight()
        }
        val intent = createSchedulerServiceIntent(false, null, null)
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Create a pending intent to be send to InterruptService to update notification and to (re-)schedule the bell.
     */
    private fun createSchedulerServicePendingIntent(isRescheduling: Boolean, nowTimeMillis: Long?, meditationPeriod: Int?):
            PendingIntent {
        val intent = createSchedulerServiceIntent(isRescheduling, nowTimeMillis, meditationPeriod)
        return PendingIntent.getService(context, SCHEDULER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Create an intent to be send to InterruptService to update notification and to (re-)schedule the bell.
     *
     * @param isRescheduling
     * True if the intents is meant for rescheduling instead of updating bell schedule.
     * @param nowTimeMillis
     * If not null millis to be given to InterruptService as now (or nextTargetTimeMillis from the perspective of the previous
     * call)
     * @param meditationPeriod
     * Zero: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    private fun createSchedulerServiceIntent(isRescheduling: Boolean, nowTimeMillis: Long?, meditationPeriod: Int?): Intent {
        Log.d(TAG, "Creating scheduler intent: isRescheduling=$isRescheduling, nowTimeMillis=$nowTimeMillis, meditationPeriod=$meditationPeriod")
        val intent = Intent(context, InterruptService::class.java)
        if (isRescheduling) {
            intent.putExtra(EXTRA_IS_RESCHEDULING, true)
        }
        if (nowTimeMillis != null) {
            intent.putExtra(EXTRA_NOW_TIME_MILLIS, nowTimeMillis)
        }
        if (meditationPeriod != null) {
            intent.putExtra(EXTRA_MEDITATION_PERIOD, meditationPeriod)
        }
        return intent
    }

    /**
     * Send a newly created intent to InterruptService to update notification and setup a new bell schedule for meditation.
     *
     * @param nextTargetTimeMillis
     * If not null millis to be given to InterruptService as now (or nextTargetTimeMillis from the perspective of the previous
     * call)
     * @param meditationPeriod
     * Zero: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    fun updateBellScheduleForMeditation(nextTargetTimeMillis: Long?, meditationPeriod: Int?) {
        Log.d(TAG, "Update bell schedule for meditation requested, nextTargetTimeMillis=$nextTargetTimeMillis")
        val intent = createSchedulerServiceIntent(false, nextTargetTimeMillis, meditationPeriod)
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Reschedule the bell by letting AlarmManager send an intent to InterruptService.
     *
     * @param nextTargetTimeMillis
     * Millis to be given to InterruptService as now (or nextTargetTimeMillis from the perspective of the previous call)
     * @param nextMeditationPeriod
     * null if not meditating, otherwise 0: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    fun reschedule(nextTargetTimeMillis: Long, nextMeditationPeriod: Int?) {
        val pendingIntent = createSchedulerServicePendingIntent(true, nextTargetTimeMillis, nextMeditationPeriod)
        val alarmManager = AlarmManagerCompat.getInstance(context)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTargetTimeMillis, pendingIntent)
        val nextBellTime = TimeOfDay(nextTargetTimeMillis)
        Log.d(TAG, "Scheduled next bell alarm for ${nextBellTime.logString}")
    }

    companion object {

        /**
         * Random for generation of randomized intervals
         */
        private val random = Random()

        /**
         * Returns an accessor for the given context, this call also validates the preferences.
         */
        fun getInstance(context: Context): Scheduler {
            return Scheduler(context.applicationContext)
        }

        // TODO Move methods out of companion object and use prefs as usual

        /**
         * Return next time to bell after the given "now".
         *
         * @param nowTimeMillis
         * @param prefs
         * @return
         */
        fun getNextTargetTimeMillis(nowTimeMillis: Long, prefs: Prefs): Long {
            val meanInterval = prefs.interval
            val isRandomize = prefs.isRandomize
            val isNormalize = prefs.isNormalize
            val randomizedInterval = if (isRandomize) getRandomInterval(meanInterval) else meanInterval
            var targetTimeMillis = nowTimeMillis + randomizedInterval
            val normalizeMillis = prefs.normalize * Prefs.ONE_MINUTE_MILLIS
            targetTimeMillis = normalize(targetTimeMillis, meanInterval, isNormalize, normalizeMillis)
            if (!TimeOfDay(targetTimeMillis).isDaytime(prefs)) { // inactive time?
                targetTimeMillis = (getNextDaytimeStartInMillis(targetTimeMillis, prefs.daytimeStart, prefs.activeOnDaysOfWeek)
                        // start of next day time millis
                        + (if (isRandomize) randomizedInterval - meanInterval / 2 else 0)
                        // if requested randomize but never before start of day
                        + if (isNormalize) normalizeMillis else 0) // if requested normalize to minute of first ring a day
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
        fun getNextDayNightChangeInMillis(referenceTimeMillis: Long, prefs: Prefs): Long {
            val nextStart = getNextCalendarAtTimeOfDay(referenceTimeMillis, prefs.daytimeStart).timeInMillis
            val nextEnd = getNextCalendarAtTimeOfDay(referenceTimeMillis, prefs.daytimeEnd).timeInMillis
            return Math.min(nextStart, nextEnd)
        }

    }
}
