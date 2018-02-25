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
package com.googlecode.mindbell.accessors

import android.content.Context
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
class ContextAccessorMockTest {

    @Mock
    internal var prefs: PrefsAccessor? = null

    @Mock
    internal var context: Context? = null

    internal var ca: ContextAccessor? = null

    @Before
    fun setup() {
        ca = Mockito.spy(ContextAccessor(context!!, prefs!!))
        `when`(prefs!!.mutedTill).thenReturn(-1L)
        `when`(prefs!!.isMuteInFlightMode).thenReturn(false)
        `when`(prefs!!.isMuteOffHook).thenReturn(false)
        `when`(prefs!!.isMuteWithPhone).thenReturn(false)
        `when`(prefs!!.isMuteWithAudioStream).thenReturn(false)
        `when`(prefs!!.daytimeStart).thenReturn(TimeOfDay(0, 0))
        `when`(prefs!!.daytimeEnd).thenReturn(TimeOfDay(0, 0))
        `when`(prefs!!.activeOnDaysOfWeek).thenReturn(HashSet(Arrays.asList(*arrayOf(1, 2, 3, 4, 5, 6, 7))))
        Mockito.doReturn(false).`when`(ca!!).isPhoneMuted
        Mockito.doReturn(false).`when`(ca!!).isAudioStreamMuted
        Mockito.doReturn(false).`when`(ca!!).isPhoneOffHook
        Mockito.doReturn(false).`when`(ca!!).isPhoneInFlightMode
        Mockito.doReturn("bell manually muted till hh:mm").`when`(ca!!).reasonMutedTill
        Mockito.doReturn("bell muted with phone").`when`(ca!!).reasonMutedWithPhone
        Mockito.doReturn("bell muted with audio stream").`when`(ca!!).reasonMutedWithAudioStream
        Mockito.doReturn("bell muted during calls").`when`(ca!!).reasonMutedOffHook
        Mockito.doReturn("bell muted in flight mode").`when`(ca!!).reasonMutedInFlightMode
        Mockito.doReturn("bell muted during nighttime till dd hh:mm").`when`(ca!!).reasonMutedDuringNighttime
        //        Mockito.doNothing().when(ca!!).showMessage(anyString());
    }

    @Test
    fun testMutedDuringNighttime_false() {
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testMutedDuringNighttime_true() {
        val now = Calendar.getInstance().timeInMillis
        `when`(prefs!!.daytimeStart).thenReturn(TimeOfDay(now + PrefsAccessor.ONE_MINUTE_MILLIS))
        `when`(prefs!!.daytimeEnd).thenReturn(TimeOfDay(now - PrefsAccessor.ONE_MINUTE_MILLIS))
        Assert.assertTrue(ca!!.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_false1() {
        Mockito.doReturn(true).`when`(ca!!).isPhoneInFlightMode
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_false2() {
        `when`(prefs!!.isMuteInFlightMode).thenReturn(true)
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testInFlightMode_true() {
        Mockito.doReturn(true).`when`(ca!!).isPhoneInFlightMode
        `when`(prefs!!.isMuteInFlightMode).thenReturn(true)
        Assert.assertTrue(ca!!.isMuteRequested(false))
    }

    @Test
    fun testPhoneMuted_false1() {
        Mockito.doReturn(true).`when`(ca!!).isPhoneMuted
        Assert.assertFalse(ca!!.isMuteRequested(true))
    }

    @Test
    fun testPhoneMuted_false2() {
        `when`(prefs!!.isMuteWithPhone).thenReturn(true)
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testPhoneMuted_true() {
        Mockito.doReturn(true).`when`(ca!!).isPhoneMuted
        `when`(prefs!!.isMuteWithPhone).thenReturn(true)
        Assert.assertTrue(ca!!.isMuteRequested(false))
    }

    @Test
    fun testAudioStreamMuted_false1() {
        Mockito.doReturn(true).`when`(ca!!).isAudioStreamMuted
        Assert.assertFalse(ca!!.isMuteRequested(true))
    }

    @Test
    fun testAudioStreamMuted_false2() {
        `when`(prefs!!.isMuteWithAudioStream).thenReturn(true)
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testAudioStreamMuted_true() {
        Mockito.doReturn(true).`when`(ca!!).isAudioStreamMuted
        `when`(prefs!!.isMuteWithAudioStream).thenReturn(true)
        Assert.assertTrue(ca!!.isMuteRequested(false))
    }

    @Test
    fun testOffHook_false1() {
        Mockito.doReturn(true).`when`(ca!!).isPhoneOffHook
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testOffHook_false2() {
        `when`(prefs!!.isMuteOffHook).thenReturn(true)
        Assert.assertFalse(ca!!.isMuteRequested(false))
    }

    @Test
    fun testOffHook_true() {
        Mockito.doReturn(true).`when`(ca!!).isPhoneOffHook
        `when`(prefs!!.isMuteOffHook).thenReturn(true)
        Assert.assertTrue(ca!!.isMuteRequested(false))
    }

    @Test
    fun testReasonableDefault() {
        Assert.assertTrue(0 <= PrefsAccessor.DEFAULT_VOLUME)
        Assert.assertTrue(PrefsAccessor.DEFAULT_VOLUME <= 1)
    }

}
