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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AndroidContextAccessorTest extends AndroidTestCase {

    private ContextAccessor createContextAccessor() {
        Context context = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test_");;
        return AndroidContextAccessor.getInstance(context);
    }

    @Test
    public void testBellVolume() {
        // setup
        ContextAccessor ca = createContextAccessor();
        ca.getPrefs().resetOriginalVolume();
        // exercise
        ca.startPlayingSoundAndVibrate(ca.getPrefs().forRegularOperation(), null);
        // verify ... be sure to have sound checked as an activity on your emulated device
        assertEquals(ca.getAlarmMaxVolume(), ca.getAlarmVolume());
    }

    @Test
    public void testFinish() {
        // setup
        ContextAccessor ca = createContextAccessor();
        ca.getPrefs().resetOriginalVolume();
        ca.setAlarmVolume(ca.getAlarmMaxVolume() / 2);
        int alarmVolume = ca.getAlarmVolume();
        // exercise
        ca.startPlayingSoundAndVibrate(ca.getPrefs().forRegularOperation(), null);
        ca.finishBellSound();
        // verify
        assertFalse(ca.isBellSoundPlaying());
        assertEquals(alarmVolume, ca.getAlarmVolume());
    }

    @Test
    public void testOriginalVolume() {
        // setup
        ContextAccessor ca = createContextAccessor();
        ca.getPrefs().resetOriginalVolume();
        int originalVolume = ca.getAlarmVolume();
        // exercise
        ca.startPlayingSoundAndVibrate(ca.getPrefs().forRegularOperation(), null);
        ca.finishBellSound();
        // verify
        assertEquals(originalVolume, ca.getAlarmVolume());
    }

    @Test
    public void testPlay() {
        // setup
        ContextAccessor ca = createContextAccessor();
        // exercise
        ca.startPlayingSoundAndVibrate(ca.getPrefs().forRegularOperation(), null);
        // verify ... be sure to have sound checked as an activity on your emulated device
        assertTrue(ca.isBellSoundPlaying());
    }

    @Test
    public void testInFlightMode_false1() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(true);
        ca.getPrefs().setMuteInFlightMode(false);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testInFlightMode_false2() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(false);
        ca.getPrefs().setMuteInFlightMode(true);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testInFlightMode_true() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneInFlightMode(true);
        ca.getPrefs().setMuteInFlightMode(true);
        // exercise/verify
        Assert.assertTrue(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_false1() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneMuted(true);
        ca.getPrefs().setMuteWithPhone(false);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_false2() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneMuted(false);
        ca.getPrefs().setMuteWithPhone(true);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_true() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneMuted(true);
        ca.getPrefs().setMuteWithPhone(true);
        // exercise/verify
        Assert.assertTrue(ca.isMuteRequested(true));
    }

    @Test
    public void testOffHook_false1() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneOffHook(true);
        ca.getPrefs().setMuteOffHook(false);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testOffHook_false2() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneOffHook(false);
        ca.getPrefs().setMuteOffHook(true);
        // exercise/verify
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testOffHook_true() {
        // setup
        AndroidTestMockContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        ca.setPhoneOffHook(true);
        ca.getPrefs().setMuteOffHook(true);
        // exercise/verify
        Assert.assertTrue(ca.isMuteRequested(true));
    }

    @Test
    public void testReasonableDefault() {
        ContextAccessor ca = AndroidTestMockContextAccessor.getInstance();
        Assert.assertTrue(0 <= AndroidPrefsAccessor.DEFAULT_VOLUME);
        Assert.assertTrue(AndroidPrefsAccessor.DEFAULT_VOLUME <= 1);
    }

}
