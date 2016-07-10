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
package com.googlecode.mindbell.util;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.googlecode.mindbell.UpdateBellSchedule;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.util.Log;

public class Utils {

    /**
     * Read log entries of this application and return them as concatenated string.
     */
    public static String getAppLogEntriesAsString() {
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v threadtime");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append("\n").append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Could not read log " + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close log output stream" + e);
                }
            }
        }
        return null;
    }

    public static String getResourceAsString(Context context, int resid) throws NotFoundException {
        Resources resources = context.getResources();
        InputStream is = resources.openRawResource(resid);
        try {
            if (is != null && is.available() > 0) {
                final byte[] data = new byte[is.available()];
                is.read(data);
                return new String(data);
            }
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                // ignore
            }
        }
        return null;
    }

    /**
     * Convert a resource id into a Uri.
     *
     * @param context
     * @param resid
     * @return
     */
    public static Uri getResourceUri(Context context, int resid) throws NotFoundException {
        Resources resources = context.getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + resources.getResourcePackageName(resid) + "/" + resid);
    }

    /**
     * Update bell schedule and notification by using the regularly used BroadcastReceiver UpdateBellSchedule.
     *
     * @param packageContext
     */
    public static void updateBellSchedule(Context packageContext) {
        Intent intent = new Intent(packageContext, UpdateBellSchedule.class);
        PendingIntent sender = PendingIntent.getBroadcast(packageContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            sender.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Could not send: " + e.getMessage());
        }
    }
}
