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
package com.googlecode.mindbell.mission.model

import android.net.Uri
import com.googlecode.mindbell.mission.InterruptSettings
import com.googlecode.mindbell.mission.model.NoActionsReason.NONE
import com.googlecode.mindbell.util.TimeOfDay
import java.util.*

class Statistics {

    var entryList: MutableList<StatisticsEntry> = ArrayList()

    private class NoInterruptSettings : InterruptSettings {

        override val isShow: Boolean
            get() = false

        override val isSound: Boolean
            get() = false

        override val isVibrate: Boolean
            get() = false

        override val volume: Float
            get() = 0.0F

        override val isNotification: Boolean
            get() = false

        override val isDismissNotification: Boolean
            get() = false

        override val soundUri: Uri?
            get() = null

    }

    abstract class StatisticsEntry {

        private val nowTimeMillis = Calendar.getInstance().timeInMillis

        override fun toString(): String {
            val now = TimeOfDay(nowTimeMillis).logString
            return "$now ${type()}: "
        }

        abstract fun type(): String

    }

    abstract class ActionsStatisticsEntry(interruptSettings: InterruptSettings) :
            StatisticsEntry() {

        private val isShow = interruptSettings.isShow

        private val isSound = interruptSettings.isSound

        private val isVibrate = interruptSettings.isVibrate

        private val soundUriString = interruptSettings.soundUri?.toString()

        override fun toString(): String {
            val timeAndType = super.toString()
            val show = if (isShow) "show" else "no show"
            val sound = if (isSound) "sound" else "no sound"
            val vibrate = if (isVibrate) "vibrate" else "no vibration"
            return "$timeAndType$show, $sound, $vibrate, uri='$soundUriString'"
        }

    }

    class MeditationBeginningActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val
    numberOfPeriods: Int = 0) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun toString(): String {
            val timeAndType = super.toString()
            return "$timeAndType, $numberOfPeriods periods"
        }

        override fun type(): String {
            return "Meditation Beginning"
        }

    }

    class MeditationInterruptingActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val meditationPeriod: Int, private val
    numberOfPeriods: Int = 0) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun toString(): String {
            val timeAndType = super.toString()
            return "$timeAndType, period $meditationPeriod/$numberOfPeriods"
        }

        override fun type(): String {
            return "Meditation Interrupting"
        }

    }

    class MeditationEndingActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val
    isStopAutomatically: Boolean = false) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun toString(): String {
            val timeAndType = super.toString()
            val stopAutomatically = if (isStopAutomatically) "stop automatically" else "don't stop automatically"
            return "$timeAndType$stopAutomatically"
        }

        override fun type(): String {
            return "Meditation Ending"
        }

    }

    class ReminderActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun type(): String {
            return "Reminder"
        }

    }

    class RingOnceActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun type(): String {
            return "Ring Once"
        }

    }

    class ActionsFinishedStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun type(): String {
            return "Finished"
        }

    }

    class NoActionsStatisticsEntry(private val reason: NoActionsReason = NONE) : StatisticsEntry() {

        override fun toString(): String {
            val timeAndType = super.toString()
            return "$timeAndType${reason.name}"
        }

        override fun type(): String {
            return "No Actions"
        }

    }

    class ReschedulingStatisticsEntry(private val nextTargetTimeMillis: Long = 0L, private val nextMeditationPeriod: Int? = null) :
            StatisticsEntry() {

        override fun toString(): String {
            val timeAndType = super.toString()
            val targetTime = TimeOfDay(nextTargetTimeMillis).logString
            val intention = if (nextMeditationPeriod == null) "reminder" else "meditation period $nextMeditationPeriod"
            return "${timeAndType}for $intention at $targetTime"
        }

        override fun type(): String {
            return "Rescheduling"
        }

    }

    companion object {

        private val NO_INTERRUPT_SETTING = NoInterruptSettings()

    }

}