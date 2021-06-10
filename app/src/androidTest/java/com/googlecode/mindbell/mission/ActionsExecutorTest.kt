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
package com.googlecode.mindbell.mission

import android.media.AudioManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        prefs.resetSettings()
        prefs.isSound = true
        prefs.isPauseAudioOnSound = false
        prefs.isUseAudioStreamVolumeSetting = false
        prefs.audioStream = AudioManager.STREAM_ALARM
    }

    @Test
    fun testBellVolume() {
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        Assert.assertEquals(actionsExecutor.alarmMaxVolume, actionsExecutor.alarmVolume)
    }

    private fun createContextAccessor(): Scheduler {
        return Scheduler.getInstance(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testFinish() {
        actionsExecutor.alarmVolume = actionsExecutor.alarmMaxVolume / 2
        val alarmVolume = actionsExecutor.alarmVolume
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        actionsExecutor.finishBellSound()
        Assert.assertFalse(actionsExecutor.isBellSoundPlaying)
        Assert.assertEquals(alarmVolume, actionsExecutor.alarmVolume)
    }

    @Test
    fun testOriginalVolume() {
        val originalVolume = actionsExecutor.alarmVolume
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        actionsExecutor.finishBellSound()
        Assert.assertEquals(originalVolume, actionsExecutor.alarmVolume)
    }

    @Test
    fun testPlay() {
        actionsExecutor.startInterruptActions(prefs.forRegularOperation(), null)
        Assert.assertTrue(actionsExecutor.isBellSoundPlaying)
    }

}
