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
package com.googlecode.mindbell.util

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class VolumeConverterTest {

    internal var c: VolumeConverter

    @Before
    fun setUp() {
        c = VolumeConverter(40, 100)
    }

    @Test
    fun testMaxIsMax_p2v() {
        val progress = c.mMaxProgress
        val volume = c.progress2volume(progress)
        Assert.assertEquals(1.0, volume.toDouble(), 1e-7)
    }

    @Test
    fun testMaxIsMax_v2p() {
        val volume = 1f
        val progress = c.volume2progress(volume)
        Assert.assertEquals(c.mMaxProgress.toLong(), progress.toLong())
    }

    @Test
    fun testProgressWillNeverBeNegative() {
        val volume = 0.001f
        val progress = c.volume2progress(volume)
        Assert.assertTrue("Progress should not be negative, but is " + progress, progress >= 0)
    }

    @Test
    fun testRoundtrip_p() {
        val progress = 17
        val volume = c.progress2volume(progress)
        val result = c.volume2progress(volume)
        Assert.assertEquals(progress.toLong(), result.toLong())
    }

    @Test
    fun testRoundtrip_v() {
        val volume = c.progress2volume(3)
        val progress = c.volume2progress(volume)
        val result = c.progress2volume(progress)
        Assert.assertEquals(volume, result, 1e-7f)
    }

    @Test
    fun testZeroIsZero_p2v() {
        val progress = 0
        val volume = c.progress2volume(progress)
        Assert.assertEquals(0.0, volume.toDouble(), 1e-7)
    }

    @Test
    fun testZeroIsZero_v2p() {
        val volume = 0f
        val progress = c.volume2progress(volume)
        Assert.assertEquals(0, progress.toLong())
    }

}
