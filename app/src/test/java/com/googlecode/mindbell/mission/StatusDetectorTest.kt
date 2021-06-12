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

import android.content.Context
import com.googlecode.mindbell.util.TimeOfDay
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class StatusDetectorTest {

    @RelaxedMockK
    private lateinit var prefs: Prefs

    @RelaxedMockK
    private lateinit var context: Context

    // SpyK cannot be used because a non-default constructor has to be used to inject mocked prefs
    private lateinit var statusDetector: StatusDetector

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        statusDetector = spyk(StatusDetector.Companion.getInstance(context, prefs))
        every { prefs.mutedTill } returns -1L
        every { prefs.isMuteInFlightMode } returns false
        every { prefs.isMuteOffHook } returns false
        every { prefs.isMuteWithPhone } returns false
        every { prefs.isMuteWithAudioStream } returns false
        every { prefs.daytimeStart } returns TimeOfDay(0, 0)
        every { prefs.daytimeEnd } returns TimeOfDay(0, 0)
        every { prefs.activeOnDaysOfWeek } returns HashSet(Arrays.asList("1", "2", "3", "4", "5", "6", "7"))
        every { statusDetector.isPhoneMuted } returns false
        every { statusDetector.isAudioStreamMuted } returns false
        every { statusDetector.isPhoneOffHook } returns false
        every { statusDetector.isPhoneInFlightMode } returns false
        every { statusDetector.reasonMutedTill } returns "bell manually muted till hh:mm"
        every { statusDetector.reasonMutedWithPhone } returns "bell muted with phone"
        every { statusDetector.reasonMutedWithAudioStream } returns "bell muted with audio stream"
        every { statusDetector.reasonMutedOffHook } returns "bell muted during calls"
        every { statusDetector.reasonMutedInFlightMode } returns "bell muted in flight mode"
        every { statusDetector.reasonMutedDuringNighttime } returns "bell muted during nighttime till dd hh:mm"
    }

    @Test
    fun testMutedDuringNighttime_false() {
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testMutedDuringNighttime_true() {
        val now = Calendar.getInstance().timeInMillis
        every { prefs.daytimeStart } returns TimeOfDay(now + Prefs.ONE_MINUTE_MILLIS)
        every { prefs.daytimeEnd } returns TimeOfDay(now - Prefs.ONE_MINUTE_MILLIS)
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_false1() {
        every { statusDetector.isPhoneInFlightMode } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_false2() {
        every { prefs.isMuteInFlightMode } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_true() {
        every { statusDetector.isPhoneInFlightMode } returns true
        every { prefs.isMuteInFlightMode } returns true
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testPhoneMuted_false1() {
        every { statusDetector.isPhoneMuted } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(true))
    }

    @Test
    fun testPhoneMuted_false2() {
        every { prefs.isMuteWithPhone } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testPhoneMuted_true() {
        every { statusDetector.isPhoneMuted } returns true
        every { prefs.isMuteWithPhone } returns true
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testAudioStreamMuted_false1() {
        every { statusDetector.isAudioStreamMuted } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(true))
    }

    @Test
    fun testAudioStreamMuted_false2() {
        every { prefs.isMuteWithAudioStream } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testAudioStreamMuted_true() {
        every { statusDetector.isAudioStreamMuted } returns true
        every { prefs.isMuteWithAudioStream } returns true
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testOffHook_false1() {
        every { statusDetector.isPhoneOffHook } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testOffHook_false2() {
        every { prefs.isMuteOffHook } returns true
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testOffHook_true() {
        every { statusDetector.isPhoneOffHook } returns true
        every { prefs.isMuteOffHook } returns true
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testReasonableDefault() {
        Assert.assertTrue(0 <= Prefs.DEFAULT_VOLUME)
        Assert.assertTrue(Prefs.DEFAULT_VOLUME <= 1)
    }

}
