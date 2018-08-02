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
import com.fasterxml.jackson.annotation.JsonIgnore
import com.googlecode.mindbell.mission.InterruptSettings
import com.googlecode.mindbell.mission.model.NoActionsReason.NONE
import com.googlecode.mindbell.util.TimeOfDay
import java.util.*

class Statistics {

    var entryList: MutableList<StatisticsEntry> = ArrayList()

    @JsonIgnore
    fun getPreparedEntryList(): List<StatisticsEntry> {
        prepareEntries()
        return entryList
    }

    override fun toString(): String {
        prepareEntries()
        val sb = StringBuilder()
        sb.append("Statistics:")
        for (entry in entryList) {
            sb.append("\n  ${entry.toString()}")
        }
        return sb.toString()
    }

    private fun prepareEntries() {
        for ((index, entry) in entryList.withIndex()) {
            if (!entry.isPrepared) with(entry) {
                now = TimeOfDay(nowTimeMillis).logString
                comment = deriveComment()
                if (entry is ScheduledActionsStatisticsEntry) {
                    // enrich comment with data from last rescheduling entry before this one
                    val originEntry = findOriginEntry(entryList.subList(0, index))
                    if (originEntry != null) {
                        val next = TimeOfDay(originEntry.nextTargetTimeMillis).logString
                        val delayTimeMillis = entry.nowTimeMillis - originEntry.nextTargetTimeMillis
                        judgment = if (delayTimeMillis < 5000L) Judgment.ON_TIME else Judgment.OVERDUE
                        comment = "$comment, scheduled at ${originEntry.now} for $next (+$delayTimeMillis ms)"
                    }
                }
                isPrepared = true
            }
        }
    }

    /**
     * Returns the last rescheduling statistics entry in the sublist.
     */
    private fun findOriginEntry(entrySubList: List<StatisticsEntry>): ReschedulingStatisticsEntry? {
        return entrySubList.reversed().find { entry -> entry is ReschedulingStatisticsEntry } as ReschedulingStatisticsEntry?
    }

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

    enum class Judgment {
        NOT_APPLICABLE, ON_TIME, OVERDUE
    }

    abstract class StatisticsEntry {

        val nowTimeMillis = Calendar.getInstance().timeInMillis

        @JsonIgnore
        var isPrepared = false

        @JsonIgnore
        var now = ""

        @JsonIgnore
        var comment = ""

        @JsonIgnore
        var judgment = Judgment.NOT_APPLICABLE

        override fun toString(): String {
            return "$now $comment $judgment"
        }

        open fun deriveComment(): String {
            return "${type()}"
        }

        abstract fun type(): String

    }

    class FinishedStatisticsEntry : StatisticsEntry() {

        override fun type(): String {
            return "Finished"
        }

    }

    class NoActionsStatisticsEntry(private val reason: NoActionsReason = NONE) : StatisticsEntry() {

        override fun deriveComment(): String {
            return "${super.deriveComment()} because of ${reason.name}"
        }

        override fun type(): String {
            return "No actions"
        }

    }

    class ReschedulingStatisticsEntry(val nextTargetTimeMillis: Long = 0L, private val nextMeditationPeriod: Int? = null) :
            StatisticsEntry() {

        override fun deriveComment(): String {
            val targetTime = TimeOfDay(nextTargetTimeMillis).logString
            val intention = if (nextMeditationPeriod == null) "reminder" else "meditation period $nextMeditationPeriod"
            return "${super.deriveComment()} for $intention at $targetTime"
        }

        override fun type(): String {
            return "Rescheduling"
        }

    }

    abstract class ActionsStatisticsEntry(interruptSettings: InterruptSettings) :
            StatisticsEntry() {

        private val isShow = interruptSettings.isShow

        private val isSound = interruptSettings.isSound

        private val isVibrate = interruptSettings.isVibrate

        private val soundUriString = interruptSettings.soundUri?.toString()

        override fun deriveComment(): String {
            val show = if (isShow) "show" else "no show"
            val sound = if (isSound) "sound" else "no sound"
            val vibrate = if (isVibrate) "vibrate" else "no vibration"
            return "${super.deriveComment()} with $show, $sound, $vibrate, uri='$soundUriString'"
        }

    }

    class RingOnceActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING) :
            ActionsStatisticsEntry(interruptSettings) {

        override fun type(): String {
            return "Ringing once"
        }

    }

    abstract class ScheduledActionsStatisticsEntry(interruptSettings: InterruptSettings) :
            ActionsStatisticsEntry(interruptSettings) {

    }

    class MeditationBeginningActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val
    numberOfPeriods: Int = 0) :
            ScheduledActionsStatisticsEntry(interruptSettings) {

        override fun deriveComment(): String {
            return "${super.deriveComment()}, $numberOfPeriods periods"
        }

        override fun type(): String {
            return "Beginning meditation"
        }

    }

    class MeditationInterruptingActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val meditationPeriod: Int, private val
    numberOfPeriods: Int = 0) :
            ScheduledActionsStatisticsEntry(interruptSettings) {

        override fun deriveComment(): String {
            return "${super.deriveComment()}, period $meditationPeriod/$numberOfPeriods"
        }

        override fun type(): String {
            return "Interrupting meditation"
        }

    }

    class MeditationEndingActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val
    isStopAutomatically: Boolean = false) :
            ScheduledActionsStatisticsEntry(interruptSettings) {

        override fun deriveComment(): String {
            val stopAutomatically = if (isStopAutomatically) "stop automatically" else "don't stop automatically"
            return "${super.deriveComment()}, $stopAutomatically"
        }

        override fun type(): String {
            return "Ending meditation"
        }

    }

    class ReminderActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING) :
            ScheduledActionsStatisticsEntry(interruptSettings) {

        override fun type(): String {
            return "Reminding"
        }

    }

    class SuppressedActionsStatisticsEntry(interruptSettings: InterruptSettings = NO_INTERRUPT_SETTING, private val reason: NoActionsReason = NONE) :
            ScheduledActionsStatisticsEntry(interruptSettings) {

        override fun deriveComment(): String {
            return "${super.deriveComment()}, ${reason.name}"
        }

        override fun type(): String {
            return "Actions suppressed"
        }

    }

    companion object {

        private val NO_INTERRUPT_SETTING = NoInterruptSettings()

    }

}