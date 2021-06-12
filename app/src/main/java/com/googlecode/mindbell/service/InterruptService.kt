/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2020 Uwe Damken
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
import com.googlecode.mindbell.mission.Prefs.Companion.KEEP_ALIVE_NOTIFICATION_ID
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

        val notifier = Notifier.getInstance(applicationContext)

        // Ensure this service may do its work (binding it to a foreground notification let's survive longer executions, too)
        if (!isRescheduling) {
            startForeground(KEEP_ALIVE_NOTIFICATION_ID, notifier.createKeepAliveNotification())
        }

        // Create working environment
        val prefs = Prefs.getInstance(applicationContext)

        // Evaluate next time to remind and reschedule or terminate method if neither active nor meditating
        val handlerResult: HandlerResult = when {

            prefs.isMeditating -> handleMeditatingBell(nowTimeMillis, meditationPeriod) // meditation has higher prio than active

            prefs.isActive -> handleActiveBell(nowTimeMillis, isRescheduling)

            else -> HandlerResult(null, null, NoActionsStatisticsEntry(INACTIVE), null)
        }

        notifier.showReminderNotificationOnWearables(handlerResult.interruptSettings)

        prefs.addStatisticsEntry(handlerResult.statisticsEntry)

        if (handlerResult.reschedule != null) {
            val scheduler = Scheduler.getInstance(this)
            scheduler.reschedule(handlerResult.reschedule.nextTargetTimeMillis, handlerResult.reschedule.nextMeditationPeriod)
        }

        if (handlerResult.interruptSettings == null) {
            prefs.addStatisticsEntry(FinishedStatisticsEntry())
        } else {
            val actionsExecutor = ActionsExecutor.getInstance(applicationContext)
            actionsExecutor.startInterruptActions(handlerResult.interruptSettings, handlerResult.meditationStopper)
        }

        return START_STICKY
    }

    /**
     * Reschedules next alarm and rings differently depending on the currently started (!) meditation period.
     */
    private fun handleMeditatingBell(nowTimeMillis: Long, meditationPeriod: Int):
            HandlerResult {

        val prefs = Prefs.getInstance(this)

        val numberOfPeriods = prefs.numberOfPeriods

        when {
            meditationPeriod == 0 -> { // beginning of ramp-up period?

                val nextTargetTimeMillis = nowTimeMillis + prefs.rampUpTimeMillis
                val reschedule = Reschedule(nextTargetTimeMillis, meditationPeriod + 1)
                return HandlerResult(null, null, NoActionsStatisticsEntry(MEDITATION_RAMP_UP), reschedule)

            }
            meditationPeriod == 1 -> { // beginning of meditation period 1

                val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
                val reschedule = Reschedule(nextTargetTimeMillis, meditationPeriod + 1)
                val interruptSettings = prefs.forMeditationBeginning()
                val statisticsEntry = MeditationBeginningActionsStatisticsEntry(interruptSettings, numberOfPeriods)
                return HandlerResult(interruptSettings, null, statisticsEntry, reschedule)

            }
            meditationPeriod <= numberOfPeriods -> { // beginning of meditation period 2..n

                val nextTargetTimeMillis = nowTimeMillis + prefs.getMeditationPeriodMillis(meditationPeriod)
                val reschedule = Reschedule(nextTargetTimeMillis, meditationPeriod + 1)
                val interruptSettings = prefs.forMeditationInterrupting()
                val statisticsEntry = MeditationInterruptingActionsStatisticsEntry(interruptSettings, meditationPeriod, numberOfPeriods)
                return HandlerResult(interruptSettings, null, statisticsEntry, reschedule)

            }
            else -> { // end of last meditation period

                val meditationStopper: Runnable? = if (prefs.isStopMeditationAutomatically) {
                    val actionsExecutor = ActionsExecutor.getInstance(applicationContext)
                    Runnable { actionsExecutor.stopMeditation() }
                } else {
                    null
                }
                val interruptSettings = prefs.forMeditationEnding()
                val statisticsEntry = MeditationEndingActionsStatisticsEntry(interruptSettings, meditationStopper != null)
                return HandlerResult(interruptSettings, meditationStopper, statisticsEntry, null)

            }
        }
    }

    /**
     * Reschedules next alarm, shows bell, plays bell sound and vibrates - whatever is requested.
     */
    private fun handleActiveBell(nowTimeMillis: Long, isRescheduling: Boolean):
            HandlerResult {

        val statusDetector = StatusDetector.getInstance(this)
        val prefs = Prefs.getInstance(this)

        val interruptSettings = prefs.forRegularOperation()
        val nextTargetTimeMillis = Scheduler.getNextTargetTimeMillis(nowTimeMillis, prefs)
        val reschedule = Reschedule(nextTargetTimeMillis, null)

        if (!isRescheduling) {

            return HandlerResult(null, null, NoActionsStatisticsEntry(BUTTON_OR_PREFS_OR_REBOOT_OR_UPDATE), reschedule)

        } else if (!TimeOfDay().isDaytime(prefs)) {

            return HandlerResult(null, null, SuppressedActionsStatisticsEntry(interruptSettings, NIGHT_TIME), reschedule)

        } else if (statusDetector.isMuteRequested(true)) {

            // TODO Add more precise reason
            return HandlerResult(null, null, SuppressedActionsStatisticsEntry(interruptSettings, MUTED), reschedule)

        } else {

            return HandlerResult(interruptSettings, null, ReminderActionsStatisticsEntry(interruptSettings), reschedule)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "InterruptService is being destroyed")
        super.onDestroy()
    }

    class HandlerResult(val interruptSettings: InterruptSettings?, val meditationStopper: Runnable?, val statisticsEntry:
    StatisticsEntry, val reschedule: Reschedule?)

    class Reschedule(val nextTargetTimeMillis: Long, val nextMeditationPeriod: Int?)

}
