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
import com.googlecode.mindbell.Prefs.Companion.EXTRA_IS_RESCHEDULING
import com.googlecode.mindbell.Prefs.Companion.EXTRA_MEDITATION_PERIOD
import com.googlecode.mindbell.Prefs.Companion.EXTRA_NOW_TIME_MILLIS
import com.googlecode.mindbell.Prefs.Companion.INTERRUPT_NOTIFICATION_ID
import com.googlecode.mindbell.util.TimeOfDay
import java.util.*

/**
 * Delivers an interrupt to the user by executing interrupt actions (show/sound/vibrate) and rescheduling.
 */
class InterruptService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        // Fetch intent arguments
        val isRescheduling = intent.getBooleanExtra(EXTRA_IS_RESCHEDULING, false)
        val nowTimeMillis = intent.getLongExtra(EXTRA_NOW_TIME_MILLIS, Calendar.getInstance().timeInMillis)
        val meditationPeriod = intent.getIntExtra(EXTRA_MEDITATION_PERIOD, -1)

        ReminderShowActivity.logDebug("InterruptService received intent: isRescheduling=" + isRescheduling + ", nowTimeMillis=" + nowTimeMillis +
                ", meditationPeriod=" + meditationPeriod)

        // Create working environment
        val prefs = Prefs.getInstance(applicationContext)

        var pair: Pair<InterruptSettings?, Runnable?>

        // Evaluate next time to remind and reschedule or terminate method if neither active nor meditating
        if (prefs.isMeditating) { // Meditating overrides Active therefore check this first

            pair = handleMeditatingBell(nowTimeMillis, meditationPeriod)

        } else if (prefs.isActive) {

            pair = handleActiveBell(nowTimeMillis, isRescheduling)

        } else {

            ReminderShowActivity.logDebug("Bell is neither meditating nor active -- not reminding, not rescheduling.")
            pair = Pair(null, null)

        }

        val notifier = Notifier.getInstance(applicationContext)
        startForeground(INTERRUPT_NOTIFICATION_ID, notifier.createInterruptNotification(pair.first))

        // Update notification just in case state has changed or MindBell missed a muting
        notifier.updateStatusNotification()

        if (pair.first == null) {
            stopSelf()
        } else {
            val actionsExecutor = ActionsExecutor.getInstance(applicationContext)
            actionsExecutor.startInterruptActions(pair.first!!, pair.second, this)
        }

        return START_STICKY
    }

    /**
     * Reschedules next alarm and rings differently depending on the currently started (!) meditation period.
     */
    private fun handleMeditatingBell(nowTimeMillis: Long, meditationPeriod: Int):
            Pair<InterruptSettings?, Runnable?> {

        val scheduler = Scheduler.getInstance(this)
        val prefs = Prefs.getInstance(this)

        val numberOfPeriods = prefs.numberOfPeriods

        if (meditationPeriod == 0) { // beginning of ramp-up period?

            val nextTargetTimeMillis = nowTimeMillis + prefs.rampUpTimeMillis
            scheduler.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Pair(null, null)

        } else if (meditationPeriod == 1) { // beginning of meditation period 1

            val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
            scheduler.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Pair(prefs.forMeditationBeginning(), null)

        } else if (meditationPeriod <= numberOfPeriods) { // beginning of meditation period 2..n

            val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
            scheduler.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Pair(prefs.forMeditationInterrupting(), null)

        } else { // end of last meditation period

            val meditationStopper: Runnable?
            if (prefs.isStopMeditationAutomatically) {
                val actionsExecutor = ActionsExecutor.getInstance(applicationContext)
                meditationStopper = Runnable { actionsExecutor.stopMeditation() }
                ReminderShowActivity.logDebug("Meditation is over -- not rescheduling -- automatically stopping meditation mode.")
            } else {
                meditationStopper = null
                ReminderShowActivity.logDebug("Meditation is over -- not rescheduling -- meditation mode remains to be active.")
            }
            return Pair(prefs.forMeditationEnding(), meditationStopper)

        }
    }

    /**
     * Reschedules next alarm, shows bell, plays bell sound and vibrates - whatever is requested.
     */
    private fun handleActiveBell(nowTimeMillis: Long, isRescheduling: Boolean):
            Pair<InterruptSettings?, Runnable?> {

        val scheduler = Scheduler.getInstance(this)
        val statusDetector = StatusDetector.getInstance(this)
        val prefs = Prefs.getInstance(this)

        val nextTargetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        scheduler.reschedule(nextTargetTimeMillis, null)

        if (!isRescheduling) {

            ReminderShowActivity.logDebug("Not reminding (show/sound/vibrate), has been called by activate bell button or preferences or when boot " + "completed or after updating")
            return Pair(null, null)

        } else if (!TimeOfDay().isDaytime(prefs)) {

            ReminderShowActivity.logDebug("Not reminding (show/sound/vibrate), it is night time")
            return Pair(null, null)

        } else if (statusDetector.isMuteRequested(true)) {

            ReminderShowActivity.logDebug("Not reminding (show/sound/vibrate), bell is muted")
            return Pair(null, null)

        } else {

            ReminderShowActivity.logDebug("Start reminder actions (show/sound/vibrate")
            return Pair(prefs.forRegularOperation(), null)
        }
    }

}
