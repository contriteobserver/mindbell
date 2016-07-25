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
package com.googlecode.mindbell.test.accessors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.util.TimeOfDay;

public class MockPrefsAccessor extends PrefsAccessor {

    private boolean showBell = true;

    private boolean statusNotification = true;

    private final boolean statusNotificationVisibilityPublic = true;

    private final boolean statusIconMaterialDesign = true;

    private boolean randomize = true;

    private int normalize = -1;

    private TimeOfDay daytimeEnd = new TimeOfDay(21, 0);

    private String daytimeEndString = "21:00";

    private TimeOfDay daytimeStart = new TimeOfDay(9, 0);

    private String daytimeStartString = "09:00";

    private Set<Integer> activeOnDaysOfWeek = new HashSet<Integer>(Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }));

    private String activeOnDaysOfWeekString = "Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday";

    private long interval = 3600000;

    private boolean isSettingMuteWithPhone = false;

    private boolean isSettingMuteOffHook = false;

    private boolean isSettingMuteInFlightMode = false;

    private boolean bellActive = true;

    private final String pattern = "100:200:100:600";

    /**
     * Constructs an accessor for preferences in the given context, please use {@link MockContextAccessor#getPrefs()} instead of
     * calling this directly.
     */
    protected MockPrefsAccessor() {
    }

    @Override
    public boolean doShowBell() {
        return showBell;
    }

    @Override
    public boolean doStatusNotification() {
        return statusNotification;
    }

    @Override
    public Set<Integer> getActiveOnDaysOfWeek() {
        return activeOnDaysOfWeek;
    }

    @Override
    public String getActiveOnDaysOfWeekString() {
        return activeOnDaysOfWeekString;
    }

    @Override
    public float getBellVolume(float defaultVolume) {
        return 0.5f;
    }

    @Override
    public TimeOfDay getDaytimeEnd() {
        return daytimeEnd;
    }

    @Override
    public String getDaytimeEndString() {
        return daytimeEndString;
    }

    @Override
    public TimeOfDay getDaytimeStart() {
        return daytimeStart;
    }

    @Override
    public String getDaytimeStartString() {
        return daytimeStartString;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public int getNormalize() {
        return normalize;
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean isBellActive() {
        return bellActive;
    }

    @Override
    public boolean isRandomize() {
        return randomize;
    }

    @Override
    public boolean isSettingMuteInFlightMode() {
        return isSettingMuteInFlightMode;
    }

    @Override
    public boolean isSettingMuteOffHook() {
        return isSettingMuteOffHook;
    }

    @Override
    public boolean isSettingMuteWithPhone() {
        return isSettingMuteWithPhone;
    }

    @Override
    public boolean makeStatusNotificationVisibilityPublic() {
        return statusNotificationVisibilityPublic;
    }

    public void setActiveOnDaysOfWeek(Set<Integer> activeOnDaysOfWeek) {
        this.activeOnDaysOfWeek = activeOnDaysOfWeek;
    }

    public void setActiveOnDaysOfWeekString(String activeOnDaysOfWeekString) {
        this.activeOnDaysOfWeekString = activeOnDaysOfWeekString;
    }

    @Override
    public void setBellActive(boolean theBellActive) {
        this.bellActive = theBellActive;
    }

    public void setDaytimeEnd(TimeOfDay theDaytimeEnd) {
        this.daytimeEnd = theDaytimeEnd;
    }

    public void setDaytimeEndString(String theDaytimeEndString) {
        this.daytimeEndString = theDaytimeEndString;
    }

    public void setDaytimeStart(TimeOfDay theDaytimeStart) {
        this.daytimeStart = theDaytimeStart;
    }

    public void setDaytimeStartString(String theDaytimeStartString) {
        this.daytimeStartString = theDaytimeStartString;
    }

    public void setInterval(long theInterval) {
        this.interval = theInterval;
    }

    public void setNormalize(int normalize) {
        this.normalize = normalize;
    }

    public void setRandomize(boolean randomize) {
        this.randomize = randomize;
    }

    public void setSettingMuteInFlightMode(boolean isSettingMuteInFlightMode) {
        this.isSettingMuteInFlightMode = isSettingMuteInFlightMode;
    }

    public void setSettingMuteOffHook(boolean value) {
        isSettingMuteOffHook = value;
    }

    public void setSettingMuteWithPhone(boolean value) {
        isSettingMuteWithPhone = value;
    }

    public void setShowBell(boolean theShowBell) {
        this.showBell = theShowBell;
    }

    @Override
    public void setStatusNotification(boolean theStatusNotification) {
        this.statusNotification = theStatusNotification;
    }

    @Override
    public boolean useStatusIconMaterialDesign() {
        return statusIconMaterialDesign;
    }

}
