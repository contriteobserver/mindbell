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
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContextAccessorTest {

    private lateinit var ca: ContextAccessor

    @Before
    fun setUp() {
        ca = ContextAccessor.getInstance(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testBellVolume() {
        // setup
        ca.prefs.resetOriginalVolume()
        // exercise
        ca.startReminderActions(ca.prefs.forRegularOperation(), null)
        // verify ... be sure to have sound checked as an activity on your emulated device
        // verify ... be sure to have pause audio unchecked on your emulated device
        // verify ... be sure to have audio stream set to alarm on your emulated device
        // verify ... be sure to have use audio stream volume disable on your emulated device
        Assert.assertEquals(ca.alarmMaxVolume, ca.alarmVolume)
    }

    private fun createContextAccessor(): ContextAccessor {
        return ContextAccessor.getInstance(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testFinish() {
        // setup
        ca.prefs.resetOriginalVolume()
        ca.alarmVolume = ca.alarmMaxVolume / 2
        val alarmVolume = ca.alarmVolume
        // exercise
        ca.startReminderActions(ca.prefs.forRegularOperation(), null)
        ca.finishBellSound()
        // verify
        Assert.assertFalse(ca.isBellSoundPlaying)
        Assert.assertEquals(alarmVolume, ca.alarmVolume)
    }

    @Test
    fun testOriginalVolume() {
        // setup
        ca.prefs.resetOriginalVolume()
        val originalVolume = ca.alarmVolume
        // exercise
        ca.startReminderActions(ca.prefs.forRegularOperation(), null)
        ca.finishBellSound()
        // verify
        Assert.assertEquals(originalVolume, ca.alarmVolume)
    }

    @Test
    fun testPlay() {
        // setup
        // exercise
        ca.startReminderActions(ca.prefs.forRegularOperation(), null)
        // verify ... be sure to have sound checked as an activity on your emulated device
        Assert.assertTrue(ca.isBellSoundPlaying)
    }

}
