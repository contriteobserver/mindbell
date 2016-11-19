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
package com.googlecode.mindbell.accessors;

import android.net.Uri;

import com.googlecode.mindbell.util.TimeOfDay;

import java.util.Set;

public class AndroidTestMockPrefsAccessor extends PrefsAccessor {

    private boolean muteWithPhone = false;

    private boolean muteOffHook = false;

    private boolean muteInFlightMode = false;

    /**
     * Constructs an accessor for preferences in the given context, please use {@link AndroidTestMockContextAccessor#getPrefs()}
     * instead of calling this directly.
     */
    protected AndroidTestMockPrefsAccessor() {
    }

    @Override
    public boolean isShow() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getRingtone() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isSound() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public Uri getSoundUri() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isStatus() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setStatus(boolean theStatusNotification) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isNotification() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public Set<Integer> getActiveOnDaysOfWeek() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setActiveOnDaysOfWeek(Set<Integer> activeOnDaysOfWeek) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getActiveOnDaysOfWeekString() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setActiveOnDaysOfWeekString(String activeOnDaysOfWeekString) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public float getVolume() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public TimeOfDay getDaytimeEnd() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setDaytimeEnd(TimeOfDay theDaytimeEnd) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setDaytimeEndString(String theDaytimeEndString) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public TimeOfDay getDaytimeStart() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setDaytimeStart(TimeOfDay theDaytimeStart) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setDaytimeStartString(String theDaytimeStartString) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getInterval() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setInterval(long theInterval) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getNormalize() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setNormalize(int normalize) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getPattern() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getNotificationText() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getNotificationTitle() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isActive() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setActive(boolean active) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isMeditating() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setMeditating(boolean meditating) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isRandomize() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setRandomize(boolean randomize) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isMuteInFlightMode() {
        return muteInFlightMode;
    }

    public void setMuteInFlightMode(boolean muteInFlightMode) {
        this.muteInFlightMode = muteInFlightMode;
    }

    @Override
    public boolean isMuteOffHook() {
        return muteOffHook;
    }

    public void setMuteOffHook(boolean muteOffHook) {
        this.muteOffHook = muteOffHook;
    }

    @Override
    public boolean isNoSoundOnMusic() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isPauseAudioOnSound() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isMuteWithPhone() {
        return muteWithPhone;
    }

    public void setMuteWithPhone(boolean muteWithPhone) {
        this.muteWithPhone = muteWithPhone;
    }

    @Override
    public boolean isKeepScreenOn() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setKeepScreenOn(boolean keepScreenOn) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isStatusVisibilityPublic() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isNotificationVisibilityPublic() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setShowBell(boolean theShowBell) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean useStatusIconMaterialDesign() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getRampUpTimeMillis() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getRampUpTime() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setRampUpTime(int rampUpTime) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getNumberOfPeriods() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setNumberOfPeriods(int numberOfPeriods) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getMeditationDurationMillis() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getMeditationDuration() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setMeditationDuration(int meditationDuration) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getRampUpStartingTimeMillis() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setRampUpStartingTimeMillis(long rampUpStartingTimeMillis) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getMeditationStartingTimeMillis() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setMeditationStartingTimeMillis(long meditationStartingTimeMillis) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getMeditationEndingTimeMillis() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setMeditationEndingTimeMillis(long meditationEndingTimeMillis) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public long getMutedTill() {
        return -1;
    }

    @Override
    public void setMutedTill(long mutedTill) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getMeditationBeginningBell() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getMeditationInterruptingBell() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public String getMeditationEndingBell() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getOriginalVolume() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setOriginalVolume(int originalVolume) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void resetOriginalVolume() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getPopup() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void setPopup(int popup) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void resetPopup() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public ActivityPrefsAccessor forRegularOperation() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public ActivityPrefsAccessor forTapping() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public ActivityPrefsAccessor forMeditationBeginning() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public ActivityPrefsAccessor forMeditationInterrupting() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public ActivityPrefsAccessor forMeditationEnding() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

}
