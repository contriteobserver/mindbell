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

import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.accessors.MockContextAccessor;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class ContextAccessorTest {

    @Test
    public void testAlarmVolume() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        // exercise
        ca.startBellSound(null);
        // verify
        Assert.assertEquals(ca.getAlarmMaxVolume(), ca.getAlarmVolume());
    }

    @Test
    public void testFinish() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        ca.setAlarmVolume(ca.getAlarmMaxVolume() / 2);
        int alarmVolume = ca.getAlarmVolume();
        // exercise
        ca.startBellSound(null);
        ca.finishBellSound();
        // verify
        Assert.assertFalse(ca.isBellSoundPlaying());
        Assert.assertEquals(alarmVolume, ca.getAlarmVolume());
    }

    @Test
    public void testInFlightMode_false1() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(true);
        ca.getPrefs().setSettingMuteInFlightMode(false);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testInFlightMode_false2() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(false);
        ca.getPrefs().setSettingMuteInFlightMode(true);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testInFlightMode_true() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(true);
        ca.getPrefs().setSettingMuteInFlightMode(true);
        // exercise/verify
        Assert.assertTrue(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_false1() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneMuted(true);
        ca.getPrefs().setSettingMuteWithPhone(false);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_false2() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneMuted(false);
        ca.getPrefs().setSettingMuteWithPhone(true);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_true() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneMuted(true);
        ca.getPrefs().setSettingMuteWithPhone(true);
        // exercise/verify
        Assert.assertTrue(ca.isMuteRequested(true));
    }

    @Test
    public void testOffHook_false1() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneOffHook(true);
        ca.getPrefs().setSettingMuteOffHook(false);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testOffHook_false2() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneOffHook(false);
        ca.getPrefs().setSettingMuteOffHook(true);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testOffHook_true() {
        // setup
        MockContextAccessor ca = MockContextAccessor.getInstance();
        ca.setPhoneOffHook(true);
        ca.getPrefs().setSettingMuteOffHook(true);
        // exercise/verify
        Assert.assertTrue(ca.isMuteRequested(true));
    }

    @Test
    public void testOriginalVolume() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        int originalVolume = ca.getAlarmVolume();
        // exercise
        ca.startBellSound(null);
        ca.finishBellSound();
        // verify
        Assert.assertEquals(originalVolume, ca.getAlarmVolume());

    }

    @Test
    public void testPlay() {
        // setup
        ContextAccessor ca = MockContextAccessor.getInstance();
        // exercise
        ca.startBellSound(null);
        // verify
        Assert.assertTrue(ca.isBellSoundPlaying());
    }

    @Test
    public void testReasonableDefault() {
        ContextAccessor ca = MockContextAccessor.getInstance();
        float bellDefaultVolume = ca.getBellDefaultVolume();
        Assert.assertTrue(0 <= bellDefaultVolume);
        Assert.assertTrue(bellDefaultVolume <= 1);
    }
}
