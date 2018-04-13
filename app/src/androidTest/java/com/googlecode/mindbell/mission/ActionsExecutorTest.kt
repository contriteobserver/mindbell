/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2014-2018 Uwe Damken
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
package com.googlecode.mindbell.mission

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionsExecutorTest {

    private lateinit var actionsExecutor: ActionsExecutor

    private lateinit var prefs: Prefs

    @Before
    fun setUp() {
        actionsExecutor = ActionsExecutor.getInstance(InstrumentationRegistry.getTargetContext())
        prefs = Prefs.getInstance(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testBellVolume() {
        // setup
        prefs.resetOriginalVolume()
        // exercise
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        // verify ... be sure to have sound checked as an activity on your emulated device
        // verify ... be sure to have pause audio unchecked on your emulated device
        // verify ... be sure to have audio stream set to alarm on your emulated device
        // verify ... be sure to have use audio stream volume disable on your emulated device
        Assert.assertEquals(actionsExecutor.alarmMaxVolume, actionsExecutor.alarmVolume)
    }

    private fun createContextAccessor(): Scheduler {
        return Scheduler.getInstance(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testFinish() {
        // setup
        prefs.resetOriginalVolume()
        actionsExecutor.alarmVolume = actionsExecutor.alarmMaxVolume / 2
        val alarmVolume = actionsExecutor.alarmVolume
        // exercise
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        actionsExecutor.finishBellSound()
        // verify
        Assert.assertFalse(actionsExecutor.isBellSoundPlaying)
        Assert.assertEquals(alarmVolume, actionsExecutor.alarmVolume)
    }

    @Test
    fun testOriginalVolume() {
        // setup
        prefs.resetOriginalVolume()
        val originalVolume = actionsExecutor.alarmVolume
        // exercise
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        actionsExecutor.finishBellSound()
        // verify
        Assert.assertEquals(originalVolume, actionsExecutor.alarmVolume)
    }

    @Test
    fun testPlay() {
        // setup
        // exercise
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        // verify ... be sure to have sound checked as an activity on your emulated device
        Assert.assertTrue(actionsExecutor.isBellSoundPlaying)
    }

}
