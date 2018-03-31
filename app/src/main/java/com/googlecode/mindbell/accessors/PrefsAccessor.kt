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

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import com.googlecode.mindbell.MindBell
import com.googlecode.mindbell.R
import com.googlecode.mindbell.R.string.*
import com.googlecode.mindbell.accessors.PrefsAccessor.Preference.Type.*
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.Utils
import java.util.*

class PrefsAccessor
/**
 * Constructs an accessor for preferences in the given context, please use [ContextAccessor.getPrefs] instead of
 * calling this directly.
 */
(context: Context, logSettings: Boolean) {
    // *The* SharedPreferences
    private val settings: SharedPreferences

    // Map of all used preferences - with type and default value - by resid
    private val preferenceMap = TreeMap<Int, Preference>()

    // Map of allowed internal values for a string or string set preference by resid
    private val entryValuesMap = HashMap<Int, Array<String>>()

    private val weekdayEntryValues: Array<String>
    private val weekdayAbbreviationEntries: Array<String>

    private val activityPrefsForRegularOperation = ActivityPrefsAccessorForRegularOperation()

    private val activityPrefsForRingingOnce = ActivityPrefsAccessorForRingingOnce()

    private val activityPrefsForMeditationBeginning = ActivityPrefsAccessorForMeditationBeginning()

    private val activityPrefsForMeditationInterrupting = ActivityPrefsAccessorForMeditationInterrupting()

    private val activityPrefsForMeditationEnding = ActivityPrefsAccessorForMeditationEnding()

    val vibrationPattern: LongArray
        get() = getVibrationPattern(pattern)

    val pattern: String
        get() = getStringSetting(keyPattern)

    val isNormalize: Boolean
        get() = isNormalize(normalize)

    val normalize: Int
        get() = Integer.valueOf(getStringSetting(keyNormalize))!!

    /**
     * Returns the number of meditation periods derived from the pattern of periods lengths.
     */
    val numberOfPeriods: Int
        get() = deriveNumberOfPeriods(patternOfPeriods)

    var patternOfPeriods: String
        get() = getStringSetting(keyPatternOfPeriods)
        set(patternOfPeriods) = setSetting(keyPatternOfPeriods, patternOfPeriods)

    val volume: Float
        get() = getFloatSetting(keyVolume)

    val isShow: Boolean
        get() = getBooleanSetting(keyShow)

    val isSound: Boolean
        get() = getBooleanSetting(keySound)

    var isStatus: Boolean
        get() = getBooleanSetting(keyStatus)
        set(statusNotification) = setSetting(keyStatus, statusNotification)

    val isNotification: Boolean
        get() = getBooleanSetting(keyNotification)

    val isDismissNotification: Boolean
        get() = getBooleanSetting(keyDismissNotification)

    // Warning: Similar code in MindBellPreferences#setMultiSelectListPreferenceSummary()
    // internal weekday value in locale oriented order
    // active on this day?
    // add day to the list of active days
    val activeOnDaysOfWeekString: String
        get() {
            val activeOnDaysOfWeek = activeOnDaysOfWeek
            val sb = StringBuilder()
            for (dayOfWeekValue in weekdayEntryValues) {
                val dayOfWeekValueAsInteger = Integer.valueOf(dayOfWeekValue)
                if (activeOnDaysOfWeek.contains(dayOfWeekValueAsInteger)) {
                    if (sb.length > 0) {
                        sb.append(", ")
                    }
                    sb.append(getWeekdayAbbreviation(dayOfWeekValueAsInteger!!))
                }
            }
            return sb.toString()
        }

    val activeOnDaysOfWeek: Set<Int>
        get() {
            val strings = getStringSetSetting(keyActiveOnDaysOfWeek)
            val integers = HashSet<Int>()
            for (string in strings) {
                integers.add(Integer.valueOf(string))
            }
            return integers
        }

    val audioStream: Int
        get() = getAudioStream(getStringSetting(keyAudioStream))

    val isUseAudioStreamVolumeSetting: Boolean
        get() = getBooleanSetting(keyUseAudioStreamVolumeSetting)

    val meditationVolume: Float
        get() = getFloatSetting(keyMeditationVolume)

    val ringtone: String
        get() = getStringSetting(keyRingtone)

    private val isUseWorkaroundBell: Boolean
        get() = getBooleanSetting(keyUseWorkaroundBell)

    val daytimeEnd: TimeOfDay
        get() = TimeOfDay(getStringSetting(keyEnd))

    val daytimeStart: TimeOfDay
        get() = TimeOfDay(getStringSetting(keyStart))

    val interval: Long
        get() = TimeOfDay(getStringSetting(keyFrequency)).interval * ONE_MINUTE_MILLIS

    val notificationText: String
        get() = getStringSetting(keyNotificationText)

    val notificationTitle: String
        get() = getStringSetting(keyNotificationTitle)

    var isActive: Boolean
        get() = getBooleanSetting(keyActive)
        set(active) = setSetting(keyActive, active)

    var isMeditating: Boolean
        get() = getBooleanSetting(keyMeditating)
        set(meditating) = setSetting(keyMeditating, meditating)

    val isRandomize: Boolean
        get() = getBooleanSetting(keyRandomize)

    val isMuteInFlightMode: Boolean
        get() = getBooleanSetting(keyMuteInFlightMode)

    val isMuteOffHook: Boolean
        get() = getBooleanSetting(keyMuteOffHook)

    val isNoSoundOnMusic: Boolean
        get() = getBooleanSetting(keyNoSoundOnMusic)

    val isPauseAudioOnSound: Boolean
        get() = getBooleanSetting(keyPauseAudioOnSound)

    val isMuteWithPhone: Boolean
        get() = getBooleanSetting(keyMuteWithPhone)

    val isMuteWithAudioStream: Boolean
        get() = getBooleanSetting(keyMuteWithAudioStream)

    val isVibrate: Boolean
        get() = getBooleanSetting(keyVibrate)

    var isKeepScreenOn: Boolean
        get() = getBooleanSetting(keyKeepScreenOn)
        set(keepScreenOn) = setSetting(keyKeepScreenOn, keepScreenOn)

    var isStartMeditationDirectly: Boolean
        get() = getBooleanSetting(keyStartMeditationDirectly)
        set(startMeditationDirectly) = setSetting(keyStartMeditationDirectly, startMeditationDirectly)

    var isStopMeditationAutomatically: Boolean
        get() = getBooleanSetting(keyStopMeditationAutomatically)
        set(stopMeditationAutomatically) = setSetting(keyStopMeditationAutomatically, stopMeditationAutomatically)

    val isStatusVisibilityPublic: Boolean
        get() = getBooleanSetting(keyStatusVisibilityPublic)

    val isNotificationVisibilityPublic: Boolean
        get() = getBooleanSetting(keyNotificationVisibilityPublic)

    val rampUpTimeMillis: Long
        get() = rampUpTime.interval * 1000L

    var rampUpTime: TimeOfDay
        get() = TimeOfDay(getStringSetting(keyRampUpTime))
        set(rampUpTime) = setSetting(keyRampUpTime, rampUpTime.persistString)

    val meditationDurationMillis: Long
        get() = meditationDuration.interval * ONE_MINUTE_MILLIS

    var meditationDuration: TimeOfDay
        get() = TimeOfDay(getStringSetting(keyMeditationDuration))
        set(meditationDuration) = setSetting(keyMeditationDuration, meditationDuration.persistString)

    var rampUpStartingTimeMillis: Long
        get() = getLongSetting(keyRampUpStartingTimeMillis)
        set(rampUpStartingTimeMillis) = setSetting(keyRampUpStartingTimeMillis, rampUpStartingTimeMillis)

    var meditationStartingTimeMillis: Long
        get() = getLongSetting(keyMeditationStartingTimeMillis)
        set(meditationStartingTimeMillis) = setSetting(keyMeditationStartingTimeMillis, meditationStartingTimeMillis)

    var meditationEndingTimeMillis: Long
        get() = getLongSetting(keyMeditationEndingTimeMillis)
        set(meditationEndingTimeMillis) = setSetting(keyMeditationEndingTimeMillis, meditationEndingTimeMillis)

    var mutedTill: Long
        get() = getLongSetting(keyMutedTill)
        set(mutedTill) = setSetting(keyMutedTill, mutedTill)

    val meditationBeginningBell: String
        get() = getStringSetting(keyMeditationBeginningBell)

    val meditationInterruptingBell: String
        get() = getStringSetting(keyMeditationInterruptingBell)

    val meditationEndingBell: String
        get() = getStringSetting(keyMeditationEndingBell)

    var originalVolume: Int
        get() = getIntSetting(keyOriginalVolume)
        set(originalVolume) = setSetting(keyOriginalVolume, originalVolume)

    var popup: Int
        get() = getIntSetting(keyPopup)
        set(popup) = setSetting(keyPopup, popup)

    init {
        settings = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

        // Define entries and entry values
        weekdayEntryValues = context.resources.getStringArray(R.array.weekdayEntryValues)
        weekdayAbbreviationEntries = context.resources.getStringArray(R.array.weekdayAbbreviationEntries)

        // Check the settings and reset invalid ones
        checkSettings(context, logSettings)
    }

    /**
     * Returns the current string setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private fun getStringSetting(resid: Int): String {
        return getSetting(resid) as String
    }

    /**
     * Returns the current setting of the preference with the given resid
     *
     * @param resid
     * @return
     */
    private fun getSetting(resid: Int): Any {
        return getSetting(preferenceMap[resid]!!)
    }

    /**
     * Returns the current setting of the preference.
     *
     * @param preference
     * @return
     */
    private fun getSetting(preference: Preference): Any {
        when (preference.type) {
            BOOLEAN -> return settings.getBoolean(preference.key, preference.defaultValue as Boolean)
            FLOAT -> return settings.getFloat(preference.key, preference.defaultValue as Float)
            INTEGER -> return settings.getInt(preference.key, preference.defaultValue as Int)
            LONG -> return settings.getLong(preference.key, preference.defaultValue as Long)
            STRING, TIME_STRING -> return settings.getString(preference.key, preference.defaultValue as String)
            STRING_SET -> return settings.getStringSet(preference.key, preference.defaultValue as Set<String>)
            else -> {
                throw IllegalArgumentException("Preference '" + preference.key + "' has a non supported type: " + preference.type)
            }
        }
    }

    fun getMeditationPeriodMillis(meditationPeriod: Int): Long {
        return derivePeriodMillis(patternOfPeriods, meditationDuration.interval, meditationPeriod)
    }

    /**
     * Checks that any data in the SharedPreferences are of the expected type. Should we find anything that doesn't fit the
     * expectations, we delete it and recreate it with it's default value.
     */
    fun checkSettings(context: Context, logSettings: Boolean) {

        // Define all preference keys and their default values
        addPreference(keyActive, false, BOOLEAN, context)
        addPreference(keyActiveOnDaysOfWeek, HashSet(Arrays.asList(*arrayOf("1", "2", "3", "4", "5", "6", "7"))),
                STRING_SET, context)
        addPreference(keyAudioStream, "0", STRING, context)
        addPreference(keyDismissNotification, false, BOOLEAN, context)
        addPreference(keyEnd, "21:00", TIME_STRING, context)
        addPreference(keyFrequency, "00:15", TIME_STRING, context) // 15 min
        addPreference(keyKeepScreenOn, true, BOOLEAN, context)
        addPreference(keyMeditating, false, BOOLEAN, context)
        addPreference(keyMeditationBeginningBell, "3", STRING, context)
        addPreference(keyMeditationDuration, "00:25", TIME_STRING, context)
        addPreference(keyMeditationEndingBell, "2", STRING, context)
        addPreference(keyMeditationEndingTimeMillis, -1L, LONG, context)
        addPreference(keyMeditationInterruptingBell, "1", STRING, context)
        addPreference(keyMeditationStartingTimeMillis, -1L, LONG, context)
        addPreference(keyMuteInFlightMode, false, BOOLEAN, context)
        addPreference(keyMuteOffHook, true, BOOLEAN, context)
        addPreference(keyMutedTill, -1L, LONG, context)
        addPreference(keyMuteWithAudioStream, true, BOOLEAN, context)
        addPreference(keyMuteWithPhone, true, BOOLEAN, context)
        addPreference(keyNormalize, NORMALIZE_NONE, STRING, context)
        addPreference(keyNoSoundOnMusic, false, BOOLEAN, context)
        addPreference(keyNotification, false, BOOLEAN, context)
        addPreference(keyNotificationText, context.getText(R.string.prefsNotificationTextDefault), STRING, context)
        addPreference(keyNotificationTitle, context.getText(R.string.prefsNotificationTitleDefault), STRING, context)
        addPreference(keyNotificationVisibilityPublic, true, BOOLEAN, context)
        addPreference(keyOriginalVolume, -1, INTEGER, context)
        addPreference(keyPattern, "100:200:100:600", STRING, context)
        addPreference(keyPatternOfPeriods, "x", STRING, context)
        addPreference(keyPauseAudioOnSound, false, BOOLEAN, context)
        addPreference(keyPopup, -1, INTEGER, context)
        addPreference(keyRampUpStartingTimeMillis, -1L, LONG, context)
        addPreference(keyRampUpTime, "00:30", TIME_STRING, context)
        addPreference(keyRandomize, true, BOOLEAN, context)
        addPreference(keyReminderBell, DEFAULT_REMINDER_BELL, STRING, context)
        addPreference(keyRingtone, "", STRING, context) // no useful default, code relies on <defaultValue>.isEmpty()
        addPreference(keyShow, true, BOOLEAN, context)
        addPreference(keySound, true, BOOLEAN, context)
        addPreference(keyStart, "09:00", TIME_STRING, context)
        addPreference(keyStartMeditationDirectly, false, BOOLEAN, context)
        addPreference(keyStatus, false, BOOLEAN, context)
        addPreference(keyStatusIconMaterialDesign, true, BOOLEAN, context)
        addPreference(keyStatusVisibilityPublic, true, BOOLEAN, context)
        addPreference(keyStopMeditationAutomatically, false, BOOLEAN, context)
        addPreference(keyUseWorkaroundBell, false, BOOLEAN, context)
        addPreference(keyUseAudioStreamVolumeSetting, true, BOOLEAN, context)
        addPreference(keyVibrate, false, BOOLEAN, context)
        addPreference(keyVolume, DEFAULT_VOLUME, FLOAT, context)
        addPreference(keyMeditationVolume, volume, FLOAT, context) // for existing users: use standard volume as default here

        // Map preference keys to their allowed entryValues
        entryValuesMap.put(keyActiveOnDaysOfWeek, context.resources.getStringArray(R.array.weekdayEntryValues))
        entryValuesMap.put(keyAudioStream, context.resources.getStringArray(R.array.audioStreamPersistedEntryValues))
        entryValuesMap.put(keyMeditationBeginningBell, context.resources.getStringArray(R.array.meditationBellEntryValues))
        entryValuesMap.put(keyMeditationEndingBell, context.resources.getStringArray(R.array.meditationBellEntryValues))
        entryValuesMap.put(keyMeditationInterruptingBell, context.resources.getStringArray(R.array.meditationBellEntryValues))
        entryValuesMap.put(keyNormalize, context.resources.getStringArray(R.array.normalizeEntryValues))
        entryValuesMap.put(keyNotificationText, arrayOf()) // we cannot verify the entered notification text
        entryValuesMap.put(keyNotificationTitle, arrayOf()) // we cannot verify the entered notification title
        entryValuesMap.put(keyPattern, context.resources.getStringArray(R.array.patternEntryValues))
        entryValuesMap.put(keyPatternOfPeriods, arrayOf()) // we cannot verify the entered notification text
        entryValuesMap.put(keyReminderBell, context.resources.getStringArray(R.array.reminderBellEntryValues))
        entryValuesMap.put(keyRingtone, arrayOf()) // we don't need to know the possible ringtone values

        // Convert old settings from previous versions
        convertOldSettings(context)

        // Track whether a stacktrace shall be logged to find out the reason for sometimes deleted preferences
        var logStackTrace = false

        // Remove preference settings of wrong type and set non-existent preference to their default value
        for (preference in preferenceMap.values) {
            logStackTrace = removeInvalidSetting(preference) || logStackTrace
            logStackTrace = resetMissingSetting(preference) || logStackTrace
        }

        // Log stacktrace if a setting has been deleted or set to its default
        if (logStackTrace) {
            MindBell.logWarn("At least one setting has been deleted or reset to its default", Exception())
        } else {
            MindBell.logDebug("Preferences checked and found to be ok")
        }

        // Finally report all currently existing settings if requested
        if (logSettings) {
            val sb = StringBuilder()
            sb.append("Effective settings: ")
            val orderedSettings = TreeMap<String, Any>(settings.all)
            for ((key, value) in orderedSettings) {
                sb.append(key).append("=").append(value).append(", ")
            }
            sb.setLength(sb.length - 2) // remove last ", "
            MindBell.logDebug(sb.toString())
        }
    }

    /**
     * Puts a newly created Preference into the referenceMap with the given resid as key of the map.
     *
     * @param resid
     * @param defaultValue
     * @param type
     * @param context
     */
    private fun addPreference(resid: Int, defaultValue: Any, type: Preference.Type, context: Context) {
        preferenceMap.put(resid, Preference(resid, context.getString(resid), defaultValue, type))
    }

    /**
     * Convert old settings from previous versions ... remove code after a while.
     */
    private fun convertOldSettings(context: Context) {
        // Version 3.1.0 replaced numberOfPeriods by patternOfPeriods
        val keyNumberOfPeriods = "numberOfPeriods"
        val oldNumberOfPeriods = settings.getInt(keyNumberOfPeriods, 0)
        if (oldNumberOfPeriods > 0) {
            var patternOfPeriods = derivePatternOfPeriods(oldNumberOfPeriods)
            patternOfPeriods = patternOfPeriods
            settings.edit().remove(keyNumberOfPeriods).apply()
            MindBell.logWarn("Converted old setting for '" + keyNumberOfPeriods + "' (" + oldNumberOfPeriods + ") to '" +
                    context.getText(keyPatternOfPeriods) + "' (" + patternOfPeriods + ")")
        }
        // Version 3.2.0 replaces frequency milliseconds string by time string
        val keyFrequency = context.getString(R.string.keyFrequency)
        val oldFrequency = settings.getString(keyFrequency, null)
        if (oldFrequency != null && !oldFrequency.contains(":")) {
            try {
                val milliseconds = java.lang.Long.parseLong(oldFrequency)
                val frequency = TimeOfDay.fromMillisecondsInterval(milliseconds).persistString
                setSetting(R.string.keyFrequency, frequency)
                MindBell.logWarn("Converted old value for '$keyFrequency' from '$oldFrequency' to '$frequency'")
            } catch (e: NumberFormatException) {
                // invalid preference will be removed by removeInvalidSetting()
            }

        }
        // Version 3.2.0 replaces rampup time seconds by time string
        val keyRampUpTime = context.getString(R.string.keyRampUpTime)
        try {
            val oldRampUpTime = settings.getInt(keyRampUpTime, -1)
            if (oldRampUpTime >= 0) {
                val rampUpTime = TimeOfDay.fromSecondsInterval(oldRampUpTime).persistString
                setSetting(R.string.keyRampUpTime, rampUpTime)
                MindBell.logWarn("Converted old value for '$keyRampUpTime' from '$oldRampUpTime' to '$rampUpTime'")
            }
        } catch (e: ClassCastException) {
            // preference has already been converted
        }

        // Version 3.2.0 replaces meditation duration minutes by time string
        val keyMeditationDuration = context.getString(R.string.keyMeditationDuration)
        try {
            val oldMeditationDuration = settings.getInt(keyMeditationDuration, -1)
            if (oldMeditationDuration >= 0) {
                val meditationDuration = TimeOfDay.fromSecondsInterval(oldMeditationDuration).persistString
                setSetting(R.string.keyMeditationDuration, meditationDuration)
                MindBell.logWarn("Converted old value for '" + keyMeditationDuration + "' from '" + oldMeditationDuration + "' to '" +
                        meditationDuration + "'")
            }
        } catch (e: ClassCastException) {
            // preference has already been converted
        }

        // Version 3.2.5 renamed statusVisiblityPublic to statusVisibilityPublic
        val keyStatusVisiblityPublic = "statusVisiblityPublic"
        if (settings.contains(keyStatusVisiblityPublic)) {
            val statusVisibilityPublic = settings.getBoolean(keyStatusVisiblityPublic,
                    preferenceMap[keyStatusVisibilityPublic]!!.defaultValue as Boolean)
            setSetting(keyStatusVisibilityPublic, statusVisibilityPublic)
            settings.edit().remove(keyStatusVisiblityPublic).apply()
            MindBell.logWarn("Converted old setting for '" + keyStatusVisiblityPublic + "' (" + statusVisibilityPublic + ") to '" +
                    context.getText(keyStatusVisibilityPublic) + "' (" + statusVisibilityPublic + ")")
        }
        // Version 3.2.5 introduced keyUseAudioStreamVolumeSetting (default true) but should be false for users of older versions
        if (!settings.contains(context.getText(keyUseAudioStreamVolumeSetting).toString()) && settings.contains(context.getText(keyActive).toString())) {
            val useAudioStreamVolumeSetting = false
            setSetting(keyUseAudioStreamVolumeSetting, useAudioStreamVolumeSetting)
            MindBell.logWarn("Created setting for '" + context.getText(keyUseAudioStreamVolumeSetting) + "' with non-default (" +
                    useAudioStreamVolumeSetting + ") because an older version was already installed")
        }
        // Version 3.2.6 replaced useStandardBell by reminderBell
        val keyUseStandardBell = "useStandardBell"
        if (settings.contains(keyUseStandardBell)) {
            val useStandardBell = settings.getBoolean(keyUseStandardBell, java.lang.Boolean.TRUE)
            val reminderBell = if (useStandardBell) DEFAULT_REMINDER_BELL else BELL_ENTRY_VALUE_INDEX_NO_SOUND.toString()
            setSetting(keyReminderBell, reminderBell)
            settings.edit().remove(keyUseStandardBell).apply()
            MindBell.logWarn("Converted old setting for '" + keyUseStandardBell + "' (" + useStandardBell + ") to '" +
                    context.getText(keyReminderBell) + "' (" + reminderBell + ")")
        }
    }

    /**
     * Removes a preference setting if it is not of the expected type or has an invalid valid and in that case returns true.
     *
     * @param preference
     * @return
     */
    private fun removeInvalidSetting(preference: Preference): Boolean {
        try {
            val value = getSetting(preference)
            when (preference.type) {
                BOOLEAN, FLOAT, INTEGER, LONG -> {
                }
                STRING -> {
                    val stringValue = value as String?
                    if (stringValue != null) {
                        val entryValues = Arrays.asList(*entryValuesMap[preference.resid]!!)
                        if (entryValues != null && !entryValues.isEmpty() && !entryValues.contains(stringValue)) {
                            settings.edit().remove(preference.key).apply()
                            MindBell.logWarn("Removed setting '$preference' since it had wrong value '$stringValue'")
                            return true
                        }
                    }
                }
                STRING_SET -> {
                    val stringSetValue = value as Set<String>?
                    if (stringSetValue != null) {
                        for (aStringInSet in stringSetValue) {
                            val entryValues = Arrays.asList(*entryValuesMap[preference.resid]!!)
                            if (aStringInSet != null && !entryValues.contains(aStringInSet)) {
                                settings.edit().remove(preference.key).apply()
                                MindBell.logWarn("Removed setting '" + preference + "' since it had (at least one) wrong value '" +
                                        aStringInSet + "'")
                                return true
                            }
                        }
                    }
                }
                TIME_STRING -> {
                    val timeStringValue = value as String?
                    if (timeStringValue != null) {
                        if (!timeStringValue.matches(TIME_STRING_REGEX.toRegex())) {
                            settings.edit().remove(preference.key).apply()
                            MindBell.logWarn("Removed setting '$preference' since it is not a time string '$timeStringValue'")
                            return true
                        }
                    }
                }
            }// no more to do, retrieving the value is all that can be done
            return false
        } catch (e: ClassCastException) {
            settings.edit().remove(preference.key).apply()
            MindBell.logWarn("Removed setting '$preference' since it had wrong type: $e")
            return true
        }

    }

    /**
     * Resets a preference to its default value if it is missing and in that case returns true.
     *
     * @param preference
     * @return
     */
    private fun resetMissingSetting(preference: Preference): Boolean {
        if (!settings.contains(preference.key)) {
            resetSetting(preference)
            MindBell.logWarn("Reset missing setting for '" + preference.key + "' to '" + preference.defaultValue + "'")
            return true
        }
        return false
    }

    /**
     * Returns the current boolean setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private fun getBooleanSetting(resid: Int): Boolean {
        return (getSetting(resid) as Boolean?)!!
    }

    /**
     * Sets the preference with the given resid to the given value.
     *
     * @param resid
     * @param value
     */
    private fun setSetting(resid: Int, value: Any) {
        setSetting(preferenceMap[resid]!!, value)
    }

    /**
     * Sets the preference to the given value.
     *
     * @param preference
     * @param value
     */
    private fun setSetting(preference: Preference, value: Any) {
        when (preference.type) {
            BOOLEAN -> settings.edit().putBoolean(preference.key, value as Boolean).apply()
            FLOAT -> settings.edit().putFloat(preference.key, value as Float).apply()
            INTEGER -> settings.edit().putInt(preference.key, value as Int).apply()
            LONG -> settings.edit().putLong(preference.key, value as Long).apply()
            STRING, TIME_STRING -> settings.edit().putString(preference.key, value as String).apply()
            STRING_SET -> settings.edit().putStringSet(preference.key, value as Set<String>).apply()
            else -> MindBell.logError("Preference '" + preference.key + "' has a non supported type: " + preference.type)
        }
    }

    fun getWeekdayAbbreviation(dayOfWeek: Int): String {
        return weekdayAbbreviationEntries[dayOfWeek - 1]
    }

    /**
     * Returns the current string set setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private fun getStringSetSetting(resid: Int): Set<String> {
        return getSetting(resid) as Set<String>
    }

    /**
     * MindBell's own volume settings are only allowed to be used with sound going to alarm stream.
     */
    fun mustUseAudioStreamVolumeSetting(): Boolean {
        return audioStream != AudioManager.STREAM_ALARM
    }

    /**
     * Returns the current float setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private fun getFloatSetting(resid: Int): Float {
        return (getSetting(resid) as Float?)!!
    }

    /**
     * Returns the chosen sound depending on settings for reminderBell, ringtone and useWorkaroundBell.
     */
    fun getReminderSoundUri(context: Context): Uri? {
        // This implementation is almost the same as MindBellPreferences#getReminderSoundUri()
        var soundUri = getReminderBellSoundUri(context)
        if (soundUri == null) { // use system notification ringtone if reminder bell sound is not set
            val ringtone = ringtone
            if (ringtone.isEmpty()) {
                soundUri = getDefaultReminderBellSoundUri(context)
            } else {
                return Uri.parse(ringtone)
            }
        }
        return soundUri
    }

    fun getReminderBellSoundUri(context: Context): Uri? {
        return getBellSoundUri(context, getStringSetting(keyReminderBell))
    }

    fun getDefaultReminderBellSoundUri(context: Context): Uri? {
        return getBellSoundUri(context, DEFAULT_REMINDER_BELL)
    }

    fun getBellSoundUri(context: Context, key: String): Uri? {
        return getBellSoundUri(context, key, isUseWorkaroundBell)
    }

    fun useStatusIconMaterialDesign(): Boolean {
        return getBooleanSetting(keyStatusIconMaterialDesign)
    }

    /**
     * Returns the current long setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private fun getLongSetting(resid: Int): Long {
        return (getSetting(resid) as Long?)!!
    }

    /**
     * Returns the current int setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private fun getIntSetting(resid: Int): Int {
        return (getSetting(resid) as Int?)!!
    }

    fun resetOriginalVolume() {
        resetSetting(keyOriginalVolume)
    }

    /**
     * Resets the preference with the given resid to its default value.
     *
     * @param resid
     */
    private fun resetSetting(resid: Int) {
        resetSetting(preferenceMap[resid]!!)
    }

    /**
     * Resets the preference with the given resid to its default value.
     *
     * @param preference
     */
    private fun resetSetting(preference: Preference) {
        setSetting(preference, preference.defaultValue)
    }

    fun resetPopup() {
        resetSetting(keyPopup)
    }

    fun forRegularOperation(): ActivityPrefsAccessor {
        return activityPrefsForRegularOperation
    }

    fun forRingingOnce(): ActivityPrefsAccessor {
        return activityPrefsForRingingOnce
    }

    fun forMeditationBeginning(): ActivityPrefsAccessor {
        return activityPrefsForMeditationBeginning
    }

    fun forMeditationInterrupting(): ActivityPrefsAccessor {
        return activityPrefsForMeditationInterrupting
    }

    fun forMeditationEnding(): ActivityPrefsAccessor {
        return activityPrefsForMeditationEnding
    }

    internal class Preference(val resid: Int, val key: String, val defaultValue: Any, val type: Type) {

        internal enum class Type {
            BOOLEAN, FLOAT, INTEGER, LONG, STRING, STRING_SET, TIME_STRING
        }

    }

    private inner class ActivityPrefsAccessorForRegularOperation : ActivityPrefsAccessor {

        override val isShow: Boolean
            get() = this@PrefsAccessor.isShow

        override val isSound: Boolean
            get() = this@PrefsAccessor.isSound

        override val isVibrate: Boolean
            get() = this@PrefsAccessor.isVibrate

        override val volume: Float
            get() = this@PrefsAccessor.volume

        override val isNotification: Boolean
            get() = this@PrefsAccessor.isNotification

        override val isDismissNotification: Boolean
            get() = this@PrefsAccessor.isDismissNotification

        override fun getSoundUri(context: Context): Uri? {
            return this@PrefsAccessor.getReminderSoundUri(context)
        }

    }

    private inner class ActivityPrefsAccessorForRingingOnce : ActivityPrefsAccessor {

        override val isShow: Boolean
            get() = false

        override val isSound: Boolean
            get() = true

        override val isVibrate: Boolean
            get() = false

        override val volume: Float
            get() = this@PrefsAccessor.volume

        override val isNotification: Boolean
            get() = false

        override val isDismissNotification: Boolean
            get() = false

        override fun getSoundUri(context: Context): Uri? {
            return this@PrefsAccessor.getReminderSoundUri(context)
        }

    }

    private abstract inner class ActivityPrefsAccessorForMeditation : ActivityPrefsAccessor {

        override val isShow: Boolean
            get() = false

        override val isSound: Boolean
            get() = true

        override val isVibrate: Boolean
            get() = false

        override val volume: Float
            get() = this@PrefsAccessor.meditationVolume

        override val isNotification: Boolean
            get() = false

        override val isDismissNotification: Boolean
            get() = false

        abstract override fun getSoundUri(context: Context): Uri?

    }

    private inner class ActivityPrefsAccessorForMeditationBeginning : ActivityPrefsAccessorForMeditation() {

        override fun getSoundUri(context: Context): Uri? {
            return if (isStartMeditationDirectly) null else this@PrefsAccessor.getBellSoundUri(context, meditationBeginningBell)
        }

    }

    private inner class ActivityPrefsAccessorForMeditationInterrupting : ActivityPrefsAccessorForMeditation() {

        override fun getSoundUri(context: Context): Uri? {
            return this@PrefsAccessor.getBellSoundUri(context, meditationInterruptingBell)
        }

    }

    private inner class ActivityPrefsAccessorForMeditationEnding : ActivityPrefsAccessorForMeditation() {

        override fun getSoundUri(context: Context): Uri? {
            return this@PrefsAccessor.getBellSoundUri(context, meditationEndingBell)
        }

    }

    companion object {

        /**
         * One minute in milliseconds.
         */
        val ONE_MINUTE_MILLIS = 60000L

        /**
         * One minute in milliseconds plus an error indicator millisecond value.
         */
        val ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION = ONE_MINUTE_MILLIS + 1L
        val ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING = ONE_MINUTE_MILLIS + 2L
        val ONE_MINUTE_MILLIS_NEGATIVE_PERIOD = ONE_MINUTE_MILLIS + 3L
        val ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT = ONE_MINUTE_MILLIS + 4L
        val ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING = ONE_MINUTE_MILLIS + 5L

        /**
         * Regular expressions to verify a pattern of periods string.
         */
        val STATIC_PERIOD_REGEX = "([1-9][0-9]{0,2})"
        val VARIABLE_PERIOD_REGEX = "(x)"
        val PERIOD_REGEX = "($STATIC_PERIOD_REGEX|$VARIABLE_PERIOD_REGEX)"
        val PERIOD_SEPARATOR = ","
        val PERIOD_SEPARATOR_REGEX = PERIOD_SEPARATOR
        val PERIOD_SEPARATOR_WITH_BLANKS_REGEX = " *$PERIOD_SEPARATOR_REGEX *"
        val PERIOD_SEPARATOR_WITH_BLANK = ", "

        /**
         * Minimum value for ramp up time
         */
        val MIN_RAMP_UP_TIME = TimeOfDay(0, 5)

        /**
         * Minimum value for meditation duration
         */
        val MIN_MEDITATION_DURATION = TimeOfDay(0, 1)

        /**
         * Time to wait for displayed bell to be send back (without silence in the beginning).
         */
        val WAITING_TIME = 10000L

        /**
         * Silence added in the beginning of the standard bell as workaround for phones starting sounds with increasing volume.
         */
        val WORKAROUND_SILENCE_TIME = 3000L

        val NORMALIZE_NONE = "-1"

        val DEFAULT_VOLUME = 0.501187234f

        /**
         * Regular expression to verify a time string preference
         */
        private val TIME_STRING_REGEX = "\\d?\\d(:\\d\\d)?"

        /**
         * Index of entry in bellEntryValues that represents the 'use system notification ringtone' setting.
         */
        private val BELL_ENTRY_VALUE_INDEX_NO_SOUND = 0

        /**
         * Default reminder bell sound, also used in case the set sound uri is not accessible.
         */
        private val DEFAULT_REMINDER_BELL = "1"

        /**
         * Unique string to be added to a Scheduling Intent to see which meditation period the bell is in.
         */
        val EXTRA_MEDITATION_PERIOD = "com.googlecode.mindbell.Scheduler.MeditationPeriod"

        /**
         * Unique string to be added to a Scheduling Intent to see who sent it.
         */

        val EXTRA_IS_RESCHEDULING = "com.googlecode.mindbell.Scheduler.IsRescheduling"

        /**
         * Unique string to be added to a Scheduling Intent to see for which time the bell was scheduled.
         */
        val EXTRA_NOW_TIME_MILLIS = "com.googlecode.mindbell.Scheduler.NowTimeMillis"

        /**
         * Unique string to be added to a MindBell intent to see if it has to be kept open.
         */
        val EXTRA_KEEP = "com.googlecode.mindbell.MindBell.Keep"

        /**
         * Unique string to be added to an Intent to see if MindBellMain is opened to stop meditation mode.
         */
        val EXTRA_STOP_MEDITATION = "com.googlecode.mindbell.MindBellMail.StopMeditation"

        fun isUseStandardBell(reminderBell: String): Boolean {
            return Integer.valueOf(reminderBell) != BELL_ENTRY_VALUE_INDEX_NO_SOUND
        }

        fun getDefaultReminderBellSoundUri(context: Context, isUseWorkaroundBell: Boolean): Uri {
            return getBellSoundUri(context, DEFAULT_REMINDER_BELL, isUseWorkaroundBell)!!
        }

        fun getBellSoundUri(context: Context, key: String, isUseWorkaroundBell: Boolean): Uri? {
            val arrayIdentifier = if (isUseWorkaroundBell) R.array.bellWorkaroundFilenameEntries else R.array.bellFilenameEntries
            val bellFilenameArray = context.resources.getStringArray(arrayIdentifier)
            val index = Integer.valueOf(key)!!
            if (index == BELL_ENTRY_VALUE_INDEX_NO_SOUND) {
                return null
            }
            val identifier = context.resources.getIdentifier(bellFilenameArray[index], "raw", context.packageName)
            return Utils.getResourceUri(context, identifier)
        }

        /**
         * Returns a patternOfPeriods string that corresponds with the numberOfPeriods: 1 -> "x", 2 -> "x, x", ...
         */
        fun derivePatternOfPeriods(numberOfPeriods: Int): String {
            val sb = StringBuilder()
            for (i in 0 until numberOfPeriods) {
                if (sb.length > 0) {
                    sb.append(", ")
                }
                sb.append("x")
            }
            return sb.toString()
        }

        /**
         * Returns the length of a specific meditation period in millis, or 6000xL in case of inconsistent arguments. The latter makes
         * MindBell more robust. If for any reason the patterns string is invalid the periods get one minute lengths instead of
         * experiencing exceptions.
         */
        fun derivePeriodMillis(patternOfPeriods: String, meditationDuration: Int, meditationPeriod: Int): Long {
            val periodIndex = meditationPeriod - 1
            val periods = patternOfPeriods.split(PERIOD_SEPARATOR_REGEX.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            // Verify the patternOfPeriods string and calculate the length of a variable period
            var numberOfVariablePeriods = 0
            var sumOfPeriodsLengths = 0
            for (i in periods.indices) {
                periods[i] = periods[i].trim({ it <= ' ' })
                val period = periods[i]
                if (period.matches(STATIC_PERIOD_REGEX.toRegex())) {
                    sumOfPeriodsLengths += Integer.valueOf(period)!!
                } else if (period.matches(VARIABLE_PERIOD_REGEX.toRegex())) {
                    numberOfVariablePeriods++
                } else {
                    return ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION
                }
            }
            if (numberOfVariablePeriods == 0) {
                return ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING
            }
            val millisOfVariablePeriods = (meditationDuration - sumOfPeriodsLengths) * ONE_MINUTE_MILLIS / numberOfVariablePeriods
            if (millisOfVariablePeriods < 0) {
                return ONE_MINUTE_MILLIS_NEGATIVE_PERIOD
            } else if (millisOfVariablePeriods < ONE_MINUTE_MILLIS) {
                return ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT
            }
            return if (periodIndex < 0 || periodIndex >= periods.size) {
                ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING // avoid IndexOutOfBoundsException
            } else if (periods[periodIndex].matches(STATIC_PERIOD_REGEX.toRegex())) {
                Integer.valueOf(periods[periodIndex])!! * ONE_MINUTE_MILLIS
            } else {
                millisOfVariablePeriods
            }
        }

        /**
         * Returns the given pattern string as an array of long values.
         */
        fun getVibrationPattern(pattern: String): LongArray {
            val msAsString = pattern.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val ms = LongArray(msAsString.size)
            for (i in ms.indices) {
                ms[i] = java.lang.Long.valueOf(msAsString[i])!!
            }
            return ms
        }

        /**
         * Returns true if the given normalize value means normalization is on.
         */
        fun isNormalize(normalizeValue: Int): Boolean {
            return normalizeValue >= 0
        }

        /**
         * Returns a numberOfPeriods string that corresponds with the patternOfPeriods: "x" -> 1, "3, x" -> 2, ...
         */
        fun deriveNumberOfPeriods(patternOfPeriods: String): Int {
            return patternOfPeriods.split(PERIOD_SEPARATOR_REGEX.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size
        }

        fun getAudioStream(audioStreamSetting: String): Int {
            when (audioStreamSetting) {
                "1" -> return AudioManager.STREAM_NOTIFICATION
                "2" -> return AudioManager.STREAM_MUSIC
                "0" -> return AudioManager.STREAM_ALARM
            // fall-thru to use "0" as default
                else -> return AudioManager.STREAM_ALARM
            }
        }
    }

}
