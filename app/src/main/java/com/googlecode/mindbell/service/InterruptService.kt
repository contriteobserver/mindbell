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
package com.googlecode.mindbell.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.googlecode.mindbell.mission.*
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_IS_RESCHEDULING
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_MEDITATION_PERIOD
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_NOW_TIME_MILLIS
import com.googlecode.mindbell.mission.Prefs.Companion.INTERRUPT_NOTIFICATION_ID
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import com.googlecode.mindbell.mission.model.NoActionsReason.*
import com.googlecode.mindbell.mission.model.Statistics.*
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

        Log
                .d(TAG, "InterruptService received intent: isRescheduling=$isRescheduling, nowTimeMillis=$nowTimeMillis, meditationPeriod=$meditationPeriod")

        // Create working environment
        val prefs = Prefs.getInstance(applicationContext)

        val triple: Triple<InterruptSettings?, Runnable?, StatisticsEntry>

        // Evaluate next time to remind and reschedule or terminate method if neither active nor meditating
        triple = when {

        // Meditating overrides Active therefore check this first
            prefs.isMeditating -> handleMeditatingBell(nowTimeMillis, meditationPeriod)

            prefs.isActive -> handleActiveBell(nowTimeMillis, isRescheduling)

            else -> {
                Log.d(TAG, "Bell is neither meditating nor active -- not reminding, not rescheduling.")
                Triple(null, null, NoActionsStatisticsEntry(INACTIVE))
            }
        }

        val notifier = Notifier.getInstance(applicationContext)
        startForeground(INTERRUPT_NOTIFICATION_ID, notifier.createInterruptNotification(triple.first))

        // Update notification just in case state has changed or MindBell missed a muting
        notifier.updateStatusNotification()

        prefs.addStatisticsEntry(triple.third)

        if (triple.first == null) {
            stopSelf()
        } else {
            val actionsExecutor = ActionsExecutor.getInstance(applicationContext)
            actionsExecutor.startInterruptActions(triple.first!!, triple.second, this)
        }

        return START_STICKY
    }

    /**
     * Reschedules next alarm and rings differently depending on the currently started (!) meditation period.
     */
    private fun handleMeditatingBell(nowTimeMillis: Long, meditationPeriod: Int):
            Triple<InterruptSettings?, Runnable?, StatisticsEntry> {

        val scheduler = Scheduler.getInstance(this)
        val prefs = Prefs.getInstance(this)

        val numberOfPeriods = prefs.numberOfPeriods

        if (meditationPeriod == 0) { // beginning of ramp-up period?

            val nextTargetTimeMillis = nowTimeMillis + prefs.rampUpTimeMillis
            scheduler.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            return Triple(null, null, NoActionsStatisticsEntry(MEDITATION_RAMP_UP))

        } else if (meditationPeriod == 1) { // beginning of meditation period 1

            val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
            scheduler.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            val interruptSettings = prefs.forMeditationBeginning()
            val statisticsEntry = MeditationBeginningActionsStatisticsEntry(interruptSettings, numberOfPeriods)
            return Triple(interruptSettings, null, statisticsEntry)

        } else if (meditationPeriod <= numberOfPeriods) { // beginning of meditation period 2..n

            val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
            scheduler.reschedule(nextTargetTimeMillis, meditationPeriod + 1)
            val interruptSettings = prefs.forMeditationInterrupting()
            val statisticsEntry = MeditationInterruptingActionsStatisticsEntry(interruptSettings, meditationPeriod, numberOfPeriods)
            return Triple(interruptSettings, null, statisticsEntry)

        } else { // end of last meditation period

            val meditationStopper: Runnable?
            if (prefs.isStopMeditationAutomatically) {
                val actionsExecutor = ActionsExecutor.getInstance(applicationContext)
                meditationStopper = Runnable { actionsExecutor.stopMeditation() }
                Log.d(TAG, "Meditation is over -- not rescheduling -- automatically stopping meditation mode.")
            } else {
                meditationStopper = null
                Log.d(TAG, "Meditation is over -- not rescheduling -- meditation mode remains to be active.")
            }
            val interruptSettings = prefs.forMeditationEnding()
            val statisticsEntry = MeditationEndingActionsStatisticsEntry(interruptSettings, meditationStopper != null)
            return Triple(interruptSettings, meditationStopper, statisticsEntry)

        }
    }

    /**
     * Reschedules next alarm, shows bell, plays bell sound and vibrates - whatever is requested.
     */
    private fun handleActiveBell(nowTimeMillis: Long, isRescheduling: Boolean):
            Triple<InterruptSettings?, Runnable?, StatisticsEntry> {

        val scheduler = Scheduler.getInstance(this)
        val statusDetector = StatusDetector.getInstance(this)
        val prefs = Prefs.getInstance(this)

        val nextTargetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        scheduler.reschedule(nextTargetTimeMillis, null)

        if (!isRescheduling) {

            Log
                    .d(TAG, "Not reminding (show/sound/vibrate), has been called by activate bell button or preferences or when boot completed or after updating")
            return Triple(null, null, NoActionsStatisticsEntry(BUTTON_OR_PREFS_OR_REBOOT))

        } else if (!TimeOfDay().isDaytime(prefs)) {

            Log.d(TAG, "Not reminding (show/sound/vibrate), it is night time")
            return Triple(null, null, NoActionsStatisticsEntry(NIGHT_TIME))

        } else if (statusDetector.isMuteRequested(true)) {

            Log.d(TAG, "Not reminding (show/sound/vibrate), bell is muted")
            return Triple(null, null, NoActionsStatisticsEntry(MUTED)) // TODO Add more precise reason

        } else {

            Log.d(TAG, "Start reminder actions (show/sound/vibrate")
            val interruptSettings = prefs.forRegularOperation()
            return Triple(interruptSettings, null, ReminderActionsStatisticsEntry(interruptSettings))
        }
    }

}
