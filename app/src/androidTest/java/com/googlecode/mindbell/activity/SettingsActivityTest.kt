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
import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.widget.TimePicker
import com.googlecode.mindbell.R.array.*
import com.googlecode.mindbell.R.id.iconText
import com.googlecode.mindbell.R.id.textViewSummary
import com.googlecode.mindbell.R.string.*
import com.googlecode.mindbell.ToastMatcher.Companion.checkDisplayedAndDisappearedOnToast
import com.googlecode.mindbell.ToastMatcher.Companion.onToast
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.Utils
import junit.framework.Assert.*
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(SettingsActivity::class.java, false, false) // don't launch

    private lateinit var context: Context

    private lateinit var prefs: Prefs

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getTargetContext()
        prefs = Prefs.getInstance(context)
        prefs.resetSettings()
        resources = context.resources
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

    @Test
    fun testActiveTimesPreferencesPage() {

        // Navigate to sound output preferences page
        onView(allOf(withId(title), withText(prefsCategoryActiveTimes))).perform(click())
        onView(allOf(withText(prefsCategoryActiveTimes), not(withText(preferencesTitle)))).check(matches(isDisplayed()))

        // Fetch text views of active times preferences page
        val textViewStart = onView(allOf(withId(summary), hasSibling(withText(prefsStart))))
        val textViewEnd = onView(allOf(withId(summary), hasSibling(withText(prefsEnd))))
        val textViewActiveOnDaysOfWeek = onView(allOf(withId(summary), hasSibling(withText(prefsActiveOnDaysOfWeek))))

        // Check default settings in UI and Prefs
        textViewStart.check(matches(withText(TimeOfDay(9, 0).getDisplayString(context))))
        assertEquals(TimeOfDay(9, 0), prefs.daytimeStart)

        textViewEnd.check(matches(withText(TimeOfDay(21, 0).getDisplayString(context))))
        assertEquals(TimeOfDay(21, 0), prefs.daytimeEnd)

        val wholeWeekSet = HashSet(Arrays.asList("1", "2", "3", "4", "5", "6", "7"))
        val wholeWeekSummary = Utils.deriveOrderedEntrySummary(wholeWeekSet, resources.getStringArray(weekdayEntries), resources.getStringArray(weekdayEntryValues))
        textViewActiveOnDaysOfWeek.check(matches(withText(wholeWeekSummary)))
        assertEquals(TimeOfDay(21, 0), prefs.daytimeEnd)

        // Choose start at 11:12
        textViewStart.perform(ViewActions.scrollTo(), click())
        onView(withText(prefsStart)).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(11, 12))
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(ViewActions.scrollTo(), click())

        textViewStart.check(matches(withText(TimeOfDay(11, 12).getDisplayString(context))))
        assertEquals(TimeOfDay(11, 12), prefs.daytimeStart)

        // Choose end at 23:24
        textViewEnd.perform(ViewActions.scrollTo(), click())
        onView(withText(prefsEnd)).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(23, 24))
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(ViewActions.scrollTo(), click())

        textViewEnd.check(matches(withText(TimeOfDay(23, 24).getDisplayString(context))))
        assertEquals(TimeOfDay(23, 24), prefs.daytimeEnd)

        // Choose only Monday
        textViewActiveOnDaysOfWeek.perform(click())  // open selection
        for (i in 0..6) {
            onView(withText(resources.getStringArray(weekdayEntries)[i])).perform(click()) // un-choose all days
        }
        val mondaySet = HashSet(Arrays.asList("2"))
        val mondayWeekSummary = Utils.deriveOrderedEntrySummary(mondaySet, resources.getStringArray(weekdayEntries), resources.getStringArray(weekdayEntryValues))
        onView(withText(mondayWeekSummary)).perform(click()) // choose only Monday
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(ViewActions.scrollTo(), click())
        textViewActiveOnDaysOfWeek.check(matches(withText(mondayWeekSummary)))
        assertEquals(mondaySet.toString(), prefs.activeOnDaysOfWeek.toString())

    }

}
