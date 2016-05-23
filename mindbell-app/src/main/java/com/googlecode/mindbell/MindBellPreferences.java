/*******************************************************************************
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
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
 *******************************************************************************/
package com.googlecode.mindbell;

import java.util.HashSet;
import java.util.Set;

import com.googlecode.mindbell.accessors.AndroidPrefsAccessor;
import com.googlecode.mindbell.util.Utils;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import com.googlecode.mindbell.R;

public class MindBellPreferences extends PreferenceActivity {
    /**
     * @author marc
     *
     */
    private static final class ListChangeListener implements Preference.OnPreferenceChangeListener {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            assert preference instanceof ListPreference;
            ListPreference lp = (ListPreference) preference;
            int index = lp.findIndexOfValue((String) newValue);
            if (index != -1) {
                CharSequence newEntry = lp.getEntries()[index];
                lp.setSummary(newEntry);
                return true;
            }
            return false;
        }
    }

    private static final class MultiSelectListChangeListener implements Preference.OnPreferenceChangeListener {
        public boolean onPreferenceChange(Preference preference, Object newValues) {
            assert preference instanceof MultiSelectListPreference;
            MultiSelectListPreference mslp = (MultiSelectListPreference) preference;
            if (((Set<?>) newValues).isEmpty()) {
                Toast.makeText(mslp.getContext(), R.string.atLeastOneActiveDayNeeded, Toast.LENGTH_SHORT).show();
                return false;
            }
            setMultiSelectListPreferenceSummary(mslp, (Set<?>) newValues);
            return true;
        }
    }

    public static final String TAG = "MindBell";

    private static void setMultiSelectListPreferenceSummary(MultiSelectListPreference mslp, Set<?> newValues) {
        String[] weekdayAbbreviations = mslp.getContext().getResources().getStringArray(R.array.weekdayAbbreviations);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < weekdayAbbreviations.length; i++) {
            if (((HashSet<?>) newValues).contains(String.valueOf(i + 1))) { // is this day selected?
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(weekdayAbbreviations[i]); // add day to the list of active days
            }
        }
        mslp.setSummary(sb.toString());
    }

    private final Preference.OnPreferenceChangeListener listChangeListener = new ListChangeListener();

    private final Preference.OnPreferenceChangeListener multiSelectListChangeListener = new MultiSelectListChangeListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check settings, delete any settings that are not valid
        new AndroidPrefsAccessor(this);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_1);
        addPreferencesFromResource(R.xml.preferences_2); // notifications depend on SDK
        addPreferencesFromResource(R.xml.preferences_3);

        setupListPreference(R.string.keyFrequency);
        setupListPreference(R.string.keyStart);
        setupListPreference(R.string.keyEnd);
        setupMultiSelectListPreference(R.string.keyActiveOnDaysOfWeek);

    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.updateBellSchedule(this);
    }

    private void setupListPreference(int keyID) {
        ListPreference lp = (ListPreference) getPreferenceScreen().findPreference(getText(keyID));
        lp.setSummary(lp.getEntry());
        lp.setOnPreferenceChangeListener(listChangeListener);
    }

    private void setupMultiSelectListPreference(int keyID) {
        MultiSelectListPreference mslp = (MultiSelectListPreference) getPreferenceScreen().findPreference(getText(keyID));
        setMultiSelectListPreferenceSummary(mslp, mslp.getValues());
        mslp.setOnPreferenceChangeListener(multiSelectListChangeListener);
    }

}