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
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.googlecode.mindbell.BuildConfig
import com.googlecode.mindbell.R
import com.googlecode.mindbell.R.string.*
import com.googlecode.mindbell.mission.Prefs.Preference.Type.*
import com.googlecode.mindbell.mission.model.Statistics
import com.googlecode.mindbell.mission.model.Statistics.StatisticsEntry
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.Utils
import java.util.*

/**
 * This singleton class gives access to all constants and preferences of MindBell.
 */
class Prefs private constructor(val context: Context) {

    private val settings: SharedPreferences = context
            .getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    private var sharedPreferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener

    private val weekdayEntryValues = context.resources.getStringArray(R.array.weekdayEntryValues)

    private val weekdayAbbreviationEntries = context.resources.getStringArray(R.array.weekdayAbbreviationEntries)

    // Map of all used preferences - with type and default value - by resid
    private val preferenceMap = TreeMap<Int, Preference>()

    // Map of allowed internal values for a string or string set preference by resid
    private val entryValuesMap = HashMap<Int, Array<String>>()

    private val interruptSettingsForRegularOperation = InterruptSettingsForRegularOperation()

    private val interruptSettingsForRingingOnce = InterruptSettingsForRingingOnce()

    private val interruptSettingsForMeditationBeginning = InterruptSettingsForMeditationBeginning()

    private val interruptSettingsForMeditationInterrupting = InterruptSettingsForMeditationInterrupting()

    private val interruptSettingsForMeditationEnding = InterruptSettingsForMeditationEnding()

    val vibrationPattern: LongArray
        get() = getVibrationPattern(pattern)

    private val pattern: String
        get() = getStringSetting(keyPattern)

    val isNormalize: Boolean
        get() = normalize >= 0

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

    val activeOnDaysOfWeekString: String
        get() {
            // Warning: Similar code in SettingsActivity#setMultiSelectListPreferenceSummary()
            val activeOnDaysOfWeek = activeOnDaysOfWeek
            val sb = StringBuilder()
            for (dayOfWeekValue in weekdayEntryValues) {
                val dayOfWeekValueAsInteger = Integer.valueOf(dayOfWeekValue) // internal weekday value in locale oriented order
                if (activeOnDaysOfWeek.contains(dayOfWeekValueAsInteger)) { // active on this day?
                    if (sb.isNotEmpty()) {
                        sb.append(", ")
                    }
                    sb.append(getWeekdayAbbreviation(dayOfWeekValueAsInteger!!)) // add day to the list of active days
                }
            }
            return sb.toString()
        }

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

    private var statistics: Statistics
        get() = parseStatistics(getStringSetting(keyStatistics)) ?: Statistics()
        set(value) = setSetting(keyStatistics, dumpStatistics(value))

    private val xmlMapper = ObjectMapper()

    init {

        xmlMapper.enableDefaultTyping()
        xmlMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY); // dump private fields, too

        // Define all preference keys and their default values
        fillPreferenceMap()

        // Map preference keys to their allowed entryValues
        fillEntryValuesMap()

        // Check that any data in the SharedPreferences are of the expected type
        checkSettings()

        // Register shared preferences change listener to update bell schedule accordingly
        sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            run {
                val preference = getPreference(key)
                if (preference.isUpdateBellScheduleForReminderOnChange) {
                    Log.d(TAG, "Setting for '${preference.key}' has been changed to '${getSetting(preference)}'")
                    Scheduler.getInstance(context).updateBellScheduleForReminder(true)
                }
            }
        }
        settings.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

    }

    /**
     * Return statics as String
     */
    fun getStatisticsString(): String {
        return statistics.toString()
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
     * Returns the preference with the given key
     *
     * @param key
     * @return
     */
    private fun getPreference(key: String): Preference {
        return preferenceMap.filterValues { preference -> preference.key == key }.values.first()
    }

    /**
     * Returns the current setting of the preference.
     *
     * @param preference
     * @return
     */
    private fun getSetting(preference: Preference): Any {
        @Suppress("UNCHECKED_CAST") // it's sure a string set
        return when (preference.type) {
            BOOLEAN -> settings.getBoolean(preference.key, preference.defaultValue as Boolean)
            FLOAT -> settings.getFloat(preference.key, preference.defaultValue as Float)
            INTEGER -> settings.getInt(preference.key, preference.defaultValue as Int)
            LONG -> settings.getLong(preference.key, preference.defaultValue as Long)
            STRING, STATISTICS_STRING, TIME_STRING -> settings.getString(preference.key, preference.defaultValue as String)
            STRING_SET -> settings.getStringSet(preference.key, preference.defaultValue as Set<String>)
        }
    }

    fun getMeditationPeriodMillis(meditationPeriod: Int): Long {
        return derivePeriodMillis(patternOfPeriods, meditationDuration.interval, meditationPeriod)
    }

    /**
     * Resets all settings by removing all shared preferences and setting all preferences to default values.
     */
    fun resetSettings() {
        settings.edit().clear().apply()
        checkSettings()
    }

    /**
     * Checks that any data in the SharedPreferences are of the expected type. Should we find anything that doesn't fit the
     * expectations, we delete it and recreate it with it's default value.
     */
    private fun checkSettings() {

        // Convert old settings from previous versions
        convertOldSettings()

        // Track whether a stacktrace shall be logged to find out the reason for sometimes deleted preferences
        var logStackTrace = false

        // Remove preference settings of wrong type and set non-existent preference to their default value
        for (preference in preferenceMap.values) {
            logStackTrace = removeInvalidSetting(preference) || logStackTrace
            logStackTrace = resetMissingSetting(preference) || logStackTrace
        }

        // Log stacktrace if a setting has been deleted or set to its default
        if (logStackTrace) {
            Log.w(TAG, "At least one setting has been deleted or reset to its default", Exception())
        } else {
            Log.d(TAG, "Preferences checked and found to be ok")
        }

        // Report all currently existing settings
        logSettings()
    }

    /**
     * Fill entryValuesMap with all preference keys and their allowed entryValues
     */
    private fun fillEntryValuesMap() {
        entryValuesMap[keyActiveOnDaysOfWeek] = context.resources.getStringArray(R.array.weekdayEntryValues)
        entryValuesMap[keyAudioStream] = context.resources.getStringArray(R.array.audioStreamPersistedEntryValues)
        entryValuesMap[keyMeditationBeginningBell] = context.resources.getStringArray(R.array.meditationBellEntryValues)
        entryValuesMap[keyMeditationEndingBell] = context.resources.getStringArray(R.array.meditationBellEntryValues)
        entryValuesMap[keyMeditationInterruptingBell] = context.resources.getStringArray(R.array.meditationBellEntryValues)
        entryValuesMap[keyNormalize] = context.resources.getStringArray(R.array.normalizeEntryValues)
        entryValuesMap[keyNotificationText] = arrayOf() // we cannot verify the entered notification text
        entryValuesMap[keyNotificationTitle] = arrayOf() // we cannot verify the entered notification title
        entryValuesMap[keyPattern] = context.resources.getStringArray(R.array.patternEntryValues)
        entryValuesMap[keyPatternOfPeriods] = arrayOf() // we cannot verify the entered notification text
        entryValuesMap[keyReminderBell] = context.resources.getStringArray(R.array.reminderBellEntryValues)
        entryValuesMap[keyRingtone] = arrayOf() // we don't need to know the possible ringtone values
    }

    /**
     * Fill preferenceMap with all preference keys and their default values.
     */
    private fun fillPreferenceMap() {
        addPreference(keyActive, false, BOOLEAN, false) // schedule updated by direct call
        addPreference(keyActiveOnDaysOfWeek, HashSet(Arrays.asList("1", "2", "3", "4", "5", "6", "7")),
                STRING_SET,
                true)
        addPreference(keyAudioStream, "0", STRING, false)
        addPreference(keyDismissNotification, false, BOOLEAN, false)
        addPreference(keyEnd, "21:00", TIME_STRING, true)
        addPreference(keyFrequency, "00:15", TIME_STRING, true) // 15 min
        addPreference(keyKeepScreenOn, true, BOOLEAN, false)
        addPreference(keyMeditating, false, BOOLEAN, false)
        addPreference(keyMeditationBeginningBell, "3", STRING, false)
        addPreference(keyMeditationDuration, "00:25", TIME_STRING, false)
        addPreference(keyMeditationEndingBell, "2", STRING, false)
        addPreference(keyMeditationEndingTimeMillis, -1L, LONG, false)
        addPreference(keyMeditationInterruptingBell, "1", STRING, false)
        addPreference(keyMeditationStartingTimeMillis, -1L, LONG, false)
        addPreference(keyMuteInFlightMode, false, BOOLEAN, false)
        addPreference(keyMuteOffHook, true, BOOLEAN, false)
        addPreference(keyMutedTill, -1L, LONG, false)
        addPreference(keyMuteWithAudioStream, true, BOOLEAN, false)
        addPreference(keyMuteWithPhone, true, BOOLEAN, false)
        addPreference(keyNormalize, NORMALIZE_NONE, STRING, true)
        addPreference(keyNoSoundOnMusic, false, BOOLEAN, false)
        addPreference(keyNotification, false, BOOLEAN, false)
        addPreference(keyNotificationText, context.getText(prefsNotificationTextDefault), STRING, false)
        addPreference(keyNotificationTitle, context.getText(prefsNotificationTitleDefault), STRING, false)
        addPreference(keyNotificationVisibilityPublic, true, BOOLEAN, false)
        addPreference(keyOriginalVolume, -1, INTEGER, false)
        addPreference(keyPattern, "100:200:100:600", STRING, false)
        addPreference(keyPatternOfPeriods, "x", STRING, false)
        addPreference(keyPauseAudioOnSound, false, BOOLEAN, false)
        addPreference(keyPopup, -1, INTEGER, false)
        addPreference(keyRampUpStartingTimeMillis, -1L, LONG, false)
        addPreference(keyRampUpTime, "00:30", TIME_STRING, false)
        addPreference(keyRandomize, true, BOOLEAN, true)
        addPreference(keyReminderBell, DEFAULT_REMINDER_BELL, STRING, false)
        addPreference(keyRingtone, "", STRING, false) // no useful default, code relies on <defaultValue>.isEmpty()
        addPreference(keyShow, true, BOOLEAN, false)
        addPreference(keySound, true, BOOLEAN, false)
        addPreference(keyStart, "09:00", TIME_STRING, true)
        addPreference(keyStartMeditationDirectly, false, BOOLEAN, false)
        addPreference(keyStatistics, dumpStatistics(Statistics()), STATISTICS_STRING, false)
        addPreference(keyStatus, false, BOOLEAN, true)
        addPreference(keyStatusIconMaterialDesign, true, BOOLEAN, true)
        addPreference(keyStatusVisibilityPublic, true, BOOLEAN, true)
        addPreference(keyStopMeditationAutomatically, false, BOOLEAN, false)
        addPreference(keyUseWorkaroundBell, false, BOOLEAN, false)
        addPreference(keyUseAudioStreamVolumeSetting, true, BOOLEAN, false)
        addPreference(keyVibrate, false, BOOLEAN, false)
        addPreference(keyVolume, DEFAULT_VOLUME, FLOAT, false)
        addPreference(keyMeditationVolume, volume, FLOAT, false) // for existing users: use standard volume as default here
    }

    /**
     * Write all currently existing settings to the log.
     */
    fun logSettings() {
        val sb = StringBuilder()
        sb.append("Effective settings:")
        val orderedSettings = TreeMap<String, Any>(settings.all)
        for ((key, value) in orderedSettings) {
            if (key != preferenceMap[keyStatistics]!!.key) {  // skip statistics in favor of logStatistics()
                sb.append("\n  ").append(key).append("=").append(value)
            }
        }
        Log.d(TAG, sb.toString())
    }

    /**
     * Write all currently existing statistics to the log.
     */
    fun logStatistics() {
        Log.d(TAG, statistics.toString())
    }

    /**
     * Puts a newly created Preference into the referenceMap with the given resid as key of the map.
     *
     * @param resid
     * @param defaultValue
     * @param type
     */
    private fun addPreference(resid: Int, defaultValue: Any, type: Preference.Type, isUpdateBellScheduleForReminderOnChange: Boolean) {
        preferenceMap[resid] = Preference(resid, context.getString(resid), defaultValue, type, isUpdateBellScheduleForReminderOnChange)
    }

    /**
     * Convert old settings from previous versions ... remove code after a while.
     */
    private fun convertOldSettings() {
        // Version 3.1.0 replaced numberOfPeriods by patternOfPeriods
        val keyNumberOfPeriods = "numberOfPeriods"
        val oldNumberOfPeriods = settings.getInt(keyNumberOfPeriods, 0)
        if (oldNumberOfPeriods > 0) {
            val patternOfPeriods = derivePatternOfPeriods(oldNumberOfPeriods)
            settings.edit().remove(keyNumberOfPeriods).apply()
            Log
                    .w(TAG, "Converted old setting for '$keyNumberOfPeriods' ($oldNumberOfPeriods) to '${context.getText(keyPatternOfPeriods)}' ($patternOfPeriods)")
        }
        // Version 3.2.0 replaces frequency milliseconds string by time string
        val keyFrequency = context.getString(R.string.keyFrequency)
        val oldFrequency = settings.getString(keyFrequency, null)
        if (oldFrequency != null && !oldFrequency.contains(":")) {
            try {
                val milliseconds = java.lang.Long.parseLong(oldFrequency)
                val frequency = TimeOfDay.fromMillisecondsInterval(milliseconds).persistString
                setSetting(R.string.keyFrequency, frequency)
                Log.w(TAG, "Converted old value for '$keyFrequency' from '$oldFrequency' to '$frequency'")
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
                Log.w(TAG, "Converted old value for '$keyRampUpTime' from '$oldRampUpTime' to '$rampUpTime'")
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
                Log
                        .w(TAG, "Converted old value for '$keyMeditationDuration' from '$oldMeditationDuration' to '$meditationDuration'")
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
            Log
                    .w(TAG, "Converted old setting for '$keyStatusVisiblityPublic' ($statusVisibilityPublic) to '${context.getText(keyStatusVisibilityPublic)}' ($statusVisibilityPublic)")
        }
        // Version 3.2.5 introduced keyUseAudioStreamVolumeSetting (default true) but should be false for users of older versions
        if (!settings.contains(context.getText(keyUseAudioStreamVolumeSetting).toString()) && settings.contains(context.getText(keyActive).toString())) {
            val useAudioStreamVolumeSetting = false
            setSetting(keyUseAudioStreamVolumeSetting, useAudioStreamVolumeSetting)
            Log
                    .w(TAG, "Created setting for '${context.getText(keyUseAudioStreamVolumeSetting)}' with non-default ($useAudioStreamVolumeSetting) because an older version was already installed")
        }
        // Version 3.2.6 replaced useStandardBell by reminderBell
        val keyUseStandardBell = "useStandardBell"
        if (settings.contains(keyUseStandardBell)) {
            val useStandardBell = settings.getBoolean(keyUseStandardBell, java.lang.Boolean.TRUE)
            val reminderBell = if (useStandardBell) DEFAULT_REMINDER_BELL else BELL_ENTRY_VALUE_INDEX_NO_SOUND.toString()
            setSetting(keyReminderBell, reminderBell)
            settings.edit().remove(keyUseStandardBell).apply()
            Log
                    .w(TAG, "Converted old setting for '$keyUseStandardBell' ($useStandardBell) to '${context.getText(keyReminderBell)}' ($reminderBell)")
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
                            Log.w(TAG, "Removed setting '$preference' since it had wrong value '$stringValue'")
                            return true
                        }
                    }
                }
                STRING_SET -> {
                    @Suppress("UNCHECKED_CAST") // it's sure a string set
                    val stringSetValue = value as Set<String>?
                    if (stringSetValue != null) {
                        for (aStringInSet in stringSetValue) {
                            val entryValues = Arrays.asList(*entryValuesMap[preference.resid]!!)
                            if (!entryValues.contains(aStringInSet)) {
                                settings.edit().remove(preference.key).apply()
                                Log.w(TAG, "Removed setting '$preference' since it had (at least one) wrong value '$aStringInSet'")
                                return true
                            }
                        }
                    }
                }
                STATISTICS_STRING -> {
                    val statisticsStringValue = value as String?
                    if (statisticsStringValue != null) {
                        if (parseStatistics(statisticsStringValue) == null) {
                            settings.edit().remove(preference.key).apply()
                            Log.w(TAG, "Removed setting '$preference' since it is not a statistics string " +
                                    "'$statisticsStringValue'")
                            return true
                        }
                    }
                }
                TIME_STRING -> {
                    val timeStringValue = value as String?
                    if (timeStringValue != null) {
                        if (!timeStringValue.matches(TIME_STRING_REGEX.toRegex())) {
                            settings.edit().remove(preference.key).apply()
                            Log.w(TAG, "Removed setting '$preference' since it is not a time string '$timeStringValue'")
                            return true
                        }
                    }
                }
            }// no more to do, retrieving the value is all that can be done
            return false
        } catch (e: ClassCastException) {
            settings.edit().remove(preference.key).apply()
            Log.w(TAG, "Removed setting '$preference' since it had wrong type: $e")
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
            Log.w(TAG, "Reset missing setting for '${preference.key}' to '${preference.defaultValue}'")
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
        @Suppress("UNCHECKED_CAST") // it's sure a string set
        when (preference.type) {
            BOOLEAN -> settings.edit().putBoolean(preference.key, value as Boolean).apply()
            FLOAT -> settings.edit().putFloat(preference.key, value as Float).apply()
            INTEGER -> settings.edit().putInt(preference.key, value as Int).apply()
            LONG -> settings.edit().putLong(preference.key, value as Long).apply()
            STRING, STATISTICS_STRING, TIME_STRING -> settings.edit().putString(preference.key, value as String).apply()
            STRING_SET -> settings.edit().putStringSet(preference.key, value as Set<String>).apply()
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
        @Suppress("UNCHECKED_CAST") // it's sure a string set
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
    fun getReminderSoundUri(): Uri? {
        // This implementation is almost the same as SettingsActivity#getReminderSoundUri()
        var soundUri = getReminderBellSoundUri()
        if (soundUri == null) { // use system notification ringtone if reminder bell sound is not set
            val ringtone = ringtone
            if (ringtone.isEmpty()) {
                soundUri = getDefaultReminderBellSoundUri()
            } else {
                return Uri.parse(ringtone)
            }
        }
        return soundUri
    }

    private fun getReminderBellSoundUri(): Uri? {
        return getBellSoundUri(getStringSetting(keyReminderBell))
    }

    fun getDefaultReminderBellSoundUri(): Uri? {
        return getBellSoundUri(DEFAULT_REMINDER_BELL)
    }

    fun getBellSoundUri(key: String): Uri? {
        return getBellSoundUri(key, isUseWorkaroundBell)
    }

    fun useStatusIconMaterialDesign(): Boolean {
        return getBooleanSetting(keyStatusIconMaterialDesign)
    }

    fun getDefaultReminderBellSoundUri(isUseWorkaroundBell: Boolean): Uri {
        return getBellSoundUri(DEFAULT_REMINDER_BELL, isUseWorkaroundBell)!!
    }

    fun getBellSoundUri(key: String, isUseWorkaroundBell: Boolean): Uri? {
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

    fun resetStatistics() {
        resetSetting(keyStatistics)
    }

    fun addStatisticsEntry(newEntry: StatisticsEntry) {
        val newStatistics = statistics
        while (newStatistics.entryList.size >= MAX_STATISTICS_ENTRY_COUNT) {
            newStatistics.entryList.removeAt(0)
        }
        newStatistics.entryList.add(newEntry)
        statistics = newStatistics
        Log.d(TAG, "StatisticsEntry added: $newEntry")
    }

    private fun parseStatistics(statisticsString: String): Statistics? {
        try {
            return xmlMapper.readValue<Statistics>(statisticsString, Statistics::class.java)
        } catch (e: JsonProcessingException) {
            Log.d(TAG, "Parsing $statisticsString failed", e)
            return null
        }
    }

    private fun dumpStatistics(newStatistics: Statistics): String {
        return xmlMapper.writeValueAsString(newStatistics)
    }

    fun forRegularOperation(): InterruptSettings {
        return interruptSettingsForRegularOperation
    }

    fun forRingingOnce(): InterruptSettings {
        return interruptSettingsForRingingOnce
    }

    fun forMeditationBeginning(): InterruptSettings {
        return interruptSettingsForMeditationBeginning
    }

    fun forMeditationInterrupting(): InterruptSettings {
        return interruptSettingsForMeditationInterrupting
    }

    fun forMeditationEnding(): InterruptSettings {
        return interruptSettingsForMeditationEnding
    }

    internal class Preference(val resid: Int, val key: String, val defaultValue: Any, val type: Type, val
    isUpdateBellScheduleForReminderOnChange: Boolean) {

        internal enum class Type {
            BOOLEAN, FLOAT, INTEGER, LONG, STRING, STRING_SET, STATISTICS_STRING, TIME_STRING
        }

    }

    private inner class InterruptSettingsForRegularOperation : InterruptSettings {

        override val isShow: Boolean
            get() = this@Prefs.isShow

        override val isSound: Boolean
            get() = this@Prefs.isSound

        override val isVibrate: Boolean
            get() = this@Prefs.isVibrate

        override val volume: Float
            get() = this@Prefs.volume

        override val isNotification: Boolean
            get() = this@Prefs.isNotification

        override val isDismissNotification: Boolean
            get() = this@Prefs.isDismissNotification

        override val soundUri: Uri?
            get() = this@Prefs.getReminderSoundUri()

    }

    private inner class InterruptSettingsForRingingOnce : InterruptSettings {

        override val isShow: Boolean
            get() = false

        override val isSound: Boolean
            get() = true

        override val isVibrate: Boolean
            get() = false

        override val volume: Float
            get() = this@Prefs.volume

        override val isNotification: Boolean
            get() = false

        override val isDismissNotification: Boolean
            get() = false

        override val soundUri: Uri?
            get() = this@Prefs.getReminderSoundUri()

    }

    private abstract inner class InterruptSettingsForMeditation : InterruptSettings {

        override val isShow: Boolean
            get() = false

        override val isSound: Boolean
            get() = true

        override val isVibrate: Boolean
            get() = false

        override val volume: Float
            get() = this@Prefs.meditationVolume

        override val isNotification: Boolean
            get() = false

        override val isDismissNotification: Boolean
            get() = false

    }

    private inner class InterruptSettingsForMeditationBeginning : InterruptSettingsForMeditation() {

        override val soundUri: Uri?
            get() = if (isStartMeditationDirectly) null else this@Prefs.getBellSoundUri(meditationBeginningBell)

    }

    private inner class InterruptSettingsForMeditationInterrupting : InterruptSettingsForMeditation() {

        override val soundUri: Uri?
            get() = this@Prefs.getBellSoundUri(meditationInterruptingBell)

    }

    private inner class InterruptSettingsForMeditationEnding : InterruptSettingsForMeditation() {

        override val soundUri: Uri?
            get() = this@Prefs.getBellSoundUri(meditationEndingBell)

    }

    companion object {

        /**
         * Tag to be used for logging.
         */
        const val TAG = "MindBell"

        /**
         * One minute in milliseconds.
         */
        const val ONE_MINUTE_MILLIS = 60000L

        /**
         * One minute in milliseconds plus an error indicator millisecond value.
         */
        const val ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION = ONE_MINUTE_MILLIS + 1L
        const val ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING = ONE_MINUTE_MILLIS + 2L
        const val ONE_MINUTE_MILLIS_NEGATIVE_PERIOD = ONE_MINUTE_MILLIS + 3L
        const val ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT = ONE_MINUTE_MILLIS + 4L
        const val ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING = ONE_MINUTE_MILLIS + 5L

        /**
         * Regular expressions to verify a pattern of periods string.
         */
        private const val STATIC_PERIOD_REGEX = "([1-9][0-9]{0,2})"
        private const val VARIABLE_PERIOD_REGEX = "(x)"
        private const val PERIOD_SEPARATOR = ","
        private const val PERIOD_SEPARATOR_REGEX = PERIOD_SEPARATOR
        const val PERIOD_SEPARATOR_WITH_BLANKS_REGEX = " *$PERIOD_SEPARATOR_REGEX *"
        const val PERIOD_SEPARATOR_WITH_BLANK = ", "

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
        const val WAITING_TIME = 10000L

        const val NORMALIZE_NONE = "-1"

        const val DEFAULT_VOLUME = 0.501187234f

        /**
         * Regular expression to verify a time string preference
         */
        private const val TIME_STRING_REGEX = "\\d?\\d(:\\d\\d)?"

        /**
         * Index of entry in bellEntryValues that represents the 'use system notification ringtone' setting.
         */
        private const val BELL_ENTRY_VALUE_INDEX_NO_SOUND = 0

        /**
         * Default reminder bell sound, also used in case the set sound uri is not accessible.
         */
        private const val DEFAULT_REMINDER_BELL = "1"

        /**
         * Unique string to be added to a Scheduling Intent to see which meditation period the bell is in.
         */
        const val EXTRA_MEDITATION_PERIOD = "com.googlecode.mindbell.service.InterruptService.MeditationPeriod"

        /**
         * Unique string to be added to a Scheduling Intent to see who sent it.
         */
        const val EXTRA_IS_RESCHEDULING = "com.googlecode.mindbell.service.InterruptService.IsRescheduling"

        /**
         * Unique string to be added to a Scheduling Intent to see for which time the bell was scheduled.
         */
        const val EXTRA_NOW_TIME_MILLIS = "com.googlecode.mindbell.service.InterruptService.NowTimeMillis"

        /**
         * Unique string to be added to a MindBell intent to see if it has to be kept open.
         */
        const val EXTRA_KEEP = "com.googlecode.mindbell.activity.ReminderShowActivity.Keep"

        /**
         * Unique string to be added to an Intent to see if MainActivity is opened to stop meditation mode.
         */
        const val EXTRA_STOP_MEDITATION = "com.googlecode.mindbell.MindBellMail.StopMeditation"

        /**
         * IDs for notification channels created by MindBell.
         */
        const val STATUS_NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.status"
        const val INTERRUPT_NOTIFICATION_CHANNEL_ID = "${BuildConfig.APPLICATION_ID}.interrupt"

        /**
         * IDs for notifications created by MindBell.
         */
        const val STATUS_NOTIFICATION_ID = 0x7f030001 // historically, has been R.layout.bell for a long time
        const val INTERRUPT_NOTIFICATION_ID = STATUS_NOTIFICATION_ID + 1

        /**
         * Request codes for intents created by MindBell.
         */
        const val SCHEDULER_REQUEST_CODE = 0
        const val UPDATE_STATUS_NOTIFICATION_REQUEST_CODE = 1
        const val UPDATE_STATUS_NOTIFICATION_MUTED_TILL_REQUEST_CODE = 2
        const val UPDATE_STATUS_NOTIFICATION_DAY_NIGHT_REQUEST_CODE = 3
        const val REQUEST_CODE_STATUS = 10
        const val REQUEST_CODE_MUTE_OFF_HOOK = 11
        const val REQUEST_CODE_RINGTONE = 12

        /**
         * Maximum number of statistics entries to be stored in preferences.
         */
        const val MAX_STATISTICS_ENTRY_COUNT = 50

        /*
         * The one and only instance of this class.
         */
        @SuppressLint("StaticFieldLeak") // it's fine to hold an ApplicationContext: https://stackoverflow.com/a/39841446
        @Volatile
        private var instance: Prefs? = null

        /*
         * Returns the one and only instance of this class.
         */
        @Synchronized
        fun getInstance(context: Context): Prefs {
            if (instance == null) {
                instance = Prefs(context.applicationContext)
            }
            return instance!!
        }

        fun isUseStandardBell(reminderBell: String): Boolean {
            return Integer.valueOf(reminderBell) != BELL_ENTRY_VALUE_INDEX_NO_SOUND
        }

        /**
         * Returns a patternOfPeriods string that corresponds with the numberOfPeriods: 1 -> "x", 2 -> "x, x", ...
         */
        fun derivePatternOfPeriods(numberOfPeriods: Int): String {
            val sb = StringBuilder()
            for (i in 0 until numberOfPeriods) {
                if (sb.isNotEmpty()) {
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
                when {
                    period.matches(STATIC_PERIOD_REGEX.toRegex()) -> sumOfPeriodsLengths += Integer.valueOf(period)
                    period.matches(VARIABLE_PERIOD_REGEX.toRegex()) -> numberOfVariablePeriods++
                    else -> return ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION
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
         * Returns a numberOfPeriods string that corresponds with the patternOfPeriods: "x" -> 1, "3, x" -> 2, ...
         */
        fun deriveNumberOfPeriods(patternOfPeriods: String): Int {
            return patternOfPeriods.split(PERIOD_SEPARATOR_REGEX.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray().size
        }

        fun getAudioStream(audioStreamSetting: String): Int {
            return when (audioStreamSetting) {
                "1" -> AudioManager.STREAM_NOTIFICATION
                "2" -> AudioManager.STREAM_MUSIC
                "0" -> AudioManager.STREAM_ALARM
            // fall-thru to use "0" as default
                else -> AudioManager.STREAM_ALARM
            }
        }
    }

}
