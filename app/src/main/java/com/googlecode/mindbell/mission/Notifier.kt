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
package com.googlecode.mindbell.mission

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import android.widget.Toast
import com.googlecode.mindbell.R
import com.googlecode.mindbell.mission.Prefs.Companion.INTERRUPT_NOTIFICATION_CHANNEL_ID
import com.googlecode.mindbell.mission.Prefs.Companion.KEEP_ALIVE_NOTIFICATION_CHANNEL_ID
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import com.googlecode.mindbell.mission.Prefs.Companion.UPDATE_STATUS_NOTIFICATION_DAY_NIGHT_REQUEST_CODE
import com.googlecode.mindbell.mission.Prefs.Companion.UPDATE_STATUS_NOTIFICATION_MUTED_TILL_REQUEST_CODE
import com.googlecode.mindbell.mission.Prefs.Companion.WEARABLE_INTERRUPT_NOTIFICATION_ID
import com.googlecode.mindbell.receiver.RefreshReceiver
import com.googlecode.mindbell.util.AlarmManagerCompat
import com.googlecode.mindbell.util.NotificationManagerCompatExtension
import com.googlecode.mindbell.util.TimeOfDay
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
     * Create the keep alive notification which is to be displayed on devices that need a foreground service to keep MindBell
     * alive because too aggressive battery optimization prohibits wakeup on alarms.
     */
    @SuppressLint("InlinedApi") // IMPORTANCE_LOW is ignored in compat class for API level < 26
    fun createKeepAliveNotification(): Notification {

        // Create notification channel
        NotificationManagerCompatExtension.getInstance(context).createNotificationChannel(KEEP_ALIVE_NOTIFICATION_CHANNEL_ID, //
                context.getText(R.string.keepAliveNotification).toString(), //
                context.getText(R.string.keepAliveDescription).toString(), //
                NotificationManager.IMPORTANCE_LOW) // no notification sound for API level >= 26

        // Now create the notification
        @Suppress("DEPRECATION") // getColor() deprecated now but not for older API levels < 23
        return NotificationCompat.Builder(context.applicationContext, KEEP_ALIVE_NOTIFICATION_CHANNEL_ID) //
                .setCategory(NotificationCompat.CATEGORY_ALARM) //
                .setAutoCancel(true) // cancel notification on touch
                .setColor(context.resources.getColor(R.color.backgroundColor)) //
                .setContentTitle(context.getText(R.string.keepAliveNotificationTitle)) //
                .setGroup(KEEP_ALIVE_NOTIFICATION_CHANNEL_ID) // group phone and wearable notification
                .setOngoing(true) // notifications bound to a foreground service are always ongoing
                .setSmallIcon(R.drawable.ic_keepalive) //
                .setSound(null) // no notification sound for API level < 26
                .setGroupSummary(true) // summarize group on phone not on wearable
                // .setVibrate() has no effect on the phone itself, only on a wearable
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE) //
                .build()
    }

    /**
     * Display reminder notification on wearables it the options says so.
     */
    fun showReminderNotificationOnWearables(interruptSettings: InterruptSettings? = null) {
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
                    .setVisibility(if (prefs.isNotificationVisibilityPublic) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
            if (interruptSettings.isVibrate) {
                wearableNotificationBuilder.setVibrate(prefs.vibrationPattern)
            }
            NotificationManagerCompat.from(context).notify(WEARABLE_INTERRUPT_NOTIFICATION_ID, wearableNotificationBuilder.build())
        }
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
        val intent = Intent(context, RefreshReceiver::class.java)
        intent.setAction("com.googlecode.mindbell.UPDATE_STATUS_NOTIFICATION") // just for documentation and logging
        return PendingIntent.getBroadcast(context, requestCode, intent,
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
