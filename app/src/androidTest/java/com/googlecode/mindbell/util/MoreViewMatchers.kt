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

import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import android.view.View
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.`is`

/**
 * This adds actions not provided in final (!) class PickerActions.
 */

class MoreViewMatchers {

    class TextViewHasErrorTextMatcher(val stringMatcher: Matcher<String>) : BoundedMatcher<View, TextView>(TextView::class.java) {

        override fun describeTo(description: Description) {
            description.appendText("with error: ")
            stringMatcher.describeTo(description)
        }

        override fun matchesSafely(view: TextView): Boolean {
            return stringMatcher.matches(view.error)
        }
    }

    companion object {

        /**
         * Returns a matcher that matches [TextView] based on text view error string value.
         *
         * **Note:** View's error property can be `null`, to match against it use `
         * textViewHasErrorText(nullValue(String.class)`
         */
        fun textViewHasErrorText(stringMatcher: Matcher<String>): Matcher<View> {
            return TextViewHasErrorTextMatcher(checkNotNull(stringMatcher))
        }

        /**
         * Returns a matcher that matches [TextView] based on text view error string value.
         */
        fun textViewHasErrorText(expectedError: String): Matcher<View> {
            return textViewHasErrorText(`is`<String>(expectedError))
        }

        /**
         * Returns a matcher that matches views of a class with a name containing DialogTitle
         * to avoid the need to know the package that really implements it.
         */
        fun isDialogTitle(): Matcher<View>? {
            return withClassName(Matchers.containsString("DialogTitle"))
        }

    }

}