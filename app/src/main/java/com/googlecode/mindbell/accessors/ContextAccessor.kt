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

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.os.Build
import android.os.Vibrator
import android.provider.Settings
import android.provider.Settings.Global
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import android.widget.Toast
import com.googlecode.mindbell.*
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_IS_RESCHEDULING
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_MEDITATION_PERIOD
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_NOW_TIME_MILLIS
import com.googlecode.mindbell.logic.SchedulerLogic
import com.googlecode.mindbell.util.AlarmManagerCompat
import com.googlecode.mindbell.util.NotificationManagerCompatExtension
import com.googlecode.mindbell.util.TimeOfDay
import java.io.IOException
import java.text.MessageFormat
import java.util.*

class ContextAccessor : AudioManager.OnAudioFocusChangeListener {
    // ApplicationContext of MindBell
    private val context: Context
    // Accessor to all preferences
    var prefs: PrefsAccessor? = null
        protected set

    val reasonMutedTill: String
        get() {
            val mutedTill = TimeOfDay(prefs!!.mutedTill)
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
            return audioMan.getStreamVolume(prefs!!.audioStream) == 0
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
                    SchedulerLogic.getNextDaytimeStartInMillis(Calendar.getInstance().timeInMillis, prefs!!.daytimeStart,
                            prefs!!.activeOnDaysOfWeek))
            val weekdayAbbreviation = prefs!!.getWeekdayAbbreviation(nextStartTime.weekday!!)
            return MessageFormat.format(context.getText(R.string.reasonMutedDuringNighttime).toString(), weekdayAbbreviation,
                    nextStartTime.getDisplayString(context))
        }

    // if we hold a reference we haven't finished bell sound completely so only the reference is checked
    val isBellSoundPlaying: Boolean
        get() = mediaPlayer != null

    val alarmMaxVolume: Int
        get() {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioMan.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        }

    var alarmVolume: Int
        get() {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioMan.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        set(volume) {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioMan.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)

        }

    /**
     * Constructor is private just in case we want to make this a singleton.
     */
    private constructor(context: Context, logSettings: Boolean) {
        this.context = context.applicationContext
        this.prefs = PrefsAccessor(context, logSettings)
    }

    /**
     * Constructor is protected to allow for JUnit tests only.
     */
    constructor(context: Context, prefs: PrefsAccessor) {
        this.context = context
        this.prefs = prefs
    }

    /**
     * Return whether bell should be muted and show reason message if shouldShowMessage is true.
     */
    fun isMuteRequested(shouldShowMessage: Boolean): Boolean { // FIXME dkn Always called with true
        return getMuteRequestReason(shouldShowMessage) != null
    }

    /**
     * Check whether bell should be muted, show reason if requested, and return reason, null otherwise.
     */
    fun getMuteRequestReason(shouldShowMessage: Boolean): String? {
        var reason: String? = null
        if (System.currentTimeMillis() < prefs!!.mutedTill) { // Muted manually?
            reason = reasonMutedTill
        } else if (prefs!!.isMuteWithPhone && isPhoneMuted) { // Mute bell with phone?
            reason = reasonMutedWithPhone
        } else if (prefs!!.isMuteWithAudioStream && isAudioStreamMuted) { // Mute bell with audio stream?
            reason = reasonMutedWithAudioStream
        } else if (prefs!!.isMuteOffHook && isPhoneOffHook) { // Mute bell while phone is off hook (or ringing)?
            reason = reasonMutedOffHook
        } else if (prefs!!.isMuteInFlightMode && isPhoneInFlightMode) { // Mute bell while in flight mode?
            reason = reasonMutedInFlightMode
        } else if (!TimeOfDay().isDaytime(prefs!!)) { // Always mute bell during nighttime
            reason = reasonMutedDuringNighttime
        }
        if (reason != null && shouldShowMessage) {
            showMessage(reason)
        }
        return reason
    }

    fun showMessage(message: String) {
        MindBell.logDebug(message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Returns name of the airplane-mode-on constant depending on the version of Android.
     */
    private fun retrieveAirplaneModeOnConstantName(): String {
        return if (Build.VERSION.SDK_INT >= 17) {
            Global.AIRPLANE_MODE_ON
        } else {
            Settings.System.AIRPLANE_MODE_ON
        }
    }

    fun startPlayingSoundAndVibrate(activityPrefs: ActivityPrefsAccessor, runWhenDone: Runnable?) {

        // Stop an already ongoing sound, this isn't wrong when phone and bell are muted, too
        finishBellSound()

        // Update ring notification and vibrate on either phone or wearable
        if (activityPrefs.isNotification) {
            updateReminderNotification(activityPrefs)
        }

        // Raise alarm volume to the max but keep the original volume for reset by finishBellSound() and start playing sound if
        // requested by preferences
        var playingSoundStarted = false
        if (activityPrefs.isSound) {
            playingSoundStarted = startPlayingSound(activityPrefs, runWhenDone)
        }

        // Explicitly start vibration if not already done by ring notification
        if (activityPrefs.isVibrate && !activityPrefs.isNotification) {
            startVibration()
        }

        // If ring notification and its dismissal is requested, then we have to wait for a while to dismiss the ring notification
        // afterwards. So a new thread is created that waits and dismisses the ring notification afterwards.
        if (activityPrefs.isNotification && activityPrefs.isDismissNotification) {
            startWaiting(Runnable { cancelRingNotification(activityPrefs) })
        }

        // A non-null runWhenDone means there is something to do at the end (hiding the bell after displaying or stopping the
        // meditation automatically). This is typically done when finishing playing the sound. But if playing a sound has not
        // been started because of preferences or because sound has been suppressed then we after to do it now - or after a
        // little while if the bell will be displayed by MindBell.onStart() after leaving this method.
        if (!playingSoundStarted && runWhenDone != null) {
            if (activityPrefs.isShow) {
                startWaiting(runWhenDone)
            } else {
                runWhenDone.run()
            }
        }

    }

    fun finishBellSound() {
        if (isBellSoundPlaying) { // do we hold a reference to a MediaPlayer?
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
                MindBell.logDebug("Ongoing MediaPlayer stopped")
            }
            mediaPlayer!!.reset() // get rid of "mediaplayer went away with unhandled events" log entries
            mediaPlayer!!.release()
            mediaPlayer = null
            MindBell.logDebug("Reference to MediaPlayer released")
            if (prefs!!.isPauseAudioOnSound) {
                if (audioManager!!.abandonAudioFocus(this) == AUDIOFOCUS_REQUEST_FAILED) {
                    MindBell.logDebug("Abandon of audio focus failed")
                } else {
                    MindBell.logDebug("Audio focus successfully abandoned")
                }
            }
        }
        // Reset volume to originalVolume if it has been set before (does not equal -1)
        if (prefs!!.isUseAudioStreamVolumeSetting) { // we don't care about setting the volume
            MindBell.logDebug("Finish bell sound found without touching audio stream volume")
        } else {
            val originalVolume = prefs!!.originalVolume
            if (originalVolume < 0) {
                MindBell.logDebug("Finish bell sound found originalVolume $originalVolume, alarm volume left untouched")
            } else {
                val alarmMaxVolume = alarmMaxVolume
                if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
                    MindBell.logDebug(
                            "Finish bell sound found originalVolume $originalVolume to be max, alarm volume left untouched")
                } else {
                    MindBell.logDebug("Finish bell sound found originalVolume $originalVolume, setting alarm volume to it")
                    alarmVolume = originalVolume
                }
                prefs!!.resetOriginalVolume() // no longer needed therefore invalidate it
            }
        }
    }

    /**
     * This is about updating the reminder notification when executing reminder actions.
     */
    fun updateReminderNotification(activityPrefs: ActivityPrefsAccessor) {

        // Create notification channel
        NotificationManagerCompatExtension.from(context).createNotificationChannel(REMINDER_NOTIFICATION_CHANNEL_ID, context
                .getText(R.string.prefsCategoryRingNotification).toString(), context.getText(R.string.summaryNotification).toString())

        // Derive visibility
        val visibility = if (prefs!!.isNotificationVisibilityPublic)
            NotificationCompat.VISIBILITY_PUBLIC
        else
            NotificationCompat.VISIBILITY_PRIVATE

        // Now do the notification update
        val notificationBuilder = NotificationCompat.Builder(context.applicationContext) //
                .setCategory(NotificationCompat.CATEGORY_ALARM) //
                .setAutoCancel(true) // cancel notification on touch
                .setColor(context.resources.getColor(R.color.backgroundColor)) //
                .setContentTitle(prefs!!.notificationTitle) //
                .setContentText(prefs!!.notificationText).setSmallIcon(R.drawable.ic_stat_bell_ring) //
                .setVisibility(visibility)
        if (activityPrefs.isVibrate) {
            notificationBuilder.setVibrate(prefs!!.vibrationPattern)
        }
        val notification = notificationBuilder.build()
        NotificationManagerCompat.from(context).notify(RING_NOTIFICATION_ID, notification)
    }

    /**
     * Start playing bell sound and call runWhenDone when playing finishes but only if bell is not muted - returns true when
     * sound has been started, false otherwise.
     */
    private fun startPlayingSound(activityPrefs: ActivityPrefsAccessor, runWhenDone: Runnable?): Boolean {
        val bellUri = activityPrefs.getSoundUri(context)
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (prefs!!.isNoSoundOnMusic && audioManager!!.isMusicActive) {
            MindBell.logDebug("Sound suppressed because setting is no sound on music and music is playing")
            return false
        } else if (bellUri == null) {
            MindBell.logDebug("Sound suppressed because no sound has been set")
            return false
        } else if (prefs!!.isPauseAudioOnSound) {
            val requestResult = audioManager!!.requestAudioFocus(this, prefs!!.audioStream, retrieveDurationHint())
            if (requestResult == AUDIOFOCUS_REQUEST_FAILED) {
                MindBell.logDebug("Sound suppressed because setting is pause audio on sound and request of audio focus failed")
                return false
            }
            MindBell.logDebug("Audio focus successfully requested")
        }
        if (prefs!!.isUseAudioStreamVolumeSetting) { // we don't care about setting the volume
            MindBell.logDebug("Start playing sound without touching audio stream volume")
        } else {
            val originalVolume = alarmVolume
            val alarmMaxVolume = alarmMaxVolume
            if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
                MindBell.logDebug(
                        "Start playing sound found originalVolume $originalVolume to be max, alarm volume left untouched")
            } else {
                MindBell.logDebug(
                        "Start playing sound found and stored originalVolume $originalVolume, setting alarm volume to max")
                alarmVolume = alarmMaxVolume
                prefs!!.originalVolume = originalVolume
            }
        }
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioStreamType(prefs!!.audioStream)
        if (!prefs!!.isUseAudioStreamVolumeSetting) { // care about setting the volume
            val bellVolume = activityPrefs.volume
            mediaPlayer!!.setVolume(bellVolume, bellVolume)
        }
        try {
            try {
                mediaPlayer!!.setDataSource(context, bellUri)
            } catch (e: IOException) { // probably because of withdrawn permissions, hence use default bell
                mediaPlayer!!.setDataSource(context, prefs!!.getDefaultReminderBellSoundUri(context)!!)
            }

            mediaPlayer!!.prepare()
            mediaPlayer!!.setOnCompletionListener {
                finishBellSound()
                runWhenDone?.run()
            }
            mediaPlayer!!.start()
            return true
        } catch (e: IOException) {
            MindBell.logError("Could not start playing sound: " + e.message, e)
            finishBellSound()
            return false
        }

    }

    /**
     * Vibrate with the requested vibration pattern.
     */
    private fun startVibration() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(prefs!!.vibrationPattern, -1)
    }

    /**
     * Start waiting for a specific time period and call runWhenDone when time is over.
     */
    private fun startWaiting(runWhenDone: Runnable) {
        Thread(Runnable {
            try {
                Thread.sleep(PrefsAccessor.WAITING_TIME)
            } catch (e: InterruptedException) {
                // doesn't care if sleep was interrupted, just move on
            }

            runWhenDone.run()
        }).start()
    }

    /**
     * Cancel the ring notification (after ringing the bell).
     */
    fun cancelRingNotification(activityPrefs: ActivityPrefsAccessor) {
        NotificationManagerCompat.from(context).cancel(RING_NOTIFICATION_ID)
    }

    /**
     * Returns duration hint for requesting audio focus depending on the version of Android.
     */
    private fun retrieveDurationHint(): Int {
        return if (Build.VERSION.SDK_INT >= 19) {
            AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        } else {
            AUDIOFOCUS_GAIN_TRANSIENT
        }
    }

    /**
     * Send a newly created intent to Scheduler to update notification and setup a new bell schedule for reminder, if
     * requested cancel and newly setup alarms to update noticiation status depending on day-night mode.
     */
    fun updateBellScheduleForReminder(renewDayNightAlarm: Boolean) {
        MindBell.logDebug("Update bell schedule for reminder requested, renewDayNightAlarm=" + renewDayNightAlarm)
        if (renewDayNightAlarm) {
            scheduleUpdateStatusNotificationDayNight()
        }
        val sender = createSchedulerBroadcastIntent(false, null, null)
        try {
            sender.send()
        } catch (e: PendingIntent.CanceledException) {
            MindBell.logError("Could not update bell schedule for reminder: " + e.message, e)
        }

    }

    /**
     * Schedule an update status notification for the start or the end of the active period.
     */
    fun scheduleUpdateStatusNotificationDayNight() {
        val targetTimeMillis = SchedulerLogic.getNextDayNightChangeInMillis(Calendar.getInstance().timeInMillis, prefs!!)
        scheduleUpdateStatusNotification(targetTimeMillis, UPDATE_STATUS_NOTIFICATION_DAY_NIGHT_REQUEST_CODE, "day-night")
    }

    /**
     * Create an intent to be send to Scheduler to update notification and to (re-)schedule the bell.
     *
     * @param isRescheduling
     * True if the intents is meant for rescheduling instead of updating bell schedule.
     * @param nowTimeMillis
     * If not null millis to be given to Scheduler as now (or nextTargetTimeMillis from the perspective of the previous
     * call)
     * @param meditationPeriod
     * Zero: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    private fun createSchedulerBroadcastIntent(isRescheduling: Boolean, nowTimeMillis: Long?, meditationPeriod: Int?): PendingIntent {
        MindBell.logDebug("Creating scheduler intent: isRescheduling=" + isRescheduling + ", nowTimeMillis=" + nowTimeMillis +
                ", meditationPeriod=" + meditationPeriod)
        val intent = Intent(context, Scheduler::class.java)
        if (isRescheduling) {
            intent.putExtra(EXTRA_IS_RESCHEDULING, true)
        }
        if (nowTimeMillis != null) {
            intent.putExtra(EXTRA_NOW_TIME_MILLIS, nowTimeMillis)
        }
        if (meditationPeriod != null) {
            intent.putExtra(EXTRA_MEDITATION_PERIOD, meditationPeriod)
        }
        return PendingIntent.getBroadcast(context, SCHEDULER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Schedule an update status notification for the future.
     */
    private fun scheduleUpdateStatusNotification(targetTimeMillis: Long, requestCode: Int, info: String) {
        val sender = createRefreshBroadcastIntent(requestCode)
        val alarmManager = AlarmManagerCompat(context)
        alarmManager.cancel(sender) // cancel old alarm, it has either gone away or became obsolete
        if (prefs!!.isActive) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, sender)
            val scheduledTime = TimeOfDay(targetTimeMillis)
            MindBell.logDebug("Update status notification scheduled for " + scheduledTime.logString + " (" + info + ")")
        }
    }

    /**
     * Create an intent to be send to UpdateStatusNotification to update notification.
     */
    private fun createRefreshBroadcastIntent(requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(context, requestCode, Intent("com.googlecode.mindbell.UPDATE_STATUS_NOTIFICATION"),
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * Send a newly created intent to Scheduler to update notification and setup a new bell schedule for meditation.
     *
     * @param nextTargetTimeMillis
     * Millis to be given to Scheduler as now (or nextTargetTimeMillis from the perspective of the previous call)
     * @param meditationPeriod
     * Zero: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    fun updateBellScheduleForMeditation(nextTargetTimeMillis: Long, meditationPeriod: Int) {
        MindBell.logDebug("Update bell schedule for meditation requested, nextTargetTimeMillis=" + nextTargetTimeMillis)
        val sender = createSchedulerBroadcastIntent(false, nextTargetTimeMillis, meditationPeriod)
        try {
            sender.send()
        } catch (e: PendingIntent.CanceledException) {
            MindBell.logError("Could not update bell schedule for meditation: " + e.message, e)
        }

    }

    /**
     * Reschedule the bell by letting AlarmManager send an intent to Scheduler.
     *
     * @param nextTargetTimeMillis
     * Millis to be given to Scheduler as now (or nextTargetTimeMillis from the perspective of the previous call)
     * @param nextMeditationPeriod
     * null if not meditating, otherwise 0: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    fun reschedule(nextTargetTimeMillis: Long, nextMeditationPeriod: Int?) {
        val sender = createSchedulerBroadcastIntent(true, nextTargetTimeMillis, nextMeditationPeriod)
        val alarmManager = AlarmManagerCompat(context)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTargetTimeMillis, sender)
        val nextBellTime = TimeOfDay(nextTargetTimeMillis)
        MindBell.logDebug("Scheduled next bell alarm for " + nextBellTime.logString)
    }

    /**
     * Send an intent to MindBellMain to finally stop meditation (change status, stop countdown) automatically instead of
     * pressing the stop meditation button manually.
     */
    fun stopMeditation() {
        MindBell.logDebug("Starting activity MindBellMain to stop meditation")
        val intent = Intent(context, MindBellMain::class.java)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK // context may be service context only, not an activity context
                or Intent.FLAG_ACTIVITY_CLEAR_TASK) // MindBellMain becomes the new root to let back button return to other apps
        intent.putExtra(PrefsAccessor.EXTRA_STOP_MEDITATION, true)
        context.startActivity(intent)
    }

    /**
     * Shows bell by starting activity MindBell
     */
    fun showBell() {
        MindBell.logDebug("Starting activity MindBell to show bell")
        val intent = Intent(context, MindBell::class.java)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK // context may be service context only, not an activity context
                or Intent.FLAG_ACTIVITY_CLEAR_TASK) // MindBell becomes the new root to let back button return to other apps
        context.startActivity(intent)
    }

    /**
     * This is about updating the status notification on changes in system settings.
     */
    fun updateStatusNotification() {
        if (!prefs!!.isActive && !prefs!!.isMeditating || !prefs!!.isStatus) {// bell inactive or no notification wanted?
            MindBell.logInfo("Remove status notification because of inactive and non-meditating bell or unwanted status notification")
            removeStatusNotification()
            return
        }
        // Choose material design or pre material design status icons
        val bellActiveDrawable: Int
        val bellActiveButMutedDrawable: Int
        if (prefs!!.useStatusIconMaterialDesign()) {
            bellActiveDrawable = R.drawable.ic_stat_bell_active
            bellActiveButMutedDrawable = R.drawable.ic_stat_bell_active_but_muted
        } else {
            bellActiveDrawable = R.drawable.golden_bell_status_active
            bellActiveButMutedDrawable = R.drawable.golden_bell_status_active_but_muted
        }
        // Suppose bell is active and not muted and all settings can be satisfied
        var statusDrawable = bellActiveDrawable
        var contentTitle = context.getText(R.string.statusTitleBellActive)
        val contentText: String
        val muteRequestReason = getMuteRequestReason(false)
        var targetClass: Class<*> = MindBellMain::class.java
        // Override icon and notification text if bell is muted or permissions are insufficient
        if (!canSettingsBeSatisfied(prefs!!)) { // Insufficient permissions => override icon/text, switch notifications off
            statusDrawable = R.drawable.ic_warning_white_24dp
            contentTitle = context.getText(R.string.statusTitleNotificationsDisabled)
            contentText = context.getText(R.string.statusTextNotificationsDisabled).toString()
            targetClass = MindBellPreferences::class.java
            // Status Notification would not be correct during incoming or outgoing calls because of the missing permission to
            // listen to phone state changes. Therefore we switch off notification and ask user for permission when he tries
            // to enable notification again. In this very moment we cannot ask for permission to avoid an ANR in receiver
            // UpdateStatusNotification.
            prefs!!.isStatus = false
        } else if (prefs!!.isMeditating) {// Bell meditation => override icon and notification text
            statusDrawable = R.drawable.ic_stat_bell_meditating
            contentTitle = context.getText(R.string.statusTitleBellMeditating)
            contentText = MessageFormat.format(context.getText(R.string.statusTextBellMeditating).toString(), //
                    prefs!!.meditationDuration.interval, //
                    TimeOfDay(prefs!!.meditationEndingTimeMillis).getDisplayString(context))
        } else if (muteRequestReason != null) { // Bell muted => override icon and notification text
            statusDrawable = bellActiveButMutedDrawable
            contentText = muteRequestReason
        } else { // enrich standard notification by times and days
            contentText = MessageFormat.format(context.getText(R.string.statusTextBellActive).toString(), //
                    prefs!!.daytimeStart.getDisplayString(context), //
                    prefs!!.daytimeEnd.getDisplayString(context), //
                    prefs!!.activeOnDaysOfWeekString)
        }

        // Create notification channel
        NotificationManagerCompatExtension.from(context).createNotificationChannel(STATUS_NOTIFICATION_CHANNEL_ID, context
                .getText(R.string.prefsCategoryStatusNotification).toString(), context.getText(R.string.summaryStatus).toString())

        // Now do the notification update
        MindBell.logInfo("Update status notification: " + contentText)
        val openAppIntent = PendingIntent.getActivity(context, 0, Intent(context, targetClass), PendingIntent.FLAG_UPDATE_CURRENT)
        val muteIntent = PendingIntent.getActivity(context, 2, Intent(context, MuteActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val visibility = if (prefs!!.isStatusVisibilityPublic) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE
        val notificationBuilder = NotificationCompat.Builder(context.applicationContext, STATUS_NOTIFICATION_CHANNEL_ID) //
                .setCategory(NotificationCompat.CATEGORY_STATUS) //
                .setColor(context.resources.getColor(R.color.backgroundColor)) //
                .setContentTitle(contentTitle) //
                .setContentText(contentText) //
                .setContentIntent(openAppIntent) //
                .setOngoing(true) // ongoing is *not* shown on wearable
                .setSmallIcon(statusDrawable) //
                .setVisibility(visibility)
        if (!prefs!!.isMeditating) {
            // Do not allow other actions than stopping meditation while meditating
            notificationBuilder //
                    .addAction(R.drawable.ic_action_refresh_status, context.getText(R.string.statusActionRefreshStatus),
                            createRefreshBroadcastIntent(UPDATE_STATUS_NOTIFICATION_REQUEST_CODE)) //
                    .addAction(R.drawable.ic_stat_bell_active_but_muted, context.getText(R.string.statusActionMuteFor), muteIntent)
        }
        val notification = notificationBuilder.build()
        NotificationManagerCompat.from(context).notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun removeStatusNotification() {
        NotificationManagerCompat.from(context).cancel(STATUS_NOTIFICATION_ID)
    }

    /**
     * Returns true if mute bell with phone isn't requested or if the app has the permission to be informed in case of incoming or
     * outgoing calls. Notification bell could not be turned over correctly if muting with phone were requested without permission
     * granted.
     */
    private fun canSettingsBeSatisfied(prefs: PrefsAccessor): Boolean {
        val result = !prefs.isMuteOffHook || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        MindBell.logDebug("Can settings be satisfied? -> " + result)
        return result
    }

    /**
     * Schedule an update status notification for the end of a manual mute.
     */
    fun scheduleUpdateStatusNotificationMutedTill(targetTimeMillis: Long) {
        scheduleUpdateStatusNotification(targetTimeMillis, UPDATE_STATUS_NOTIFICATION_MUTED_TILL_REQUEST_CODE, "muted till")
    }

    override fun onAudioFocusChange(focusChange: Int) {
        MindBell.logDebug("Callback onAudioFocusChange() received focusChange=" + focusChange)
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT // could be handled by only pausing playback (not useful for bell sound)
                , AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK // could also be handled by lowering volume (not useful for bell sound)
            -> finishBellSound()
            else -> {
            }
        }
    }

    companion object {

        private val REMINDER_NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.reminder"

        private val STATUS_NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.status"

        private val STATUS_NOTIFICATION_ID = 0x7f030001 // historically, has been R.layout.bell for a long time

        private val RING_NOTIFICATION_ID = STATUS_NOTIFICATION_ID + 1

        private val SCHEDULER_REQUEST_CODE = 0

        private val UPDATE_STATUS_NOTIFICATION_REQUEST_CODE = 1

        private val UPDATE_STATUS_NOTIFICATION_MUTED_TILL_REQUEST_CODE = 2

        private val UPDATE_STATUS_NOTIFICATION_DAY_NIGHT_REQUEST_CODE = 4

        // Keep MediaPlayer to finish a started sound explicitly, reclaimed when app gets destroyed: http://stackoverflow.com/a/2476171
        private var mediaPlayer: MediaPlayer? = null
        private var audioManager: AudioManager? = null

        /**
         * Returns an accessor for the given context, this call also validates the preferences.
         */
        fun getInstance(context: Context): ContextAccessor {
            return ContextAccessor(context, false)
        }

        /**
         * Returns an accessor for the given context, this call also validates the preferences.
         */
        fun getInstanceAndLogPreferences(context: Context): ContextAccessor {
            return ContextAccessor(context, true)
        }
    }
}
