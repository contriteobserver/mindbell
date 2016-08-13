/*******************************************************************************
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
 *******************************************************************************/
package com.googlecode.mindbell;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

import java.util.Calendar;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.logic.RingingLogic;
import com.googlecode.mindbell.logic.SchedulerLogic;
import com.googlecode.mindbell.util.AlarmManagerCompat;
import com.googlecode.mindbell.util.TimeOfDay;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Ring the bell and reschedule.
 */
public class Scheduler extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "Scheduler received intent");

        AlarmManagerCompat alarmManager = new AlarmManagerCompat(context);
        ContextAccessor contextAccessor = AndroidContextAccessor.getInstance(context);
        PrefsAccessor prefs = contextAccessor.getPrefs();

        // Update notification, just in case MindBell wasn't informed about any muting
        contextAccessor.updateStatusNotification();

        if (!prefs.isActive()) {
            Log.d(TAG, "Bell is not active -- not ringing, not rescheduling.");
            return;
        }

        // fetch intent argument ids
        final String extraNextTargetTimeMillis = context.getText(R.string.extraNextTargetTimeMillis).toString();
        final String extraIsRescheduling = context.getText(R.string.extraIsRescheduling).toString();

        // reschedule (to enable constant precise ringing, the current time is assumed to be the target time of the last intent)
        long nowMillis = intent.getLongExtra(extraNextTargetTimeMillis, Calendar.getInstance().getTimeInMillis());
        long nextTargetTimeMillis = SchedulerLogic.getNextTargetTimeMillis(nowMillis, prefs);
        Intent nextIntent = new Intent(context, Scheduler.class);
        nextIntent.putExtra(extraIsRescheduling, true);
        nextIntent.putExtra(extraNextTargetTimeMillis, nextTargetTimeMillis);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTargetTimeMillis, sender);
        TimeOfDay nextBellTime = new TimeOfDay(nextTargetTimeMillis);
        Log.d(TAG, "Scheduled next bell alarm for " + nextBellTime.getDisplayString());

        if (!intent.getBooleanExtra(extraIsRescheduling, false)) {
            Log.d(TAG, "Not ringing, has been called by preferences, activate bell button or when boot completed");
            return;
        }

        // ring if daytime
        if (!(new TimeOfDay()).isDaytime(prefs)) {
            Log.d(TAG, "Not ringing, it is night time");
            return;
        }

        if (prefs.isShow()) {
            Log.d(TAG, "Show bell, then play sound and vibrate if requested");

            Intent ringBell = new Intent(context, MindBell.class);
            PendingIntent bellIntent = PendingIntent.getActivity(context, -1, ringBell, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                bellIntent.send(); // show MindBell activity and call RingingLogic.ringBellAndWait()
            } catch (CanceledException e) {
                Log.d(TAG, "Cannot show bell, play sound and vibrate: " + e.getMessage(), e);
            }

        } else { // ring audio-only immediately:
            Log.d(TAG, "Play sound and vibrate if requested but do not show bell");
            RingingLogic.ringBellAndWait(contextAccessor);
        }

    }

}
