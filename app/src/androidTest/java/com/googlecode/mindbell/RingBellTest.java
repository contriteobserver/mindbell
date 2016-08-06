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
package com.googlecode.mindbell;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.RenamingDelegatingContext;
import android.test.suitebuilder.annotation.SmallTest;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.logic.RingingLogic;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RingBellTest {

    private Context context = null;
    private final Map<String, Boolean> booleanSettings = new HashMap<String, Boolean>();

    private final Runnable dummyRunnable = new Runnable() {
        public void run() {
        };
    };

    private Runnable getDummyRunnable() {
        return dummyRunnable;
    }

    private void setBooleanContext(int keyID, boolean value) {
        String key = context.getString(keyID);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        // Log.d(MindBellPreferences.LOGTAG, "The following settings are in the shared prefs:");
        // for (Entry<String, ?> k : sp.getAll().entrySet()) {
        // Log.d(MindBellPreferences.LOGTAG, k.getKey() + " = " + k.getValue());
        // }
        if (!booleanSettings.containsKey(key)) {
            if (sp.contains(key)) {
                boolean orig = sp.getBoolean(key, value);
                // Log.d(MindBellPreferences.LOGTAG, "Remembering setting: " + key + " == " + orig);
                booleanSettings.put(key, orig);
            } else {
                // Log.d(MindBellPreferences.LOGTAG, "Remembering that setting was unset: " + key);
                booleanSettings.put(key, null);
            }
        }
        SharedPreferences.Editor spe = sp.edit();
        spe.putBoolean(key, value);
        spe.commit();
    }

    private void setContextMuteInFlightMode(boolean value) {
        setBooleanContext(AndroidContextAccessor.KEYMUTEINFLIGHTMODE, value);
    }

    private void setContextMuteOffHook(boolean value) {
        setBooleanContext(AndroidContextAccessor.KEYMUTEOFFHOOK, value);
    }

    private void setContextMuteWithPhone(boolean value) {
        setBooleanContext(AndroidContextAccessor.KEYMUTEWITHPHONE, value);
    }

    @Before
    public void setUp() throws Exception {
        context = new RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test_");;
    }

    @After
    public void tearDown() throws Exception {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor spe = sp.edit();
        for (String key : booleanSettings.keySet()) {
            Boolean value = booleanSettings.get(key);
            if (value == null) {
                // Log.d(MindBellPreferences.LOGTAG, "Restoring setting to unset: " + key);
                spe.remove(key);
            } else {
                // Log.d(MindBellPreferences.LOGTAG, "Restoring setting: " + key + " = " + value);
                spe.putBoolean(key, value);
            }
        }
        spe.commit();
    }

    @Test
    public void testMuteOffHook_false() {
        // setup
        setContextMuteOffHook(false);
        // exercise
        ContextAccessor ca = AndroidContextAccessor.getInstance(context);
        // verify
        assertFalse(ca.getPrefs().isMuteOffHook());
    }

    @Test
    public void testMuteOffHook_true() {
        // setup
        setContextMuteOffHook(true);
        // exercise
        ContextAccessor ca = AndroidContextAccessor.getInstance(context);
        // verify
        assertTrue(ca.getPrefs().isMuteOffHook());
    }

    @Test
    public void testMuteWithPhone_false() {
        // setup
        setContextMuteWithPhone(false);
        // exercise
        ContextAccessor ca = AndroidContextAccessor.getInstance(context);
        // verify
        assertFalse(ca.getPrefs().isMuteWithPhone());
    }

    @Test
    public void testMuteWithPhone_true() {
        // setup
        setContextMuteWithPhone(true);
        // exercise
        ContextAccessor ca = AndroidContextAccessor.getInstance(context);
        // verify
        assertTrue(ca.getPrefs().isMuteWithPhone());
    }

    @Test
    public void testPreconditions() {
        assertNotNull(context);
    }

    @Test
    public void testRingBell_throwNPE1() {
        try {
            RingingLogic.ringBell((ContextAccessor) null, getDummyRunnable());
            fail("Should have thrown null pointer exception");
        } catch (NullPointerException npe) {
            // OK, expected
        }
    }

    @Test
    public void testRingBell_true() {
        // setup
        setContextMuteWithPhone(false);
        setContextMuteOffHook(false);
        // exercise
        boolean isRinging = RingingLogic.ringBell(AndroidContextAccessor.getInstance(context), null);
        // verify
        assertTrue(isRinging);
    }

    @Test
    public void testExpires() throws InterruptedException {
        // timeout shorter than sound duration: cannot finish
        long timeout = 100L;
        final RingingLogic.KeepAlive keepAlive = new RingingLogic.KeepAlive(AndroidContextAccessor.getInstance(context), timeout);
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
        // timeout longer than sound duration: finishing is easy (15000 as used in Scheduler)
        long timeout = 15000L;
        final RingingLogic.KeepAlive keepAlive = new RingingLogic.KeepAlive(AndroidContextAccessor.getInstance(context), timeout);
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
