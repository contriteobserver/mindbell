/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2016 Uwe Damken
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
package com.googlecode.mindbell

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Global
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import com.googlecode.mindbell.accessors.PrefsAccessor
import com.googlecode.mindbell.logic.SchedulerLogic
import com.googlecode.mindbell.util.TimeOfDay
import java.text.MessageFormat
import java.util.*

/**
 * This class detects all statuses of MindBell itself and of the phone that are relevant for MindBell.
 */
class StatusDetector private constructor(val context: Context) {

    private var prefs = PrefsAccessor.getInstance(context)

    val reasonMutedTill: String
        get() {
            val mutedTill = TimeOfDay(prefs.mutedTill)
            return MessageFormat.format(context.getText(R.string.reasonMutedTill).toString(), mutedTill.getDisplayString(context))
        }

    val isPhoneMuted: Boolean
        get() {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioMan.getStreamVolume(AudioManager.STREAM_RING) == 0
        }

    val reasonMutedWithPhone: String
        get() = context.getText(R.string.reasonMutedWithPhone).toString()

    val isAudioStreamMuted: Boolean
        get() {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioMan.getStreamVolume(prefs.audioStream) == 0
        }

    val reasonMutedWithAudioStream: String
        get() = context.getText(R.string.reasonMutedWithAudioStream).toString()

    val isPhoneOffHook: Boolean
        get() {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            return telephonyManager.callState != TelephonyManager.CALL_STATE_IDLE
        }

    val reasonMutedOffHook: String
        get() = context.getText(R.string.reasonMutedOffHook).toString()

    val isPhoneInFlightMode: Boolean
        get() = Settings.System.getInt(context.contentResolver, retrieveAirplaneModeOnConstantName(), 0) == 1

    val reasonMutedInFlightMode: String
        get() = context.getText(R.string.reasonMutedInFlightMode).toString()

    val reasonMutedDuringNighttime: String
        get() {
            val nextStartTime = TimeOfDay(
                    SchedulerLogic.getNextDaytimeStartInMillis(Calendar.getInstance().timeInMillis, prefs.daytimeStart,
                            prefs.activeOnDaysOfWeek))
            val weekdayAbbreviation = prefs.getWeekdayAbbreviation(nextStartTime.weekday!!)
            return MessageFormat.format(context.getText(R.string.reasonMutedDuringNighttime).toString(), weekdayAbbreviation,
                    nextStartTime.getDisplayString(context))
        }

    /**
     * Return whether bell should be muted and show reason message if shouldShowMessage is true.
     */
    fun isMuteRequested(shouldShowMessage: Boolean): Boolean {
        return getMuteRequestReason(shouldShowMessage) != null
    }

    /**
     * Check whether bell should be muted, show reason if requested, and return reason, null otherwise.
     */
    fun getMuteRequestReason(shouldShowMessage: Boolean): String? {
        var reason: String? = null
        if (System.currentTimeMillis() < prefs.mutedTill) { // Muted manually?
            reason = reasonMutedTill
        } else if (prefs.isMuteWithPhone && isPhoneMuted) { // Mute bell with phone?
            reason = reasonMutedWithPhone
        } else if (prefs.isMuteWithAudioStream && isAudioStreamMuted) { // Mute bell with audio stream?
            reason = reasonMutedWithAudioStream
        } else if (prefs.isMuteOffHook && isPhoneOffHook) { // Mute bell while phone is off hook (or ringing)?
            reason = reasonMutedOffHook
        } else if (prefs.isMuteInFlightMode && isPhoneInFlightMode) { // Mute bell while in flight mode?
            reason = reasonMutedInFlightMode
        } else if (!TimeOfDay().isDaytime(prefs)) { // Always mute bell during nighttime
            reason = reasonMutedDuringNighttime
        }
        if (reason != null && shouldShowMessage) {
            val notifier = Notifier.getInstance(context)
            notifier.showMessage(reason)
        }
        return reason
    }

    /**
     * Returns name of the airplane-mode-on constant depending on the version of Android.
     */
    private fun retrieveAirplaneModeOnConstantName(): String {
        return if (Build.VERSION.SDK_INT >= 17) {
            Global.AIRPLANE_MODE_ON
        } else {
            @Suppress("DEPRECATION") // deprecated now but not for older API levels < 17
            Settings.System.AIRPLANE_MODE_ON
        }
    }

    /**
     * Returns true if mute bell with phone isn't requested or if the app has the permission to be informed in case of incoming or
     * outgoing calls. Notification bell could not be turned over correctly if muting with phone were requested without permission
     * granted.
     */
    fun canSettingsBeSatisfied(): Boolean {
        val result = !prefs.isMuteOffHook || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        ReminderShowActivity.logDebug("Can settings be satisfied? -> " + result)
        return result
    }

    companion object {

        /**
         * Returns an instance of this class.
         */
        fun getInstance(context: Context): StatusDetector {
            return StatusDetector(context.applicationContext)
        }

    }
}
