/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2018-2018 Uwe Damken
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

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.googlecode.mindbell.R
import java.util.*
import kotlinx.android.synthetic.main.icon_picker_preference.view.*
import kotlinx.android.synthetic.main.icon_picker_preference.*
import kotlinx.android.synthetic.main.icon_picker_preference_item.view.*
import kotlinx.android.synthetic.main.icon_picker_preference_item.*

/**
 * Idea taken from https://stackoverflow.com/a/32226553
 *
 * IconPickerPreference requires an entries array (texts to be displayed next to the icon) and an entryValues
 * array (filenames of the icons) which are both the same length.
 */

class IconPickerPreference(context: Context, attrs: AttributeSet) : ListPreferenceWithSummaryFix(context, attrs) {

    private lateinit var iconItemList: MutableList<IconItem>

    private var currentIndex = 0 // default value if android:defaultValue is not set

    private var mageViewSelectedIcon: ImageView? = null

    private var textViewSummary: TextView? = null

    init {

        val iconTextArray = entries
        val iconFilenameArray = entryValues

        if (iconTextArray == null || iconFilenameArray == null || iconTextArray.size != iconFilenameArray.size) {
            throw IllegalStateException(
                    "IconPickerPreference requires an entries array and an entryValues array which are both the same length")
        }

        iconItemList = ArrayList()
        for (i in iconTextArray.indices) {
            val item = IconItem(iconTextArray[i], iconFilenameArray[i], false)
            iconItemList.add(item)
        }
    }

    override fun getValue(): String {
        return currentIndex.toString()
    }

    override fun onBindView(view: View) {
        super.onBindView(view)

        updateSummary()
    }

    /**
     * Update summary shown in the preference page. This includes a summary text and the chosen icon.
     */
    private fun updateSummary() {
        val selectedIconItem = iconItemList!![currentIndex]
        val identifier = context.resources.getIdentifier(selectedIconItem.iconFilename, "drawable", context.packageName)
        mageViewSelectedIcon!!.setImageResource(identifier)
        textViewSummary!!.text = selectedIconItem.iconText
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        if (iconItemList != null) {
            for (i in iconItemList.indices) {
                val item = iconItemList[i]
                if (item.isChecked) {
                    persistString(i.toString())
                    currentIndex = i
                    updateSummary()
                    break
                }
            }
        }
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        var newValue: String? = null
        if (restoreValue) {
            if (defaultValue == null) {
                newValue = getPersistedString("0")
            } else {
                newValue = getPersistedString(defaultValue.toString())
            }
        } else {
            newValue = defaultValue!!.toString()
        }
        currentIndex = Integer.parseInt(newValue)
        iconItemList!![currentIndex].isChecked = true
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.setPositiveButton(null, null)
        val customListPreferenceAdapter = CustomListPreferenceAdapter(context, R.layout.icon_picker_preference_item, iconItemList)
        builder.setAdapter(customListPreferenceAdapter, null)
    }

    private inner class CustomListPreferenceAdapter(context: Context, private val resource: Int, objects: List<IconItem>)// there is no way to get this back from super class
        : ArrayAdapter<IconItem>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
            val iconItem = getItem(position)
            view.iconText.text = iconItem!!.iconText
            val identifier = context.resources.getIdentifier(iconItem.iconFilename, "drawable", context.packageName)
            view.iconImage.setImageResource(identifier)
            view.iconRadio.isChecked = iconItem.isChecked

            view.setOnClickListener {
                for (i in 0 until count) {
                    getItem(i)!!.isChecked = i == position
                }
                dialog.dismiss()
            }

            return view
        }
    }

    private inner class IconItem(iconText: CharSequence, iconFilename: CharSequence, var isChecked: Boolean) {

        val iconText: String

        val iconFilename: String

        init {
            this.iconText = iconText.toString()
            this.iconFilename = iconFilename.toString()
        }

    }

}