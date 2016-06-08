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

    private TimeOfDay daytimeEnd = new TimeOfDay(21, 0);

    private String daytimeEndString = "21:00";

    private TimeOfDay daytimeStart = new TimeOfDay(9, 0);

    private String daytimeStartString = "09:00";

    private Set<Integer> activeOnDaysOfWeek = new HashSet<Integer>(Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }));

    private String activeOnDaysOfWeekString = "Sunday, Monday, Tuesday, Wednesday, Thursday, Friday, Saturday";

    private long interval = 3600000;

    private boolean bellActive = true;

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
    public boolean isBellActive() {
        return bellActive;
    }

    @Override
    public boolean makeStatusNotificationVisibilityPublic() {
        return statusNotificationVisibilityPublic;
    }

    /**
     * @param activeOnDaysOfWeek
     *            the activeOnDaysOfWeek to set
     */
    public void setActiveOnDaysOfWeek(Set<Integer> activeOnDaysOfWeek) {
        this.activeOnDaysOfWeek = activeOnDaysOfWeek;
    }

    /**
     * @param activeOnDaysOfWeekString
     *            the activeOnDaysOfWeekString to set
     */
    public void setActiveOnDaysOfWeekString(String activeOnDaysOfWeekString) {
        this.activeOnDaysOfWeekString = activeOnDaysOfWeekString;
    }

    /**
     * @param theBellActive
     *            the bellActive to set
     */
    @Override
    public void setBellActive(boolean theBellActive) {
        this.bellActive = theBellActive;
    }

    /**
     * @param theDaytimeEnd
     *            the daytimeEnd to set
     */
    public void setDaytimeEnd(TimeOfDay theDaytimeEnd) {
        this.daytimeEnd = theDaytimeEnd;
    }

    /**
     * @param theDaytimeEndString
     *            the daytimeEndString to set
     */
    public void setDaytimeEndString(String theDaytimeEndString) {
        this.daytimeEndString = theDaytimeEndString;
    }

    /**
     * @param theDaytimeStart
     *            the daytimeStart to set
     */
    public void setDaytimeStart(TimeOfDay theDaytimeStart) {
        this.daytimeStart = theDaytimeStart;
    }

    /**
     * @param theDaytimeStartString
     *            the daytimeStartString to set
     */
    public void setDaytimeStartString(String theDaytimeStartString) {
        this.daytimeStartString = theDaytimeStartString;
    }

    /**
     * @param theInterval
     *            the interval to set
     */
    public void setInterval(long theInterval) {
        this.interval = theInterval;
    }

    /**
     * @param theShowBell
     *            the showBell to set
     */
    public void setShowBell(boolean theShowBell) {
        this.showBell = theShowBell;
    }

    /**
     * @param theStatusNotification
     *            the statusNotification to set
     */
    @Override
    public void setStatusNotification(boolean theStatusNotification) {
        this.statusNotification = theStatusNotification;
    }

    @Override
    public boolean useStatusIconMaterialDesign() {
        return statusIconMaterialDesign;
    }

}
