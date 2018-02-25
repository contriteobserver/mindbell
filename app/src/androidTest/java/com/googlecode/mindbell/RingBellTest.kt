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
package com.googlecode.mindbell

import android.content.Context
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.test.RenamingDelegatingContext
import com.googlecode.mindbell.accessors.ContextAccessor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class RingBellTest {

    private val booleanSettings = HashMap<String, Boolean>()
    lateinit private var context: Context

    private fun setBooleanContext(keyID: Int, value: Boolean) {
        val key = context.getString(keyID)
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        // Log.d(MindBellPreferences.LOGTAG, "The following settings are in the shared prefs:");
        // for (Entry<String, ?> k : sp.getAll().entrySet()) {
        // Log.d(MindBellPreferences.LOGTAG, k.getKey() + " = " + k.getValue());
        // }
        if (!booleanSettings.containsKey(key)) {
            if (sp.contains(key)) {
                val orig = sp.getBoolean(key, value)
                // Log.d(MindBellPreferences.LOGTAG, "Remembering setting: " + key + " == " + orig);
                booleanSettings.put(key, orig)
//            } else {
                // Log.d(MindBellPreferences.LOGTAG, "Remembering that setting was unset: " + key);
//                booleanSettings.put(key, null)
            }
        }
        val spe = sp.edit()
        spe.putBoolean(key, value)
        spe.commit()
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        context = RenamingDelegatingContext(InstrumentationRegistry.getTargetContext(), "test_")
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val spe = sp.edit()
        for (key in booleanSettings.keys) {
            val value = booleanSettings[key]
            if (value == null) {
                // Log.d(MindBellPreferences.LOGTAG, "Restoring setting to unset: " + key);
                spe.remove(key)
            } else {
                // Log.d(MindBellPreferences.LOGTAG, "Restoring setting: " + key + " = " + value);
                spe.putBoolean(key, value)
            }
        }
        spe.commit()
    }

    @Test
    fun testMuteOffHook_false() {
        // setup
        setContextMuteOffHook(false)
        // exercise
        val ca = ContextAccessor.getInstance(context)
        // verify
        assertFalse(ca.prefs.isMuteOffHook)
    }

    private fun setContextMuteOffHook(value: Boolean) {
        setBooleanContext(R.string.keyMuteOffHook, value)
    }

    @Test
    fun testMuteOffHook_true() {
        // setup
        setContextMuteOffHook(true)
        // exercise
        val ca = ContextAccessor.getInstance(context)
        // verify
        assertTrue(ca.prefs.isMuteOffHook)
    }

    @Test
    fun testMuteWithPhone_false() {
        // setup
        setContextMuteWithPhone(false)
        // exercise
        val ca = ContextAccessor.getInstance(context)
        // verify
        assertFalse(ca.prefs.isMuteWithPhone)
    }

    private fun setContextMuteWithPhone(value: Boolean) {
        setBooleanContext(R.string.keyMuteWithPhone, value)
    }

    @Test
    fun testMuteWithPhone_true() {
        // setup
        setContextMuteWithPhone(true)
        // exercise
        val ca = ContextAccessor.getInstance(context)
        // verify
        assertTrue(ca.prefs.isMuteWithPhone)
    }

    @Test
    fun testPreconditions() {
        assertNotNull(context)
    }

}
