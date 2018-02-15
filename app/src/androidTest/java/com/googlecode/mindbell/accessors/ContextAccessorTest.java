/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
 *     Copyright (C) 2014-2017 Uwe Damken
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
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ContextAccessorTest extends AndroidTestCase {

    @Test
    public void testBellVolume() {
        // setup
        ContextAccessor ca = createContextAccessor();
        PrefsAccessor prefs = ca.getPrefs();
        prefs.resetOriginalVolume();
        // exercise
        ca.startPlayingSoundAndVibrate(prefs.forRegularOperation(), null);
        // verify ... be sure to have sound checked as an activity on your emulated device
        // verify ... be sure to have pause audio unchecked on your emulated device
        // verify ... be sure to have audio stream set to alarm on your emulated device
        // verify ... be sure to have use audio stream volume disable on your emulated device
        assertEquals(ca.getAlarmMaxVolume(), ca.getAlarmVolume());
    }

    private ContextAccessor createContextAccessor() {
        Context context = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test_");
        return ContextAccessor.getInstance(context);
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

}
