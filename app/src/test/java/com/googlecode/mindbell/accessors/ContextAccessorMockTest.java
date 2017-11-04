/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
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

import android.content.Context;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
@SmallTest
public class ContextAccessorMockTest {

    @Mock
    AndroidPrefsAccessor prefs;

    @Mock
    Context context;

    ContextAccessor ca;

    @Before
    public void setup() {
        ca = Mockito.spy(new ContextAccessor(context, prefs));
        when(prefs.getMutedTill()).thenReturn(-1L);
        when(prefs.isMuteInFlightMode()).thenReturn(false);
        when(prefs.isMuteOffHook()).thenReturn(false);
        when(prefs.isMuteWithPhone()).thenReturn(false);
        Mockito.doReturn(false).when(ca).isPhoneMuted();
        Mockito.doReturn(false).when(ca).isPhoneOffHook();
        Mockito.doReturn(false).when(ca).isPhoneInFlightMode();
        Mockito.doReturn("bell manually muted till hh:mm").when(ca).getReasonMutedTill();
        Mockito.doReturn("bell muted with phone").when(ca).getReasonMutedWithPhone();
        Mockito.doReturn("bell muted during calls").when(ca).getReasonMutedOffHook();
        Mockito.doReturn("bell muted in flight mode").when(ca).getReasonMutedInFlightMode();
        //        Mockito.doNothing().when(ca).showMessage(anyString());
    }

    @Test
    public void testInFlightMode_false1() {
        Mockito.doReturn(true).when(ca).isPhoneInFlightMode();
        Assert.assertFalse(ca.isMuteRequested(false));
    }

    @Test
    public void testInFlightMode_false2() {
        when(prefs.isMuteInFlightMode()).thenReturn(true);
        Assert.assertFalse(ca.isMuteRequested(false));
    }

    @Test
    public void testInFlightMode_true() {
        Mockito.doReturn(true).when(ca).isPhoneInFlightMode();
        when(prefs.isMuteInFlightMode()).thenReturn(true);
        Assert.assertTrue(ca.isMuteRequested(false));
    }

    @Test
    public void testMuted_false1() {
        Mockito.doReturn(true).when(ca).isPhoneMuted();
        Assert.assertFalse(ca.isMuteRequested(true));
    }

    @Test
    public void testMuted_false2() {
        when(prefs.isMuteWithPhone()).thenReturn(true);
        Assert.assertFalse(ca.isMuteRequested(false));
    }

    @Test
    public void testMuted_true() {
        Mockito.doReturn(true).when(ca).isPhoneMuted();
        when(prefs.isMuteWithPhone()).thenReturn(true);
        Assert.assertTrue(ca.isMuteRequested(false));
    }

    @Test
    public void testOffHook_false1() {
        Mockito.doReturn(true).when(ca).isPhoneOffHook();
        Assert.assertFalse(ca.isMuteRequested(false));
    }

    @Test
    public void testOffHook_false2() {
        when(prefs.isMuteOffHook()).thenReturn(true);
        Assert.assertFalse(ca.isMuteRequested(false));
    }

    @Test
    public void testOffHook_true() {
        Mockito.doReturn(true).when(ca).isPhoneOffHook();
        when(prefs.isMuteOffHook()).thenReturn(true);
        Assert.assertTrue(ca.isMuteRequested(false));
    }

    @Test
    public void testReasonableDefault() {
        Assert.assertTrue(0 <= AndroidPrefsAccessor.DEFAULT_VOLUME);
        Assert.assertTrue(AndroidPrefsAccessor.DEFAULT_VOLUME <= 1);
    }

}
