/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 * for remembering what really counts
 * <p>
 * Copyright (C) 2010-2014 Marc Schroeder
 * Copyright (C) 2014-2018 Uwe Damken
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.mindbell

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.googlecode.mindbell.accessors.ActivityPrefsAccessor
import com.googlecode.mindbell.accessors.ContextAccessor
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_IS_RESCHEDULING
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_MEDITATION_PERIOD
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_NOW_TIME_MILLIS
import com.googlecode.mindbell.logic.SchedulerLogic
import com.googlecode.mindbell.util.TimeOfDay
import java.util.*

/**
 * Remind (show/sound/vibrate) and reschedule.
 */
class SchedulerService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        // Fetch intent arguments
        val isRescheduling = intent.getBooleanExtra(EXTRA_IS_RESCHEDULING, false)
        val nowTimeMillis = intent.getLongExtra(EXTRA_NOW_TIME_MILLIS, Calendar.getInstance().timeInMillis)
        val meditationPeriod = intent.getIntExtra(EXTRA_MEDITATION_PERIOD, -1)

        MindBell.logDebug("SchedulerService received intent: isRescheduling=" + isRescheduling + ", nowTimeMillis=" + nowTimeMillis +
                ", meditationPeriod=" + meditationPeriod)

        // Create working environment
        val contextAccessor = ContextAccessor.getInstance(applicationContext)
        val prefs = contextAccessor.prefs

        var pair: Pair<ActivityPrefsAccessor?, Runnable?>

        // Evaluate next time to remind and reschedule or terminate method if neither active nor meditating
        if (prefs.isMeditating) { // Meditating overrides Active therefore check this first

            pair = handleMeditatingBell(contextAccessor, nowTimeMillis, meditationPeriod)

        } else if (prefs.isActive) {

            pair = handleActiveBell(contextAccessor, nowTimeMillis, isRescheduling)

        } else {

            MindBell.logDebug("Bell is neither meditating nor active -- not reminding, not rescheduling.")
            pair = Pair(null, null)

        }

        startForeground(ContextAccessor.REMINDER_NOTIFICATION_ID, contextAccessor.createReminderNotification(pair.first))

        // Update notification just in case state has changed or MindBell missed a muting
        contextAccessor.updateStatusNotification()

        if (pair.first == null) {
            stopSelf()
        } else {
            contextAccessor.startReminderActions(pair.first!!, pair.second, this)
        }

        return START_STICKY
    }

    /**
     * Reschedules next alarm and rings differently depending on the currently started (!) meditation period.
     */
    private fun handleMeditatingBell(contextAccessor: ContextAccessor, nowTimeMillis: Long, meditationPeriod: Int):
            Pair<ActivityPrefsAccessor?, Runnable?> {
        val prefs = contextAccessor.prefs

        val numberOfPeriods = prefs.numberOfPeriods

        if (meditationPeriod == 0) { // beginning of ramp-up period?

            val nextTargetTimeMillis = nowTimeMillis + prefs.rampUpTimeMillis
            contextAccessor.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Pair(null, null)

        } else if (meditationPeriod == 1) { // beginning of meditation period 1

            val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
            contextAccessor.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Pair(prefs.forMeditationBeginning(), null)

        } else if (meditationPeriod <= numberOfPeriods) { // beginning of meditation period 2..n

            val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
            contextAccessor.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Pair(prefs.forMeditationInterrupting(), null)

        } else { // end of last meditation period

            val meditationStopper: Runnable?
            if (prefs.isStopMeditationAutomatically) {
                meditationStopper = Runnable { contextAccessor.stopMeditation() }
                MindBell.logDebug("Meditation is over -- not rescheduling -- automatically stopping meditation mode.")
            } else {
                meditationStopper = null
                MindBell.logDebug("Meditation is over -- not rescheduling -- meditation mode remains to be active.")
            }
            return Pair(prefs.forMeditationEnding(), meditationStopper)

        }
    }

    /**
     * Reschedules next alarm, shows bell, plays bell sound and vibrates - whatever is requested.
     */
    private fun handleActiveBell(contextAccessor: ContextAccessor, nowTimeMillis: Long, isRescheduling: Boolean):
            Pair<ActivityPrefsAccessor?, Runnable?> {
        val prefs = contextAccessor.prefs

        val nextTargetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowTimeMillis, prefs)
        contextAccessor.reschedule(nextTargetTimeMillis, null)

        if (!isRescheduling) {

            MindBell.logDebug("Not reminding (show/sound/vibrate), has been called by activate bell button or preferences or when boot " + "completed or after updating")
            return Pair(null, null)

        } else if (!TimeOfDay().isDaytime(prefs)) {

            MindBell.logDebug("Not reminding (show/sound/vibrate), it is night time")
            return Pair(null, null)

        } else if (contextAccessor.isMuteRequested(true)) {

            MindBell.logDebug("Not reminding (show/sound/vibrate), bell is muted")
            return Pair(null, null)

        } else {

            MindBell.logDebug("Start reminder actions (show/sound/vibrate")
            return Pair(prefs.forRegularOperation(), null)
        }
    }

}
