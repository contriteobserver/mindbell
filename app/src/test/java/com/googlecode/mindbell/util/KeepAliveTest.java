/*******************************************************************************
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
 *******************************************************************************/
package com.googlecode.mindbell.util;

import com.googlecode.mindbell.logic.RingingLogic;
import com.googlecode.mindbell.accessors.MockContextAccessor;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class KeepAliveTest {

    @Test
    public void testExpires() throws InterruptedException {
        MockContextAccessor mca = MockContextAccessor.getInstance();
        // timeout shorter than sound duration: cannot finish
        long timeout = mca.getSoundDuration() / 10;
        final RingingLogic.KeepAlive keepAlive = new RingingLogic.KeepAlive(mca, timeout);
        // exercise: it gets back before the timeout
        Thread th = new Thread() {
            @Override
            public void run() {
                keepAlive.ringBell();
            }
        };
        th.start();
        th.join(2 * timeout);
        if (th.isAlive()) {
            Assert.fail("KeepAlive doesn't expire as it should");
        }
    }

    @Test
    public void testReturnsNaturally() throws InterruptedException {
        MockContextAccessor mca = MockContextAccessor.getInstance();
        // timeout longer than sound duration: finishing is easy
        long timeout = mca.getSoundDuration() * 10;
        final RingingLogic.KeepAlive keepAlive = new RingingLogic.KeepAlive(mca, timeout);
        // exercise: it gets back before the timeout
        Thread th = new Thread() {
            @Override
            public void run() {
                keepAlive.ringBell();
            }
        };
        th.start();
        th.join(2 * timeout);
        if (th.isAlive()) {
            Assert.fail("KeepAlive doesn't return naturally as it should");
        }
    }

}
