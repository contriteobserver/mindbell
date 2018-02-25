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
package com.googlecode.mindbell.accessors

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.AndroidTestCase
import android.test.RenamingDelegatingContext
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContextAccessorTest : AndroidTestCase() {

    @Test
    fun testBellVolume() {
        // setup
        val ca = createContextAccessor()
        val prefs = ca.prefs
        prefs.resetOriginalVolume()
        // exercise
        ca.startPlayingSoundAndVibrate(prefs.forRegularOperation(), null)
        // verify ... be sure to have sound checked as an activity on your emulated device
        // verify ... be sure to have pause audio unchecked on your emulated device
        // verify ... be sure to have audio stream set to alarm on your emulated device
        // verify ... be sure to have use audio stream volume disable on your emulated device
        Assert.assertEquals(ca.alarmMaxVolume, ca.alarmVolume)
    }

    private fun createContextAccessor(): ContextAccessor {
        val context = RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test_")
        return ContextAccessor.getInstance(context)
    }

    @Test
    fun testFinish() {
        // setup
        val ca = createContextAccessor()
        ca.prefs.resetOriginalVolume()
        ca.alarmVolume = ca.alarmMaxVolume / 2
        val alarmVolume = ca.alarmVolume
        // exercise
        ca.startPlayingSoundAndVibrate(ca.prefs.forRegularOperation(), null)
        ca.finishBellSound()
        // verify
        Assert.assertFalse(ca.isBellSoundPlaying)
        Assert.assertEquals(alarmVolume, ca.alarmVolume)
    }

    @Test
    fun testOriginalVolume() {
        // setup
        val ca = createContextAccessor()
        ca.prefs.resetOriginalVolume()
        val originalVolume = ca.alarmVolume
        // exercise
        ca.startPlayingSoundAndVibrate(ca.prefs.forRegularOperation(), null)
        ca.finishBellSound()
        // verify
        Assert.assertEquals(originalVolume, ca.alarmVolume)
    }

    @Test
    fun testPlay() {
        // setup
        val ca = createContextAccessor()
        // exercise
        ca.startPlayingSoundAndVibrate(ca.prefs.forRegularOperation(), null)
        // verify ... be sure to have sound checked as an activity on your emulated device
        Assert.assertTrue(ca.isBellSoundPlaying)
    }

}
