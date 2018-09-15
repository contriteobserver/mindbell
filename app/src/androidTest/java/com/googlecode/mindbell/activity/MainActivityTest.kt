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

import android.content.res.Resources
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.PickerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TimePicker
import com.googlecode.mindbell.R.id.*
import com.googlecode.mindbell.R.string.*
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS
import com.googlecode.mindbell.mission.model.Statistics
import com.googlecode.mindbell.util.MorePickerActions
import com.googlecode.mindbell.util.MoreViewMatchers.Companion.isDialogTitle
import com.googlecode.mindbell.util.MoreViewMatchers.Companion.textViewHasErrorText
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java, false, false) // don't launch

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
    fun mainActivityTest() {

        // Close help dialog at startup
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(scrollTo(), click())

        // Open meditation dialog
        onView(allOf(withId(meditating), withContentDescription(prefsMeditatingOn))).perform(click())
        onView(withText(title_meditation_dialog)).check(matches(isDisplayed()))

        // Check default settings in UI
        onView(withId(textViewRampUpTime)).check(matches(withText("00:30 (30 s)")))
        onView(withId(textViewMeditationDuration)).check(matches(withText("00:25 (25 min)")))
        onView(withId(textViewNumberOfPeriods)).check(matches(withText("1")))
        onView(withId(textViewPatternOfPeriods)).check(matches(withText("x")))
        onView(withId(checkBoxKeepScreenOn)).check(matches(isChecked()))
        onView(withId(checkBoxStopMeditationAutomatically)).check(matches(isNotChecked()))

        // Change ramp up time
        onView(withId(textViewRampUpTimeLabel)).perform(scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsRampUpTime))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(0, 10))
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(scrollTo(), click())
        onView(withId(textViewRampUpTime)).check(matches(withText("00:10 (10 s)")))

        // Change meditation duration
        onView(withId(textViewMeditationDurationLabel)).perform(scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsMeditationDuration))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(TimePicker::class.java.name))).perform(PickerActions.setTime(0, 4))
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(scrollTo(), click())
        onView(withId(textViewMeditationDuration)).check(matches(withText("00:04 (4 min)")))

        // Change number of periods
        onView(withId(textViewNumberOfPeriodsLabel)).perform(scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsNumberOfPeriods))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(NumberPicker::class.java.name))).perform(MorePickerActions.setNumber(2))
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(scrollTo(), click())
        onView(withId(textViewNumberOfPeriods)).check(matches(withText("2")))
        onView(withId(textViewPatternOfPeriods)).check(matches(withText("x, x")))

        // Try to change pattern of periods to one fixed period only
        onView(withId(textViewPatternOfPeriodsLabel)).perform(scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsPatternOfPeriods))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(EditText::class.java.name))).perform(replaceText("1"), closeSoftKeyboard())
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(scrollTo(), click())
        onView(withId(textViewPatternOfPeriods)).check(matches(textViewHasErrorText(resources.getString(variablePeriodMissing))))

        // Change pattern of periods
        onView(withId(textViewPatternOfPeriodsLabel)).perform(scrollTo(), click())
        onView(allOf(isDialogTitle(), withText(prefsPatternOfPeriods))).check(matches(isDisplayed()))
        onView(withClassName(Matchers.equalTo(EditText::class.java.name))).perform(replaceText("1,x,1"), closeSoftKeyboard())
        onView(allOf(withId(android.R.id.button1), withText(android.R.string.ok))).perform(scrollTo(), click())
        onView(withId(textViewNumberOfPeriods)).check(matches(withText("3")))
        onView(withId(textViewPatternOfPeriods)).check(matches(withText("1, x, 1")))

        // Enable option (after disabling) to leave screen on
        onView(withId(checkBoxKeepScreenOn)).perform(scrollTo(), click()).check(matches(isNotChecked()))
        onView(withId(checkBoxKeepScreenOn)).perform(scrollTo(), click()).check(matches(isChecked()))

        // Enable option to stop meditation automatically
        onView(withId(checkBoxStopMeditationAutomatically)).perform(scrollTo(), click())

        // Start meditation
        onView(withText(buttonStartMeditation)).perform(scrollTo(), click())
        onView(allOf(withId(meditating), withContentDescription(prefsMeditatingOff))).check(matches(isDisplayed()))

        // Wait for meditation to come to an end
        do {
            Thread.sleep(ONE_MINUTE_MILLIS)
        } while (prefs.isMeditating)

        // Check meditation has stopped automatically
        onView(allOf(withId(meditating), withContentDescription(prefsMeditatingOn))).perform(click())

        // Check statistics for success
        val entryList = prefs.getStatisticsEntryList().filter { entry -> entry is Statistics.ActionsStatisticsEntry }
        assertEquals(4, entryList.size)

        val beginning = entryList[0]
        assertTrue(beginning is Statistics.MeditationBeginningActionsStatisticsEntry)
        assertEquals(Statistics.Judgment.ON_TIME, beginning.judgment)

        val interrupting1 = entryList[1]
        assertTrue(interrupting1 is Statistics.MeditationInterruptingActionsStatisticsEntry)
        assertEquals(Statistics.Judgment.ON_TIME, interrupting1.judgment)

        val interrupting2 = entryList[2]
        assertTrue(interrupting2 is Statistics.MeditationInterruptingActionsStatisticsEntry)
        assertEquals(Statistics.Judgment.ON_TIME, interrupting2.judgment)

        val ending = entryList[3]
        assertTrue(ending is Statistics.MeditationEndingActionsStatisticsEntry)
        assertEquals(Statistics.Judgment.ON_TIME, ending.judgment)

    }

}
