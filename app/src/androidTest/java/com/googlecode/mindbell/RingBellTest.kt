/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
package com.googlecode.mindbell

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.googlecode.mindbell.mission.Prefs
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class RingBellTest {

    private val booleanSettings = HashMap<String, Boolean>()
    private lateinit var context: Context

    private fun setBooleanContext(keyID: Int, value: Boolean) {
        val key = context.getString(keyID)
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        if (!booleanSettings.containsKey(key)) {
            if (sp.contains(key)) {
                val orig = sp.getBoolean(key, value)
                booleanSettings.put(key, orig)
            }
        }
        val spe = sp.edit()
        spe.putBoolean(key, value)
        spe.commit()
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getTargetContext()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val spe = sp.edit()
        for (key in booleanSettings.keys) {
            val value = booleanSettings[key]
            if (value == null) {
                spe.remove(key)
            } else {
                spe.putBoolean(key, value)
            }
        }
        spe.commit()
    }

    @Test
    fun testMuteOffHook_false() {
        setContextMuteOffHook(false)
        val prefs = Prefs.getInstance(context)
        assertFalse(prefs.isMuteOffHook)
    }

    private fun setContextMuteOffHook(value: Boolean) {
        setBooleanContext(R.string.keyMuteOffHook, value)
    }

    @Test
    fun testMuteOffHook_true() {
        setContextMuteOffHook(true)
        val prefs = Prefs.getInstance(context)
        assertTrue(prefs.isMuteOffHook)
    }

    @Test
    fun testMuteWithPhone_false() {
        setContextMuteWithPhone(false)
        val prefs = Prefs.getInstance(context)
        assertFalse(prefs.isMuteWithPhone)
    }

    private fun setContextMuteWithPhone(value: Boolean) {
        setBooleanContext(R.string.keyMuteWithPhone, value)
    }

    @Test
    fun testMuteWithPhone_true() {
        setContextMuteWithPhone(true)
        val prefs = Prefs.getInstance(context)
        assertTrue(prefs.isMuteWithPhone)
    }

    @Test
    fun testPreconditions() {
        assertNotNull(context)
    }

}
