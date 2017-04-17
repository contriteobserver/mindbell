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
package com.googlecode.mindbell.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class VolumeConverterTest {

    VolumeConverter c;

    @Before
    public void setUp() {
        c = new VolumeConverter(40, 100);
    }

    @Test
    public void testMaxIsMax_p2v() {
        int progress = c.mMaxProgress;
        float volume = c.progress2volume(progress);
        Assert.assertEquals(1, volume, 1.e-7);
    }

    @Test
    public void testMaxIsMax_v2p() {
        float volume = 1;
        int progress = c.volume2progress(volume);
        Assert.assertEquals(c.mMaxProgress, progress);
    }

    @Test
    public void testProgressWillNeverBeNegative() {
        float volume = 0.001f;
        int progress = c.volume2progress(volume);
        Assert.assertTrue("Progress should not be negative, but is " + progress, progress >= 0);
    }

    @Test
    public void testRoundtrip_p() {
        int progress = 17;
        float volume = c.progress2volume(progress);
        int result = c.volume2progress(volume);
        Assert.assertEquals(progress, result);
    }

    @Test
    public void testRoundtrip_v() {
        float volume = c.progress2volume(3);
        int progress = c.volume2progress(volume);
        float result = c.progress2volume(progress);
        Assert.assertEquals(volume, result, 1.e-7f);
    }

    @Test
    public void testZeroIsZero_p2v() {
        int progress = 0;
        float volume = c.progress2volume(progress);
        Assert.assertEquals(0, volume, 1.e-7);
    }

    @Test
    public void testZeroIsZero_v2p() {
        float volume = 0;
        int progress = c.volume2progress(volume);
        Assert.assertEquals(0, progress);
    }

}
