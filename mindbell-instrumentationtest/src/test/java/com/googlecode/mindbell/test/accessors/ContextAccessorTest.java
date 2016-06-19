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

import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.test.accessors.MockContextAccessor;

import junit.framework.TestCase;

public class ContextAccessorTest extends TestCase {

    public void testAlarmVolume() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        // exercise
        ca.startBellSound(null);
        // verify
        assertEquals(ca.getAlarmMaxVolume(), ca.getAlarmVolume());
    }

    public void testFinish() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        ca.setAlarmVolume(ca.getAlarmMaxVolume() / 2);
        int alarmVolume = ca.getAlarmVolume();
        // exercise
        ca.startBellSound(null);
        ca.finishBellSound();
        // verify
        assertFalse(ca.isBellSoundPlaying());
        assertEquals(alarmVolume, ca.getAlarmVolume());
    }

    public void testInFlightMode_false1() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(true);
        ca.getPrefs().setSettingMuteInFlightMode(false);
        // exercise/verify
        assertFalse(ca.isMuteRequested(true));
    }

    public void testInFlightMode_false2() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(false);
        ca.getPrefs().setSettingMuteInFlightMode(true);
        // exercise/verify
        assertFalse(ca.isMuteRequested(true));
    }

    public void testInFlightMode_true() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(true);
        ca.getPrefs().setSettingMuteInFlightMode(true);
        // exercise/verify
        assertTrue(ca.isMuteRequested(true));
    }

    public void testMuted_false1() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneMuted(true);
        ca.getPrefs().setSettingMuteWithPhone(false);
        // exercise/verify
        assertFalse(ca.isMuteRequested(true));
    }

    public void testMuted_false2() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneMuted(false);
        ca.getPrefs().setSettingMuteWithPhone(true);
        // exercise/verify
        assertFalse(ca.isMuteRequested(true));
    }

    public void testMuted_true() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneMuted(true);
        ca.getPrefs().setSettingMuteWithPhone(true);
        // exercise/verify
        assertTrue(ca.isMuteRequested(true));
    }

    public void testOffHook_false1() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneOffHook(true);
        ca.getPrefs().setSettingMuteOffHook(false);
        // exercise/verify
        assertFalse(ca.isMuteRequested(true));
    }

    public void testOffHook_false2() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneOffHook(false);
        ca.getPrefs().setSettingMuteOffHook(true);
        // exercise/verify
        assertFalse(ca.isMuteRequested(true));
    }

    public void testOffHook_true() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneOffHook(true);
        ca.getPrefs().setSettingMuteOffHook(true);
        // exercise/verify
        assertTrue(ca.isMuteRequested(true));
    }

    public void testOriginalVolume() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        int originalVolume = ca.getAlarmVolume();
        // exercise
        ca.startBellSound(null);
        ca.finishBellSound();
        // verify
        assertEquals(originalVolume, ca.getAlarmVolume());

    }

    public void testPlay() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        // exercise
        ca.startBellSound(null);
        // verify
        assertTrue(ca.isBellSoundPlaying());
    }

    public void testReasonableDefault() {
        ContextAccessor ca = MockContextAccessor.getInstance();
        float bellDefaultVolume = ca.getBellDefaultVolume();
        assertTrue(0 <= bellDefaultVolume);
        assertTrue(bellDefaultVolume <= 1);
    }
}
