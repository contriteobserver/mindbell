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

import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import android.view.View
import android.widget.NumberPicker
import org.hamcrest.Matcher
import org.hamcrest.Matchers

/**
 * This adds actions not provided in final (!) class PickerActions.
 */
class MorePickerActions {

    companion object {

        fun setNumber(number: Int): ViewAction {

            return object : ViewAction {

                override fun perform(uiController: UiController, view: View) {
                    val numberPicker = view as NumberPicker
                    numberPicker.value = number
                }

                override fun getDescription(): String {
                    return "set number"
                }

                override fun getConstraints(): Matcher<View> {
                    return Matchers.allOf(ViewMatchers.isAssignableFrom(NumberPicker::class.java), isDisplayed())
                }
            }

        }
    }

}
