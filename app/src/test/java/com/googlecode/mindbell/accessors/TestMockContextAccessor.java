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

import java.util.Timer;
import java.util.TimerTask;

public class TestMockContextAccessor extends ContextAccessor {
    private static final int MAX_VOLUME = 7;
    private static final float BELL_VOLUME = 0.5f;

    /**
     * Returns an accessor for the given context, just in case we want to make this a Singleton.
     */
    public static TestMockContextAccessor getInstance() {
        return new TestMockContextAccessor();
    }

    private boolean isPhoneMuted = false;
    private boolean isPhoneOffHook = false;

    private boolean isPhoneInFlightMode = false;

    private boolean isPlaying = false;
    private long mockSoundDuration = 1000; // ms
    private int alarmVolume;

    private TestMockContextAccessor() {
        this.prefs = new TestMockPrefsAccessor();
    }

    @Override
    public void finishBellSound() {
        isPlaying = false;
        setAlarmVolume(originalVolume);
    }

    @Override
    public int getAlarmMaxVolume() {
        return MAX_VOLUME;
    }

    @Override
    public int getAlarmVolume() {
        return alarmVolume;
    }

    @Override
    public float getBellVolume() {
        return BELL_VOLUME;
    }

    @Override
    public TestMockPrefsAccessor getPrefs() {
        return (TestMockPrefsAccessor) prefs;
    }

    public long getSoundDuration() {
        return mockSoundDuration;
    }

    @Override
    public boolean isBellSoundPlaying() {
        return isPlaying;
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
        alarmVolume = volume;
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
        mockSoundDuration = duration;
    }

    @Override
    public void showMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void startBellSound(final Runnable runWhenDone) {
        originalVolume = getAlarmVolume();
        setAlarmVolume(MAX_VOLUME);
        isPlaying = true;

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                finishBellSound();
                if (runWhenDone != null) {
                    runWhenDone.run();
                }
            }
        }, mockSoundDuration);
    }

}
