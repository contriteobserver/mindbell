/*
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
 */
package com.googlecode.mindbell.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

public class Utils {

    /**
     *
     */
    private static final String CUT_OFF_MESSAGE = "[... log to long ... cut off ...]";
    /**
     * A TransactionTooLargeException is thrown if extra data transferred by an intent is too large (community is experiencing 90KB,
     * 500KB, 1MB to be the max, on emulator with API level 16, 100KB is already too much, so 70KB should be enough to read), this
     * results in a FAILED BINDER TRANSACTION crash of the caller app but exception is thrown in the called app. Therefore log
     * output is limited to this size.
     */
    private static final int MAX_LOG_LENGTH = 70000;

    /**
     * Read application information and return them as concatenated string.
     */
    public static String getApplicationInformation(PackageManager packageManager, String packageName) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== beginning of application information =====").append("\n");
        sb.append("packageName").append("=").append(packageName).append("\n");
        sb
                .append("packageInfo.versionName")
                .append("=")
                .append(getApplicationVersionName(packageManager, packageName))
                .append("\n");
        sb
                .append("packageInfo.versionCode")
                .append("=")
                .append(getApplicationVersionCode(packageManager, packageName))
                .append("\n");
        sb.append("===== end of application information =====").append("\n");
        return sb.toString();
    }

    /**
     * Return name of the applications version.
     */
    public static String getApplicationVersionName(PackageManager packageManager, String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Could not retrieve package information" + e);
            return ("N/A");
        }
    }

    /**
     * Return code of the applications version.
     */
    public static int getApplicationVersionCode(PackageManager packageManager, String packageName) {
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Could not retrieve package information" + e);
            return 0;
        }
    }

    /**
     * Read log entries of this application and return them as concatenated string but try to avoid a TransactionTooLargeException
     * (which may produce a FAILED BINDER TRANSACTION) by limiting the output.
     */
    public static String getLimitedLogEntriesAsString() {
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v threadtime");
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("===== beginning of application log =====").append("\n");
            int headerLength = sb.length();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line).append("\n");
            }
            Log.i(TAG, "Length of extracted application log is: " + sb.length());
            if (sb.length() > MAX_LOG_LENGTH) {
                sb.replace(headerLength, sb.length() - MAX_LOG_LENGTH + headerLength + CUT_OFF_MESSAGE.length(), CUT_OFF_MESSAGE);
                Log.w(TAG, "Cut off extracted application log to length: " + sb.length());
            }
            sb.append("===== end of application log =====").append("\n");
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Could not read application log", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.w(TAG, "Could not close log output stream" + e);
                }
            }
        }
        return "***** application log could not be read *****\n";
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
     * Read system information and return them as concatenated string.
     */
    public static String getSystemInformation() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== beginning of system information =====").append("\n");
        sb.append("Build.DISPLAY").append("=").append(Build.DISPLAY).append("\n");
        sb.append("Build.PRODUCT").append("=").append(Build.PRODUCT).append("\n");
        sb.append("Build.MANUFACTURER").append("=").append(Build.MANUFACTURER).append("\n");
        sb.append("Build.BRAND").append("=").append(Build.BRAND).append("\n");
        sb.append("Build.MODEL").append("=").append(Build.MODEL).append("\n");
        sb.append("Build.VERSION.SDK_INT").append("=").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("===== end of system information =====").append("\n");
        return sb.toString();
    }

    /**
     * Returns true if MindBell is exclude from battery optimization.
     */
    public static boolean isAppWhitelisted(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(context.getPackageName());
        } else {
            return false;
        }
    }

}
