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

package com.googlecode.mindbell.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Hide Android API level differences from application code.
 *
 * However, with Kotlin I found no way to extend existing NotificationManagerCompat. (1) It cannot be inherited from as it is
 * final. (2) Kotlin's delegation feature is not usable because NotficationManagerCompat is no interface. (3) Classic delegation
 * would have required to *type* all delegate method because AndroidStudio is not able to generate them - probably because of (2)
 * or because one would not do that ;-).
 */
class NotificationManagerCompatExtension(val context: Context) {

    /**
     * Creates a notification channel with API level 26 or higher, does nothing otherwise.
     *
     * Following https://developer.android.com/training/notify-user/channels.html notification channel may IMHO be created
     * everytime when notifying: >> Creating an existing notification channel with its original values performs no operation, so
     * it's safe to call this code when starting an app. <<
     */
    fun createNotificationChannel(id: String, name: String, description: String, importance: Int = NotificationManager
            .IMPORTANCE_DEFAULT, lights: Boolean = false, lightColor: Int? = null, vibration: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 26) {
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(id, name, importance)
            mChannel.description = description
            mChannel.enableLights(lights)
            if (lightColor != null) {
                mChannel.lightColor = lightColor
            }
            mChannel.enableVibration(vibration)
            val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            mNotificationManager.createNotificationChannel(mChannel)
        }
    }


    companion object {

        fun from(context: Context): NotificationManagerCompatExtension {
            return NotificationManagerCompatExtension(context)
        }

    }

}