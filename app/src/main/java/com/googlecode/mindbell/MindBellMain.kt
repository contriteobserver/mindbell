/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 * for remembering what really counts
 * <p/>
 * Copyright (C) 2010-2014 Marc Schroeder
 * Copyright (C) 2014-2018 Uwe Damken
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.googlecode.mindbell

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.googlecode.mindbell.accessors.ContextAccessor
import com.googlecode.mindbell.accessors.PrefsAccessor
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.MIN_MEDITATION_DURATION
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.MIN_RAMP_UP_TIME
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.ONE_MINUTE_MILLIS_NEGATIVE_PERIOD
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING
import com.googlecode.mindbell.preference.MinutesIntervalPickerPreference
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.Utils
import kotlinx.android.synthetic.main.main.*
import kotlinx.android.synthetic.main.countdown.*
import kotlinx.android.synthetic.main.meditation_dialog.*

class MindBellMain : Activity() {

    private var contextAccessor: ContextAccessor? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MindBell.logDebug("Main activity is being created")
        contextAccessor = ContextAccessor.getInstance(this)
        // Use the following line to show popup dialog on every start
        // setPopupShown(false);
        setContentView(R.layout.main)
        imageViewShowIntro.setOnClickListener { flipToAppropriateView(true) }
        imageViewHideIntro.setOnClickListener { flipToAppropriateView(false) }
        val ringOnceOnClickListener = View.OnClickListener {
            MindBell.logDebug("Ring once")
            contextAccessor!!.updateStatusNotification()
            contextAccessor!!.startPlayingSoundAndVibrate(contextAccessor!!.prefs!!.forRingingOnce(), null)
        }
        imageViewRingOncePlayCollapsed.setOnClickListener(ringOnceOnClickListener)
        imageViewRingOnceBellCollapsed.setOnClickListener(ringOnceOnClickListener)
        imageViewRingOnceBellExpanded.setOnClickListener(ringOnceOnClickListener)
        countdownView.setOnClickListener(ringOnceOnClickListener)
    }

    /**
     * Flip to meditation view if isMeditating is true, to bell view otherwise.
     */
    private fun flipToAppropriateView(showIntro: Boolean) {
        viewFlipper.displayedChild = if (contextAccessor!!.prefs!!.isMeditating) 1 else if (showIntro) 3 else 2
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Save some place and do not show an icon on action bar
        actionBar!!.setIcon(android.R.color.transparent)
        // Inflate the currently selected menu XML resource.
        val inflater = menuInflater
        inflater.inflate(R.menu.settings, menu)
        val settingsItem = menu.findItem(R.id.settings)
        settingsItem.intent = Intent(this, MindBellPreferences::class.java)
        val muteForItem = menu.findItem(R.id.muteFor)
        muteForItem.intent = Intent(this, MuteActivity::class.java)
        val aboutItem = menu.findItem(R.id.about)
        aboutItem.intent = Intent(this, AboutActivity::class.java)
        val helpItem = menu.findItem(R.id.help)
        helpItem.setOnMenuItemClickListener { onMenuItemClickHelp() }
        val meditatingItem = menu.findItem(R.id.meditating)
        meditatingItem.setOnMenuItemClickListener { onMenuItemClickMeditating() }
        val activeItem = menu.findItem(R.id.active)
        val activeSwitch = activeItem.actionView as Switch
        activeSwitch.isChecked = contextAccessor!!.prefs!!.isActive
        activeSwitch.setOnCheckedChangeListener { buttonView, isChecked -> onCheckedChangedActive(isChecked) }
        return true
    }

    /**
     * Handles click on menu item active.
     */
    private fun onMenuItemClickMeditating(): Boolean {
        if (!contextAccessor!!.prefs!!.isMeditating) {
            showMeditationDialog()
        } else {
            toggleMeditating()
        }
        return true
    }

    /**
     * Creates and shows dialog to start meditation.
     */
    private fun showMeditationDialog() {
        val prefs = contextAccessor!!.prefs
        val view = layoutInflater.inflate(R.layout.meditation_dialog, null)
        textViewRampUpTime.text = MinutesIntervalPickerPreference.deriveSummary(prefs!!.rampUpTime, false)
        attachIntervalPickerDialog(textViewRampUpTimeLabel, textViewRampUpTime, R.string.prefsRampUpTime, MIN_RAMP_UP_TIME, false, null)
        textViewMeditationDuration.text = MinutesIntervalPickerPreference.deriveSummary(prefs.meditationDuration, true)
        attachIntervalPickerDialog(textViewMeditationDurationLabel, textViewMeditationDuration, R.string.prefsMeditationDuration,
                MIN_MEDITATION_DURATION, true, object : OnPickListener {
            override fun onPick(): Boolean {
                return isValidMeditationSetup(textViewMeditationDuration, textViewMeditationDuration,
                        textViewNumberOfPeriods, textViewPatternOfPeriods)
            }
        })
        textViewNumberOfPeriods.text = prefs.numberOfPeriods.toString()
        attachNumberPickerDialog(textViewNumberOfPeriodsLabel, textViewNumberOfPeriods, R.string.prefsNumberOfPeriods, 1, 99,
                object : OnPickListener {
                    override fun onPick(): Boolean {
                        val numberOfPeriods = Integer.valueOf(textViewNumberOfPeriods.text.toString())!!
                        textViewPatternOfPeriods.text = PrefsAccessor.derivePatternOfPeriods(numberOfPeriods)
                        return isValidMeditationSetup(textViewNumberOfPeriods, textViewMeditationDuration, textViewNumberOfPeriods,
                                textViewPatternOfPeriods)
                    }
                })
        imageViewExplanationNumberOfPeriods.setOnClickListener {
            AlertDialog.Builder(this@MindBellMain) //
                    .setTitle(R.string.prefsNumberOfPeriods) //
                    .setMessage(R.string.explanationNumberOfPeriods) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .show()
        }
        textViewPatternOfPeriods.text = prefs.patternOfPeriods
        attachEditTextDialog(textViewPatternOfPeriodsLabel, textViewPatternOfPeriods, R.string.prefsPatternOfPeriods,
                object : Normalizer {
                    override fun normalize(value: String): String {
                        return value
                                .trim { it <= ' ' }
                                .replace(PrefsAccessor.PERIOD_SEPARATOR_WITH_BLANKS_REGEX.toRegex(), PrefsAccessor.PERIOD_SEPARATOR_WITH_BLANK)
                    }
                }, object : OnEnterListener {
            override fun onEnter(value: String): Boolean {
                textViewNumberOfPeriods.text = PrefsAccessor.deriveNumberOfPeriods(value).toString()
                return isValidMeditationSetup(textViewPatternOfPeriods, textViewMeditationDuration, textViewNumberOfPeriods,
                        textViewPatternOfPeriods)
            }
        })
        imageViewExplanationPatternOfPeriods.setOnClickListener {
            AlertDialog.Builder(this@MindBellMain) //
                    .setTitle(R.string.prefsPatternOfPeriods) //
                    .setMessage(R.string.explanationPatternOfPeriods) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .show()
        }
        checkBoxKeepScreenOn.isChecked = prefs.isKeepScreenOn
        checkBoxStopMeditationAutomatically.isChecked = prefs.isStopMeditationAutomatically
        val meditationDialog = AlertDialog.Builder(this) //
                .setTitle(R.string.title_meditation_dialog) //
                .setView(view) //
                .setPositiveButton(R.string.buttonStartMeditation, null) // avoid implementation that dismisses the dialog
                .setNeutralButton(R.string.buttonStartMeditationDirectly, null) // avoid implementation that dismisses the dialog
                .setNegativeButton(android.R.string.cancel, null) //
                .show()
        // Ensure dialog is dismissed if input has been successfully validated
        meditationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            onClickStartMeditation(prefs, meditationDialog, textViewPatternOfPeriods, textViewMeditationDuration,
                    textViewNumberOfPeriods, textViewRampUpTime, checkBoxKeepScreenOn, checkBoxStopMeditationAutomatically,
                    false)
        }
        meditationDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            onClickStartMeditation(prefs, meditationDialog, textViewPatternOfPeriods, textViewMeditationDuration,
                    textViewNumberOfPeriods, textViewRampUpTime, checkBoxKeepScreenOn, checkBoxStopMeditationAutomatically,
                    true)
        }
    }

    /**
     * Validate chosen meditation dialog values, if ok store them in preferences and start meditation.
     */
    private fun onClickStartMeditation(prefs: PrefsAccessor?, meditationDialog: AlertDialog, textViewPatternOfPeriods: TextView,
                                       textViewMeditationDuration: TextView, textViewNumberOfPeriods: TextView,
                                       textViewRampUpTime: TextView, checkBoxKeepScreenOn: CheckBox,
                                       checkBoxStopMeditationAutomatically: CheckBox, startDirectly: Boolean) {
        if (isValidMeditationSetup(textViewPatternOfPeriods, textViewMeditationDuration, textViewNumberOfPeriods,
                textViewPatternOfPeriods)) {
            prefs!!.rampUpTime = MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(textViewRampUpTime.text.toString())
            prefs.meditationDuration = MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(textViewMeditationDuration.text.toString())
            prefs.patternOfPeriods = textViewPatternOfPeriods.text.toString()
            prefs.isKeepScreenOn = checkBoxKeepScreenOn.isChecked
            prefs.isStopMeditationAutomatically = checkBoxStopMeditationAutomatically.isChecked
            prefs.isStartMeditationDirectly = startDirectly
            meditationDialog.dismiss()
            toggleMeditating()
        }
    }

    /**
     * Handles change in checked state of active switch.
     */
    private fun onCheckedChangedActive(isChecked: Boolean): Boolean {
        val prefsAccessor = contextAccessor!!.prefs
        prefsAccessor!!.isActive = isChecked // toggle active/inactive
        contextAccessor!!.updateBellScheduleForReminder(true)
        val feedback = getText(if (prefsAccessor.isActive) R.string.summaryActive else R.string.summaryNotActive)
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
        return true
    }

    /**
     * Sets an OnClickListener upon the text view to open a time picker dialog when it is clicked.
     */
    private fun attachIntervalPickerDialog(textViewLabel: TextView, textView: TextView, residTitle: Int,
                                           min: TimeOfDay, isMinutesInterval: Boolean,
                                           onPickListener: OnPickListener?) {
        val onClickListener = View.OnClickListener {
            val timePicker = TimePicker(this@MindBellMain)
            timePicker.setIs24HourView(true)
            val time = MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(textView.text.toString())
            timePicker.currentHour = time.hour
            timePicker.currentMinute = time.minute
            AlertDialog.Builder(this@MindBellMain) //
                    .setTitle(residTitle) //
                    .setView(timePicker) //
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        var newTime = TimeOfDay(timePicker.currentHour, timePicker.currentMinute)
                        if (newTime.interval < min.interval) {
                            newTime = min
                        }
                        textView.text = MinutesIntervalPickerPreference.deriveSummary(newTime, isMinutesInterval)
                        onPickListener?.onPick()
                    } //
                    .setNegativeButton(android.R.string.cancel, null) //
                    .show()
        }
        textViewLabel.setOnClickListener(onClickListener)
        textView.setOnClickListener(onClickListener)
    }

    /**
     * Returns true if meditationDuration, numberOfPeriods and patternOfPeriods can be used together to setup a meditation,
     * otherwise it returns false and sets an error messages to the view specified as edited view. The latter gets the focus
     * otherwise the error message would not be displayed.
     */
    private fun isValidMeditationSetup(editedTextView: TextView, textViewMeditationDuration: TextView,
                                       textViewNumberOfPeriods: TextView, textViewPatternOfPeriods: TextView): Boolean {
        textViewMeditationDuration.error = null
        textViewNumberOfPeriods.error = null
        textViewPatternOfPeriods.error = null
        val meditationDuration = MinutesIntervalPickerPreference
                .parseTimeOfDayFromSummary(textViewMeditationDuration.text.toString())
                .interval
        val numberOfPeriods = Integer.valueOf(textViewNumberOfPeriods.text.toString())!!
        val patternOfPeriods = textViewPatternOfPeriods.text.toString()
        // validate by validating every period, this is not very efficient but all is reduced to a single implementation
        for (i in 1..numberOfPeriods) {
            val periodMillis = PrefsAccessor.derivePeriodMillis(patternOfPeriods, meditationDuration, i)
            var message: Int? = null
            if (periodMillis == ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION || periodMillis == ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING) {
                message = R.string.invalidPeriodSpecification
            } else if (periodMillis == ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING) {
                message = R.string.variablePeriodMissing
            } else if (periodMillis == ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT) {
                message = R.string.periodTooShort
            } else if (periodMillis == ONE_MINUTE_MILLIS_NEGATIVE_PERIOD) {
                message = R.string.negativePeriod
            }
            if (message != null) {
                editedTextView.error = getText(message).toString()
                editedTextView.requestFocus()
                return false
            }
        }
        return true
    }

    /**
     * Sets an OnClickListener upon the text view to open a number picker dialog when it is clicked.
     */
    private fun attachNumberPickerDialog(textViewLabel: TextView, textView: TextView, residTitle: Int,
                                         min: Int, max: Int, onPickListener: OnPickListener?) {
        val onClickListener = View.OnClickListener {
            val numberPicker = NumberPicker(this@MindBellMain)
            numberPicker.minValue = min
            numberPicker.maxValue = max
            numberPicker.value = Integer.valueOf(textView.text.toString())!!
            AlertDialog.Builder(this@MindBellMain) //
                    .setTitle(residTitle) //
                    .setView(numberPicker) //
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        val newValue = numberPicker.value
                        textView.text = newValue.toString()
                        onPickListener?.onPick()
                    } //
                    .setNegativeButton(android.R.string.cancel, null) //
                    .show()
        }
        textViewLabel.setOnClickListener(onClickListener)
        textView.setOnClickListener(onClickListener)
    }

    /**
     * Sets an OnClickListener upon the text view to open a edit text dialog when it is clicked.
     */
    private fun attachEditTextDialog(textViewLabel: TextView, textView: TextView, residTitle: Int,
                                     normalizer: Normalizer?, onEnterListener: OnEnterListener?) {
        val onClickListener = View.OnClickListener {
            val editText = EditText(this@MindBellMain)
            editText.setText(textView.text)
            AlertDialog.Builder(this@MindBellMain) //
                    .setTitle(residTitle) //
                    .setView(editText) //
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        Utils.hideKeyboard(this@MindBellMain, editText)
                        var newValue = editText.text.toString()
                        if (normalizer != null) {
                            newValue = normalizer.normalize(newValue)
                        }
                        textView.text = newValue
                        onEnterListener?.onEnter(newValue)
                    } //
                    .setNegativeButton(android.R.string.cancel) { dialog, which -> Utils.hideKeyboard(this@MindBellMain, editText) } //
                    .show()
        }
        textViewLabel.setOnClickListener(onClickListener)
        textView.setOnClickListener(onClickListener)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isMeditating = contextAccessor!!.prefs!!.isMeditating
        val meditatingItem = menu.findItem(R.id.meditating)
        meditatingItem.setIcon(if (isMeditating) R.drawable.ic_action_meditating_off else R.drawable.ic_action_meditating_on)
        meditatingItem.setTitle(if (isMeditating) R.string.prefsMeditatingOff else R.string.prefsMeditatingOn)
        // Do not allow other actions than stopping meditation while meditating
        menu.findItem(R.id.active).isVisible = !isMeditating
        menu.findItem(R.id.settings).isVisible = !isMeditating
        menu.findItem(R.id.muteFor).isVisible = !isMeditating
        menu.findItem(R.id.about).isVisible = !isMeditating
        menu.findItem(R.id.help).isVisible = !isMeditating
        return true
    }

    override fun onResume() {
        if (intent.getBooleanExtra(PrefsAccessor.EXTRA_STOP_MEDITATION, false)) {
            MindBell.logDebug("MindBellMain received stop meditation intent")
            // If the activity has once been opened from the Scheduler by automatically stopping meditation further screen
            // rotations will stop meditation, too, because getIntent() always returns the intent that initially opened the
            // activity. Hence the extra information must be removed to avoid stopping medtiation in these other cases.
            intent.removeExtra(PrefsAccessor.EXTRA_STOP_MEDITATION)
            // Scheduler detected meditation to be over and sent intent to leave meditation mode. To be sure user has not stopped
            // meditation in the meantime (between sending and receiving intent) we check that meditation is still running.
            if (contextAccessor!!.prefs!!.isMeditating) {
                toggleMeditating() // as meditation is still running this means to stop the meditation mode
            }
        } else {
            flipToAppropriateView(false)
            if (contextAccessor!!.prefs!!.isMeditating) {
                countdownView.startDisplayUpdateTimer(contextAccessor!!)
            }
            invalidateOptionsMenu() // re-call onPrepareOptionsMenu(), maybe active setting has been changed via preferences
        }
        super.onResume()
    }

    /**
     * Toggle meditating state, update view if requested and show information to user.
     */
    private fun toggleMeditating() {
        val prefs = contextAccessor!!.prefs
        prefs!!.isMeditating = !prefs.isMeditating // toggle active/inactive
        if (prefs.isMeditating) {
            val rampUpStartingTimeMillis = System.currentTimeMillis()
            val rampUpTimeMillis = if (prefs.isStartMeditationDirectly) 0L else prefs.rampUpTimeMillis
            val meditationStartingTimeMillis = rampUpStartingTimeMillis + rampUpTimeMillis
            val meditationEndingTimeMillis = meditationStartingTimeMillis + prefs.meditationDurationMillis
            // put values into preferences to make them survive an app termination because alarm goes on anyway
            prefs.rampUpStartingTimeMillis = rampUpStartingTimeMillis
            prefs.meditationStartingTimeMillis = meditationStartingTimeMillis
            prefs.meditationEndingTimeMillis = meditationEndingTimeMillis
            contextAccessor!!.updateBellScheduleForMeditation(rampUpStartingTimeMillis, if (prefs.isStartMeditationDirectly) 1 else 0)
            countdownView.startDisplayUpdateTimer(contextAccessor!!)
            if (prefs.isKeepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                MindBell.logDebug("Keep screen on activated")
            }
        } else {
            countdownView.stopDisplayUpdateTimer()
            contextAccessor!!.finishBellSound()
            contextAccessor!!.updateBellScheduleForReminder(false)
            if (prefs.isKeepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                MindBell.logDebug("Keep screen on deactivated")
            }
        }
        flipToAppropriateView(false)
        invalidateOptionsMenu() // re-call onPrepareOptionsMenu()
        val feedback = getText(if (prefs.isMeditating) R.string.summaryMeditating else R.string.summaryNotMeditating)
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        // Stop meditation when screen is rotated, otherwise timer states had to be saved
        if (contextAccessor!!.prefs!!.isMeditating) {
            countdownView.stopDisplayUpdateTimer()
        }
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkWhetherToShowPopup()
        }
    }

    private fun checkWhetherToShowPopup() {
        if (!hasShownPopup()) {
            setPopupShown(true)
            onMenuItemClickHelp()
        }
    }

    private fun hasShownPopup(): Boolean {
        val versionCode = Utils.getApplicationVersionCode(packageManager, packageName)
        val versionCodePopupShownFor = contextAccessor!!.prefs!!.popup
        return versionCode == versionCodePopupShownFor
    }

    private fun setPopupShown(shown: Boolean) {
        if (shown) {
            val versionCode = Utils.getApplicationVersionCode(packageManager, packageName)
            contextAccessor!!.prefs!!.popup = versionCode
        } else {
            contextAccessor!!.prefs!!.resetPopup()
        }
    }

    private fun onMenuItemClickHelp(): Boolean {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null)
        val versionName = Utils.getApplicationVersionName(packageManager, packageName)
        val builder = AlertDialog.Builder(this) //
                .setTitle(getText(R.string.app_name).toString() + " " + versionName) //
                .setIcon(R.mipmap.ic_launcher) //
                .setView(popupView) //
                .setPositiveButton(android.R.string.ok, null)
        builder.show()
        return true
    }

    /**
     * Callback interface to normalize a string value.
     */
    private interface Normalizer {

        fun normalize(value: String): String

    }

    /**
     * Listener that is called when an item has been picked in dialog a created by attach*PickerDialog().
     */
    private interface OnPickListener {

        fun onPick(): Boolean

    }

    /**
     * Listener that is called when an edit text has been entered in a dialog created by attachEditTextDialog().
     */
    private interface OnEnterListener {

        fun onEnter(value: String): Boolean

    }

}
