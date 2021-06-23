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

package com.googlecode.mindbell.activity

import android.R
import android.R.id.summary
import android.R.id.title
import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.os.Build
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.doesNotExist
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.PreferenceMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.widget.TimePicker
import com.googlecode.mindbell.R.array.*
import com.googlecode.mindbell.R.id.iconText
import com.googlecode.mindbell.R.id.textViewSummary
import com.googlecode.mindbell.R.string.*
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.mission.Prefs.Companion.DEFAULT_VOLUME
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS
import com.googlecode.mindbell.util.MoreViewMatchers.Companion.isDialogTitle
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.ToastMatcher.Companion.checkDisplayedAndDisappearedOnToast
import com.googlecode.mindbell.util.ToastMatcher.Companion.onToast
import com.googlecode.mindbell.util.Utils
import junit.framework.Assert.*
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(SettingsFragment::class.java, false, false) // don't launch

    private lateinit var context: Context

    private lateinit var prefs: Prefs

    private lateinit var resources: Resources

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getTargetContext()
        prefs = Prefs.getInstance(context)
        prefs.resetSettings()
        resources = context.resources
        // launch activity in test methods to allow more preferences to be changed before
    }

    @After
    fun tearDown() {
        // Revoking of permissions does no longer work this way ... it crashes the process.
        // But as long as only testReminderActionsPreferencesPage() requires this it's given by default :-(.
        // InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm revoke ${InstrumentationRegistry.getTargetContext().packageName} android.permission.READ_EXTERNAL_STORAGE")
    }

    @Test
    fun testActivityIsDisplayed() {
        activityTestRule.launchActivity(null) // launch activity now to get reset preferences
        onView(withText(preferencesTitle)).check(matches(isDisplayed()))
    }

    @Test
    fun testSoundOutputPreferencesPage() {

        // Navigate to sound output preferences page
        activityTestRule.launchActivity(null) // launch activity now to get reset preferences
        onView(allOf(withId(title), withText(prefsCategorySoundOutput))).perform(click())
        onView(allOf(withText(prefsCategorySoundOutput), not(withText(preferencesTitle)))).check(matches(isDisplayed()))

        // Fetch text views of sound output preferences page
        val preferenceUseAudioStreamVolumeSetting = onData(PreferenceMatchers.withKey(resources.getString(keyUseAudioStreamVolumeSetting)))
        val preferenceAudioStream = onData(PreferenceMatchers.withKey(resources.getString(keyAudioStream)))

        // Check default settings in UI and Prefs
        preferenceUseAudioStreamVolumeSetting.onChildView(withId(summary)).check(matches(withText(summaryUseAudioStreamVolumeSetting)))
        assertTrue(prefs.isUseAudioStreamVolumeSetting)

        preferenceAudioStream.onChildView(withId(textViewSummary)).check(matches(withText(resources.getStringArray(audioStreamEntries)[0])))
        assertEquals(AudioManager.STREAM_ALARM, prefs.audioStream)

        // Switch OFF isUseAudioStreamVolumeSetting and check in UI and Prefs
        preferenceUseAudioStreamVolumeSetting.perform(click()).check(matches(withText(summaryDontUseAudioStreamVolumeSetting)))
        assertFalse(prefs.isUseAudioStreamVolumeSetting)

        preferenceAudioStream.check(matches(not(isEnabled())))

        // Switch ON isUseAudioStreamVolumeSetting and check in UI and Prefs
        preferenceUseAudioStreamVolumeSetting.perform(click()).check(matches(withText(summaryUseAudioStreamVolumeSetting)))
        assertTrue(prefs.isUseAudioStreamVolumeSetting)

        preferenceAudioStream.check(matches(isEnabled()))

        // Choose audio stream NOTIFICATION and check in UI and Prefs
        preferenceAudioStream.perform(click())
        onView(allOf(withId(iconText), withText(resources.getStringArray(audioStreamEntries)[1]))).perform(click())
        preferenceAudioStream.onChildView(withId(textViewSummary)).check(matches(withText(resources.getStringArray(audioStreamEntries)[1])))
        assertEquals(AudioManager.STREAM_NOTIFICATION, prefs.audioStream)

        onToast(mustUseAudioStreamSetting).check(doesNotExist())

        // Audio stream != ALARM => isUseAudioStreamVolumeSetting must remain to be ON
        preferenceUseAudioStreamVolumeSetting.perform(click())

        checkDisplayedAndDisappearedOnToast(mustUseAudioStreamSetting)

        // Choose audio stream MUSIC and check in UI and Prefs
        preferenceAudioStream.perform(click())
        onView(allOf(withId(iconText), withText(resources.getStringArray(audioStreamEntries)[2]))).perform(click())
        preferenceAudioStream.onChildView(withId(textViewSummary)).check(matches(withText(resources.getStringArray(audioStreamEntries)[2])))
        assertEquals(AudioManager.STREAM_MUSIC, prefs.audioStream)

        onToast(mustUseAudioStreamSetting).check(doesNotExist())

        // Audio stream != ALARM => isUseAudioStreamVolumeSetting must remain to be ON
        preferenceUseAudioStreamVolumeSetting.perform(click())

        checkDisplayedAndDisappearedOnToast(mustUseAudioStreamSetting)

        // Choose audio stream ALARM and check in UI and Prefs
        preferenceAudioStream.perform(click())
        onView(allOf(withId(iconText), withText(resources.getStringArray(audioStreamEntries)[0]))).perform(click())
        preferenceAudioStream.onChildView(withId(textViewSummary)).check(matches(withText(resources.getStringArray(audioStreamEntries)[0])))
        assertEquals(AudioManager.STREAM_ALARM, prefs.audioStream)

        // Switch OFF isUseAudioStreamVolumeSetting and check in UI and Prefs
        preferenceUseAudioStreamVolumeSetting.perform(click()).check(matches(withText(summaryDontUseAudioStreamVolumeSetting)))
        assertFalse(prefs.isUseAudioStreamVolumeSetting)

    }

    @Test
    fun testActiveTimesPreferencesPage() {

        // Navigate to sound output preferences page
        activityTestRule.launchActivity(null) // launch activity now to get reset preferences
        onView(allOf(withId(title), withText(prefsCategoryActiveTimes))).perform(click())
        onView(allOf(withText(prefsCategoryActiveTimes), not(withText(preferencesTitle)))).check(matches(isDisplayed()))

        // Fetch text views of active times preferences page
        val preferenceStart = onData(PreferenceMatchers.withKey(resources.getString(keyStart)))
        val preferenceEnd = onData(PreferenceMatchers.withKey(resources.getString(keyEnd)))
        val preferenceActiveOnDaysOfWeek = onData(PreferenceMatchers.withKey(resources.getString(keyActiveOnDaysOfWeek)))
        val preferenceFrequency = onData(PreferenceMatchers.withKey(resources.getString(keyFrequency)))
        val preferenceRandomize = onData(PreferenceMatchers.withKey(resources.getString(keyRandomize)))
        val preferenceNormalize = onData(PreferenceMatchers.withKey(resources.getString(keyNormalize)))

        // Check default settings in UI and Prefs
        preferenceStart.onChildView(withId(summary)).check(matches(withText(TimeOfDay(9, 0).getDisplayString(context))))
        assertEquals(TimeOfDay(9, 0), prefs.daytimeStart)

        preferenceEnd.onChildView(withId(summary)).check(matches(withText(TimeOfDay(21, 0).getDisplayString(context))))
        assertEquals(TimeOfDay(21, 0), prefs.daytimeEnd)

        val wholeWeekSet = HashSet(Arrays.asList("1", "2", "3", "4", "5", "6", "7"))
        val wholeWeekSummary = Utils.deriveOrderedEntrySummary(wholeWeekSet, resources.getStringArray(weekdayEntries), resources.getStringArray(weekdayEntryValues))
        preferenceActiveOnDaysOfWeek.onChildView(withId(summary)).check(matches(withText(wholeWeekSummary)))

        preferenceFrequency.onChildView(withId(summary)).check(matches(withText("00:15 (15 min)")))
        assertEquals(15 * ONE_MINUTE_MILLIS, prefs.interval)

        preferenceRandomize.onChildView(withId(summary)).check(matches(withText(summaryRandomize)))
        assertTrue(prefs.isRandomize)

        preferenceNormalize.onChildView(withId(summary)).check(matches(withText(resources.getStringArray(normalizeEntries)[0])))
        assertFalse(prefs.isNormalize)

        // Choose start at 11:12
        preferenceStart.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsStart))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(11, 12))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        preferenceStart.onChildView(withId(summary)).check(matches(withText(TimeOfDay(11, 12).getDisplayString(context))))
        assertEquals(TimeOfDay(11, 12), prefs.daytimeStart)

        // Choose end at 23:24
        preferenceEnd.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsEnd))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(23, 24))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        preferenceEnd.onChildView(withId(summary)).check(matches(withText(TimeOfDay(23, 24).getDisplayString(context))))
        assertEquals(TimeOfDay(23, 24), prefs.daytimeEnd)

        // Choose only Monday
        preferenceActiveOnDaysOfWeek.perform(ViewActions.scrollTo(), click())  // open selection
        onView(allOf(isDialogTitle(), withText(prefsActiveOnDaysOfWeek))).check(matches(isDisplayed()))
        for (i in 0..6) {
            onView(withText(resources.getStringArray(weekdayEntries)[i])).perform(ViewActions.scrollTo(), click()) // un-choose all days
        }
        val mondaySet = HashSet(Arrays.asList("2"))
        val mondayWeekSummary = Utils.deriveOrderedEntrySummary(mondaySet, resources.getStringArray(weekdayEntries), resources.getStringArray(weekdayEntryValues))
        onView(withText(mondayWeekSummary)).perform(click()) // choose only Monday
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        preferenceActiveOnDaysOfWeek.onChildView(withId(summary)).check(matches(withText(mondayWeekSummary)))
        assertEquals(mondaySet.toString(), prefs.activeOnDaysOfWeek.toString())
        onToast(atLeastOneActiveDayNeeded).check(doesNotExist())

        // Try to choose no day at all
        preferenceActiveOnDaysOfWeek.perform(click())  // open selection
        onView(allOf(isDialogTitle(), withText(prefsActiveOnDaysOfWeek))).check(matches(isDisplayed()))
        onView(withText(mondayWeekSummary)).perform(click()) // de-choose also Monday
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        checkDisplayedAndDisappearedOnToast(atLeastOneActiveDayNeeded)
        preferenceActiveOnDaysOfWeek.onChildView(withId(summary)).check(matches(withText(mondayWeekSummary)))
        assertEquals(mondaySet.toString(), prefs.activeOnDaysOfWeek.toString())

        // Choose frequency 20 minutes
        preferenceFrequency.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsFrequency))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(0, 20))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        preferenceFrequency.onChildView(withId(summary)).check(matches(withText("00:20 (20 min)")))
        assertEquals(20 * ONE_MINUTE_MILLIS, prefs.interval)

        // Try to choose normalize
        preferenceNormalize.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsNormalize))).check(doesNotExist())

        // Disable randomize
        preferenceRandomize.perform(ViewActions.scrollTo(), click()).check(matches(withText(summaryDontRandomize)))
        assertFalse(prefs.isRandomize)

        // Choose normalize hh:05
        preferenceNormalize.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsNormalize))).check(matches(isDisplayed()))
        onView(withText(resources.getStringArray(normalizeEntries)[2])).perform(click())
        preferenceNormalize.onChildView(withId(summary)).check(matches(withText(resources.getStringArray(normalizeEntries)[2])))
        assertTrue(prefs.isNormalize)
        assertEquals(5, prefs.normalize)

        // Try to choose frequency 67 minutes
        preferenceFrequency.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsFrequency))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(1, 7))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        checkDisplayedAndDisappearedOnToast(frequencyDoesNotFitIntoAnHour)
        preferenceFrequency.onChildView(withId(summary)).check(matches(withText("00:20 (20 min)")))
        assertEquals(20 * ONE_MINUTE_MILLIS, prefs.interval)

        // Choose normalize no
        preferenceNormalize.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsNormalize))).check(matches(isDisplayed()))
        onData(Matchers.`is`(resources.getStringArray(normalizeEntries)[0])).perform(ViewActions.scrollTo(), click())
        preferenceNormalize.onChildView(withId(summary)).check(matches(withText(resources.getStringArray(normalizeEntries)[0])))
        assertFalse(prefs.isNormalize)

        // Choose frequency 67 minutes
        preferenceFrequency.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsFrequency))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(1, 7))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        preferenceFrequency.onChildView(withId(summary)).check(matches(withText("01:07 (67 min)")))
        assertEquals(67 * ONE_MINUTE_MILLIS, prefs.interval)

        // Try to choose normalize
        preferenceNormalize.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsNormalize))).check(doesNotExist())

        // Choose frequency 20 minutes ... to check later whether de-randomize does de-normalize
        preferenceFrequency.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsFrequency))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(0, 20))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())
        preferenceFrequency.onChildView(withId(summary)).check(matches(withText("00:20 (20 min)")))
        assertEquals(20 * ONE_MINUTE_MILLIS, prefs.interval)

        // Choose normalize hh:05 ... to check later whether de-randomize does de-normalize
        preferenceNormalize.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsNormalize))).check(matches(isDisplayed()))
        onView(withText(resources.getStringArray(normalizeEntries)[2])).perform(ViewActions.scrollTo(), click())
        preferenceNormalize.onChildView(withId(summary)).check(matches(withText(resources.getStringArray(normalizeEntries)[2])))
        assertTrue(prefs.isNormalize)
        assertEquals(5, prefs.normalize)

        // Enable randomize ... to check later whether de-randomize does de-normalize
        preferenceRandomize.perform(ViewActions.scrollTo(), click()).check(matches(withText(summaryRandomize)))
        assertTrue(prefs.isRandomize)
        assertFalse(prefs.isNormalize) // here we check whether de-randomize does de-normalize

    }

    @Test
    fun testReminderActionsPreferencesPage() {

        // Navigate to sound output preferences page
        activityTestRule.launchActivity(null) // launch activity now to get reset preferences
        onView(allOf(withId(title), withText(prefsCategoryReminderActions))).perform(click())
        onView(allOf(withText(prefsCategoryReminderActions), not(withText(preferencesTitle)))).check(matches(isDisplayed()))

        // Fetch text views of active times preferences page
        val preferenceShow = onData(PreferenceMatchers.withKey(resources.getString(keyShow)))
        val preferenceSound = onData(PreferenceMatchers.withKey(resources.getString(keySound)))
        val preferenceReminderBell = onData(PreferenceMatchers.withKey(resources.getString(keyReminderBell)))
        val preferenceRingtone = onData(PreferenceMatchers.withKey(resources.getString(keyRingtone)))
        val preferenceVolume = onData(PreferenceMatchers.withKey(resources.getString(keyVolume)))
        val preferenceVibrate = onData(PreferenceMatchers.withKey(resources.getString(keyVibrate)))
        val preferencePattern = onData(PreferenceMatchers.withKey(resources.getString(keyPattern)))

        // Check default settings in UI and Prefs
        preferenceShow.onChildView(withId(summary)).check(matches(withText(summaryShow)))
        assertTrue(prefs.isShow)

        preferenceSound.onChildView(withId(summary)).check(matches(withText(summarySound)))
        assertTrue(prefs.isSound)

        preferenceReminderBell.onChildView(withId(summary)).check(matches(withText(resources.getStringArray(reminderBellEntries)[1])))
        assertEquals(prefs.getDefaultReminderBellSoundUri(), prefs.getReminderSoundUri())

        preferenceRingtone.onChildView(withId(summary)).check(matches(withText(summaryRingtoneNotSet)))
        assertEquals("", prefs.ringtone)

        assertEquals(DEFAULT_VOLUME, prefs.volume) // preferenceVolume has no summary

        preferenceVibrate.onChildView(withId(summary)).check(matches(withText(summaryNoVibrate)))
        assertFalse(prefs.isVibrate)

        preferencePattern.onChildView(withId(summary)).check(matches(withText(resources.getStringArray(patternEntries)[2])))
        assertEquals(arrayOf(100L, 200L, 100L, 600L).asList(), prefs.vibrationPattern.asList())

        // Disable show
        preferenceShow.perform(ViewActions.scrollTo(), click())
        preferenceShow.onChildView(withId(summary)).check(matches(withText(summaryDontShow)))
        assertFalse(prefs.isShow)

        // Try to disable sound
        preferenceSound.perform(ViewActions.scrollTo(), click())
        checkDisplayedAndDisappearedOnToast(atLeastOneReminderActionNeeded)

        // Choose reminder bell double bell
        preferenceReminderBell.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsReminderBell))).check(matches(isDisplayed()))
        onData(Matchers.`is`(resources.getStringArray(reminderBellEntries)[2])).perform(ViewActions.scrollTo(), click())
        preferenceReminderBell.check(matches(withText(resources.getStringArray(reminderBellEntries)[2])))
        assertEquals(prefs.getBellSoundUri("2"), prefs.getReminderSoundUri())

        // Try to change volume
        preferenceVolume.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsVolume))).check(doesNotExist())

        // Try to choose ringtone
        preferenceRingtone.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsRingtone))).check(doesNotExist())

        // Choose reminder bell system sound
        preferenceReminderBell.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsReminderBell))).check(matches(isDisplayed()))
        onData(Matchers.`is`(resources.getStringArray(reminderBellEntries)[0])).perform(ViewActions.scrollTo(), click())
        allowPermission()
        preferenceReminderBell.check(matches(withText(resources.getStringArray(reminderBellEntries)[0])))
        assertEquals(prefs.getDefaultReminderBellSoundUri(), prefs.getReminderSoundUri()) // no ringtone chosen => default

        // Choose ringtone
        preferenceRingtone.perform(ViewActions.scrollTo(), click())
        pressText("Adara") // could not find a way to choose a sound independent of the name
        pressText("OK") // pressButton(1) does not work after sound selection, no idea why not
        preferenceRingtone.check(matches(withText("Adara")))

    }

    @Test
    fun testReminderActionsPreferencesPage_isUseAudioStreamVolumeOff() {

        // Switch OFF isUseAudioStreamVolumeSetting and check in UI and Prefs
        prefs.isUseAudioStreamVolumeSetting = false

        // Navigate to sound output preferences page
        activityTestRule.launchActivity(null) // launch activity now to get reset preferences
        onView(allOf(withId(title), withText(prefsCategoryReminderActions))).perform(click())
        onView(allOf(withText(prefsCategoryReminderActions), not(withText(preferencesTitle)))).check(matches(isDisplayed()))

        // Fetch text views of active times preferences page
        val preferenceVolume = onData(PreferenceMatchers.withKey(resources.getString(keyVolume)))

        // Open volume setting and press OK (cannot move the slider)
        preferenceVolume.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsVolume))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.button1), withText(R.string.ok))).perform(click())

        // Open volume setting and press CANCEL
        preferenceVolume.perform(ViewActions.scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsVolume))).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.button2), withText(R.string.cancel))).perform(click())

    }

    private fun allowPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            pressButton(1)
        }
    }

    /**
     * Press a button with the given index or fail with failMessage if its not there.
     *
     * Following David's comment to https://stackoverflow.com/a/34947797/2532583
     */
    private fun pressButton(index: Int) {
        val clickableUiObject = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).findObject(UiSelector().clickable(true).index(index))
        assertTrue("Expected button with index $index does not exist", clickableUiObject.exists())
        clickableUiObject.click()
    }

    private fun pressText(text: String) {
        val clickableUiObject = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).findObject(UiSelector().text(text))
        assertTrue("Expected text $text does not exist", clickableUiObject.exists())
        clickableUiObject.click()
    }

}
