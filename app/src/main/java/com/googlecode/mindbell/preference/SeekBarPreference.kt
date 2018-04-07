/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2007 The Android Open Source Project
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

package com.googlecode.mindbell.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar

import com.googlecode.mindbell.R

/**
 * @hide
 */
open class SeekBarPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {
    private val mMyIcon: Drawable?

    init {

        dialogLayoutResource = R.layout.seekbar_dialog
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)

        // Steal the XML dialogIcon attribute's value
        mMyIcon = dialogIcon
        dialogIcon = null
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        val iconView = view.findViewById(R.id.imageViewIcon) as ImageView
        if (mMyIcon != null) {
            iconView.setImageDrawable(mMyIcon)
        } else {
            iconView.visibility = View.GONE
        }
    }

    companion object {
        private val TAG = "SeekBarPreference"

        protected fun getSeekBar(dialogView: View): SeekBar {
            return dialogView.findViewById(R.id.seekBarVolume) as SeekBar
        }
    }
}