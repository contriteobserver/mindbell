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
package com.googlecode.mindbell

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.PreferenceActivity
import android.preference.RingtonePreference
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.googlecode.mindbell.accessors.ContextAccessor
import com.googlecode.mindbell.accessors.PrefsAccessor
import com.googlecode.mindbell.preference.ListPreferenceWithSummaryFix
import com.googlecode.mindbell.preference.MediaVolumePreference
import com.googlecode.mindbell.preference.MinutesIntervalPickerPreference
import com.googlecode.mindbell.preference.MultiSelectListPreferenceWithSummary
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.Utils

class MindBellPreferences : PreferenceActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    // Weird, but ringtone cannot be retrieved from RingtonePreference, only from SharedPreference or in ChangeListener
    private var preferenceRingtoneValue: String? = null

    /**
     * Return information to be sent by mail.
     */
    private val infoMailText: String
        get() {
            val sb = StringBuilder()
            sb.append("\n\n------------------------------\n")
            sb.append(getText(R.string.mailInfo1))
            sb.append("\n\n")
            sb.append(getText(R.string.mailInfo2))
            sb.append("\n\n")
            sb.append(Utils.getApplicationInformation(packageManager, packageName))
            sb.append("\n")
            sb.append(Utils.systemInformation)
            sb.append("\n")
            sb.append(Utils.limitedLogEntriesAsString)
            sb.append("\n")
            sb.append(getText(R.string.mailInfo2))
            sb.append("\n\n")
            sb.append(getText(R.string.mailInfo1))
            sb.append("\n------------------------------\n\n")
            return sb.toString()
        }

    @Suppress("DEPRECATION") // some methods deprecated because fragments are state of the art instead
    override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PrefsAccessor.getInstance(this)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_1)
        addPreferencesFromResource(R.xml.preferences_2) // notifications depend on SDK
        addPreferencesFromResource(R.xml.preferences_3)

        val preferenceUseAudioStreamVolumeSetting = preferenceScreen.findPreference(getText(R.string.keyUseAudioStreamVolumeSetting)) as CheckBoxPreference
        val preferenceStatus = preferenceScreen.findPreference(getText(R.string.keyStatus)) as CheckBoxPreference
        val preferenceShow = preferenceScreen.findPreference(getText(R.string.keyShow)) as CheckBoxPreference
        val preferenceSound = preferenceScreen.findPreference(getText(R.string.keySound)) as CheckBoxPreference
        val preferenceReminderSoundLength = preferenceScreen.findPreference(getText(R.string.keyReminderSoundLength)) as Preference
        val preferenceReminderBell = preferenceScreen.findPreference(getText(R.string.keyReminderBell)) as ListPreferenceWithSummaryFix
        val preferenceVolume = preferenceScreen.findPreference(getText(R.string.keyVolume)) as MediaVolumePreference
        val preferenceRingtone = preferenceScreen.findPreference(getText(R.string.keyRingtone)) as RingtonePreference
        val preferenceVibrate = preferenceScreen.findPreference(getText(R.string.keyVibrate)) as CheckBoxPreference
        val preferencePattern = preferenceScreen.findPreference(getText(R.string.keyPattern)) as ListPreferenceWithSummaryFix
        val preferenceMuteOffHook = preferenceScreen.findPreference(getText(R.string.keyMuteOffHook)) as CheckBoxPreference
        val preferenceFrequency = preferenceScreen.findPreference(getText(R.string.keyFrequency)) as MinutesIntervalPickerPreference
        val preferenceRandomize = preferenceScreen.findPreference(getText(R.string.keyRandomize)) as CheckBoxPreference
        val preferenceNormalize = preferenceScreen.findPreference(getText(R.string.keyNormalize)) as ListPreferenceWithSummaryFix
        val preferenceActiveOnDaysOfWeek = preferenceScreen.findPreference(
                getText(R.string.keyActiveOnDaysOfWeek)) as MultiSelectListPreferenceWithSummary
        val preferenceMeditationVolume = preferenceScreen.findPreference(getText(R.string.keyMeditationVolume)) as MediaVolumePreference
        val preferenceUseWorkaroundBell = preferenceScreen.findPreference(getText(R.string.keyUseWorkaroundBell)) as CheckBoxPreference
        val preferenceFAQ = preferenceScreen.findPreference(getText(R.string.keyFAQ)) as Preference
        val preferenceBatterySettings = preferenceScreen.findPreference(getText(R.string.keyBatterySettings)) as Preference
        val preferenceSendMail = preferenceScreen.findPreference(getText(R.string.keySendMail)) as Preference

        preferenceUseAudioStreamVolumeSetting.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val isChecked = newValue as Boolean
            if (!isChecked && prefs.mustUseAudioStreamVolumeSetting()) {
                Toast.makeText(this@MindBellPreferences, R.string.mustUseAudioStreamSetting, Toast.LENGTH_SHORT).show()
                false
            } else {
                preferenceVolume.isEnabled = preferenceSound.isChecked && !isChecked
                preferenceMeditationVolume.isEnabled = !isChecked
                true
            }
        }

        preferenceStatus.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue -> mediateMuteOffHookAndStatus(preferenceMuteOffHook, newValue, REQUEST_CODE_STATUS) }

        preferenceShow.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue -> mediateShowAndSoundAndVibrate(preferenceSound, preferenceVibrate, newValue) }

        preferenceSound.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            if (mediateShowAndSoundAndVibrate(preferenceShow, preferenceVibrate, newValue) && mediateSoundDurationRelatedSettings(preferenceFrequency, preferenceUseWorkaroundBell,
                    preferenceReminderBell, preferenceRingtoneValue, newValue)) {
                val isChecked = newValue as Boolean
                preferenceReminderBell.isEnabled = isChecked
                preferenceRingtone.isEnabled = isChecked && !PrefsAccessor.isUseStandardBell(preferenceReminderBell.value)
                preferenceVolume.isEnabled = !preferenceUseAudioStreamVolumeSetting.isChecked && isChecked
                true
            } else {
                false
            }
        }

        preferenceReminderSoundLength.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            onPreferenceClickReminderSoundLength(preferenceUseWorkaroundBell.isChecked, preferenceReminderBell.value,
                    preferenceRingtoneValue)
            true
        }

        preferenceReminderBell.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val reminderBell = newValue as String
            val isChecked = PrefsAccessor.isUseStandardBell(reminderBell)
            if (PrefsAccessor.isUseStandardBell(reminderBell) || ContextCompat.checkSelfPermission(this@MindBellPreferences, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                if (mediateSoundDurationRelatedSettings(preferenceFrequency, preferenceUseWorkaroundBell, reminderBell,
                        preferenceRingtoneValue, preferenceSound)) {
                    // Allow setting this option to "off" if permission is granted
                    preferenceRingtone.isEnabled = preferenceSound.isChecked && !isChecked
                    // Weird, but ringtone cannot be retrieved from RingtonePreference, only from SharedPreference
                    setPreferenceVolumeSoundUri(preferenceVolume, reminderBell, preferenceUseWorkaroundBell.isChecked,
                            preferenceRingtoneValue)
                    true
                } else {
                    false
                }
            } else {
                // Ask for permission if this option shall be set to "off" but permission is missing
                ActivityCompat.requestPermissions(this@MindBellPreferences,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_RINGTONE)
                // As the permission request is asynchronous we have to deny setting this option (to "off")
                false
            }
        }

        preferenceRingtone.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val newRingtoneValue = newValue as String
            if (validatePreferenceRingtone(newRingtoneValue) && mediateSoundDurationRelatedSettings(preferenceFrequency, preferenceUseWorkaroundBell,
                    preferenceReminderBell, newRingtoneValue, preferenceSound)) {
                setPreferenceRingtoneSummary(preferenceRingtone, newRingtoneValue)
                setPreferenceVolumeSoundUri(preferenceVolume, preferenceReminderBell.value,
                        preferenceUseWorkaroundBell.isChecked, newRingtoneValue)
                preferenceRingtoneValue = newRingtoneValue
                true
            } else {
                false
            }
        }

        preferenceVibrate.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue -> mediateShowAndSoundAndVibrate(preferenceShow, preferenceSound, newValue) }

        preferencePattern.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val vibrator = this@MindBellPreferences.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(PrefsAccessor.getVibrationPattern(newValue as String), -1)
            true
        }

        preferenceMuteOffHook.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue -> mediateMuteOffHookAndStatus(preferenceStatus, newValue, REQUEST_CODE_MUTE_OFF_HOOK) }

        preferenceRandomize.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                // if interval deviation is selected, normalize is disabled on screen but it must be disabled in preferences,
                // too. Otherwise the following scenario could happen: set interval 1 h, de-select randomize, set normalize to
                // hh:00, select randomize, set interval 2 h, de-select randomize again ... hh:00 would be left in normalize
                // erroneously.
                preferenceNormalize.value = PrefsAccessor.NORMALIZE_NONE
            }
            true
        }

        preferenceFrequency.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            if (!mediateSoundDurationRelatedSettings(newValue, preferenceUseWorkaroundBell, preferenceReminderBell,
                    preferenceRingtoneValue, preferenceSound)) {
                return@OnPreferenceChangeListener false
            } else if (preferenceRandomize.isChecked) {
                // if interval varies randomly, ringing on the minute is disabled and set to "no" anyway
                return@OnPreferenceChangeListener true
            } else if (isFrequencyDividesAnHour(TimeOfDay(newValue as String))) {
                // if frequency is factor of an hour, ringing on the minute may be requested
                preferenceNormalize.isEnabled = true
            } else {
                // if frequency is NOT factor of an hour, ringing on the minute may NOT be set
                if (preferenceNormalize.isEnabled && isNormalize(preferenceNormalize.value)) {
                    Toast.makeText(this@MindBellPreferences, R.string.frequencyDoesNotFitIntoAnHour, Toast.LENGTH_SHORT).show()
                    return@OnPreferenceChangeListener false
                } else {
                    preferenceNormalize.isEnabled = false
                }
            }
            true
        }

        preferenceNormalize.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            if (!isNormalize(newValue as String)) {
                // if normalize - ringing on the minute - is not wanted, it's fine, no more to check here
                true
            } else if (isFrequencyDividesAnHour(preferenceFrequency.time)) {
                // if frequency is factor of an hour, requesting ringing on the minute is allowed
                true
            } else {
                // if frequency is NOT factor of an hour, ringing on the minute may NOT be set
                Toast.makeText(this@MindBellPreferences, R.string.frequencyDoesNotFitIntoAnHour, Toast.LENGTH_SHORT).show()
                false
            }
        }

        preferenceActiveOnDaysOfWeek.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValues ->
            if ((newValues as Set<*>).isEmpty()) {
                Toast.makeText(this@MindBellPreferences, R.string.atLeastOneActiveDayNeeded, Toast.LENGTH_SHORT).show()
                return@OnPreferenceChangeListener false
            }
            true
        }

        preferenceFAQ.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val faqUri = Uri.parse(getText(R.string.faq_url).toString())
            val browserIntent = Intent(Intent.ACTION_VIEW, faqUri)
            startActivity(browserIntent)
            true
        }

        preferenceBatterySettings.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            onPreferenceClickBatterySettings()
            true
        }

        preferenceUseWorkaroundBell.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            if (mediateSoundDurationRelatedSettings(preferenceFrequency, newValue, preferenceReminderBell,
                    preferenceRingtoneValue, preferenceSound)) {
                val isChecked = newValue as Boolean
                setPreferenceVolumeSoundUri(preferenceVolume, preferenceReminderBell.value, isChecked,
                        preferenceRingtoneValue)
                true
            } else {
                false
            }
        }

        preferenceSendMail.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            AlertDialog.Builder(this@MindBellPreferences) //
                    .setTitle(R.string.prefsSendMail) //
                    .setMessage(R.string.mailInfo1) //
                    .setIcon(R.mipmap.ic_launcher) //
                    .setPositiveButton(android.R.string.ok) { _, _ -> onClickReallySendInfo() } //
                    .setNegativeButton(android.R.string.cancel, null) //
                    .show()
            true
        }

        // As no PreferenceChangeListener is called without change *BY USER*, some settings have to be made explicitly
        preferenceVolume.isEnabled = preferenceSound.isChecked && !preferenceUseAudioStreamVolumeSetting.isChecked
        preferenceMeditationVolume.isEnabled = !preferenceUseAudioStreamVolumeSetting.isChecked
        preferenceReminderBell.isEnabled = preferenceSound.isChecked
        preferenceRingtone.isEnabled = preferenceSound.isChecked && !PrefsAccessor.isUseStandardBell(preferenceReminderBell.value)
        preferenceRingtoneValue = prefs.ringtone // cannot be retrieved from preference
        setPreferenceRingtoneSummary(preferenceRingtone, preferenceRingtoneValue)
        setPreferenceVolumeSoundUri(preferenceVolume, preferenceReminderBell.value, preferenceUseWorkaroundBell.isChecked,
                preferenceRingtoneValue)

    }

    /**
     * Ensures that the CheckBoxPreferences checkBoxPreferenceMuteOffHook and checkBoxPreferenceStatus cannot be both "on" without
     * having READ_PHONE_STATE permission by returning false when this rule is violated.
     */
    private fun mediateMuteOffHookAndStatus(other: CheckBoxPreference, newValue: Any, requestCode: Int): Boolean {
        if (!other.isChecked || !(newValue as Boolean) ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Allow setting this option to "on" if other option is "off" or permission is granted
            return true
        } else {
            // Ask for permission if other option is "on" and this option shall be set to "on" but permission is missing
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), requestCode)
            // As the permission request is asynchronous we have to deny setting this option (to "on")
            return false
        }
    }

    /**
     * Ensures that the CheckBoxPreferences checkBoxPreferenceShow, checkBoxPreferenceSound and checkBoxPreferenceVibrate cannot be
     * all "off", at least one must be checked.
     */
    private fun mediateShowAndSoundAndVibrate(firstOther: CheckBoxPreference, secondOther: CheckBoxPreference, newValue: Any): Boolean {
        if (!firstOther.isChecked && !secondOther.isChecked && !(newValue as Boolean)) {
            Toast.makeText(this, R.string.atLeastOneRingingActionNeeded, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    /**
     * Ensures that the duration of the chosen sound does not exceed a quarter of the chosen interval.
     */
    private fun mediateSoundDurationRelatedSettings(preferenceFrequency: MinutesIntervalPickerPreference,
                                                    preferenceUseWorkaroundBell: CheckBoxPreference,
                                                    preferenceReminderBell: ListPreferenceWithSummaryFix,
                                                    preferenceRingtoneValue: String?, newSoundValue: Any): Boolean {
        return mediateSoundDurationRelatedSettings(preferenceFrequency.time, preferenceUseWorkaroundBell.isChecked,
                preferenceReminderBell.value, preferenceRingtoneValue, newSoundValue as Boolean)
    }

    /**
     * Ensures that the duration of the chosen sound does not exceed a quarter of the chosen interval.
     */
    private fun mediateSoundDurationRelatedSettings(preferenceFrequency: MinutesIntervalPickerPreference,
                                                    preferenceUseWorkaroundBell: CheckBoxPreference, newReminderBellValue: String,
                                                    preferenceRingtoneValue: String?, preferenceSound: CheckBoxPreference): Boolean {
        return mediateSoundDurationRelatedSettings(preferenceFrequency.time, preferenceUseWorkaroundBell.isChecked,
                newReminderBellValue, preferenceRingtoneValue, preferenceSound.isChecked)
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
    private fun setPreferenceVolumeSoundUri(preferenceVolume: MediaVolumePreference, reminderBell: String, isUseWorkaroundBell: Boolean, ringtone: String?) {
        preferenceVolume.setSoundUri(getReminderSoundUri(reminderBell, isUseWorkaroundBell, ringtone)!!)
    }

    /**
     * Returns the chosen sound depending on settings for reminderBell, ringtone and useWorkaroundBell.
     */
    private fun getReminderSoundUri(reminderBell: String, isUseWorkaroundBell: Boolean, ringtone: String?): Uri? {
        var prefs = PrefsAccessor.getInstance(this)
        // This implementation is almost the same as PrefsAccessor#getReminderBellSoundUri()
        var soundUri = prefs.getBellSoundUri(reminderBell, isUseWorkaroundBell)
        if (soundUri == null) { // use system notification ringtone if reminder bell sound is not set
            if (ringtone!!.isEmpty()) {
                soundUri = prefs.getDefaultReminderBellSoundUri(isUseWorkaroundBell)
            } else {
                soundUri = Uri.parse(ringtone)
            }
        }
        return soundUri
    }

    /**
     * Returns true if the ringtone specified by newRingtoneValue is unset, empty or accessible with valid length.
     */
    private fun validatePreferenceRingtone(newRingtoneValue: String?): Boolean {
        if (newRingtoneValue != null && !newRingtoneValue.isEmpty()) {
            val ringtoneDuration = Utils.getSoundDuration(this, Uri.parse(newRingtoneValue))
            if (ringtoneDuration == null) {
                Toast.makeText(this, R.string.ringtoneNotAccessible, Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    /**
     * Ensures that the duration of the chosen sound does not exceed a quarter of the chosen interval.
     */
    private fun mediateSoundDurationRelatedSettings(preferenceFrequency: MinutesIntervalPickerPreference,
                                                    preferenceUseWorkaroundBell: CheckBoxPreference,
                                                    preferenceReminderBell: ListPreferenceWithSummaryFix,
                                                    newRingtoneValue: String, preferenceSound: CheckBoxPreference): Boolean {
        return mediateSoundDurationRelatedSettings(preferenceFrequency.time, preferenceUseWorkaroundBell.isChecked,
                preferenceReminderBell.value, newRingtoneValue, preferenceSound.isChecked)
    }

    /**
     * Sets the ringtone title into the summary of the ringtone preference.
     *
     * @param preferenceRingtone
     * @param uriString
     */
    private fun setPreferenceRingtoneSummary(preferenceRingtone: RingtonePreference, uriString: String?) {
        val summary: CharSequence
        if (uriString == null || uriString.isEmpty()) {
            summary = getText(R.string.summaryRingtoneNotSet)
        } else {
            val ringtoneUri = Uri.parse(uriString)
            val ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
            summary = ringtone.getTitle(this)
        }
        preferenceRingtone.summary = summary
    }

    /**
     * Ensures that the duration of the chosen sound does not exceed a quarter of the chosen interval.
     */
    private fun mediateSoundDurationRelatedSettings(newFrequencyValue: Any, preferenceUseWorkaroundBell: CheckBoxPreference,
                                                    preferenceReminderBell: ListPreferenceWithSummaryFix,
                                                    preferenceRingtoneValue: String?, preferenceSound: CheckBoxPreference): Boolean {
        return mediateSoundDurationRelatedSettings(TimeOfDay(newFrequencyValue as String),
                preferenceUseWorkaroundBell.isChecked, preferenceReminderBell.value, preferenceRingtoneValue,
                preferenceSound.isChecked)
    }

    /**
     * Returns true, if frequency divides an hour in whole numbers, e.g. true for 20 minutes, or if frequency is a multiple of an
     * hour (a frequency of 0 is prohibited by MinutesIntervalPickerPreference).
     */
    private fun isFrequencyDividesAnHour(frequencyValue: TimeOfDay): Boolean {
        val interval = frequencyValue.interval
        return interval % 60 == 0 || 60 % interval == 0
    }

    /**
     * Returns true, if normalize - ringing on the minute - is requested
     */
    private fun isNormalize(normalizeValue: String): Boolean {
        return PrefsAccessor.NORMALIZE_NONE != normalizeValue
    }

    private fun onPreferenceClickReminderSoundLength(useWorkaroundBell: Boolean, reminderBell: String, ringtoneValue: String?) {
        val soundUri = getReminderSoundUri(reminderBell, useWorkaroundBell, ringtoneValue)!!
        var soundDuration = Utils.getSoundDuration(this, soundUri)
        var msg =
                if (soundDuration == null) {
                    getText(R.string.ringtoneNotAccessible).toString()
                } else {
                    soundDuration /= 1000L // in seconds
                    String.format(getText(R.string.reminderSoundLength).toString(), soundDuration,
                            getText(R.string.summaryReminderSoundLength))
                }
        AlertDialog.Builder(this) //
                .setTitle(R.string.prefsReminderSoundLength) //
                .setMessage(msg) //
                .setPositiveButton(android.R.string.ok, null) //
                .show()
    }

    private fun onPreferenceClickBatterySettings() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (Utils.isAppWhitelisted(this)) {
                AlertDialog.Builder(this) //
                        .setTitle(R.string.prefsBatterySettings) //
                        .setMessage(R.string.summaryBatterySettingsWhitelisted) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            // Take user to the battery settings so he can check the settings
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } //
                        .show()
            } else {
                AlertDialog.Builder(this) //
                        .setTitle(R.string.prefsBatterySettings) //
                        .setMessage(R.string.summaryBatterySettingsNotWhitelisted) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            // Take user to the battery settings instead of adding a new permission that might result in
                            // suspending MindBell from Google Play Store. See the comments for this answer:
                            // https://stackoverflow.com/a/33114136/2532583
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            val context = this@MindBellPreferences
                            Toast
                                    .makeText(context, context.getText(R.string.battery_settings_guidance1), Toast.LENGTH_LONG)
                                    .show()
                            Toast
                                    .makeText(context, context.getText(R.string.battery_settings_guidance2), Toast.LENGTH_LONG)
                                    .show()
                            Toast
                                    .makeText(context, context.getText(R.string.battery_settings_guidance3), Toast.LENGTH_LONG)
                                    .show()
                        } //
                        .show()
            }
        } else {
            AlertDialog.Builder(this) //
                    .setTitle(R.string.prefsBatterySettings) //
                    .setMessage(R.string.summaryBatterySettingsUnknown) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .show()
        }
    }

    /**
     * Ensures that the duration of the chosen sound does not exceed a quarter of the chosen interval.
     */
    private fun mediateSoundDurationRelatedSettings(preferenceFrequency: MinutesIntervalPickerPreference,
                                                    newUseWorkaroundBellValue: Any,
                                                    preferenceReminderBell: ListPreferenceWithSummaryFix,
                                                    preferenceRingtoneValue: String?, preferenceSound: CheckBoxPreference): Boolean {
        return mediateSoundDurationRelatedSettings(preferenceFrequency.time, newUseWorkaroundBellValue as Boolean,
                preferenceReminderBell.value, preferenceRingtoneValue, preferenceSound.isChecked)
    }

    /**
     * Handles click on confirmation to send info.
     */
    private fun onClickReallySendInfo() {
        PrefsAccessor.getInstance(this).logSettings()
        MindBell.logDebug("Excluded from battery optimization (always false for SDK < 23)? -> " + Utils.isAppWhitelisted(this))
        val i = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", getText(R.string.emailAddress).toString(), null))
        i.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.emailSubject))
        i.putExtra(Intent.EXTRA_TEXT, infoMailText)
        try {
            startActivity(Intent.createChooser(i, getText(R.string.emailChooseApp)))
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, getText(R.string.noEmailClients), Toast.LENGTH_SHORT).show()
        }

    }

    /**
     * Ensures that the duration of the chosen sound does not exceed a quarter of the chosen interval. Sound might occur every
     * interval +/- 50%. So minimum time for sound and silence if half of the interval. IMHO at least half of that time should be
     * silence. That's where the quarter comes from. This check ignores the fact that randomizing the interval might be switched
     * off.
     */
    private fun mediateSoundDurationRelatedSettings(frequency: TimeOfDay, useWorkaroundBell: Boolean, reminderBell: String,
                                                    ringtoneValue: String?, soundValue: Boolean): Boolean {
        if (!soundValue) { // everything's fine if no sound has to be played at all
            return true
        }
        val soundUri = getReminderSoundUri(reminderBell, useWorkaroundBell, ringtoneValue)!!
        var soundDuration = Utils.getSoundDuration(this, soundUri)
        if (soundDuration == null) {
            Toast.makeText(this, R.string.ringtoneNotAccessible, Toast.LENGTH_SHORT).show()
            return false
        }
        soundDuration /= 1000L // in seconds
        val maxDuration = frequency.interval * 60L / 4L // in seconds
        if (soundDuration > maxDuration) {
            val msg = String.format(getText(R.string.ringtoneDurationTooLong).toString(), soundDuration, maxDuration,
                    frequency.interval * 60L)
            MindBell.logWarn(msg + " (" + soundUri.toString() + ")")
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            return false
        } else if (soundDuration > 10L) {
            Toast.makeText(this, getText(R.string.summaryReminderSoundLength), Toast.LENGTH_LONG).show()
        }
        return true
    }

    public override fun onPause() {
        super.onPause()
        PrefsAccessor.getInstance(this).logSettings()
        ContextAccessor.getInstance(this).updateBellScheduleForReminder(true)
    }

    @Suppress("DEPRECATION") // some methods deprecated because fragments are state of the art instead
    override
    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE_STATUS -> {
                val checkBoxPreferenceStatus = preferenceScreen.findPreference(getText(R.string.keyStatus)) as CheckBoxPreference
                handleMuteHookOffAndStatusPermissionRequestResult(checkBoxPreferenceStatus, grantResults)
            }
            REQUEST_CODE_MUTE_OFF_HOOK -> {
                val checkBoxPreferenceMuteOffHook = preferenceScreen.findPreference(getText(R.string.keyMuteOffHook)) as CheckBoxPreference
                handleMuteHookOffAndStatusPermissionRequestResult(checkBoxPreferenceMuteOffHook, grantResults)
            }
            REQUEST_CODE_RINGTONE -> handleRingtonePermissionRequestResult(grantResults)
            else -> {
            }
        }
    }

    private fun handleMuteHookOffAndStatusPermissionRequestResult(one: CheckBoxPreference, grantResults: IntArray) {
        if (grantResults.size == 0) {
            // if request is cancelled, the result arrays are empty, so leave this option "off" and don't explain it
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // User granted the needed permission therefore this option is set to "on"
            if (one.onPreferenceChangeListener.onPreferenceChange(one, java.lang.Boolean.TRUE)) {
                one.isChecked = true // WARNING: This does NOT call the onPreferenceValueChangeListener
            }
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this@MindBellPreferences,
                Manifest.permission.READ_PHONE_STATE)) {
            // User denied the needed permission and can be given an explanation, so we show an explanation
            AlertDialog.Builder(this@MindBellPreferences) //
                    .setTitle(R.string.reasonReadPhoneStateTitle) //
                    .setMessage(R.string.reasonReadPhoneStateText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create() //
                    .show()
        } else {
            // User denied the needed permission and checked never ask again
            AlertDialog.Builder(this@MindBellPreferences) //
                    .setTitle(R.string.neverAskAgainReadPhoneStateTitle) //
                    .setMessage(R.string.neverAskAgainReadPhoneStateText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create()//
                    .show()
        }
    }

    private fun handleRingtonePermissionRequestResult(grantResults: IntArray) {
        @Suppress("DEPRECATION") // getPreferenceScreen() deprecated because fragments are state of the art instead
        val preferenceReminderBell = preferenceScreen.findPreference(getText(R.string.keyReminderBell)) as ListPreferenceWithSummaryFix
        if (grantResults.size == 0) {
            // if request is cancelled, the result arrays are empty, so leave this option "on" and don't explain it
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // User granted the needed permission therefore this option is set to "off"
            if (preferenceReminderBell.onPreferenceChangeListener.onPreferenceChange(preferenceReminderBell, "0")) {
                preferenceReminderBell.value = "0" // WARNING: This does NOT call the onPreferenceValueChangeListener
            }
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this@MindBellPreferences,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            // User denied the needed permission and can be given an explanation, so we show an explanation
            AlertDialog.Builder(this@MindBellPreferences) //
                    .setTitle(R.string.reasonReadExternalStorageTitle) //
                    .setMessage(R.string.reasonReadExternalStorageText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create() //
                    .show()
        } else {
            // User denied the needed permission and checked never ask again
            AlertDialog.Builder(this@MindBellPreferences) //
                    .setTitle(R.string.neverAskAgainReadExternalStorageTitle) //
                    .setMessage(R.string.neverAskAgainReadExternalStorageText) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .create()//
                    .show()
        }
    }

    companion object {

        private val REQUEST_CODE_STATUS = 0

        private val REQUEST_CODE_MUTE_OFF_HOOK = 1

        private val REQUEST_CODE_RINGTONE = 2
    }

}