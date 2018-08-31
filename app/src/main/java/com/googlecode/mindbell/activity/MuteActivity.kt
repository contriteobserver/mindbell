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
package com.googlecode.mindbell.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.NumberPicker
import com.googlecode.mindbell.R
import com.googlecode.mindbell.mission.Notifier
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.mission.StatusDetector

/**
 * Activity to ask for the time period to mute the bell for.
 */
class MuteActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val numberPicker = NumberPicker(this)
        numberPicker.minValue = 0
        numberPicker.maxValue = 24
        numberPicker.value = deriveMuteForDefaultHoursDependingOnStatus(this)
        numberPicker.displayedValues = createDisplayedHourValues(numberPicker.maxValue)
        AlertDialog.Builder(this) //
                .setTitle(R.string.statusActionMuteFor) //
                .setView(numberPicker) //
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val muteForHours = numberPicker.value
                    val prefs = Prefs.getInstance(this)
                    if (muteForHours > 0) { // if not off remember choice for the next time
                        prefs.isMuteForDefaultHours = muteForHours
                    }
                    muteTill(this, muteForHours)
                    this@MuteActivity.finish()
                } //
                .setNegativeButton(android.R.string.cancel) { _, _ -> this@MuteActivity.finish() } //
                .show()
    }

    private fun createDisplayedHourValues(hours: Int): Array<String> {
        return Array(hours + 1) { i -> if (i == 0) getText((R.string.prefsMutedTillOff)).toString() else i.toString() + " h" }
    }

    companion object {

        fun deriveMuteForDefaultHoursDependingOnStatus(context: Context): Int {
            val prefs = Prefs.getInstance(context)
            val statusDetector = StatusDetector.getInstance(context)
            return if (statusDetector.isMutedTill) 0 else prefs.isMuteForDefaultHours
        }

        fun muteTill(context: Context, hours: Int) {
            val notifier = Notifier.getInstance(context)
            val prefs = Prefs.getInstance(context)
            val nextTargetTimeMillis = System.currentTimeMillis() + hours * 3600000L
            prefs.mutedTill = nextTargetTimeMillis
            notifier.updateStatusNotification()
            notifier.scheduleRefreshMutedTill(nextTargetTimeMillis)
        }

    }

}
