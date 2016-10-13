/*
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
 */
package com.googlecode.mindbell;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.AndroidPrefsAccessor;
import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.preference.ListPreferenceWithSummaryFix;
import com.googlecode.mindbell.preference.MediaVolumePreference;
import com.googlecode.mindbell.preference.MultiSelectListPreferenceWithSummary;
import com.googlecode.mindbell.util.Utils;

import java.util.Set;

public class MindBellPreferences extends PreferenceActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String TAG = "MindBell";

    private static final int REQUEST_CODE_STATUS = 0;

    private static final int REQUEST_CODE_MUTE_OFF_HOOK = 1;

    // Weird, but ringtone cannot be retrieved from RingtonePreference, only from SharedPreference or in ChangeListener
    private String preferenceRingtoneValue;

    @SuppressWarnings("deprecation") // deprecation is because MindBell is not fragment-based
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check settings, delete any settings that are not valid
        final PrefsAccessor prefs = AndroidContextAccessor.getInstance(this).getPrefs();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_1);
        addPreferencesFromResource(R.xml.preferences_2); // notifications depend on SDK
        addPreferencesFromResource(R.xml.preferences_3);

        final CheckBoxPreference preferenceStatus =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyStatus));
        final CheckBoxPreference preferenceShow =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyShow));
        final CheckBoxPreference preferenceSound =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keySound));
        final CheckBoxPreference preferenceUseStandardBell =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyUseStandardBell));
        final MediaVolumePreference preferenceVolume =
                (MediaVolumePreference) getPreferenceScreen().findPreference(getText(R.string.keyVolume));
        final RingtonePreference preferenceRingtone =
                (RingtonePreference) getPreferenceScreen().findPreference(getText(R.string.keyRingtone));
        final CheckBoxPreference preferenceVibrate =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyVibrate));
        final ListPreferenceWithSummaryFix preferencePattern =
                (ListPreferenceWithSummaryFix) getPreferenceScreen().findPreference(getText(R.string.keyPattern));
        final CheckBoxPreference preferenceMuteOffHook =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyMuteOffHook));
        final ListPreferenceWithSummaryFix preferenceFrequency =
                (ListPreferenceWithSummaryFix) getPreferenceScreen().findPreference(getText(R.string.keyFrequency));
        final CheckBoxPreference preferenceRandomize =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyRandomize));
        final ListPreferenceWithSummaryFix preferenceNormalize =
                (ListPreferenceWithSummaryFix) getPreferenceScreen().findPreference(getText(R.string.keyNormalize));
        final MultiSelectListPreferenceWithSummary preferenceActiveOnDaysOfWeek =
                (MultiSelectListPreferenceWithSummary) getPreferenceScreen().findPreference(
                        getText(R.string.keyActiveOnDaysOfWeek));

        preferenceStatus.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return mediateMuteOffHookAndStatus(preferenceMuteOffHook, newValue, REQUEST_CODE_STATUS);
            }

        });

        preferenceShow.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return mediateShowAndSoundAndVibrate(preferenceSound, preferenceVibrate, newValue);
            }

        });

        preferenceSound.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (mediateShowAndSoundAndVibrate(preferenceShow, preferenceVibrate, newValue)) {
                    boolean isChecked = (Boolean) newValue;
                    preferenceUseStandardBell.setEnabled(isChecked);
                    preferenceRingtone.setEnabled(isChecked && !preferenceUseStandardBell.isChecked());
                    return true;
                } else {
                    return false;
                }
            }

        });

        preferenceUseStandardBell.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isChecked = (Boolean) newValue;
                preferenceRingtone.setEnabled(preferenceSound.isChecked() && !isChecked);
                // Weird, but ringtone cannot be retrieved from RingtonePreference, only from SharedPreference
                setPreferenceVolumeSoundUri(preferenceVolume, isChecked, preferenceRingtoneValue);
                return true;
            }

        });

        preferenceRingtone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preferenceRingtoneValue = (String) newValue;
                if (!validatePreferenceRingtone(preferenceRingtoneValue)) {
                    return false;
                }
                setPreferenceRingtoneSummary(preferenceRingtone, preferenceRingtoneValue);
                setPreferenceVolumeSoundUri(preferenceVolume, preferenceUseStandardBell.isChecked(), preferenceRingtoneValue);
                return true;
            }

        });

        preferenceVibrate.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return mediateShowAndSoundAndVibrate(preferenceShow, preferenceSound, newValue);
            }

        });

        preferencePattern.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Vibrator vibrator = (Vibrator) MindBellPreferences.this.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(PrefsAccessor.getVibrationPattern((String) newValue), -1);
                return true;
            }

        });

        preferenceMuteOffHook.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return mediateMuteOffHookAndStatus(preferenceStatus, newValue, REQUEST_CODE_MUTE_OFF_HOOK);
            }

        });

        preferenceRandomize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) {
                    // if interval deviation is selected, normalize is disabled on screen but it must be disabled in preferences,
                    // too. Otherwise the following scenario could happen: set interval 1 h, de-select randomize, set normalize to
                    // hh:00, select randomize, set interval 2 h, de-select randomize again ... hh:00 would be left in normalize
                    // erroneously.
                    preferenceNormalize.setValue(AndroidPrefsAccessor.NORMALIZE_NONE);
                }
                return true;
            }

        });

        preferenceFrequency.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preferenceRandomize.isChecked()) {
                    // if interval varies randomly, ringing on the minute is disabled and set to "no" anyway
                    return true;
                } else if (isFrequencyDividesAnHour((String) newValue)) {
                    // if frequency is factor of an hour, ringing on the minute may be requested
                    preferenceNormalize.setEnabled(true);
                } else {
                    // if frequency is NOT factor of an hour, ringing on the minute may NOT be set
                    if (preferenceNormalize.isEnabled() && isNormalize(preferenceNormalize.getValue())) {
                        Toast.makeText(MindBellPreferences.this, R.string.frequencyDoesNotFitIntoAnHour, Toast.LENGTH_SHORT).show();
                        return false;
                    } else {
                        preferenceNormalize.setEnabled(false);
                    }
                }
                return true;
            }

        });

        preferenceNormalize.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (!isNormalize((String) newValue)) {
                    // if normalize - ringing on the minute - is not wanted, it's fine, no more to check here
                    return true;
                } else if (isFrequencyDividesAnHour(preferenceFrequency.getValue())) {
                    // if frequency is factor of an hour, requesting ringing on the minute is allowed
                    return true;
                } else {
                    // if frequency is NOT factor of an hour, ringing on the minute may NOT be set
                    Toast.makeText(MindBellPreferences.this, R.string.frequencyDoesNotFitIntoAnHour, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

        });

        preferenceActiveOnDaysOfWeek.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValues) {
                if (((Set<?>) newValues).isEmpty()) {
                    Toast.makeText(MindBellPreferences.this, R.string.atLeastOneActiveDayNeeded, Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }

        });

        // As no PreferenceChangeListener is called without change, some settings have to be made explicitely
        preferenceUseStandardBell.setEnabled(preferenceSound.isChecked());
        preferenceRingtone.setEnabled(preferenceSound.isChecked() && !preferenceUseStandardBell.isChecked());
        preferenceRingtoneValue = prefs.getRingtone(); // cannot be retrieved from preference
        setPreferenceRingtoneSummary(preferenceRingtone, preferenceRingtoneValue);
        setPreferenceVolumeSoundUri(preferenceVolume, preferenceUseStandardBell.isChecked(), preferenceRingtoneValue);

    }

    /**
     * Ensures that the CheckBoxPreferences checkBoxPreferenceMuteOffHook and checkBoxPreferenceStatus cannot be both "on" without
     * having READ_PHONE_STATE permission by returning false when this rule is violated.
     */
    private boolean mediateMuteOffHookAndStatus(CheckBoxPreference other, Object newValue, int requestCode) {
        if (!other.isChecked() || !((Boolean) newValue) ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED) {
            // Allow setting this option to "on" if other option is "off" or permission is granted
            return true;
        } else {
            // Ask for permission if other option is "on" and this option shall be set to "on" but permission is missing
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, requestCode);
            // As the permission request is asynchronous we have to deny setting this option (to "on")
            return false;
        }
    }

    /**
     * Ensures that the CheckBoxPreferences checkBoxPreferenceShow, checkBoxPreferenceSound and checkBoxPreferenceVibrate cannot be
     * all "off", at least one must be checked.
     */
    private boolean mediateShowAndSoundAndVibrate(CheckBoxPreference firstOther, CheckBoxPreference secondOther, Object newValue) {
        if (!firstOther.isChecked() && !secondOther.isChecked() && !((Boolean) newValue)) {
            Toast.makeText(this, R.string.atLeastOneRingingActionNeeded, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Set sound uri in preferenceVolume depending on preferenceUseStandardBell and preferenceRingtone, so real sound is used for
     * volume setting.
     *
     * @param preferenceVolume
     * @param useStandardBell
     * @param ringtoneUriString
     */
    private void setPreferenceVolumeSoundUri(MediaVolumePreference preferenceVolume, boolean useStandardBell,
                                             String ringtoneUriString) {
        Uri soundUri;
        // This implementation is almost the same as AndroidPrefsAccessor#getSoundUri()
        if (useStandardBell || ringtoneUriString.isEmpty()) {
            soundUri = Utils.getResourceUri(this, R.raw.bell10s);
        } else {
            soundUri = Uri.parse(ringtoneUriString);
        }
        preferenceVolume.setSoundUri(soundUri);
    }

    /**
     * Returns true if the ringtone specified by uriString is unset, empty or accessible.
     *
     * @param uriString
     * @return
     */
    private boolean validatePreferenceRingtone(String uriString) {
        if (uriString != null && !uriString.isEmpty()) {
            Uri uri = Uri.parse(uriString);
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(this, uri);
            } catch (Exception e) {
                Log.w(TAG, "Sound <" + uriString + "> not accessible", e);
                Toast.makeText(this, R.string.ringtoneNotAccessible, Toast.LENGTH_SHORT).show();
                return false;
            }
            String durationString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationString == null || Long.parseLong(durationString) > (AndroidPrefsAccessor.WAITING_TIME - 1000L)) {
                Toast.makeText(this, R.string.ringtoneDurationTooLongOrInvalid, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    /**
     * Sets the ringtone title into the summary of the ringtone preference.
     *
     * @param preferenceRingtone
     * @param uriString
     */
    private void setPreferenceRingtoneSummary(RingtonePreference preferenceRingtone, String uriString) {
        CharSequence summary;
        if (uriString == null || uriString.isEmpty()) {
            summary = getText(R.string.summaryRingtoneNotSet);
        } else {
            Uri ringtoneUri = Uri.parse(uriString);
            Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
            summary = ringtone.getTitle(this);
        }
        preferenceRingtone.setSummary(summary);
    }

    /**
     * Returns true, if frequency divides an hour in whole numbers, e.g. true for 20 minutes.
     */
    private boolean isFrequencyDividesAnHour(String frequencyValue) {
        long frequencyValueInMinutes = Long.parseLong(frequencyValue) / 60000L;
        return 60 % frequencyValueInMinutes == 0;
    }

    /**
     * Returns true, if normalize - ringing on the minute - is requested
     */
    private boolean isNormalize(String normalizeValue) {
        return !AndroidPrefsAccessor.NORMALIZE_NONE.equals(normalizeValue);
    }

    @Override
    public void onPause() {
        super.onPause();
        AndroidContextAccessor.getInstanceAndLogPreferences(this).updateBellSchedule();
    }

    @SuppressWarnings("deprecation") // deprecation is because MindBell is not fragment-based
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_STATUS:
                CheckBoxPreference checkBoxPreferenceStatus =
                        (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyStatus));
                handleMuteHookOffAndStatusPermissionRequestResult(checkBoxPreferenceStatus, grantResults);
                break;
            case REQUEST_CODE_MUTE_OFF_HOOK:
                CheckBoxPreference checkBoxPreferenceMuteOffHook =
                        (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyMuteOffHook));
                handleMuteHookOffAndStatusPermissionRequestResult(checkBoxPreferenceMuteOffHook, grantResults);
                break;
            default:
                break;
        }
    }

    private void handleMuteHookOffAndStatusPermissionRequestResult(CheckBoxPreference one, int[] grantResults) {
        if (grantResults.length == 0) {
            // if request is cancelled, the result arrays are empty, so leave this option "off" and don't explain it
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // User granted the needed permission therefore this option is set to "on"
            one.setChecked(true);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MindBellPreferences.this,
                Manifest.permission.READ_PHONE_STATE)) {
            // User denied the needed permission and can be given an explanation, so we show an explanation
            new AlertDialog.Builder(MindBellPreferences.this) //
                    .setTitle(R.string.reasonReadPhoneStateTitle) //
                    .setMessage(R.string.reasonReadPhoneStateText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create() //
                    .show();
        } else {
            // User denied the needed permission and checked never ask again
            new AlertDialog.Builder(MindBellPreferences.this) //
                    .setTitle(R.string.neverAskAgainReadPhoneStateTitle) //
                    .setMessage(R.string.neverAskAgainReadPhoneStateText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create()//
                    .show();
        }
    }

}