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
import android.content.SharedPreferences
import android.test.suitebuilder.annotation.SmallTest
import com.googlecode.mindbell.util.TimeOfDay
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner.Silent::class)
@SmallTest
class StatusDetectorMockTest {

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var prefs: Prefs

    @Mock
    private lateinit var context: Context

    // Spy in setup()
    private lateinit var statusDetector: StatusDetector

    @Before
    fun setup() {
        statusDetector = Mockito.spy(StatusDetector.getInstance(context))
        `when`(prefs.mutedTill).thenReturn(-1L)
        `when`(prefs.isMuteInFlightMode).thenReturn(false)
        `when`(prefs.isMuteOffHook).thenReturn(false)
        `when`(prefs.isMuteWithPhone).thenReturn(false)
        `when`(prefs.isMuteWithAudioStream).thenReturn(false)
        `when`(prefs.daytimeStart).thenReturn(TimeOfDay(0, 0))
        `when`(prefs.daytimeEnd).thenReturn(TimeOfDay(0, 0))
        `when`(prefs.activeOnDaysOfWeek).thenReturn(HashSet(Arrays.asList(*arrayOf(1, 2, 3, 4, 5, 6, 7))))
        Mockito.doReturn(false).`when`(statusDetector).isPhoneMuted
        Mockito.doReturn(false).`when`(statusDetector).isAudioStreamMuted
        Mockito.doReturn(false).`when`(statusDetector).isPhoneOffHook
        Mockito.doReturn(false).`when`(statusDetector).isPhoneInFlightMode
        Mockito.doReturn("bell manually muted till hh:mm").`when`(statusDetector).reasonMutedTill
        Mockito.doReturn("bell muted with phone").`when`(statusDetector).reasonMutedWithPhone
        Mockito.doReturn("bell muted with audio stream").`when`(statusDetector).reasonMutedWithAudioStream
        Mockito.doReturn("bell muted during calls").`when`(statusDetector).reasonMutedOffHook
        Mockito.doReturn("bell muted in flight mode").`when`(statusDetector).reasonMutedInFlightMode
        Mockito.doReturn("bell muted during nighttime till dd hh:mm").`when`(statusDetector).reasonMutedDuringNighttime
        //        Mockito.doNothing().when(statusDetector).showMessage(anyString());
    }

    @Test
    fun testMutedDuringNighttime_false() {
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testMutedDuringNighttime_true() {
        val now = Calendar.getInstance().timeInMillis
        `when`(prefs.daytimeStart).thenReturn(TimeOfDay(now + Prefs.ONE_MINUTE_MILLIS))
        `when`(prefs.daytimeEnd).thenReturn(TimeOfDay(now - Prefs.ONE_MINUTE_MILLIS))
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_false1() {
        Mockito.doReturn(true).`when`(statusDetector).isPhoneInFlightMode
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_false2() {
        `when`(prefs.isMuteInFlightMode).thenReturn(true)
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_true() {
        Mockito.doReturn(true).`when`(statusDetector).isPhoneInFlightMode
        `when`(prefs.isMuteInFlightMode).thenReturn(true)
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testPhoneMuted_false1() {
        Mockito.doReturn(true).`when`(statusDetector).isPhoneMuted
        Assert.assertFalse(statusDetector.isMuteRequested(true))
    }

    @Test
    fun testPhoneMuted_false2() {
        `when`(prefs.isMuteWithPhone).thenReturn(true)
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testPhoneMuted_true() {
        Mockito.doReturn(true).`when`(statusDetector).isPhoneMuted
        `when`(prefs.isMuteWithPhone).thenReturn(true)
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testAudioStreamMuted_false1() {
        Mockito.doReturn(true).`when`(statusDetector).isAudioStreamMuted
        Assert.assertFalse(statusDetector.isMuteRequested(true))
    }

    @Test
    fun testAudioStreamMuted_false2() {
        `when`(prefs.isMuteWithAudioStream).thenReturn(true)
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testAudioStreamMuted_true() {
        Mockito.doReturn(true).`when`(statusDetector).isAudioStreamMuted
        `when`(prefs.isMuteWithAudioStream).thenReturn(true)
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testOffHook_false1() {
        Mockito.doReturn(true).`when`(statusDetector).isPhoneOffHook
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testOffHook_false2() {
        `when`(prefs.isMuteOffHook).thenReturn(true)
        Assert.assertFalse(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testOffHook_true() {
        Mockito.doReturn(true).`when`(statusDetector).isPhoneOffHook
        `when`(prefs.isMuteOffHook).thenReturn(true)
        Assert.assertTrue(statusDetector.isMuteRequested(false))
    }

    @Test
    fun testReasonableDefault() {
        Assert.assertTrue(0 <= Prefs.DEFAULT_VOLUME)
        Assert.assertTrue(Prefs.DEFAULT_VOLUME <= 1)
    }

}
