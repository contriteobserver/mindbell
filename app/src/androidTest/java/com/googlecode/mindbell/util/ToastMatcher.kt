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

package com.googlecode.mindbell.util

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
import android.view.WindowManager.LayoutParams.TYPE_TOAST
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * This class allows to match Toast messages in tests with Espresso.
 *
 * Idea taken from: https://stackoverflow.com/a/33387980
 *
 * Adapted version shared as: https://stackoverflow.com/a/49834662/2532583
 *
 * Usage in test class:
 *
 * import com.googlecode.mindbell.util.ToastMatcher.Companion.onToast
 *
 * // To assert a toast does *not* pop up:
 * onToast("text").check(doesNotExist())
 * onToast(textId).check(doesNotExist())
 *
 * // To assert a toast does pop up:
 * onToast("text").check(matches(isDisplayed()))
 * onToast(textId).check(matches(isDisplayed()))
 */
class ToastMatcher(private val maxFailures: Int = DEFAULT_MAX_FAILURES) : TypeSafeMatcher<Root>() {

    /** Restrict number of false results from matchesSafely to avoid endless loop */
    private var failures = 0

    override fun describeTo(description: Description) {
        description.appendText("is toast")
    }

    public override fun matchesSafely(root: Root): Boolean {
        val type = root.windowLayoutParams.get().type
        @Suppress("DEPRECATION") // TYPE_TOAST is deprecated in favor of TYPE_APPLICATION_OVERLAY
        if (type == TYPE_TOAST || type == TYPE_APPLICATION_OVERLAY) {
            val windowToken = root.decorView.windowToken
            val appToken = root.decorView.applicationWindowToken
            if (windowToken === appToken) {
                // windowToken == appToken means this window isn't contained by any other windows.
                // if it was a window for an activity, it would have TYPE_BASE_APPLICATION.
                return true
            }
        }
        // Method is called again if false is returned which is useful because a toast may take some time to pop up. But for
        // obvious reasons an infinite wait isn't of help. So false is only returned as often as maxFailures specifies.
        return (++failures >= maxFailures)
    }

    companion object {

        /** Default for maximum number of retries to wait for the toast to pop up */
        private const val DEFAULT_MAX_FAILURES = 5

        fun onToast(text: String, maxRetries: Int = DEFAULT_MAX_FAILURES) = onView(withText(text)).inRoot(isToast(maxRetries))!!

        fun onToast(textId: Int, maxRetries: Int = DEFAULT_MAX_FAILURES) = onView(withText(textId)).inRoot(isToast(maxRetries))!!

        private fun isToast(maxRetries: Int = DEFAULT_MAX_FAILURES): Matcher<Root> {
            return ToastMatcher(maxRetries)
        }

        fun checkDisplayedAndDisappearedOnToast(text: String) {
            onToast(text).check(matches(isDisplayed()))
            try {
                while (true) {
                    Thread.sleep(500L)
                    onToast(text).check(matches(isDisplayed()))
                }
            } catch (e: NoMatchingViewException) {
                // This exception occurs when the toast has been taken off the screen
            }
        }

        fun checkDisplayedAndDisappearedOnToast(textId: Int) {
            onToast(textId).check(matches(isDisplayed()))
            try {
                while (true) {
                    Thread.sleep(500L)
                    onToast(textId).check(matches(isDisplayed()))
                }
            } catch (e: NoMatchingViewException) {
                // This exception occurs when the toast has been taken off the screen
            }
        }

    }

}
