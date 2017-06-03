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

import android.net.Uri;

import com.googlecode.mindbell.util.TimeOfDay;

import java.util.Set;

public abstract class PrefsAccessor {

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

    public abstract boolean isShow();

    public abstract boolean isSound();

    public abstract boolean isStatus();

    public abstract void setStatus(boolean statusNotification);

    public abstract boolean isNotification();

    public abstract String getRingtone();

    public abstract Uri getSoundUri();

    public abstract Set<Integer> getActiveOnDaysOfWeek();

    public abstract boolean isDismissNotification();

    public abstract String getActiveOnDaysOfWeekString();

    public abstract float getMeditationVolume();

    public abstract float getVolume();

    public abstract Uri getStandardSoundUri();

    public abstract TimeOfDay getDaytimeEnd();

    public abstract TimeOfDay getDaytimeStart();

    public abstract long getInterval();

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

    public abstract String getPattern();

    public abstract String getNotificationText();

    public abstract String getNotificationTitle();

    public abstract boolean isActive();

    public abstract void setActive(boolean active);

    public abstract boolean isMeditating();

    public abstract void setMeditating(boolean meditating);

    public boolean isNormalize() {
        return isNormalize(getNormalize());
    }

    /**
     * Returns true if the given normalize value means normalization is on.
     */
    public static boolean isNormalize(int normalizeValue) {
        return normalizeValue >= 0;
    }

    public abstract int getNormalize();

    public abstract boolean isRandomize();

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

    public abstract String getPatternOfPeriods();

    public abstract void setPatternOfPeriods(String patternOfPeriods);

    public long getMeditationPeriodMillis(int meditationPeriod) {
        return derivePeriodMillis(getPatternOfPeriods(), getMeditationDuration().getInterval(), meditationPeriod);
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

    public abstract TimeOfDay getMeditationDuration();

    public abstract void setMeditationDuration(TimeOfDay meditationDuration);

    public boolean isMuteInFlightMode() {
        return true;
    }

    public boolean isMuteOffHook() {
        return true;
    }

    public abstract boolean isNoSoundOnMusic();

    public abstract boolean isPauseAudioOnSound();

    public boolean isMuteWithPhone() {
        return true;
    }

    public boolean isVibrate() {
        return false;
    }

    public abstract boolean isKeepScreenOn();

    public abstract void setKeepScreenOn(boolean keepScreenOn);

    public abstract boolean isStartMeditationDirectly();

    public abstract void setStartMeditationDirectly(boolean startMeditationDirectly);

    public abstract boolean isStopMeditationAutomatically();

    public abstract void setStopMeditationAutomatically(boolean stopMeditationAutomatically);

    public abstract boolean isStatusVisibilityPublic();

    public abstract boolean isNotificationVisibilityPublic();

    public abstract boolean useStatusIconMaterialDesign();

    public abstract long getRampUpTimeMillis();

    public abstract TimeOfDay getRampUpTime();

    public abstract void setRampUpTime(TimeOfDay rampUpTime);

    public abstract long getMeditationDurationMillis();

    public abstract long getRampUpStartingTimeMillis();

    public abstract void setRampUpStartingTimeMillis(long rampUpStartingTimeMillis);

    public abstract long getMeditationStartingTimeMillis();

    public abstract void setMeditationStartingTimeMillis(long meditationStartingTimeMillis);

    public abstract long getMeditationEndingTimeMillis();

    public abstract void setMeditationEndingTimeMillis(long meditationEndingTimeMillis);

    public abstract long getMutedTill();

    public abstract void setMutedTill(long mutedTill);

    public abstract String getMeditationBeginningBell();

    public abstract String getMeditationInterruptingBell();

    public abstract String getMeditationEndingBell();

    public abstract int getOriginalVolume();

    public abstract void setOriginalVolume(int originalVolume);

    public abstract void resetOriginalVolume();

    public abstract int getPopup();

    public abstract void setPopup(int popup);

    public abstract void resetPopup();

    public abstract ActivityPrefsAccessor forRegularOperation();

    public abstract ActivityPrefsAccessor forRingingOnce();

    public abstract ActivityPrefsAccessor forMeditationBeginning();

    public abstract ActivityPrefsAccessor forMeditationInterrupting();

    public abstract ActivityPrefsAccessor forMeditationEnding();

}