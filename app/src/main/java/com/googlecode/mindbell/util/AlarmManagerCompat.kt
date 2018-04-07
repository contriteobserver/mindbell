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
package com.googlecode.mindbell.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

/**
 * Hide Android API level differences from application code. See these links for more information:
 *
 * https://developer.android.com/reference/android/app/AlarmManager.html
 * https://lab.getbase.com/androids-new-doze-and-app-standby/
 */
class AlarmManagerCompat private constructor(context: Context) {

    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * For API level prior to 19 [AlarmManager.set] means to be exact and allowed while idle. Hence
     * MindBell should behave the same way on all API levels. Battery consumption should not be higher than before. And it's the
     * basic idea of MindBell to get interrupted.
     */
    fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(type, triggerAtMillis, operation)
        } else {
            alarmManager.set(type, triggerAtMillis, operation)
        }
    }

    /**
     * Delegate for [AlarmManager.cancel]
     */
    fun cancel(operation: PendingIntent) {
        alarmManager.cancel(operation)
    }

    companion object {

        fun getInstance(context: Context): AlarmManagerCompat {
            return AlarmManagerCompat(context.applicationContext)
        }

    }

}
