/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
 *     Copyright (C) 2014-2017 Uwe Damken
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
package com.googlecode.mindbell.accessors;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
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
import java.util.SortedMap;
import java.util.TreeMap;

import static com.googlecode.mindbell.MindBellPreferences.TAG;
import static com.googlecode.mindbell.R.string.keyActive;
import static com.googlecode.mindbell.R.string.keyActiveOnDaysOfWeek;
import static com.googlecode.mindbell.R.string.keyDismissNotification;
import static com.googlecode.mindbell.R.string.keyEnd;
import static com.googlecode.mindbell.R.string.keyFrequency;
import static com.googlecode.mindbell.R.string.keyKeepScreenOn;
import static com.googlecode.mindbell.R.string.keyMeditating;
import static com.googlecode.mindbell.R.string.keyMeditationBeginningBell;
import static com.googlecode.mindbell.R.string.keyMeditationDuration;
import static com.googlecode.mindbell.R.string.keyMeditationEndingBell;
import static com.googlecode.mindbell.R.string.keyMeditationEndingTimeMillis;
import static com.googlecode.mindbell.R.string.keyMeditationInterruptingBell;
import static com.googlecode.mindbell.R.string.keyMeditationStartingTimeMillis;
import static com.googlecode.mindbell.R.string.keyMeditationVolume;
import static com.googlecode.mindbell.R.string.keyMuteInFlightMode;
import static com.googlecode.mindbell.R.string.keyMuteOffHook;
import static com.googlecode.mindbell.R.string.keyMuteWithPhone;
import static com.googlecode.mindbell.R.string.keyMutedTill;
import static com.googlecode.mindbell.R.string.keyNoSoundOnMusic;
import static com.googlecode.mindbell.R.string.keyNormalize;
import static com.googlecode.mindbell.R.string.keyNotification;
import static com.googlecode.mindbell.R.string.keyNotificationText;
import static com.googlecode.mindbell.R.string.keyNotificationTitle;
import static com.googlecode.mindbell.R.string.keyNotificationVisibilityPublic;
import static com.googlecode.mindbell.R.string.keyOriginalVolume;
import static com.googlecode.mindbell.R.string.keyPattern;
import static com.googlecode.mindbell.R.string.keyPatternOfPeriods;
import static com.googlecode.mindbell.R.string.keyPauseAudioOnSound;
import static com.googlecode.mindbell.R.string.keyPopup;
import static com.googlecode.mindbell.R.string.keyRampUpStartingTimeMillis;
import static com.googlecode.mindbell.R.string.keyRampUpTime;
import static com.googlecode.mindbell.R.string.keyRandomize;
import static com.googlecode.mindbell.R.string.keyRingtone;
import static com.googlecode.mindbell.R.string.keyShow;
import static com.googlecode.mindbell.R.string.keySound;
import static com.googlecode.mindbell.R.string.keyStart;
import static com.googlecode.mindbell.R.string.keyStartMeditationDirectly;
import static com.googlecode.mindbell.R.string.keyStatus;
import static com.googlecode.mindbell.R.string.keyStatusIconMaterialDesign;
import static com.googlecode.mindbell.R.string.keyStatusVisibilityPublic;
import static com.googlecode.mindbell.R.string.keyStopMeditationAutomatically;
import static com.googlecode.mindbell.R.string.keyUseStandardBell;
import static com.googlecode.mindbell.R.string.keyVibrate;
import static com.googlecode.mindbell.R.string.keyVolume;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.BOOLEAN;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.FLOAT;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.INTEGER;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.LONG;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.STRING;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.STRING_SET;
import static com.googlecode.mindbell.accessors.AndroidPrefsAccessor.Preference.Type.TIME_STRING;

public class AndroidPrefsAccessor {

    /**
     * One minute in milliseconds.
     */
    public static final long ONE_MINUTE_MILLIS = 60000L;
    /**
     * One minute in milliseconds plus an error indicator millisecond value.
     */
    public static final long ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION = ONE_MINUTE_MILLIS + 1L;
    public static final long ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING = ONE_MINUTE_MILLIS + 2L;
    public static final long ONE_MINUTE_MILLIS_NEGATIVE_PERIOD = ONE_MINUTE_MILLIS + 3L;
    public static final long ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT = ONE_MINUTE_MILLIS + 4L;
    public static final long ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING = ONE_MINUTE_MILLIS + 5L;
    /**
     * Regular expressions to verify a pattern of periods string.
     */
    public static final String STATIC_PERIOD_REGEX = "([1-9][0-9]{0,2})";
    public static final String VARIABLE_PERIOD_REGEX = "(x)";
    public static final String PERIOD_REGEX = "(" + STATIC_PERIOD_REGEX + "|" + VARIABLE_PERIOD_REGEX + ")";
    public static final String PERIOD_SEPARATOR = ",";
    public static final String PERIOD_SEPARATOR_REGEX = PERIOD_SEPARATOR;
    public static final String PERIOD_SEPARATOR_WITH_BLANKS_REGEX = " *" + PERIOD_SEPARATOR_REGEX + " *";
    public static final String PERIOD_SEPARATOR_WITH_BLANK = ", ";
    /**
     * Minimum value for ramp up time
     */
    public static final TimeOfDay MIN_RAMP_UP_TIME = new TimeOfDay(0, 5);
    /**
     * Minimum value for meditation duration
     */
    public static final TimeOfDay MIN_MEDITATION_DURATION = new TimeOfDay(0, 1);
    /**
     * Time to wait for displayed bell to be send back
     */
    public static final long WAITING_TIME = 10000L;
    public static final String NORMALIZE_NONE = "-1";
    public static final float DEFAULT_VOLUME = 0.501187234f;
    /**
     * Regular expression to verify a time string preference
     */
    private static final String TIME_STRING_REGEX = "\\d?\\d(:\\d\\d)?";
    /**
     * Unique string to be added to a Scheduling Intent to see which meditation period the bell is in.
     */
    public static String EXTRA_MEDITATION_PERIOD = "com.googlecode.mindbell.Scheduler.MeditationPeriod";
    /**
     * Unique string to be added to a Scheduling Intent to see who sent it.
     */
    public static String EXTRA_IS_RESCHEDULING = "com.googlecode.mindbell.Scheduler.IsRescheduling";
    /**
     * Unique string to be added to a Scheduling Intent to see for which time the bell was scheduled.
     */
    public static String EXTRA_NOW_TIME_MILLIS = "com.googlecode.mindbell.Scheduler.NowTimeMillis";
    /**
     * Unique string to be added to an Intent to see if MindBellMain is opened to stop meditation mode.
     */
    public static String EXTRA_STOP_MEDITATION = "com.googlecode.mindbell.MindBellMail.StopMeditation";
    // *The* SharedPreferences
    private final SharedPreferences settings;

    // Map of all used preferences - with type and default value - by resid
    private final SortedMap<Integer, Preference> preferenceMap = new TreeMap<>();

    // Map of allowed internal values for a string or string set preference by resid
    private final Map<Integer, String[]> entryValuesMap = new HashMap<>();

    private final String[] weekdayEntryValues;
    private final String[] weekdayAbbreviationEntries;

    private final Map<String, Uri> bellResourceUriMap;

    private ActivityPrefsAccessor activityPrefsForRegularOperation = new ActivityPrefsAccessorForRegularOperation();

    private ActivityPrefsAccessor activityPrefsForRingingOnce = new ActivityPrefsAccessorForRingingOnce();

    private ActivityPrefsAccessor activityPrefsForMeditationBeginning = new ActivityPrefsAccessorForMeditationBeginning();

    private ActivityPrefsAccessor activityPrefsForMeditationInterrupting = new ActivityPrefsAccessorForMeditationInterrupting();

    private ActivityPrefsAccessor activityPrefsForMeditationEnding = new ActivityPrefsAccessorForMeditationEnding();

    /**
     * Constructs an accessor for preferences in the given context, please use {@link AndroidContextAccessor#getPrefs()} instead of
     * calling this directly.
     */
    protected AndroidPrefsAccessor(Context context, boolean logSettings) {
        settings = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);

        // Define entries and entry values
        weekdayEntryValues = context.getResources().getStringArray(R.array.weekdayEntryValues);
        weekdayAbbreviationEntries = context.getResources().getStringArray(R.array.weekdayAbbreviationEntries);

        // Define bell resource uri values ... doing it here because this needs a context
        bellResourceUriMap = new HashMap<>();
        // no entry for "0", let getSoundUri() return null to play no sound at all
        bellResourceUriMap.put("1", Utils.getResourceUri(context, R.raw.bell10s));
        bellResourceUriMap.put("2", Utils.getResourceUri(context, R.raw.bell20s));
        bellResourceUriMap.put("3", Utils.getResourceUri(context, R.raw.bell30s));

        // Check the settings and reset invalid ones
        checkSettings(context, logSettings);
    }

    /**
     * Returns a patternOfPeriods string that corresponds with the numberOfPeriods: 1 -> "x", 2 -> "x, x", ...
     */
    public static String derivePatternOfPeriods(int numberOfPeriods) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numberOfPeriods; i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("x");
        }
        return sb.toString();
    }

    /**
     * Returns the length of a specific meditation period in millis, or 6000xL in case of inconsistent arguments. The latter makes
     * MindBell more robust. If for any reason the patterns string is invalid the periods get one minute lengths instead of
     * experiencing exceptions.
     */
    public static long derivePeriodMillis(String patternOfPeriods, int meditationDuration, int meditationPeriod) {
        int periodIndex = meditationPeriod - 1;
        String[] periods = patternOfPeriods.split(PERIOD_SEPARATOR_REGEX);
        // Verify the patternOfPeriods string and calculate the length of a variable period
        int numberOfVariablePeriods = 0;
        int sumOfPeriodsLengths = 0;
        for (int i = 0; i < periods.length; i++) {
            periods[i] = periods[i].trim();
            String period = periods[i];
            if (period.matches(STATIC_PERIOD_REGEX)) {
                sumOfPeriodsLengths += Integer.valueOf(period);
            } else if (period.matches(VARIABLE_PERIOD_REGEX)) {
                numberOfVariablePeriods++;
            } else {
                return ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION;
            }
        }
        if (numberOfVariablePeriods == 0) {
            return ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING;
        }
        long millisOfVariablePeriods = (meditationDuration - sumOfPeriodsLengths) * ONE_MINUTE_MILLIS / numberOfVariablePeriods;
        if (millisOfVariablePeriods < 0) {
            return ONE_MINUTE_MILLIS_NEGATIVE_PERIOD;
        } else if (millisOfVariablePeriods < ONE_MINUTE_MILLIS) {
            return ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT;
        }
        if (periodIndex < 0 || periodIndex >= periods.length) {
            return ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING; // avoid IndexOutOfBoundsException
        } else if (periods[periodIndex].matches(STATIC_PERIOD_REGEX)) {
            return Integer.valueOf(periods[periodIndex]) * ONE_MINUTE_MILLIS;
        } else {
            return millisOfVariablePeriods;
        }
    }

    public long[] getVibrationPattern() {
        return getVibrationPattern(getPattern());
    }

    /**
     * Returns the given pattern string as an array of long values.
     */
    public static long[] getVibrationPattern(String pattern) {
        String[] msAsString = pattern.split(":");
        long[] ms = new long[msAsString.length];
        for (int i = 0; i < ms.length; i++) {
            ms[i] = Long.valueOf(msAsString[i]);
        }
        return ms;
    }

    public String getPattern() {
        return getStringSetting(keyPattern);
    }

    /**
     * Returns the current string setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private String getStringSetting(int resid) {
        return (String) getSetting(resid);
    }

    /**
     * Returns the current setting of the preference with the given resid
     *
     * @param resid
     * @return
     */
    private Object getSetting(int resid) {
        return getSetting(preferenceMap.get(resid));
    }

    /**
     * Returns the current setting of the preference.
     *
     * @param preference
     * @return
     */
    private Object getSetting(Preference preference) {
        switch (preference.type) {
            case BOOLEAN:
                return settings.getBoolean(preference.key, (Boolean) preference.defaultValue);
            case FLOAT:
                return settings.getFloat(preference.key, (Float) preference.defaultValue);
            case INTEGER:
                return settings.getInt(preference.key, (Integer) preference.defaultValue);
            case LONG:
                return settings.getLong(preference.key, (Long) preference.defaultValue);
            case STRING:
            case TIME_STRING:
                return settings.getString(preference.key, (String) preference.defaultValue);
            case STRING_SET:
                return settings.getStringSet(preference.key, (Set<String>) preference.defaultValue);
            default:
                Log.e(TAG, "Preference '" + preference.key + "' has a non supported type: " + preference.type);
                return null;
        }
    }

    public boolean isNormalize() {
        return isNormalize(getNormalize());
    }

    /**
     * Returns true if the given normalize value means normalization is on.
     */
    public static boolean isNormalize(int normalizeValue) {
        return normalizeValue >= 0;
    }

    public int getNormalize() {
        return Integer.valueOf(getStringSetting(keyNormalize));
    }

    /**
     * Returns the number of meditation periods derived from the pattern of periods lengths.
     */
    public int getNumberOfPeriods() {
        return deriveNumberOfPeriods(getPatternOfPeriods());
    }

    /**
     * Returns a numberOfPeriods string that corresponds with the patternOfPeriods: "x" -> 1, "3, x" -> 2, ...
     */
    public static int deriveNumberOfPeriods(String patternOfPeriods) {
        return patternOfPeriods.split(PERIOD_SEPARATOR_REGEX).length;
    }

    public String getPatternOfPeriods() {
        return getStringSetting(keyPatternOfPeriods);
    }

    public void setPatternOfPeriods(String patternOfPeriods) {
        setSetting(keyPatternOfPeriods, patternOfPeriods);
    }

    public long getMeditationPeriodMillis(int meditationPeriod) {
        return derivePeriodMillis(getPatternOfPeriods(), getMeditationDuration().getInterval(), meditationPeriod);
    }

    /**
     * Checks that any data in the SharedPreferences are of the expected type. Should we find anything that doesn't fit the
     * expectations, we delete it and recreate it with it's default value.
     */
    public void checkSettings(Context context, boolean logSettings) {

        // Define all preference keys and their default values
        addPreference(keyActive, false, BOOLEAN, context);
        addPreference(keyActiveOnDaysOfWeek, new HashSet<>(Arrays.asList(new String[]{"1", "2", "3", "4", "5", "6", "7"})),
                STRING_SET, context);
        addPreference(keyDismissNotification, false, BOOLEAN, context);
        addPreference(keyEnd, "21:00", TIME_STRING, context);
        addPreference(keyFrequency, "00:15", TIME_STRING, context); // 15 min
        addPreference(keyKeepScreenOn, true, BOOLEAN, context);
        addPreference(keyMeditating, false, BOOLEAN, context);
        addPreference(keyMeditationBeginningBell, "3", STRING, context);
        addPreference(keyMeditationDuration, "00:25", TIME_STRING, context);
        addPreference(keyMeditationEndingBell, "2", STRING, context);
        addPreference(keyMeditationEndingTimeMillis, -1L, LONG, context);
        addPreference(keyMeditationInterruptingBell, "1", STRING, context);
        addPreference(keyMeditationStartingTimeMillis, -1L, LONG, context);
        addPreference(keyMutedTill, -1L, LONG, context);
        addPreference(keyMuteInFlightMode, false, BOOLEAN, context);
        addPreference(keyMuteOffHook, true, BOOLEAN, context);
        addPreference(keyMuteWithPhone, true, BOOLEAN, context);
        addPreference(keyNormalize, NORMALIZE_NONE, STRING, context);
        addPreference(keyNoSoundOnMusic, false, BOOLEAN, context);
        addPreference(keyNotification, false, BOOLEAN, context);
        addPreference(keyNotificationText, context.getText(R.string.prefsNotificationTextDefault), STRING, context);
        addPreference(keyNotificationTitle, context.getText(R.string.prefsNotificationTitleDefault), STRING, context);
        addPreference(keyNotificationVisibilityPublic, true, BOOLEAN, context);
        addPreference(keyOriginalVolume, -1, INTEGER, context);
        addPreference(keyPattern, "100:200:100:600", STRING, context);
        addPreference(keyPatternOfPeriods, "x", STRING, context);
        addPreference(keyPauseAudioOnSound, false, BOOLEAN, context);
        addPreference(keyPopup, -1, INTEGER, context);
        addPreference(keyRampUpStartingTimeMillis, -1L, LONG, context);
        addPreference(keyRampUpTime, "00:30", TIME_STRING, context);
        addPreference(keyRandomize, true, BOOLEAN, context);
        addPreference(keyRingtone, "", STRING, context); // no useful default, code relies on <defaultValue>.isEmpty()
        addPreference(keyShow, true, BOOLEAN, context);
        addPreference(keySound, true, BOOLEAN, context);
        addPreference(keyStart, "09:00", TIME_STRING, context);
        addPreference(keyStartMeditationDirectly, false, BOOLEAN, context);
        addPreference(keyStatus, true, BOOLEAN, context);
        addPreference(keyStatusIconMaterialDesign, true, BOOLEAN, context);
        addPreference(keyStatusVisibilityPublic, true, BOOLEAN, context);
        addPreference(keyStopMeditationAutomatically, false, BOOLEAN, context);
        addPreference(keyUseStandardBell, true, BOOLEAN, context);
        addPreference(keyVibrate, false, BOOLEAN, context);
        addPreference(keyVolume, DEFAULT_VOLUME, FLOAT, context);
        addPreference(keyMeditationVolume, getVolume(), FLOAT, context); // for existing users: use standard volume as default here

        // Map preference keys to their allowed entryValues
        entryValuesMap.put(keyActiveOnDaysOfWeek, context.getResources().getStringArray(R.array.weekdayEntryValues));
        entryValuesMap.put(keyMeditationBeginningBell, context.getResources().getStringArray(R.array.bellEntryValues));
        entryValuesMap.put(keyMeditationEndingBell, context.getResources().getStringArray(R.array.bellEntryValues));
        entryValuesMap.put(keyMeditationInterruptingBell, context.getResources().getStringArray(R.array.bellEntryValues));
        entryValuesMap.put(keyNormalize, context.getResources().getStringArray(R.array.normalizeEntryValues));
        entryValuesMap.put(keyNotificationText, new String[]{}); // we cannot verify the entered notification text
        entryValuesMap.put(keyNotificationTitle, new String[]{}); // we cannot verify the entered notification title
        entryValuesMap.put(keyPattern, context.getResources().getStringArray(R.array.patternEntryValues));
        entryValuesMap.put(keyPatternOfPeriods, new String[]{}); // we cannot verify the entered notification text
        entryValuesMap.put(keyRingtone, new String[]{}); // we don't need to know the possible ringtone values

        // Convert old settings from previous versions
        convertOldSettings(context);

        // Track whether a stacktrace shall be logged to find out the reason for sometimes deleted preferences
        boolean logStackTrace = false;

        // Remove preference settings of wrong type and set non-existent preference to their default value
        for (Preference preference : preferenceMap.values()) {
            logStackTrace = removeInvalidSetting(preference) || logStackTrace;
            logStackTrace = resetMissingSetting(preference) || logStackTrace;
        }

        // Log stacktrace if a setting has been deleted or set to its default
        if (logStackTrace) {
            Log.w(TAG, new Exception("At least one setting has been deleted or reset to its default"));
        } else {
            MindBell.logDebug("Preferences checked and found to be ok");
        }

        // Finally report all currently existing settings if requested
        if (logSettings) {
            StringBuilder sb = new StringBuilder();
            sb.append("Effective settings: ");
            TreeMap<String, Object> orderedSettings = new TreeMap<>(settings.getAll());
            for (Map.Entry<String, Object> setting : orderedSettings.entrySet()) {
                sb.append(setting.getKey()).append("=").append(setting.getValue()).append(", ");
            }
            sb.setLength(sb.length() - 2); // remove last ", "
            MindBell.logDebug(sb.toString());
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
    private void addPreference(int resid, Object defaultValue, Preference.Type type, Context context) {
        preferenceMap.put(resid, new Preference(resid, context.getString(resid), defaultValue, type));
    }

    public float getVolume() {
        return getFloatSetting(keyVolume);
    }

    /**
     * Convert old settings from previous versions ... remove code after a while.
     */
    private void convertOldSettings(Context context) {
        // Version 3.1.0 replaced numberOfPeriods by patternOfPeriods
        String keyNumberOfPeriods = "numberOfPeriods";
        int oldNumberOfPeriods = settings.getInt(keyNumberOfPeriods, 0);
        if (oldNumberOfPeriods > 0) {
            String patternOfPeriods = derivePatternOfPeriods(oldNumberOfPeriods);
            setPatternOfPeriods(patternOfPeriods);
            settings.edit().remove(keyNumberOfPeriods).apply();
            Log.w(TAG, "Converted old setting for '" + keyNumberOfPeriods + "' (" + oldNumberOfPeriods + ") to '" +
                    context.getText(keyPatternOfPeriods) +
                    "' (" + patternOfPeriods + ")");
        }
        // Version 3.2.0 replaces frequency milliseconds string by time string
        String keyFrequency = context.getString(R.string.keyFrequency);
        String oldFrequency = settings.getString(keyFrequency, null);
        if (oldFrequency != null && !oldFrequency.contains(":")) {
            try {
                long milliseconds = Long.parseLong(oldFrequency);
                String frequency = TimeOfDay.fromMillisecondsInterval(milliseconds).getPersistString();
                setSetting(R.string.keyFrequency, frequency);
                Log.w(TAG, "Converted old value for '" + keyFrequency + "' from '" + oldFrequency +
                        "' to '" + frequency + "'");
            } catch (NumberFormatException e) {
                // invalid preference will be removed by removeInvalidSetting()
            }
        }
        // Version 3.2.0 replaces rampup time seconds by time string
        String keyRampUpTime = context.getString(R.string.keyRampUpTime);
        try {
            int oldRampUpTime = settings.getInt(keyRampUpTime, -1);
            if (oldRampUpTime >= 0) {
                String rampUpTime = TimeOfDay.fromSecondsInterval(oldRampUpTime).getPersistString();
                setSetting(R.string.keyRampUpTime, rampUpTime);
                Log.w(TAG, "Converted old value for '" + keyRampUpTime + "' from '" + oldRampUpTime +
                        "' to '" + rampUpTime + "'");
            }
        } catch (ClassCastException e) {
            // preference has already been converted
        }
        // Version 3.2.0 replaces meditation duration minutes by time string
        String keyMeditationDuration = context.getString(R.string.keyMeditationDuration);
        try {
            int oldMeditationDuration = settings.getInt(keyMeditationDuration, -1);
            if (oldMeditationDuration >= 0) {
                String meditationDuration = TimeOfDay.fromSecondsInterval(oldMeditationDuration).getPersistString();
                setSetting(R.string.keyMeditationDuration, meditationDuration);
                Log.w(TAG, "Converted old value for '" + keyMeditationDuration + "' from '" + oldMeditationDuration +
                        "' to '" + meditationDuration + "'");
            }
        } catch (ClassCastException e) {
            // preference has already been converted
        }
    }

    /**
     * Removes a preference setting if it is not of the expected type or has an invalid valid and in that case returns true.
     *
     * @param preference
     * @return
     */
    private boolean removeInvalidSetting(Preference preference) {
        try {
            Object value = getSetting(preference);
            switch (preference.type) {
                case BOOLEAN:
                case FLOAT:
                case INTEGER:
                case LONG:
                    // no more to do, retrieving the value is all that can be done
                    break;
                case STRING:
                    String stringValue = (String) value;
                    if (stringValue != null) {
                        List<String> entryValues = Arrays.asList(entryValuesMap.get(preference.resid));
                        if (entryValues != null && !entryValues.isEmpty() && !entryValues.contains(stringValue)) {
                            settings.edit().remove(preference.key).apply();
                            Log.w(TAG, "Removed setting '" + preference + "' since it had wrong value '" + stringValue + "'");
                            return true;
                        }
                    }
                    break;
                case STRING_SET:
                    Set<String> stringSetValue = (Set<String>) value;
                    if (stringSetValue != null) {
                        for (String aStringInSet : stringSetValue) {
                            List<String> entryValues = Arrays.asList(entryValuesMap.get(preference.resid));
                            if (aStringInSet != null && !entryValues.contains(aStringInSet)) {
                                settings.edit().remove(preference.key).apply();
                                Log.w(TAG, "Removed setting '" + preference + "' since it had (at least one) wrong value '" +
                                        aStringInSet + "'");
                                return true;
                            }
                        }
                    }
                    break;
                case TIME_STRING:
                    String timeStringValue = (String) value;
                    if (timeStringValue != null) {
                        if (!timeStringValue.matches(TIME_STRING_REGEX)) {
                            settings.edit().remove(preference.key).apply();
                            Log.w(TAG, "Removed setting '" + preference + "' since it is not a time string '" +
                                    timeStringValue + "'");
                            return true;
                        }
                    }
                    break;
            }
            return false;
        } catch (ClassCastException e) {
            settings.edit().remove(preference.key).apply();
            Log.w(TAG, "Removed setting '" + preference + "' since it had wrong type: " + e);
            return true;
        }
    }

    /**
     * Resets a preference to its default value if it is missing and in that case returns true.
     *
     * @param preference
     * @return
     */
    private boolean resetMissingSetting(Preference preference) {
        if (!settings.contains(preference.key)) {
            resetSetting(preference);
            Log.w(TAG, "Reset missing setting for '" + preference.key + "' to '" + preference.defaultValue + "'");
            return true;
        }
        return false;
    }

    public boolean isShow() {
        return getBooleanSetting(keyShow);
    }

    /**
     * Returns the current boolean setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private boolean getBooleanSetting(int resid) {
        return (Boolean) getSetting(resid);
    }

    public boolean isSound() {
        return getBooleanSetting(keySound);
    }

    public boolean isStatus() {
        return getBooleanSetting(keyStatus);
    }

    public void setStatus(boolean statusNotification) {
        setSetting(keyStatus, statusNotification);
    }

    /**
     * Sets the preference with the given resid to the given value.
     *
     * @param resid
     * @param value
     */
    private void setSetting(int resid, Object value) {
        setSetting(preferenceMap.get(resid), value);
    }

    /**
     * Sets the preference to the given value.
     *
     * @param preference
     * @param value
     */
    private void setSetting(Preference preference, Object value) {
        switch (preference.type) {
            case BOOLEAN:
                settings.edit().putBoolean(preference.key, (Boolean) value).apply();
                break;
            case FLOAT:
                settings.edit().putFloat(preference.key, (Float) value).apply();
                break;
            case INTEGER:
                settings.edit().putInt(preference.key, (Integer) value).apply();
                break;
            case LONG:
                settings.edit().putLong(preference.key, (Long) value).apply();
                break;
            case STRING:
            case TIME_STRING:
                settings.edit().putString(preference.key, (String) value).apply();
                break;
            case STRING_SET:
                settings.edit().putStringSet(preference.key, (Set<String>) value).apply();
                break;
            default:
                Log.e(TAG, "Preference '" + preference.key + "' has a non supported type: " + preference.type);
                break;
        }
    }

    public boolean isNotification() {
        return getBooleanSetting(keyNotification);
    }

    public boolean isDismissNotification() {
        return getBooleanSetting(keyDismissNotification);
    }

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

    public Set<Integer> getActiveOnDaysOfWeek() {
        Set<String> strings = getStringSetSetting(keyActiveOnDaysOfWeek);
        Set<Integer> integers = new HashSet<>();
        for (String string : strings) {
            integers.add(Integer.valueOf(string));
        }
        return integers;
    }

    /**
     * Returns the current string set setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private Set<String> getStringSetSetting(int resid) {
        return (Set<String>) getSetting(resid);
    }

    public float getMeditationVolume() {
        return getFloatSetting(keyMeditationVolume);
    }

    /**
     * Returns the current float setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private float getFloatSetting(int resid) {
        return (Float) getSetting(resid);
    }

    public String getRingtone() {
        return getStringSetting(keyRingtone);
    }

    public Uri getSoundUri() {
        // This implementation is almost the same as MindBellPreferences#setPreferenceVolumeSoundUri()
        String ringtone = getRingtone();
        if (getBooleanSetting(keyUseStandardBell) || ringtone.isEmpty()) {
            return getStandardSoundUri();
        } else {
            return Uri.parse(ringtone);
        }
    }

    public Uri getStandardSoundUri() {
        return bellResourceUriMap.get("1");
    }

    public TimeOfDay getDaytimeEnd() {
        return new TimeOfDay(getStringSetting(keyEnd));
    }

    public TimeOfDay getDaytimeStart() {
        return new TimeOfDay(getStringSetting(keyStart));
    }

    public long getInterval() {
        return new TimeOfDay(getStringSetting(keyFrequency)).getInterval() * ONE_MINUTE_MILLIS;
    }

    public String getNotificationText() {
        return getStringSetting(keyNotificationText);
    }

    public String getNotificationTitle() {
        return getStringSetting(keyNotificationTitle);
    }

    public boolean isActive() {
        return getBooleanSetting(keyActive);
    }

    public void setActive(boolean active) {
        setSetting(keyActive, active);
    }

    public boolean isMeditating() {
        return getBooleanSetting(keyMeditating);
    }

    public void setMeditating(boolean meditating) {
        setSetting(keyMeditating, meditating);
    }

    public boolean isRandomize() {
        return getBooleanSetting(keyRandomize);
    }

    public boolean isMuteInFlightMode() {
        return getBooleanSetting(keyMuteInFlightMode);
    }

    public boolean isMuteOffHook() {
        return getBooleanSetting(keyMuteOffHook);
    }

    public boolean isNoSoundOnMusic() {
        return getBooleanSetting(keyNoSoundOnMusic);
    }

    public boolean isPauseAudioOnSound() {
        return getBooleanSetting(keyPauseAudioOnSound);
    }

    public boolean isMuteWithPhone() {
        return getBooleanSetting(keyMuteWithPhone);
    }

    public boolean isVibrate() {
        return getBooleanSetting(keyVibrate);
    }

    public boolean isKeepScreenOn() {
        return getBooleanSetting(keyKeepScreenOn);
    }

    public void setKeepScreenOn(boolean keepScreenOn) {
        setSetting(keyKeepScreenOn, keepScreenOn);
    }

    public boolean isStartMeditationDirectly() {
        return getBooleanSetting(keyStartMeditationDirectly);
    }

    public void setStartMeditationDirectly(boolean startMeditationDirectly) {
        setSetting(keyStartMeditationDirectly, startMeditationDirectly);
    }

    public boolean isStopMeditationAutomatically() {
        return getBooleanSetting(keyStopMeditationAutomatically);
    }

    public void setStopMeditationAutomatically(boolean stopMeditationAutomatically) {
        setSetting(keyStopMeditationAutomatically, stopMeditationAutomatically);
    }

    public boolean isStatusVisibilityPublic() {
        return getBooleanSetting(keyStatusVisibilityPublic);
    }

    public boolean isNotificationVisibilityPublic() {
        return getBooleanSetting(keyNotificationVisibilityPublic);
    }

    public boolean useStatusIconMaterialDesign() {
        return getBooleanSetting(keyStatusIconMaterialDesign);
    }

    public long getRampUpTimeMillis() {
        return getRampUpTime().getInterval() * 1000L;
    }

    public TimeOfDay getRampUpTime() {
        return new TimeOfDay(getStringSetting(keyRampUpTime));
    }

    public void setRampUpTime(TimeOfDay rampUpTime) {
        setSetting(keyRampUpTime, rampUpTime.getPersistString());
    }

    public long getMeditationDurationMillis() {
        return getMeditationDuration().getInterval() * ONE_MINUTE_MILLIS;
    }

    public TimeOfDay getMeditationDuration() {
        return new TimeOfDay(getStringSetting(keyMeditationDuration));
    }

    public void setMeditationDuration(TimeOfDay meditationDuration) {
        setSetting(keyMeditationDuration, meditationDuration.getPersistString());
    }

    public long getRampUpStartingTimeMillis() {
        return getLongSetting(keyRampUpStartingTimeMillis);
    }

    /**
     * Returns the current long setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private long getLongSetting(int resid) {
        return (Long) getSetting(resid);
    }

    public void setRampUpStartingTimeMillis(long rampUpStartingTimeMillis) {
        setSetting(keyRampUpStartingTimeMillis, rampUpStartingTimeMillis);
    }

    public long getMeditationStartingTimeMillis() {
        return getLongSetting(keyMeditationStartingTimeMillis);
    }

    public void setMeditationStartingTimeMillis(long meditationStartingTimeMillis) {
        setSetting(keyMeditationStartingTimeMillis, meditationStartingTimeMillis);
    }

    public long getMeditationEndingTimeMillis() {
        return getLongSetting(keyMeditationEndingTimeMillis);
    }

    public void setMeditationEndingTimeMillis(long meditationEndingTimeMillis) {
        setSetting(keyMeditationEndingTimeMillis, meditationEndingTimeMillis);
    }

    public long getMutedTill() {
        return getLongSetting(keyMutedTill);
    }

    public void setMutedTill(long mutedTill) {
        setSetting(keyMutedTill, mutedTill);
    }

    public String getMeditationBeginningBell() {
        return getStringSetting(keyMeditationBeginningBell);
    }

    public String getMeditationInterruptingBell() {
        return getStringSetting(keyMeditationInterruptingBell);
    }

    public String getMeditationEndingBell() {
        return getStringSetting(keyMeditationEndingBell);
    }

    public int getOriginalVolume() {
        return getIntSetting(keyOriginalVolume);
    }

    /**
     * Returns the current int setting of the preference with the given resid.
     *
     * @param resid
     * @return
     */
    private int getIntSetting(int resid) {
        return (Integer) getSetting(resid);
    }

    public void setOriginalVolume(int originalVolume) {
        setSetting(keyOriginalVolume, originalVolume);
    }

    public void resetOriginalVolume() {
        resetSetting(keyOriginalVolume);
    }

    /**
     * Resets the preference with the given resid to its default value.
     *
     * @param resid
     */
    private void resetSetting(int resid) {
        resetSetting(preferenceMap.get(resid));
    }

    /**
     * Resets the preference with the given resid to its default value.
     *
     * @param preference
     */
    private void resetSetting(Preference preference) {
        setSetting(preference, preference.defaultValue);
    }

    public int getPopup() {
        return getIntSetting(keyPopup);
    }

    public void setPopup(int popup) {
        setSetting(keyPopup, popup);
    }

    public void resetPopup() {
        resetSetting(keyPopup);
    }

    public ActivityPrefsAccessor forRegularOperation() {
        return activityPrefsForRegularOperation;
    }

    public ActivityPrefsAccessor forRingingOnce() {
        return activityPrefsForRingingOnce;
    }

    public ActivityPrefsAccessor forMeditationBeginning() {
        return activityPrefsForMeditationBeginning;
    }

    public ActivityPrefsAccessor forMeditationInterrupting() {
        return activityPrefsForMeditationInterrupting;
    }

    public ActivityPrefsAccessor forMeditationEnding() {
        return activityPrefsForMeditationEnding;
    }

    static class Preference {

        private int resid;

        private String key;
        private Object defaultValue;
        private Type type;

        public Preference(int resid, String key, Object defaultValue, Type type) {
            this.resid = resid;
            this.key = key;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        enum Type {BOOLEAN, FLOAT, INTEGER, LONG, STRING, STRING_SET, TIME_STRING}

    }

    private class ActivityPrefsAccessorForRegularOperation implements ActivityPrefsAccessor {

        @Override
        public boolean isShow() {
            return AndroidPrefsAccessor.this.isShow();
        }

        @Override
        public boolean isSound() {
            return AndroidPrefsAccessor.this.isSound();
        }

        @Override
        public boolean isVibrate() {
            return AndroidPrefsAccessor.this.isVibrate();
        }

        @Override
        public Uri getSoundUri() {
            return AndroidPrefsAccessor.this.getSoundUri();
        }

        @Override
        public float getVolume() {
            return AndroidPrefsAccessor.this.getVolume();
        }

        @Override
        public boolean isNotification() {
            return AndroidPrefsAccessor.this.isNotification();
        }

        @Override
        public boolean isDismissNotification() {
            return AndroidPrefsAccessor.this.isDismissNotification();
        }

    }

    private class ActivityPrefsAccessorForRingingOnce implements ActivityPrefsAccessor {

        @Override
        public boolean isShow() {
            return false;
        }

        @Override
        public boolean isSound() {
            return true;
        }

        @Override
        public boolean isVibrate() {
            return false;
        }

        @Override
        public Uri getSoundUri() {
            return AndroidPrefsAccessor.this.getSoundUri();
        }

        @Override
        public float getVolume() {
            return AndroidPrefsAccessor.this.getVolume();
        }

        @Override
        public boolean isNotification() {
            return false;
        }

        @Override
        public boolean isDismissNotification() {
            return false;
        }

    }

    private abstract class ActivityPrefsAccessorForMeditation implements ActivityPrefsAccessor {

        @Override
        public boolean isShow() {
            return false;
        }

        @Override
        public boolean isSound() {
            return true;
        }

        @Override
        public boolean isVibrate() {
            return false;
        }

        @Override
        public abstract Uri getSoundUri();

        @Override
        public float getVolume() {
            return AndroidPrefsAccessor.this.getMeditationVolume();
        }

        @Override
        public boolean isNotification() {
            return false;
        }

        @Override
        public boolean isDismissNotification() {
            return false;
        }

    }

    private class ActivityPrefsAccessorForMeditationBeginning extends ActivityPrefsAccessorForMeditation {

        @Override
        public Uri getSoundUri() {
            return (isStartMeditationDirectly()) ? null : bellResourceUriMap.get(getMeditationBeginningBell());
        }

    }

    private class ActivityPrefsAccessorForMeditationInterrupting extends ActivityPrefsAccessorForMeditation {

        @Override
        public Uri getSoundUri() {
            return bellResourceUriMap.get(getMeditationInterruptingBell());
        }

    }

    private class ActivityPrefsAccessorForMeditationEnding extends ActivityPrefsAccessorForMeditation {

        @Override
        public Uri getSoundUri() {
            return bellResourceUriMap.get(getMeditationEndingBell());
        }

    }

}
