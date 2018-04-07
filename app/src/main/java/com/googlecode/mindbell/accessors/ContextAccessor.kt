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
package com.googlecode.mindbell.accessors

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.support.v4.content.ContextCompat
import com.googlecode.mindbell.InterruptService
import com.googlecode.mindbell.Notifier
import com.googlecode.mindbell.ReminderShowActivity
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_IS_RESCHEDULING
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_MEDITATION_PERIOD
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_NOW_TIME_MILLIS
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.SCHEDULER_REQUEST_CODE
import com.googlecode.mindbell.util.AlarmManagerCompat
import com.googlecode.mindbell.util.TimeOfDay

class ContextAccessor private constructor(val context: Context) {

    private var notifier = Notifier.getInstance(context)

    private var prefs = PrefsAccessor.getInstance(context)

    /**
     * Send a newly created intent to InterruptService to update notification and setup a new bell schedule for reminder, if
     * requested cancel and newly setup alarms to update notification status depending on day-night mode.
     */
    fun updateBellScheduleForReminder(renewDayNightAlarm: Boolean) {
        ReminderShowActivity.logDebug("Update bell schedule for reminder requested, renewDayNightAlarm=" + renewDayNightAlarm)
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
        ReminderShowActivity.logDebug("Creating scheduler intent: isRescheduling=" + isRescheduling + ", nowTimeMillis=" + nowTimeMillis +
                ", meditationPeriod=" + meditationPeriod)
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
     */
    fun updateBellScheduleForMeditation() {
        ReminderShowActivity.logDebug("Update bell schedule for meditation requested")
        val intent = createSchedulerServiceIntent(false, null, null)
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
        ReminderShowActivity.logDebug("Scheduled next bell alarm for " + nextBellTime.logString)
    }

    companion object {

        // Keep MediaPlayer to finish a started sound explicitly, reclaimed when app gets destroyed: http://stackoverflow.com/a/2476171
        private var mediaPlayer: MediaPlayer? = null
        private var audioManager: AudioManager? = null

        /**
         * Returns an accessor for the given context, this call also validates the preferences.
         */
        fun getInstance(context: Context): ContextAccessor {
            return ContextAccessor(context.applicationContext)
        }

    }
}
