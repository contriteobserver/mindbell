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

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object Utils {

    /**
     *
     */
    private const val CUT_OFF_MESSAGE = "[... log to long ... cut off ...]"
    /**
     * A TransactionTooLargeException is thrown if extra data transferred by an intent is too large (community is experiencing 90KB,
     * 500KB, 1MB to be the max, on emulator with API level 16, 100KB is already too much, so 70KB should be enough to read), this
     * results in a FAILED BINDER TRANSACTION crash of the caller app but exception is thrown in the called app. Therefore log
     * output is limited to this size.
     */
    private const val MAX_LOG_LENGTH = 70000

    /**
     * Read log entries of this application and return them as concatenated string but try to avoid a TransactionTooLargeException
     * (which may produce a FAILED BINDER TRANSACTION) by limiting the output.
     */
    val limitedLogEntriesAsString: String
        get() {
            var reader: BufferedReader? = null
            try {
                val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
                reader = BufferedReader(InputStreamReader(process.inputStream))
                val sb = StringBuilder()
                sb.append("===== beginning of application log =====").append("\n")
                val headerLength = sb.length
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line).append("\n")
                    line = reader.readLine()
                }
                Log.i(TAG, "Length of extracted application log is: ${sb.length}")
                if (sb.length > MAX_LOG_LENGTH) {
                    sb.replace(headerLength, sb.length - MAX_LOG_LENGTH + headerLength + CUT_OFF_MESSAGE.length, CUT_OFF_MESSAGE)
                    Log.w(TAG, "Cut off extracted application log to length: ${sb.length}")
                }
                sb.append("===== end of application log =====").append("\n")
                return sb.toString()
            } catch (e: IOException) {
                Log.e(TAG, "Could not read application log", e)
            } finally {
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                        Log.w(TAG, "Could not close log output stream", e)
                    }

                }
            }
            return "***** application log could not be read *****\n"
        }

    /**
     * Read system information and return them as concatenated string.
     */
    val systemInformation: String
        get() {
            val sb = StringBuilder()
            sb.append("===== beginning of system information =====").append("\n")
            sb.append("Build.DISPLAY").append("=").append(Build.DISPLAY).append("\n")
            sb.append("Build.PRODUCT").append("=").append(Build.PRODUCT).append("\n")
            sb.append("Build.MANUFACTURER").append("=").append(Build.MANUFACTURER).append("\n")
            sb.append("Build.BRAND").append("=").append(Build.BRAND).append("\n")
            sb.append("Build.MODEL").append("=").append(Build.MODEL).append("\n")
            sb.append("Build.VERSION.SDK_INT").append("=").append(Build.VERSION.SDK_INT).append("\n")
            sb.append("===== end of system information =====").append("\n")
            return sb.toString()
        }

    /**
     * Read application information and return them as concatenated string.
     */
    fun getApplicationInformation(packageManager: PackageManager, packageName: String): String {
        val sb = StringBuilder()
        sb.append("===== beginning of application information =====").append("\n")
        sb.append("packageName").append("=").append(packageName).append("\n")
        sb
                .append("packageInfo.versionName")
                .append("=")
                .append(getApplicationVersionName(packageManager, packageName))
                .append("\n")
        sb
                .append("packageInfo.versionCode")
                .append("=")
                .append(getApplicationVersionCode(packageManager, packageName))
                .append("\n")
        sb.append("===== end of application information =====").append("\n")
        return sb.toString()
    }

    /**
     * Return name of the applications version.
     */
    fun getApplicationVersionName(packageManager: PackageManager, packageName: String): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "Could not retrieve package information", e)
            "N/A"
        }

    }

    /**
     * Return code of the applications version.
     */
    fun getApplicationVersionCode(packageManager: PackageManager, packageName: String): Int {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionCode
        } catch (e: NameNotFoundException) {
            Log.e(TAG, "Could not retrieve package information", e)
            0
        }

    }

    /**
     * Returns the duration (in milliseconds) of the sound specified by the soundUri or null if the sound is not accessible,
     * probably because permissions have been withdrawn from behind.
     *
     * @param context
     * @param soundUri
     * @return
     */
    fun getSoundDuration(context: Context, soundUri: Uri): Long? {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, soundUri)
            val durationString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationString == null) null else java.lang.Long.parseLong(durationString)
        } catch (e: Exception) {
            Log.w(TAG, "Sound <$soundUri> not accessible", e)
            null
        }

    }


    /**
     * Convert a resource id into a Uri.
     *
     * @param context
     * @param resid
     * @return
     */
    @Throws(NotFoundException::class)
    fun getResourceUri(context: Context, resid: Int): Uri {
        val resources = context.resources
        return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${resources.getResourcePackageName(resid)}/$resid")
    }

    /**
     * Returns true if MindBell is exclude from battery optimization.
     */
    fun isAppWhitelisted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            false
        }
    }

    /**
     * Closes the virtual keyboard that has been opened automatically when entering the text view. It's absolutely weird that the
     * application has to care about closing the keyboard but it has to.
     */
    fun hideKeyboard(context: Context, textView: TextView) {
        val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(textView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    /**
     * Returns an array of strings as an array of char sequences.
     */
    private fun asCharSequenceArray(stringArray: Array<String>): Array<CharSequence> {
        // Kotlin does not seem to offer a more convenient way to convert these types of arrays.
        val stringList = ArrayList<String>()
        stringList.addAll(stringArray)
        return stringList.toArray(arrayOfNulls<CharSequence>(stringArray.size))
    }

    /**
     * Returns a summary containing a list of comma separated entries. The content of the list depends on the given selected
     * values. The order of the list depends on the entry values, an ordered list of indices into the entries array.
     */
    fun deriveOrderedEntrySummary(values: Set<String>, entries: Array<CharSequence>, entryValues: Array<CharSequence>): String {
        val sb = StringBuilder()
        for (i in entryValues.indices) { // walk through entries, as they are ordered when presented to the user
            if (values.contains(entryValues[i])) { // is internal representation value in the set of selected values?
                if (sb.isNotEmpty()) {
                    sb.append(", ")
                }
                sb.append(entries[i])
            }
        }
        return sb.toString()
    }

    /**
     * Convenience method to call deriveOrderedEntrySummary() with arrays of character sequences.
     */
    fun deriveOrderedEntrySummary(values: Set<String>, entries: Array<String>, entryValues: Array<String>): String {
        return deriveOrderedEntrySummary(values, asCharSequenceArray(entries), asCharSequenceArray(entryValues))
    }

}
