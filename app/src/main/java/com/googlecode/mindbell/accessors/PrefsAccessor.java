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

import android.net.Uri;

import com.googlecode.mindbell.util.TimeOfDay;

import java.util.Set;

public abstract class PrefsAccessor {

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

    public abstract boolean isShow();

    public abstract boolean isSound();

    public abstract boolean isStatus();

    public abstract String getRingtone();

    public abstract Uri getSoundUri();

    public abstract Set<Integer> getActiveOnDaysOfWeek();

    public abstract String getActiveOnDaysOfWeekString();

    public abstract float getVolume(float defaultVolume);

    public abstract TimeOfDay getDaytimeEnd();

    public abstract String getDaytimeEndString();

    public abstract TimeOfDay getDaytimeStart();

    public abstract String getDaytimeStartString();

    public abstract long getInterval();

    public abstract int getNormalize();

    public abstract String getPattern();

    public long[] getVibrationPattern() {
        return getVibrationPattern(getPattern());
    }

    public abstract boolean isActive();

    public abstract boolean isMeditating();

    public boolean isNormalize() {
        return isNormalize(getNormalize());
    }

    /**
     * Returns true if the given normalize value means normalization is on.
     */
    public static boolean isNormalize(int normalizeValue) {
        return normalizeValue >= 0;
    }

    public abstract boolean isRandomize();

    public boolean isMuteInFlightMode() {
        return true;
    }

    public boolean isMuteOffHook() {
        return true;
    }

    public boolean isMuteWithPhone() {
        return true;
    }

    public boolean isVibrate() {
        return false;
    }

    public abstract boolean isStatusNotificationVisibilityPublic();

    public abstract void isActive(boolean active);

    public abstract void isMeditating(boolean meditating);

    public abstract void setStatus(boolean statusNotification);

    public abstract boolean useStatusIconMaterialDesign();

    public abstract long getRampUpTimeMillis();

    public abstract String getRampUpTime();

    public abstract void setRampUpTime(String rampUpTime);

    public abstract int getNumberOfPeriodsAsInteger();

    public abstract String getNumberOfPeriods();

    public abstract void setNumberOfPeriods(String numberOfPeriods);

    public abstract long getMeditationDurationMillis();

    public abstract String getMeditationDuration();

    public abstract void setMeditationDuration(String meditationDuration);

    public abstract long getRampUpStartingTimeMillis();

    public abstract void setRampUpStartingTimeMillis(long rampUpStartingTimeMillis);

    public abstract long getMeditationStartingTimeMillis();

    public abstract void setMeditationStartingTimeMillis(long meditationStartingTimeMillis);

    public abstract long getMeditationEndingTimeMillis();

    public abstract void setMeditationEndingTimeMillis(long meditationEndingTimeMillis);

    public abstract int getPopup();

    public abstract void setPopup(int popup);

    public abstract void resetPopup();
}