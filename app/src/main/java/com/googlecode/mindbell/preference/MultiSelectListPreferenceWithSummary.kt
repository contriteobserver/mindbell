package com.googlecode.mindbell.preference

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.preference.MultiSelectListPreference
import android.util.AttributeSet
import com.googlecode.mindbell.util.Utils


/**
 * Adds a summary to MultiSelectListPreference by showing all selected entries.
 */
class MultiSelectListPreferenceWithSummary : MultiSelectListPreference {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    override fun setValues(values: Set<String>) {
        super.setValues(values)
        summary = summary
    }

    override fun getSummary(): CharSequence {
        val entries = entries // entries as shown to the user
        val entryValues = entryValues // values internally representing the entries
        val values = values // selected values
        return Utils.deriveOrderedEntrySummary(values, entries, entryValues)
    }

}
