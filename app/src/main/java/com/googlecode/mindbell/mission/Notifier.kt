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
package com.googlecode.mindbell.mission

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log
import android.widget.Toast
import com.googlecode.mindbell.R
import com.googlecode.mindbell.activity.MainActivity
import com.googlecode.mindbell.activity.MuteActivity
import com.googlecode.mindbell.mission.Prefs.Companion.INTERRUPT_NOTIFICATION_CHANNEL_ID
import com.googlecode.mindbell.mission.Prefs.Companion.STATUS_NOTIFICATION_CHANNEL_ID
import com.googlecode.mindbell.mission.Prefs.Companion.STATUS_NOTIFICATION_ID
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import com.googlecode.mindbell.mission.Prefs.Companion.UPDATE_STATUS_NOTIFICATION_DAY_NIGHT_REQUEST_CODE
import com.googlecode.mindbell.mission.Prefs.Companion.UPDATE_STATUS_NOTIFICATION_MUTED_TILL_REQUEST_CODE
import com.googlecode.mindbell.mission.Prefs.Companion.UPDATE_STATUS_NOTIFICATION_REQUEST_CODE
import com.googlecode.mindbell.mission.Prefs.Companion.WEARABLE_INTERRUPT_NOTIFICATION_ID
import com.googlecode.mindbell.util.AlarmManagerCompat
import com.googlecode.mindbell.util.NotificationManagerCompatExtension
import com.googlecode.mindbell.util.TimeOfDay
import java.text.MessageFormat
import java.util.*

/**
 * This class delivers all messages and notifications of MindBell.
 */
class Notifier private constructor(val context: Context, val prefs: Prefs) {

    fun showMessage(message: String) {
        Log.d(TAG, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Create the reminder notification which is to be displayed when executing reminder actions. A corresponding notification
     * is displayed on wearables it the options says so.
     */
    @SuppressLint("InlinedApi") // IMPORTANCE_LOW is ignored in compat class for API level < 26
    fun createInterruptNotification(interruptSettings: InterruptSettings? = null): Notification {

        // Create notification channel
        NotificationManagerCompatExtension.getInstance(context).createNotificationChannel(INTERRUPT_NOTIFICATION_CHANNEL_ID, context
                .getText(R.string.prefsCategoryReminderNotification).toString(), context.getText(R.string.summaryNotificationOnWearables)
                .toString(), //
                NotificationManager.IMPORTANCE_LOW) // no notification sound for API level >= 26

        // Derive visibility
        val visibility = if (prefs.isNotificationVisibilityPublic)
            NotificationCompat.VISIBILITY_PUBLIC
        else
            NotificationCompat.VISIBILITY_PRIVATE

        // Now create the notification
        @Suppress("DEPRECATION") // getColor() deprecated now but not for older API levels < 23
        val phoneNotificationBuilder = NotificationCompat.Builder(context.applicationContext, INTERRUPT_NOTIFICATION_CHANNEL_ID) //
                .setCategory(NotificationCompat.CATEGORY_ALARM) //
                .setAutoCancel(true) // cancel notification on touch
                .setColor(context.resources.getColor(R.color.backgroundColor)) //
                .setContentTitle(prefs.notificationTitle) //
                .setContentText(prefs.notificationText) //
                .setGroup(INTERRUPT_NOTIFICATION_CHANNEL_ID) // group phone and wearable notification
                .setOngoing(false) // notifications bound to a foreground service are always ongoing
                .setSmallIcon(R.drawable.ic_stat_reminding) //
                .setSound(null) // no notification sound for API level < 26
                .setGroupSummary(true) // summarize group on phone not on wearable
                // .setVibrate() has no effect on the phone itself, only on a wearable
                .setVisibility(visibility)
        if (interruptSettings != null && interruptSettings.isNotificationOnWearables) {
            @Suppress("DEPRECATION") // getColor() deprecated now but not for older API levels < 23
            val wearableNotificationBuilder = NotificationCompat.Builder(context.applicationContext, INTERRUPT_NOTIFICATION_CHANNEL_ID) //
                    .setCategory(NotificationCompat.CATEGORY_ALARM) //
                    .setAutoCancel(true) // cancel notification on touch
                    .setColor(context.resources.getColor(R.color.backgroundColor)) //
                    .setContentTitle(prefs.notificationTitle) //
                    .setContentText(prefs.notificationText) //
                    .setGroup(INTERRUPT_NOTIFICATION_CHANNEL_ID) // group phone and wearable notification
                    .setOngoing(false) // ongoing notification are not displayed on wearables
                    .setSmallIcon(R.drawable.ic_stat_reminding) //
                    .setSound(null) // no notification sound for API level < 21, would play on default channel anyway
                    .setGroupSummary(false) // summarize group on phone not on wearable
                    .setVisibility(visibility)
            if (interruptSettings.isVibrate) {
                wearableNotificationBuilder.setVibrate(prefs.vibrationPattern)
            }
            NotificationManagerCompat.from(context).notify(WEARABLE_INTERRUPT_NOTIFICATION_ID, wearableNotificationBuilder.build())
        }
        return phoneNotificationBuilder.build()
    }

    /**
     * This is about updating the status notification on changes in system settings.
     */
    fun updateStatusNotification() {
        if (!prefs.isActive && !prefs.isMeditating || !prefs.isStatus) {// bell inactive or no notification wanted?
            Log.i(TAG, "Remove status notification because of inactive and non-meditating bell or unwanted status notification")
            removeStatusNotification()
            return
        }
        // Suppose bell is active and not muted and all settings can be satisfied
        var statusDrawable = R.drawable.ic_stat_active
        var contentTitle = context.getText(R.string.statusTitleBellActive)
        val contentText: String
        val statusDetector = StatusDetector.getInstance(context)
        val muteRequestReason = statusDetector.getMuteRequestReason(false)
        var targetClass: Class<*> = MainActivity::class.java
        // Override icon and notification text if bell is muted or permissions are insufficient
        if (prefs.isMeditating) {// Bell meditation => override icon and notification text
            statusDrawable = R.drawable.ic_stat_meditating
            contentTitle = context.getText(R.string.statusTitleBellMeditating)
            contentText = MessageFormat.format(context.getText(R.string.statusTextBellMeditating).toString(), //
                    prefs.meditationDuration.interval, //
                    TimeOfDay(prefs.meditationEndingTimeMillis).getDisplayString(context))
        } else if (muteRequestReason != null) { // Bell muted => override icon and notification text
            statusDrawable = muteRequestReason.muteReasonType.drawable
            contentText = muteRequestReason.message
        } else { // enrich standard notification by times and days
            contentText = MessageFormat.format(context.getText(R.string.statusTextBellActive).toString(), //
                    prefs.daytimeStart.getDisplayString(context), //
                    prefs.daytimeEnd.getDisplayString(context), //
                    prefs.activeOnDaysOfWeekString)
        }

        // Create notification channel
        NotificationManagerCompatExtension.getInstance(context).createNotificationChannel(STATUS_NOTIFICATION_CHANNEL_ID, context
                .getText(R.string.prefsCategoryStatusNotification).toString(), context.getText(R.string.summaryStatus).toString())

        // Now do the notification update
        Log.i(TAG, "Update status notification: $contentText")
        val openAppIntent = PendingIntent.getActivity(context, 0, Intent(context, targetClass), PendingIntent.FLAG_UPDATE_CURRENT)
        val muteIntent = PendingIntent.getActivity(context, 2, Intent(context, MuteActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val visibility = if (prefs.isStatusVisibilityPublic) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE
        @Suppress("DEPRECATION") // getColor() deprecated now but not for older API levels < 23
        val notificationBuilder = NotificationCompat.Builder(context.applicationContext, STATUS_NOTIFICATION_CHANNEL_ID) //
                .setCategory(NotificationCompat.CATEGORY_STATUS) //
                .setColor(context.resources.getColor(R.color.backgroundColor)) //
                .setContentTitle(contentTitle) //
                .setContentText(contentText) //
                .setContentIntent(openAppIntent) //
                .setOngoing(true) // ongoing is *not* shown on wearable
                .setSmallIcon(statusDrawable) //
                .setSound(null) //
                .setVisibility(visibility)
        if (!prefs.isMeditating) {
            // Do not allow other actions than stopping meditation while meditating
            notificationBuilder //
                    .addAction(R.drawable.ic_action_refresh_status, context.getText(R.string.statusActionRefreshStatus),
                            createRefreshBroadcastIntent(UPDATE_STATUS_NOTIFICATION_REQUEST_CODE)) //
                    .addAction(R.drawable.ic_action_mute_for, context.getText(R.string.statusActionMuteFor), muteIntent)
        }
        val notification = notificationBuilder.build()
        NotificationManagerCompat.from(context).notify(STATUS_NOTIFICATION_ID, notification)
    }

    private fun removeStatusNotification() {
        NotificationManagerCompat.from(context).cancel(STATUS_NOTIFICATION_ID)
    }

    /**
     * Schedule a refresh to update status notification in the future.
     */
    private fun scheduleRefresh(targetTimeMillis: Long, requestCode: Int, info: String) {
        val sender = createRefreshBroadcastIntent(requestCode)
        val alarmManager = AlarmManagerCompat.getInstance(context)
        alarmManager.cancel(sender) // cancel old alarm, it has either gone away or became obsolete
        if (prefs.isActive) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMillis, sender)
            val scheduledTime = TimeOfDay(targetTimeMillis)
            Log.d(TAG, "Update status notification scheduled for ${scheduledTime.logString} ($info)")
        }
    }

    /**
     * Schedule a refresh to update status notification for the end of a manual mute.
     */
    fun scheduleRefreshMutedTill(targetTimeMillis: Long) {
        scheduleRefresh(targetTimeMillis, UPDATE_STATUS_NOTIFICATION_MUTED_TILL_REQUEST_CODE, "muted till")
    }

    /**
     * Schedule a refresh to update status notification for the start or the end of the active period.
     */
    fun scheduleRefreshDayNight() {
        val targetTimeMillis = Scheduler.getNextDayNightChangeInMillis(Calendar.getInstance().timeInMillis, prefs)
        scheduleRefresh(targetTimeMillis, UPDATE_STATUS_NOTIFICATION_DAY_NIGHT_REQUEST_CODE, "day-night")
    }

    /**
     * Create an intent to be send to RefreshReceiver to update status notification.
     */
    private fun createRefreshBroadcastIntent(requestCode: Int): PendingIntent {
        return PendingIntent.getBroadcast(context, requestCode, Intent("com.googlecode.mindbell.UPDATE_STATUS_NOTIFICATION"),
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {

        /**
         * Returns an instance of this class.
         */
        fun getInstance(context: Context): Notifier {
            return Notifier(context.applicationContext, Prefs.getInstance(context))
        }

        /**
         * Returns an instance of this class.
         *
         * WARNING: Only to be used for unit tests. Initializing prefs with declaration lets unit test fail.
         */
        internal fun getInstance(context: Context, prefs: Prefs): Notifier {
            return Notifier(context.applicationContext, prefs)
        }

    }
}
