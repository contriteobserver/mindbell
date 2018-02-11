/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
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
package com.googlecode.mindbell;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.AndroidPrefsAccessor;
import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.preference.ListPreferenceWithSummaryFix;
import com.googlecode.mindbell.preference.MediaVolumePreference;
import com.googlecode.mindbell.preference.MinutesIntervalPickerPreference;
import com.googlecode.mindbell.preference.MultiSelectListPreferenceWithSummary;
import com.googlecode.mindbell.util.TimeOfDay;
import com.googlecode.mindbell.util.Utils;

import java.util.Set;

public class MindBellPreferences extends PreferenceActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String TAG = "MindBell";

    private static final int REQUEST_CODE_STATUS = 0;

    private static final int REQUEST_CODE_MUTE_OFF_HOOK = 1;

    private static final int REQUEST_CODE_RINGTONE = 2;

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

        final CheckBoxPreference preferenceUseAudioStreamVolumeSetting =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyUseAudioStreamVolumeSetting));
        final CheckBoxPreference preferenceStatus =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyStatus));
        final CheckBoxPreference preferenceShow =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyShow));
        final CheckBoxPreference preferenceSound =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keySound));
        final ListPreferenceWithSummaryFix preferenceReminderBell =
                (ListPreferenceWithSummaryFix) getPreferenceScreen().findPreference(getText(R.string.keyReminderBell));
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
        final MinutesIntervalPickerPreference preferenceFrequency =
                (MinutesIntervalPickerPreference) getPreferenceScreen().findPreference(getText(R.string.keyFrequency));
        final CheckBoxPreference preferenceRandomize =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyRandomize));
        final ListPreferenceWithSummaryFix preferenceNormalize =
                (ListPreferenceWithSummaryFix) getPreferenceScreen().findPreference(getText(R.string.keyNormalize));
        final MultiSelectListPreferenceWithSummary preferenceActiveOnDaysOfWeek =
                (MultiSelectListPreferenceWithSummary) getPreferenceScreen().findPreference(
                        getText(R.string.keyActiveOnDaysOfWeek));
        final MediaVolumePreference preferenceMeditationVolume =
                (MediaVolumePreference) getPreferenceScreen().findPreference(getText(R.string.keyMeditationVolume));
        final CheckBoxPreference preferenceUseWorkaroundBell =
                (CheckBoxPreference) getPreferenceScreen().findPreference(getText(R.string.keyUseWorkaroundBell));
        final Preference preferenceFAQ = (Preference) getPreferenceScreen().findPreference(getText(R.string.keyFAQ));
        final Preference preferenceBatterySettings =
                (Preference) getPreferenceScreen().findPreference(getText(R.string.keyBatterySettings));
        final Preference preferenceSendMail = (Preference) getPreferenceScreen().findPreference(getText(R.string.keySendMail));

        preferenceUseAudioStreamVolumeSetting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isChecked = (Boolean) newValue;
                if (!isChecked && prefs.mustUseAudioStreamVolumeSetting()) {
                    Toast.makeText(MindBellPreferences.this, R.string.mustUseAudioStreamSetting, Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    preferenceVolume.setEnabled(preferenceSound.isChecked() && !isChecked);
                    preferenceMeditationVolume.setEnabled(!isChecked);
                    return true;
                }
            }

        });

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
                    preferenceReminderBell.setEnabled(isChecked);
                    preferenceRingtone.setEnabled(
                            isChecked && !AndroidPrefsAccessor.isUseStandardBell(preferenceReminderBell.getValue()));
                    preferenceVolume.setEnabled(!preferenceUseAudioStreamVolumeSetting.isChecked() && isChecked);
                    return true;
                } else {
                    return false;
                }
            }

        });

        preferenceReminderBell.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String reminderBell = (String) newValue;
                boolean isChecked = AndroidPrefsAccessor.isUseStandardBell(reminderBell);
                if (AndroidPrefsAccessor.isUseStandardBell(reminderBell) ||
                        ContextCompat.checkSelfPermission(MindBellPreferences.this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                PackageManager.PERMISSION_GRANTED) {
                    // Allow setting this option to "off" if permission is granted
                    preferenceRingtone.setEnabled(preferenceSound.isChecked() && !isChecked);
                    // Weird, but ringtone cannot be retrieved from RingtonePreference, only from SharedPreference
                    setPreferenceVolumeSoundUri(preferenceVolume, reminderBell, preferenceUseWorkaroundBell.isChecked(),
                            preferenceRingtoneValue);
                    return true;
                } else {
                    // Ask for permission if this option shall be set to "off" but permission is missing
                    ActivityCompat.requestPermissions(MindBellPreferences.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_RINGTONE);
                    // As the permission request is asynchronous we have to deny setting this option (to "off")
                    return false;
                }
            }

        });

        preferenceRingtone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preferenceRingtoneValue = (String) newValue;
                if (!validatePreferenceRingtone(preferenceRingtoneValue)) {
                    return false;
                }
                setPreferenceRingtoneSummary(preferenceRingtone, preferenceRingtoneValue);
                setPreferenceVolumeSoundUri(preferenceVolume, preferenceReminderBell.getValue(),
                        preferenceUseWorkaroundBell.isChecked(), preferenceRingtoneValue);
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
                } else if (isFrequencyDividesAnHour(new TimeOfDay((String) newValue))) {
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
                } else if (isFrequencyDividesAnHour(preferenceFrequency.getTime())) {
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

        preferenceFAQ.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                Uri faqUri = Uri.parse(getText(R.string.faq_url).toString());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, faqUri);
                startActivity(browserIntent);
                return true;
            }

        });

        preferenceBatterySettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                onPreferenceClickBatterySettings();
                return true;
            }


        });

        preferenceUseWorkaroundBell.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isChecked = (Boolean) newValue;
                setPreferenceVolumeSoundUri(preferenceVolume, preferenceReminderBell.getValue(), isChecked,
                        preferenceRingtoneValue);
                return true;
            }

        });

        preferenceSendMail.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(MindBellPreferences.this) //
                        .setTitle(R.string.prefsSendMail) //
                        .setMessage(R.string.mailInfo1) //
                        .setIcon(R.mipmap.ic_launcher) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onClickReallySendInfo();
                            }
                        }) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .show();
                return true;
            }

        });

        // As no PreferenceChangeListener is called without change *BY USER*, some settings have to be made explicitly
        preferenceVolume.setEnabled(preferenceSound.isChecked() && !preferenceUseAudioStreamVolumeSetting.isChecked());
        preferenceMeditationVolume.setEnabled(!preferenceUseAudioStreamVolumeSetting.isChecked());
        preferenceReminderBell.setEnabled(preferenceSound.isChecked());
        preferenceRingtone.setEnabled(
                preferenceSound.isChecked() && !AndroidPrefsAccessor.isUseStandardBell(preferenceReminderBell.getValue()));
        preferenceRingtoneValue = prefs.getRingtone(); // cannot be retrieved from preference
        setPreferenceRingtoneSummary(preferenceRingtone, preferenceRingtoneValue);
        setPreferenceVolumeSoundUri(preferenceVolume, preferenceReminderBell.getValue(), preferenceUseWorkaroundBell.isChecked(),
                preferenceRingtoneValue);

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
     * @param reminderBell
     * @param isUseWorkaroundBell
     * @param ringtone
     */
    private void setPreferenceVolumeSoundUri(MediaVolumePreference preferenceVolume, String reminderBell,
                                             boolean isUseWorkaroundBell, String ringtone) {
        Uri soundUri = AndroidPrefsAccessor.getBellSoundUri(this, reminderBell, isUseWorkaroundBell);
        if (soundUri == null) { // use system notification ringtone if reminder bell sound is not set
            if (ringtone.isEmpty()) {
                soundUri = AndroidPrefsAccessor.getDefaultReminderBellSoundUri(this, isUseWorkaroundBell);
            } else {
                soundUri = Uri.parse(ringtone);
            }
        }
        preferenceVolume.setSoundUri(soundUri);
    }

    /**
     * Returns true if the ringtone specified by uriString is unset, empty or accessible with valid length.
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
            long maxWaitingTime = AndroidPrefsAccessor.WAITING_TIME + AndroidPrefsAccessor.WORKAROUND_SILENCE_TIME;
            maxWaitingTime += maxWaitingTime / 100L; // add 1% tolerance
            if (durationString == null || Long.parseLong(durationString) > maxWaitingTime) {
                String msg = String.format(" (%s ms > %d ms)", durationString, maxWaitingTime);
                Log.w(TAG, "Sound <" + uriString + "> too long" + msg);
                Toast.makeText(this, getText(R.string.ringtoneDurationTooLongOrInvalid) + msg, Toast.LENGTH_SHORT).show();
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
     * Returns true, if frequency divides an hour in whole numbers, e.g. true for 20 minutes, or if frequency is a multiple of an
     * hour (a frequency of 0 is prohibited by MinutesIntervalPickerPreference).
     */
    private boolean isFrequencyDividesAnHour(TimeOfDay frequencyValue) {
        int interval = frequencyValue.getInterval();
        return interval % 60 == 0 || 60 % interval == 0;
    }

    /**
     * Returns true, if normalize - ringing on the minute - is requested
     */
    private boolean isNormalize(String normalizeValue) {
        return !AndroidPrefsAccessor.NORMALIZE_NONE.equals(normalizeValue);
    }

    private void onPreferenceClickBatterySettings() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Utils.isAppWhitelisted(this)) {
                new AlertDialog.Builder(this) //
                        .setTitle(R.string.prefsBatterySettings) //
                        .setMessage(R.string.summaryBatterySettingsWhitelisted) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Take user to the battery settings so he can check the settings
                                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                            }
                        }) //
                        .show();
            } else {
                new AlertDialog.Builder(this) //
                        .setTitle(R.string.prefsBatterySettings) //
                        .setMessage(R.string.summaryBatterySettingsNotWhitelisted) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Take user to the battery settings instead of adding a new permission that might result in
                                // suspending MindBell from Google Play Store. See the comments for this answer:
                                // https://stackoverflow.com/a/33114136/2532583
                                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                                Context context = MindBellPreferences.this;
                                Toast
                                        .makeText(context, context.getText(R.string.battery_settings_guidance1), Toast.LENGTH_LONG)
                                        .show();
                                Toast
                                        .makeText(context, context.getText(R.string.battery_settings_guidance2), Toast.LENGTH_LONG)
                                        .show();
                                Toast
                                        .makeText(context, context.getText(R.string.battery_settings_guidance3), Toast.LENGTH_LONG)
                                        .show();
                            }
                        }) //
                        .show();
            }
        } else {
            new AlertDialog.Builder(this) //
                    .setTitle(R.string.prefsBatterySettings) //
                    .setMessage(R.string.summaryBatterySettingsUnknown) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .show();
        }
    }

    /**
     * Handles click on confirmation to send info.
     */
    private void onClickReallySendInfo() {
        AndroidContextAccessor.getInstanceAndLogPreferences(this); // write settings to log
        MindBell.logDebug("Excluded from battery optimization (always false for SDK < 23)? -> " + Utils.isAppWhitelisted(this));
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getText(R.string.emailAddress).toString(), null));
        i.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.emailSubject));
        i.putExtra(Intent.EXTRA_TEXT, getInfoMailText());
        try {
            startActivity(Intent.createChooser(i, getText(R.string.emailChooseApp)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getText(R.string.noEmailClients), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Return information to be sent by mail.
     */
    private String getInfoMailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n------------------------------\n");
        sb.append(getText(R.string.mailInfo1));
        sb.append("\n\n");
        sb.append(getText(R.string.mailInfo2));
        sb.append("\n\n");
        sb.append(Utils.getApplicationInformation(getPackageManager(), getPackageName()));
        sb.append("\n");
        sb.append(Utils.getSystemInformation());
        sb.append("\n");
        sb.append(Utils.getLimitedLogEntriesAsString());
        sb.append("\n");
        sb.append(getText(R.string.mailInfo2));
        sb.append("\n\n");
        sb.append(getText(R.string.mailInfo1));
        sb.append("\n------------------------------\n\n");
        return sb.toString();
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
            case REQUEST_CODE_RINGTONE:
                handleRingtonePermissionRequestResult(grantResults);
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
            if (one.getOnPreferenceChangeListener().onPreferenceChange(one, Boolean.TRUE)) {
                one.setChecked(true); // WARNING: This does NOT call the onPreferenceValueChangeListener
            }
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

    private void handleRingtonePermissionRequestResult(int[] grantResults) {
        ListPreferenceWithSummaryFix preferenceReminderBell =
                (ListPreferenceWithSummaryFix) getPreferenceScreen().findPreference(getText(R.string.keyReminderBell));
        if (grantResults.length == 0) {
            // if request is cancelled, the result arrays are empty, so leave this option "on" and don't explain it
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // User granted the needed permission therefore this option is set to "off"
            if (preferenceReminderBell.getOnPreferenceChangeListener().onPreferenceChange(preferenceReminderBell, "0")) {
                preferenceReminderBell.setValue("0"); // WARNING: This does NOT call the onPreferenceValueChangeListener
            }
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(MindBellPreferences.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // User denied the needed permission and can be given an explanation, so we show an explanation
            new AlertDialog.Builder(MindBellPreferences.this) //
                    .setTitle(R.string.reasonReadExternalStorageTitle) //
                    .setMessage(R.string.reasonReadExternalStorageText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create() //
                    .show();
        } else {
            // User denied the needed permission and checked never ask again
            new AlertDialog.Builder(MindBellPreferences.this) //
                    .setTitle(R.string.neverAskAgainReadExternalStorageTitle) //
                    .setMessage(R.string.neverAskAgainReadExternalStorageText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create()//
                    .show();
        }
    }

}