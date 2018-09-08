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

package com.googlecode.mindbell.activity


import android.R.id.summary
import android.R.id.title
import android.content.res.Resources
import android.media.AudioManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import com.googlecode.mindbell.R.array.audioStreamEntries
import com.googlecode.mindbell.R.id.iconText
import com.googlecode.mindbell.R.id.textViewSummary
import com.googlecode.mindbell.R.string.*
import com.googlecode.mindbell.ToastMatcher.Companion.checkDisplayedAndDisappearedOnToast
import com.googlecode.mindbell.ToastMatcher.Companion.onToast
import com.googlecode.mindbell.mission.Prefs
import junit.framework.Assert.*
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(SettingsActivity::class.java, false, false) // don't launch

    private lateinit var prefs: Prefs

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        prefs = Prefs.getInstance(InstrumentationRegistry.getTargetContext())
        prefs.resetSettings()
        resources = InstrumentationRegistry.getTargetContext().resources
        activityTestRule.launchActivity(null) // launch activity now to get reset preferences
    }

    @Test
    fun testActivityIsDisplayed() {
        onView(withText(preferencesTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun testSoundOutputPreferencesPage() {

        // Navigate to sound output preferences page
        onView(allOf(withId(title), withText(prefsCategorySoundOutput))).perform(click())
        onView(allOf(withText(prefsCategorySoundOutput), not(withText(preferencesTitle)))).check(matches(isDisplayed()))

        // Fetch text views of sound output preferences page
        val textViewUseAudioStreamVolumeSetting = onView(allOf(withId(summary), hasSibling(withText(prefsUseAudioStreamVolumeSetting))))
        val textViewAudioStream = onView(allOf(withId(textViewSummary), hasSibling(withText(prefsAudioStream))))

        // Check default settings in UI and Prefs
        textViewUseAudioStreamVolumeSetting.check(matches(withText(summaryUseAudioStreamVolumeSetting)))
        assertTrue(prefs.isUseAudioStreamVolumeSetting)

        textViewAudioStream.check(matches(withText(resources.getStringArray(audioStreamEntries)[0])))
        assertEquals(AudioManager.STREAM_ALARM, prefs.audioStream)

        // Switch OFF isUseAudioStreamVolumeSetting and check in UI and Prefs
        textViewUseAudioStreamVolumeSetting.perform(click()).check(matches(withText(summaryDontUseAudioStreamVolumeSetting)))
        assertFalse(prefs.isUseAudioStreamVolumeSetting)

        textViewAudioStream.check(matches(not(isEnabled())))

        // Switch ON isUseAudioStreamVolumeSetting and check in UI and Prefs
        textViewUseAudioStreamVolumeSetting.perform(click()).check(matches(withText(summaryUseAudioStreamVolumeSetting)))
        assertTrue(prefs.isUseAudioStreamVolumeSetting)

        textViewAudioStream.check(matches(isEnabled()))

        // Choose audio stream NOTIFICATION and check in UI and Prefs
        textViewAudioStream.perform(click())
        onView(allOf(withId(iconText), withText(resources.getStringArray(audioStreamEntries)[1]))).perform(click())
        textViewAudioStream.check(matches(withText(resources.getStringArray(audioStreamEntries)[1])))
        assertEquals(AudioManager.STREAM_NOTIFICATION, prefs.audioStream)

        onToast(mustUseAudioStreamSetting).check(doesNotExist())

        // Audio stream != ALARM => isUseAudioStreamVolumeSetting must remain to be ON
        textViewUseAudioStreamVolumeSetting.perform(click())

        checkDisplayedAndDisappearedOnToast(mustUseAudioStreamSetting)

        // Choose audio stream MUSIC and check in UI and Prefs
        textViewAudioStream.perform(click())
        onView(allOf(withId(iconText), withText(resources.getStringArray(audioStreamEntries)[2]))).perform(click())
        textViewAudioStream.check(matches(withText(resources.getStringArray(audioStreamEntries)[2])))
        assertEquals(AudioManager.STREAM_MUSIC, prefs.audioStream)

        onToast(mustUseAudioStreamSetting).check(doesNotExist())

        // Audio stream != ALARM => isUseAudioStreamVolumeSetting must remain to be ON
        textViewUseAudioStreamVolumeSetting.perform(click())

        checkDisplayedAndDisappearedOnToast(mustUseAudioStreamSetting)

        // Choose audio stream ALARM and check in UI and Prefs
        textViewAudioStream.perform(click())
        onView(allOf(withId(iconText), withText(resources.getStringArray(audioStreamEntries)[0]))).perform(click())
        textViewAudioStream.check(matches(withText(resources.getStringArray(audioStreamEntries)[0])))
        assertEquals(AudioManager.STREAM_ALARM, prefs.audioStream)

        // Switch OFF isUseAudioStreamVolumeSetting and check in UI and Prefs
        textViewUseAudioStreamVolumeSetting.perform(click()).check(matches(withText(summaryDontUseAudioStreamVolumeSetting)))
        assertFalse(prefs.isUseAudioStreamVolumeSetting)

    }

}
