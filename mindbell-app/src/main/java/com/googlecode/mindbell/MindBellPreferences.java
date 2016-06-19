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

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.util.Utils;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public class MindBellPreferences extends PreferenceActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

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

    private static final int REQUEST_CODE_STATUS = 0;

    private static final int REQUEST_CODE_MUTE_OFF_HOOK = 1;

    private static void setMultiSelectListPreferenceSummary(MultiSelectListPreference mslp, Set<?> newValues) {
        // Warning: Similar code in AndroidPrefsAccessor#getActiveOnDaysOfWeekString()
        String[] daysOfWeekValues = mslp.getContext().getResources().getStringArray(R.array.daysOfWeekValues);
        String[] weekdayAbbreviations = mslp.getContext().getResources().getStringArray(R.array.weekdayAbbreviations);
        StringBuilder sb = new StringBuilder();
        for (String dayOfWeekValue : daysOfWeekValues) { // internal weekday value in locale oriented order
            Integer dayOfWeekValueAsInteger = Integer.valueOf(dayOfWeekValue);
            if (((HashSet<?>) newValues).contains(dayOfWeekValue)) { // is this day selected?
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(weekdayAbbreviations[dayOfWeekValueAsInteger - 1]); // add day to the list of active days
            }
        }
        mslp.setSummary(sb.toString());
    }

    private final Preference.OnPreferenceChangeListener listChangeListener = new ListChangeListener();

    private final Preference.OnPreferenceChangeListener multiSelectListChangeListener = new MultiSelectListChangeListener();

    private void handleMuteHookOffAndStatusPermissionRequestResult(CheckBoxPreference one, int[] grantResults) {
        if (grantResults.length == 0) {
            // if request is cancelled, the result arrays are empty, so leave this option "off" and don't explain it
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // User granted the needed permission therefore this option is set to "on"
            one.setChecked(true);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MindBellPreferences.this,
                Manifest.permission.READ_PHONE_STATE)) {
            // User denied the needed permission and can be given an explanation, so we show an explanation
            AlertDialog dialog = new AlertDialog.Builder(MindBellPreferences.this) //
                    .setTitle(R.string.reasonReadPhoneStateTitle) //
                    .setMessage(R.string.reasonReadPhoneStateText) //
                    .setPositiveButton(R.string.buttonOk, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // nothing to do
                        }
                    }) //
                    .create();
            dialog.show();
        } else {
            // User denied the needed permission and checked never ask again
            AlertDialog dialog = new AlertDialog.Builder(MindBellPreferences.this) //
                    .setTitle(R.string.neverAskAgainReadPhoneStateTitle) //
                    .setMessage(R.string.neverAskAgainPhoneStateText) //
                    .setPositiveButton(R.string.buttonOk, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // nothing to do
                        }
                    }) //
                    .create();
            dialog.show();
        }
    }

    /**
     * Ensures that the CheckBoxPreferences checkBoxPreferenceMuteOffHook and checkBoxPreferenceStatus cannot be both "on" without
     * having READ_PHONE_STATE permission by returning false when this rule is violated.
     */
    private boolean mediateMuteOffHookAndStatus(CheckBoxPreference other, Object newValue, int requestCode) {
        if (!other.isChecked() || !((Boolean) newValue) || ContextCompat.checkSelfPermission(MindBellPreferences.this,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Allow setting this option to "on" if other option is "off" or permission is granted
            return true;
        } else {
            // Ask for permission if other option is "on" and this option shall be set to "on" but permission is missing
            ActivityCompat.requestPermissions(MindBellPreferences.this, new String[] { Manifest.permission.READ_PHONE_STATE },
                    requestCode);
            // As the permission request is asynchronous we habe to deny setting this option (to "on")
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check settings, delete any settings that are not valid
        AndroidContextAccessor.getInstance(this).getPrefs();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_1);
        addPreferencesFromResource(R.xml.preferences_2); // notifications depend on SDK
        addPreferencesFromResource(R.xml.preferences_3);

        setupListPreference(R.string.keyFrequency);
        setupListPreference(R.string.keyStart);
        setupListPreference(R.string.keyEnd);
        setupMultiSelectListPreference(R.string.keyActiveOnDaysOfWeek);

        final CheckBoxPreference checkBoxPreferenceStatus = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(getText(R.string.keyStatus));
        final CheckBoxPreference checkBoxPreferenceMuteOffHook = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(getText(R.string.keyMuteOffHook));

        checkBoxPreferenceStatus.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return mediateMuteOffHookAndStatus(checkBoxPreferenceMuteOffHook, newValue, REQUEST_CODE_STATUS);
            }

        });

        checkBoxPreferenceMuteOffHook.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return mediateMuteOffHookAndStatus(checkBoxPreferenceStatus, newValue, REQUEST_CODE_MUTE_OFF_HOOK);
            }

        });

    }

    @Override
    public void onPause() {
        super.onPause();
        Utils.updateBellSchedule(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
        case REQUEST_CODE_STATUS:
            CheckBoxPreference checkBoxPreferenceStatus = (CheckBoxPreference) getPreferenceScreen()
                    .findPreference(getText(R.string.keyStatus));
            handleMuteHookOffAndStatusPermissionRequestResult(checkBoxPreferenceStatus, grantResults);
            break;
        case REQUEST_CODE_MUTE_OFF_HOOK:
            CheckBoxPreference checkBoxPreferenceMuteOffHook = (CheckBoxPreference) getPreferenceScreen()
                    .findPreference(getText(R.string.keyMuteOffHook));
            handleMuteHookOffAndStatusPermissionRequestResult(checkBoxPreferenceMuteOffHook, grantResults);
            break;
        default:
            break;
        }
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