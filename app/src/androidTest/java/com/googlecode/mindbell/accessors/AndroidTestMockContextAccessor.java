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

public class AndroidTestMockContextAccessor extends ContextAccessor {

    private boolean isPhoneMuted = false;

    private boolean isPhoneOffHook = false;

    private boolean isPhoneInFlightMode = false;

    /**
     * Returns an accessor for the given context, just in case we want to make this a Singleton.
     */
    public static AndroidTestMockContextAccessor getInstance() {
        return new AndroidTestMockContextAccessor();
    }

    private AndroidTestMockContextAccessor() {
        this.prefs = new AndroidTestMockPrefsAccessor();
    }

    @Override
    public void finishBellSound() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getAlarmMaxVolume() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public int getAlarmVolume() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public AndroidTestMockPrefsAccessor getPrefs() {
        return (AndroidTestMockPrefsAccessor) prefs;
    }

    public long getSoundDuration() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isBellSoundPlaying() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public boolean isPhoneInFlightMode() {
        return isPhoneInFlightMode;
    }

    @Override
    public boolean isPhoneMuted() {
        return isPhoneMuted;
    }

    @Override
    public boolean isPhoneOffHook() {
        return isPhoneOffHook;
    }

    @Override
    public void setAlarmVolume(int volume) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    public void setPhoneInFlightMode(boolean isPhoneInFlightMode) {
        this.isPhoneInFlightMode = isPhoneInFlightMode;
    }

    public void setPhoneMuted(boolean value) {
        isPhoneMuted = value;
    }

    public void setPhoneOffHook(boolean value) {
        isPhoneOffHook = value;
    }

    public void setSoundDuration(long duration) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void startPlayingSoundAndVibrate(ActivityPrefsAccessor activityPrefs, final Runnable runWhenDone) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void showBell() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void updateStatusNotification() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void startPlayingSound(ActivityPrefsAccessor activityPrefs, Runnable runWhenDone) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void updateBellSchedule() {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void updateBellSchedule(long nowTimeMillis) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

    @Override
    public void reschedule(long nextTargetTimeMillis, Integer nextMeditationPeriod) {
        throw new UnsupportedOperationException("Test terminated ... method not implemented");
    }

}
