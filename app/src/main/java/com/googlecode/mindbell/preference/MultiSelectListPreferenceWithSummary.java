package com.googlecode.mindbell.preference;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;

import java.util.Set;

/**
 * Adds a summary to MultiSelectListPreference by showing all selected entries.
 */
public class MultiSelectListPreferenceWithSummary extends MultiSelectListPreference {

    public MultiSelectListPreferenceWithSummary(Context context) {
        super(context);
    }

    public MultiSelectListPreferenceWithSummary(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MultiSelectListPreferenceWithSummary(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MultiSelectListPreferenceWithSummary(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setValues(Set<String> values) {
        super.setValues(values);
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        CharSequence[] entries = getEntries(); // entries as shown to the user
        CharSequence[] entryValues = getEntryValues(); // values internally representing the entries
        Set<String> values = getValues(); // selected values
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entryValues.length; i++) { // walk through entries, as they are ordered as presented to the user
            if (values.contains(entryValues[i])) { // internal represention value in the set of selected values?
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(entries[i]);
            }
        }
        return sb.toString();
    }

}
