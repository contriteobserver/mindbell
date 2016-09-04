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
package com.googlecode.mindbell.accessors;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.googlecode.mindbell.MindBell;
import com.googlecode.mindbell.R;
import com.googlecode.mindbell.util.TimeOfDay;
import com.googlecode.mindbell.util.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

public class AndroidPrefsAccessor extends PrefsAccessor {

    public static final String NORMALIZE_NONE = "-1";

    private final SharedPreferences settings;
    private final String keyPopup;
    private final String keyActive;
    private final String keyMeditating;

    private final String keyUseStandardBell;
    private final String keyRingtone;
    private final String keyShow;
    private final String keySound;
    private final String keyStatus;
    private final String keyStatusVisibilityPublic;
    private final String keyStatusIconMaterialDesign;
    private final String keyMuteInFlightMode;
    private final String keyMuteOffHook;
    private final String keyMuteWithPhone;
    private final String keyVibrate;
    private final String keyPattern;

    private final String keyFrequency;
    private final String keyRandomize;
    private final String keyNormalize;
    private final String keyStart;
    private final String keyEnd;
    private final String keyActiveOnDaysOfWeek;

    private final String keyRampUpTime;
    private final String keyNumberOfPeriods;
    private final String keyMeditationDuration;
    private final String keyRampUpStartingTimeMillis;
    private final String keyMeditationStartingTimeMillis;
    private final String keyMeditationEndingTimeMillis;

    private final String keyVolume;

    /**
     * Preference default values ... must correspond with settings in xml definitions
     */
    private final int defaultPopup = -1;
    private final boolean defaultActive = false;
    private final boolean defaultMeditating = false;
    private final boolean defaultShow = true;
    private final boolean defaultSound = true;
    private final boolean defaultUseStandardBell = true;
    private final boolean defaultStatus = false;
    private final boolean defaultStatusVisibilityPublic = true;
    private final boolean defaultStatusIconMaterialDesign = true;
    private final boolean defaultMuteInFlightMode = false;
    private final boolean defaultMuteOffHook = true;
    private final boolean defaultMuteWithPhone = true;
    private final boolean defaultVibrate = false;
    // private final String defaultRingtone = ""; // there is no really useful default ringtone
    private final String defaultPattern = "100:200:100:600";
    private final String defaultFrequency = "3600000";
    private final boolean defaultRandomize = true;
    private final String defaultNormalize = NORMALIZE_NONE;
    private final String defaultStart = "9";
    private final String defaultEnd = "21";
    private final String defaultRampUpTime = "30"; // seconds
    private final String defaultNumberOfPeriods = "1";
    private final String defaultMeditationDuration = "25"; // minutes
    private final long defaultRampUpStartingTimeMillis = -1;
    private final long defaultMeditationStartingTimeMillis = -1;
    private final long defaultMeditationEndingTimeMillis = -1;
    private final Set<String> defaultActiveOnDaysOfWeek = new HashSet<>(
            Arrays.asList(new String[]{"1", "2", "3", "4", "5", "6", "7"})); // every day
    private final float defaultVolume = AndroidContextAccessor.MINUS_SIX_DB;

    private final String[] weekdayEntryValues;
    private final String[] hourEntries;
    private final String[] weekdayAbbreviationEntries;

    private final Uri bellRessourceUri;

    /**
     * Constructs an accessor for preferences in the given context, please use {@link AndroidContextAccessor#getPrefs()} instead
     * of calling this directly.
     */
    protected AndroidPrefsAccessor(Context context) {
        // From target SDK version 11 (HONEYCOMB) upwards changes made in the settings dialog do not arrive in
        // UpdateStatusNotification if MODE_MULTI_PROCESS is not set, see API docs for MODE_MULTI_PROCESS.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.settings = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_MULTI_PROCESS);
        } else {
            this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        }

        // Define all preference keys
        keyPopup = context.getString(R.string.keyPopup);
        keyActive = context.getString(R.string.keyActive);
        keyMeditating = context.getString(R.string.keyMeditating);
        keyShow = context.getString(R.string.keyShow);
        keySound = context.getString(R.string.keySound);
        keyUseStandardBell = context.getString(R.string.keyUseStandardBell);
        keyRingtone = context.getString(R.string.keyRingtone);
        keyStatus = context.getString(R.string.keyStatus);
        keyStatusVisibilityPublic = context.getString(R.string.keyStatusVisibilityPublic);
        keyStatusIconMaterialDesign = context.getString(R.string.keyStatusIconMaterialDesign);
        keyMuteInFlightMode = context.getString(R.string.keyMuteInFlightMode);
        keyMuteOffHook = context.getString(R.string.keyMuteOffHook);
        keyMuteWithPhone = context.getString(R.string.keyMuteWithPhone);
        keyVolume = context.getString(R.string.keyVolume);
        keyVibrate = context.getString(R.string.keyVibrate);
        keyPattern = context.getString(R.string.keyPattern);
        keyFrequency = context.getString(R.string.keyFrequency);
        keyRandomize = context.getString(R.string.keyRandomize);
        keyNormalize = context.getString(R.string.keyNormalize);
        keyStart = context.getString(R.string.keyStart);
        keyEnd = context.getString(R.string.keyEnd);
        keyRampUpTime = context.getString(R.string.keyRampUpTime);
        keyNumberOfPeriods = context.getString(R.string.keyNumberOfPeriods);
        keyMeditationDuration = context.getString(R.string.keyMeditationDuration);
        keyRampUpStartingTimeMillis = context.getString(R.string.keyRampUpStartingTimeMillis);
        keyMeditationStartingTimeMillis = context.getString(R.string.keyMeditationStartingTimeMillis);
        keyMeditationEndingTimeMillis = context.getString(R.string.keyMeditationEndingTimeMillis);
        keyActiveOnDaysOfWeek = context.getString(R.string.keyActiveOnDaysOfWeek);

        // Define entries and entry values
        hourEntries = context.getResources().getStringArray(R.array.hourEntries);
        weekdayEntryValues = context.getResources().getStringArray(R.array.weekdayEntryValues);
        weekdayAbbreviationEntries = context.getResources().getStringArray(R.array.weekdayAbbreviationEntries);

        bellRessourceUri = Utils.getResourceUri(context, R.raw.bell10s);

    }

    /**
     * Checks that any data in the SharedPreferences are of the expected type. Should we find anything that doesn't fit the
     * expectations, we delete it and recreate it with it's default value.
     */
    public void checkSettings(Context context, boolean logSettings) {
        // Map preference keys to their allowed entryValues
        final Map<String, String[]> entryValuesMap = new HashMap<>();
        entryValuesMap.put(keyRingtone, new String[] {}); // we don't need to know the possible ringtone values
        entryValuesMap.put(keyPattern, context.getResources().getStringArray(R.array.patternEntryValues));
        entryValuesMap.put(keyFrequency, context.getResources().getStringArray(R.array.frequencyEntryValues));
        entryValuesMap.put(keyNormalize, context.getResources().getStringArray(R.array.normalizeEntryValues));
        entryValuesMap.put(keyStart, context.getResources().getStringArray(R.array.hourEntryValues));
        entryValuesMap.put(keyEnd, context.getResources().getStringArray(R.array.hourEntryValues));
        entryValuesMap.put(keyRampUpTime, new String[] {}); // FIXME dkn Wie das prüfen mit NumberPickers
        entryValuesMap.put(keyNumberOfPeriods, new String[] {}); // FIXME dkn Wie das prüfen mit NumberPickers
        entryValuesMap.put(keyMeditationDuration, new String[] {}); // FIXME dkn Wie das prüfen mit NumberPickers
        entryValuesMap.put(keyActiveOnDaysOfWeek, context.getResources().getStringArray(R.array.weekdayEntryValues));

        // Track wheter a stacktrace shall be logged to find out the reason for sometimes deleted preferences
        boolean logStackTrace = false;

        // Check boolean settings
        String[] booleanSettings = new String[] { keyShow, keySound, keyUseStandardBell, keyStatus, keyMeditating, keyStatusVisibilityPublic, keyStatusIconMaterialDesign,
                keyActive, keyMuteInFlightMode, keyMuteOffHook, keyMuteWithPhone, keyVibrate, keyRandomize };
        for (String key : booleanSettings) {
            try {
                settings.getBoolean(key, false);
            } catch (ClassCastException e) {
                settings.edit().remove(key).apply();
                Log.w(TAG, "Removed setting '" + key + "' since it had wrong type");
                logStackTrace = true;
            }
        }
        // Check string settings
        String[] stringSettings = new String[] { keyRingtone, keyPattern, keyFrequency, keyNormalize, keyStart, keyEnd, keyRampUpTime, keyNumberOfPeriods, keyMeditationDuration };
        for (String key : stringSettings) {
            try {
                String value = settings.getString(key, null);
                if (value != null) {
                    List<String> entryValues = Arrays.asList(entryValuesMap.get(key));
                    if (entryValues != null && !entryValues.isEmpty() && !entryValues.contains(value)) {
                        settings.edit().remove(key).apply();
                        Log.w(TAG, "Removed setting '" + key + "' since it had wrong value '" + value + "'");
                        logStackTrace = true;
                    }
                }
            } catch (ClassCastException e) {
                settings.edit().remove(key).apply();
                Log.w(TAG, "Removed setting '" + key + "' since it had wrong type");
                logStackTrace = true;
            }
        }
        // Check string set settings
        String[] stringSetSettings = new String[] { keyActiveOnDaysOfWeek };
        for (String key : stringSetSettings) {
            try {
                Set<String> valueSet = settings.getStringSet(key, null);
                if (valueSet != null) {
                    for (String value : valueSet) {
                        List<String> entryValues = Arrays.asList(entryValuesMap.get(key));
                        if (value != null && !entryValues.contains(value)) {
                            settings.edit().remove(key).apply();
                            Log.w(TAG, "Removed setting '" + key + "' since it had (at least one) wrong value '" + value + "'");
                            logStackTrace = true;
                            break;
                        }
                    }
                }
            } catch (ClassCastException e) {
                settings.edit().remove(key).apply();
                Log.w(TAG, "Removed setting '" + key + "' since it had wrong type");
                logStackTrace = true;
            }
        }
        // Check int settings
        String[] intSettings = new String[] { keyPopup };
        for (String key : intSettings) {
            try {
                settings.getInt(key, 0);
            } catch (ClassCastException e) {
                settings.edit().remove(key).apply();
                Log.w(TAG, "Removed setting '" + key + "' since it had wrong type");
                logStackTrace = true;
            }
        }
        // Check long settings
        String[] longSettings = new String[] { keyRampUpStartingTimeMillis, keyMeditationStartingTimeMillis, keyMeditationEndingTimeMillis };
        for (String key : longSettings) {
            try {
                settings.getLong(key, 0);
            } catch (ClassCastException e) {
                settings.edit().remove(key).apply();
                Log.w(TAG, "Removed setting '" + key + "' since it had wrong type");
                logStackTrace = true;
            }
        }
        // Check float settings
        String[] floatSettings = new String[] { keyVolume };
        for (String key : floatSettings) {
            try {
                settings.getFloat(key, 0);
            } catch (ClassCastException e) {
                settings.edit().remove(key).apply();
                Log.w(TAG, "Removed setting '" + key + "' since it had wrong type");
                logStackTrace = true;
            }
        }

        // Check frequency as it may never fall to a too low value
        String frequencyString = settings.getString(keyFrequency, null);
        if (frequencyString != null) {
            try {
                long interval = Long.valueOf(frequencyString);
                if (interval < 1 * 60000) { // less than one minute
                    settings.edit().remove(keyFrequency).apply();
                    Log.w(TAG, "Removed setting '" + keyFrequency + "' since value '" + frequencyString + "' was too low");
                    logStackTrace = true;
                }
            } catch (NumberFormatException e) {
                settings.edit().remove(keyFrequency).apply();
                Log.w(TAG, "Removed setting '" + keyFrequency + "' since value '" + frequencyString + "' is not a number");
                logStackTrace = true;
            }
        }

        // Set default values for those preferences that are missing
        if (!settings.contains(keyPopup)) {
            setPopup(defaultPopup);
            Log.w(TAG, "Reset missing setting for '" + keyPopup + "' to '" + defaultPopup + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyActive)) {
            isActive(defaultActive);
            Log.w(TAG, "Reset missing setting for '" + keyActive + "' to '" + defaultActive + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMeditating)) {
            isMeditating(defaultMeditating);
            Log.w(TAG, "Reset missing setting for '" + keyMeditating + "' to '" + defaultMeditating + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyShow)) {
            settings.edit().putBoolean(keyShow, defaultShow).apply();
            Log.w(TAG, "Reset missing setting for '" + keyShow + "' to '" + defaultShow + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keySound)) {
            settings.edit().putBoolean(keySound, defaultSound).apply();
            Log.w(TAG, "Reset missing setting for '" + keySound + "' to '" + defaultSound + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyUseStandardBell)) {
            settings.edit().putBoolean(keyUseStandardBell, defaultUseStandardBell).apply();
            Log.w(TAG, "Reset missing setting for '" + keyUseStandardBell + "' to '" + defaultUseStandardBell + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyStatus)) {
            settings.edit().putBoolean(keyStatus, defaultStatus).apply();
            Log.w(TAG, "Reset missing setting for '" + keyStatus + "' to '" + defaultStatus + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyStatusVisibilityPublic)) {
            settings.edit().putBoolean(keyStatusVisibilityPublic, defaultStatusVisibilityPublic).apply();
            Log.w(TAG,
                    "Reset missing setting for '" + keyStatusVisibilityPublic + "' to '" + defaultStatusVisibilityPublic + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyStatusIconMaterialDesign)) {
            settings.edit().putBoolean(keyStatusIconMaterialDesign, defaultStatusIconMaterialDesign).apply();
            Log.w(TAG, "Reset missing setting for '" + keyStatusIconMaterialDesign + "' to '" + defaultStatusIconMaterialDesign
                    + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMuteInFlightMode)) {
            settings.edit().putBoolean(keyMuteInFlightMode, defaultMuteInFlightMode).apply();
            Log.w(TAG, "Reset missing setting for '" + keyMuteInFlightMode + "' to '" + defaultMuteInFlightMode + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMuteOffHook)) {
            settings.edit().putBoolean(keyMuteOffHook, defaultMuteOffHook).apply();
            Log.w(TAG, "Reset missing setting for '" + keyMuteOffHook + "' to '" + defaultMuteOffHook + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMuteWithPhone)) {
            settings.edit().putBoolean(keyMuteWithPhone, defaultMuteWithPhone).apply();
            Log.w(TAG, "Reset missing setting for '" + keyMuteWithPhone + "' to '" + defaultMuteWithPhone + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyVibrate)) {
            settings.edit().putBoolean(keyVibrate, defaultVibrate).apply();
            Log.w(TAG, "Reset missing setting for '" + keyVibrate + "' to '" + defaultVibrate + "'");
            logStackTrace = true;
        }
        // due to lack of a useful default ringtone the preference might be null, see getSoundUri()
//        if (!settings.contains(keyRingtone)) {
//            settings.edit().putString(keyRingtone, defaultRingtone).apply();
//            Log.w(TAG, "Reset missing setting for '" + keyRingtone + "' to '" + defaultRingtone + "'");
//        }
        if (!settings.contains(keyPattern)) {
            settings.edit().putString(keyPattern, defaultPattern).apply();
            Log.w(TAG, "Reset missing setting for '" + keyPattern + "' to '" + defaultPattern + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyFrequency)) {
            settings.edit().putString(keyFrequency, defaultFrequency).apply();
            Log.w(TAG, "Reset missing setting for '" + keyFrequency + "' to '" + defaultFrequency + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyRandomize)) {
            settings.edit().putBoolean(keyRandomize, defaultRandomize).apply();
            Log.w(TAG, "Reset missing setting for '" + keyRandomize + "' to '" + defaultRandomize + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyNormalize)) {
            settings.edit().putString(keyNormalize, defaultNormalize).apply();
            Log.w(TAG, "Reset missing setting for '" + keyNormalize + "' to '" + defaultNormalize + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyStart)) {
            settings.edit().putString(keyStart, defaultStart).apply();
            Log.w(TAG, "Reset missing setting for '" + keyStart + "' to '" + defaultStart + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyEnd)) {
            settings.edit().putString(keyEnd, defaultEnd).apply();
            Log.w(TAG, "Reset missing setting for '" + keyEnd + "' to '" + defaultEnd + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyRampUpTime)) {
            settings.edit().putString(keyRampUpTime, defaultRampUpTime).apply();
            Log.w(TAG, "Reset missing setting for '" + keyRampUpTime + "' to '" + defaultRampUpTime + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyNumberOfPeriods)) {
            settings.edit().putString(keyNumberOfPeriods, defaultNumberOfPeriods).apply();
            Log.w(TAG, "Reset missing setting for '" + keyNumberOfPeriods + "' to '" + defaultNumberOfPeriods + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMeditationDuration)) {
            settings.edit().putString(keyMeditationDuration, defaultMeditationDuration).apply();
            Log.w(TAG, "Reset missing setting for '" + keyMeditationDuration + "' to '" + defaultMeditationDuration + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyRampUpStartingTimeMillis)) {
            settings.edit().putLong(keyRampUpStartingTimeMillis, defaultRampUpStartingTimeMillis).apply();
            Log.w(TAG, "Reset missing setting for '" + keyRampUpStartingTimeMillis + "' to '" + defaultRampUpStartingTimeMillis + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMeditationStartingTimeMillis)) {
            settings.edit().putLong(keyMeditationStartingTimeMillis, defaultMeditationStartingTimeMillis).apply();
            Log.w(TAG, "Reset missing setting for '" + keyMeditationStartingTimeMillis + "' to '" + defaultMeditationStartingTimeMillis + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyMeditationEndingTimeMillis)) {
            settings.edit().putLong(keyMeditationEndingTimeMillis, defaultMeditationEndingTimeMillis).apply();
            Log.w(TAG, "Reset missing setting for '" + keyMeditationEndingTimeMillis + "' to '" + defaultMeditationEndingTimeMillis + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyActiveOnDaysOfWeek)) {
            settings.edit().putStringSet(keyActiveOnDaysOfWeek, defaultActiveOnDaysOfWeek).apply();
            Log.w(TAG, "Reset missing setting for '" + keyActiveOnDaysOfWeek + "' to '" + defaultActiveOnDaysOfWeek + "'");
            logStackTrace = true;
        }
        if (!settings.contains(keyVolume)) {
            settings.edit().putFloat(keyVolume, defaultVolume).apply();
            Log.w(TAG, "Reset missing setting for '" + keyVolume + "' to '" + defaultVolume + "'");
            logStackTrace = true;
        }
        // Log stacktrace if a setting has been deleted or set to its default
        if (logStackTrace) {
            Log.w(TAG, new Exception("At least one setting has been deleted or reset to its default"));
        }
        // Finally report the settings if requested
        if (logSettings) {
            StringBuilder sb = new StringBuilder();
            sb.append("Effective settings: ");
            for (String s : booleanSettings) {
                sb.append(s).append("=").append(settings.getBoolean(s, false)).append(", ");
            }
            for (String s : stringSettings) {
                sb.append(s).append("=").append(settings.getString(s, null)).append(", ");
            }
            for (String s : stringSetSettings) {
                sb.append(s).append("=").append(settings.getStringSet(s, null)).append(", ");
            }
            for (String s : intSettings) {
                sb.append(s).append("=").append(settings.getInt(s, -1)).append(", ");
            }
            for (String s : longSettings) {
                sb.append(s).append("=").append(settings.getLong(s, -1)).append(", ");
            }
            for (String s : floatSettings) {
                sb.append(s).append("=").append(settings.getFloat(s, -1)).append(", ");
            }
            sb.setLength(sb.length() - 2); // remove last ", "
            MindBell.logDebug(sb.toString());
        }
    }

    @Override
    public boolean isShow() {
        return settings.getBoolean(keyShow, defaultShow);
    }

    @Override
    public boolean isSound() {
        return settings.getBoolean(keySound, defaultSound);
    }

    @Override
    public boolean isStatus() {
        return settings.getBoolean(keyStatus, defaultStatus);
    }

    @Override
    public Set<Integer> getActiveOnDaysOfWeek() {
        Set<String> strings = settings.getStringSet(keyActiveOnDaysOfWeek, defaultActiveOnDaysOfWeek);
        Set<Integer> integers = new HashSet<>();
        for (String string : strings) {
            integers.add(Integer.valueOf(string));
        }
        return integers;
    }

    @Override
    public String getActiveOnDaysOfWeekString() {
        // Warning: Similar code in MindBellPreferences#setMultiSelectListPreferenceSummary()
        Set<Integer> activeOnDaysOfWeek = getActiveOnDaysOfWeek();
        StringBuilder sb = new StringBuilder();
        for (String dayOfWeekValue : weekdayEntryValues) { // internal weekday value in locale oriented order
            Integer dayOfWeekValueAsInteger = Integer.valueOf(dayOfWeekValue);
            if (activeOnDaysOfWeek.contains(dayOfWeekValueAsInteger)) { // active on this day?
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(weekdayAbbreviationEntries[dayOfWeekValueAsInteger - 1]); // add day to the list of active days
            }
        }
        return sb.toString();
    }

    @Override
    public float getVolume(float defaultVolume) {
        if (defaultVolume > 1f) {
            throw new IllegalArgumentException("Default volume out of range: " + defaultVolume);
        }
        try {
            return settings.getFloat(keyVolume, defaultVolume);
        } catch (ClassCastException e) {
            Log.e(TAG, "Not a float for volume", e);
            return defaultVolume;
        }
    }

    @Override
    public String getRingtone() {
        return settings.getString(keyRingtone, null);
    }

    @Override
    public Uri getSoundUri() {
        // This implementation is almost the same as MindBellPreferences#setPreferenceVolumeSoundUri()
        String ringtone = getRingtone();
        if (settings.getBoolean(keyUseStandardBell, defaultUseStandardBell) || ringtone == null) {
            return bellRessourceUri; // set initially in constructor because that needs a context
        } else {
            return Uri.parse(ringtone);
        }
    }

    @Override
    public TimeOfDay getDaytimeEnd() {
        return new TimeOfDay(getDaytimeEndHour(), 0);
    }

    private int getDaytimeEndHour() {
        return Integer.valueOf(settings.getString(keyEnd, defaultEnd));
    }

    @Override
    public String getDaytimeEndString() {
        return hourEntries[getDaytimeEndHour()];
    }

    @Override
    public TimeOfDay getDaytimeStart() {
        return new TimeOfDay(getDaytimeStartHour(), 0);
    }

    private int getDaytimeStartHour() {
        return Integer.valueOf(settings.getString(keyStart, defaultStart));
    }

    @Override
    public String getDaytimeStartString() {
        return hourEntries[getDaytimeStartHour()];
    }

    @Override
    public long getInterval() {
        long interval = Long.valueOf(settings.getString(keyFrequency, defaultFrequency));
        if (interval < 1 * 60000) { // min: 1 minute
            interval = Long.valueOf(defaultFrequency);
        }
        return interval;
    }

    @Override
    public int getNormalize() {
        return Integer.valueOf(settings.getString(keyNormalize, defaultNormalize));
    }

    @Override
    public String getPattern() {
        return settings.getString(keyPattern, defaultPattern);
    }

    @Override
    public boolean isActive() {
        return settings.getBoolean(keyActive, defaultActive);
    }

    @Override
    public boolean isMeditating() {
        return settings.getBoolean(keyMeditating, defaultMeditating);
    }

    @Override
    public boolean isRandomize() {
        return settings.getBoolean(keyRandomize, defaultRandomize);
    }

    @Override
    public boolean isMuteInFlightMode() {
        return settings.getBoolean(keyMuteInFlightMode, defaultMuteInFlightMode);
    }

    @Override
    public boolean isMuteOffHook() {
        return settings.getBoolean(keyMuteOffHook, defaultMuteOffHook);
    }

    @Override
    public boolean isMuteWithPhone() {
        return settings.getBoolean(keyMuteWithPhone, defaultMuteWithPhone);
    }

    @Override
    public boolean isVibrate() {
        return settings.getBoolean(keyVibrate, defaultVibrate);
    }

    @Override
    public boolean isStatusNotificationVisibilityPublic() {
        return settings.getBoolean(keyStatusVisibilityPublic, defaultStatusVisibilityPublic);
    }

    @Override
    public void isActive(boolean active) {
        settings.edit().putBoolean(keyActive, active).apply();
    }

    // FIXME dkn Setter mit Getter-Namen :-) ... setPrefXXX getPrefXXX getXXX umgerechnet o.ä
    @Override
    public void isMeditating(boolean meditating) {
        settings.edit().putBoolean(keyMeditating, meditating).apply();
    }

    @Override
    public void setStatus(boolean statusNotification) {
        settings.edit().putBoolean(keyStatus, statusNotification).apply();
    }

    @Override
    public boolean useStatusIconMaterialDesign() {
        return settings.getBoolean(keyStatusIconMaterialDesign, defaultStatusIconMaterialDesign);
    }

    @Override
    public long getRampUpTimeMillis() {
        return Integer.parseInt(getRampUpTime()) * 1000L;
    }

    @Override
    public String getRampUpTime() {
        return settings.getString(keyRampUpTime, defaultRampUpTime);
    }

    @Override
    public void setRampUpTime(String rampUpTime) {
        settings.edit().putString(keyRampUpTime, rampUpTime).apply();
    }

    @Override
    public int getNumberOfPeriodsAsInteger() {
        return Integer.parseInt(getNumberOfPeriods());
    }

    @Override
    public String getNumberOfPeriods() {
        return settings.getString(keyNumberOfPeriods, defaultNumberOfPeriods);
    }

    @Override
    public void setNumberOfPeriods(String numberOfPeriods) {
        settings.edit().putString(keyNumberOfPeriods, numberOfPeriods).apply();
    }

    @Override
    public long getMeditationDurationMillis() {
        return Integer.parseInt(getMeditationDuration()) * 60000L;
    }

    @Override
    public String getMeditationDuration() {
        return settings.getString(keyMeditationDuration, defaultMeditationDuration);
    }

    @Override
    public void setMeditationDuration(String meditationDuration) {
        settings.edit().putString(keyMeditationDuration, meditationDuration).apply();
    }

    @Override
    public long getRampUpStartingTimeMillis() {
        return settings.getLong(keyRampUpStartingTimeMillis, defaultRampUpStartingTimeMillis);
    }

    @Override
    public void setRampUpStartingTimeMillis(long rampUpStartingTimeMillis) {
        settings.edit().putLong(keyRampUpStartingTimeMillis, rampUpStartingTimeMillis).apply();
    }

    @Override
    public long getMeditationStartingTimeMillis() {
        return settings.getLong(keyMeditationStartingTimeMillis, defaultMeditationStartingTimeMillis);
    }

    @Override
    public void setMeditationStartingTimeMillis(long meditationStartingTimeMillis) {
        settings.edit().putLong(keyMeditationStartingTimeMillis, meditationStartingTimeMillis).apply();
    }

    @Override
    public long getMeditationEndingTimeMillis() {
        return settings.getLong(keyMeditationEndingTimeMillis, defaultMeditationEndingTimeMillis);
    }

    @Override
    public void setMeditationEndingTimeMillis(long meditationEndingTimeMillis) {
        settings.edit().putLong(keyMeditationEndingTimeMillis, meditationEndingTimeMillis).apply();
    }

    @Override
    public int getPopup() {
        return settings.getInt(keyPopup, defaultPopup);
    }

    @Override
    public void setPopup(int popup) {
        settings.edit().putInt(keyPopup, popup).apply();
    }

    @Override
    public void resetPopup() {
        settings.edit().putInt(keyPopup, defaultPopup).apply();
    }

}
