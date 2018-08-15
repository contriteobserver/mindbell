/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
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
package com.googlecode.mindbell.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.googlecode.mindbell.R
import com.googlecode.mindbell.mission.ActionsExecutor
import com.googlecode.mindbell.mission.Notifier
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.mission.Prefs.Companion.MIN_MEDITATION_DURATION
import com.googlecode.mindbell.mission.Prefs.Companion.MIN_RAMP_UP_TIME
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_NEGATIVE_PERIOD
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT
import com.googlecode.mindbell.mission.Prefs.Companion.ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import com.googlecode.mindbell.mission.Scheduler
import com.googlecode.mindbell.mission.model.Statistics
import com.googlecode.mindbell.preference.MinutesIntervalPickerPreference
import com.googlecode.mindbell.util.TimeOfDay
import com.googlecode.mindbell.util.Utils
import kotlinx.android.synthetic.main.countdown.*
import kotlinx.android.synthetic.main.main.*
import kotlinx.android.synthetic.main.meditation_dialog.view.*

class MainActivity : Activity() {

    private lateinit var prefs: Prefs

    private lateinit var scheduler: Scheduler

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Main activity is being created")
        prefs = Prefs.getInstance(this)
        scheduler = Scheduler.getInstance(this)
        // Use the following line to show popup dialog on every start
        // setPopupShown(false);
        setContentView(R.layout.main)
        imageViewShowIntro.setOnClickListener { flipToAppropriateView(true) }
        imageViewHideIntro.setOnClickListener { flipToAppropriateView(false) }
        val ringOnceOnClickListener = View.OnClickListener {
            Log.d(TAG, "Ring once")
            val notifier = Notifier.getInstance(this)
            notifier.updateStatusNotification()
            val interruptSettings = prefs.forRingingOnce()
            prefs.addStatisticsEntry(Statistics.RingOnceActionsStatisticsEntry(interruptSettings))
            val actionsExecutor = ActionsExecutor.getInstance(this)
            actionsExecutor.startInterruptActions(interruptSettings, null, null)
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
        viewFlipper.displayedChild = if (prefs.isMeditating) 1 else if (showIntro) 3 else 2
    }

    /**
     * Create UI items in the app bar.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Save some place and do not show an icon on action bar
        actionBar!!.setIcon(android.R.color.transparent)
        // Inflate the currently selected menu XML resource.
        val inflater = menuInflater
        inflater.inflate(R.menu.settings, menu)
        val settingsItem = menu.findItem(R.id.settings)
        settingsItem.intent = Intent(this, SettingsActivity::class.java)
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
        activeSwitch.setOnClickListener { view -> onClickActive(view as Switch) }
        return true
    }

    /**
     * Handles click on menu item active.
     */
    private fun onMenuItemClickMeditating(): Boolean {
        if (!prefs.isMeditating) {
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
        val view = layoutInflater.inflate(R.layout.meditation_dialog, null)
        view.textViewRampUpTime.text = MinutesIntervalPickerPreference.deriveSummary(prefs.rampUpTime, false)
        attachIntervalPickerDialog(view.textViewRampUpTimeLabel, view.textViewRampUpTime, R.string.prefsRampUpTime, MIN_RAMP_UP_TIME, false, null)
        view.textViewMeditationDuration.text = MinutesIntervalPickerPreference.deriveSummary(prefs.meditationDuration, true)
        attachIntervalPickerDialog(view.textViewMeditationDurationLabel, view.textViewMeditationDuration, R.string.prefsMeditationDuration,
                MIN_MEDITATION_DURATION, true, object : OnPickListener {
            override fun onPick(): Boolean {
                return isValidMeditationSetup(view.textViewMeditationDuration, view.textViewMeditationDuration,
                        view.textViewNumberOfPeriods, view.textViewPatternOfPeriods)
            }
        })
        view.textViewNumberOfPeriods.text = prefs.numberOfPeriods.toString()
        attachNumberPickerDialog(view.textViewNumberOfPeriodsLabel, view.textViewNumberOfPeriods, R.string.prefsNumberOfPeriods, 1, 99,
                object : OnPickListener {
                    override fun onPick(): Boolean {
                        val numberOfPeriods = Integer.valueOf(view.textViewNumberOfPeriods.text.toString())!!
                        view.textViewPatternOfPeriods.text = Prefs.derivePatternOfPeriods(numberOfPeriods)
                        return isValidMeditationSetup(view.textViewNumberOfPeriods, view.textViewMeditationDuration, view.textViewNumberOfPeriods,
                                view.textViewPatternOfPeriods)
                    }
                })
        view.imageViewExplanationNumberOfPeriods.setOnClickListener {
            AlertDialog.Builder(this@MainActivity) //
                    .setTitle(R.string.prefsNumberOfPeriods) //
                    .setMessage(R.string.explanationNumberOfPeriods) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .show()
        }
        view.textViewPatternOfPeriods.text = prefs.patternOfPeriods
        attachEditTextDialog(view.textViewPatternOfPeriodsLabel, view.textViewPatternOfPeriods, R.string.prefsPatternOfPeriods,
                object : Normalizer {
                    override fun normalize(value: String): String {
                        return value
                                .trim { it <= ' ' }
                                .replace(Prefs.PERIOD_SEPARATOR_WITH_BLANKS_REGEX.toRegex(), Prefs.PERIOD_SEPARATOR_WITH_BLANK)
                    }
                }, object : OnEnterListener {
            override fun onEnter(value: String): Boolean {
                view.textViewNumberOfPeriods.text = Prefs.deriveNumberOfPeriods(value).toString()
                return isValidMeditationSetup(view.textViewPatternOfPeriods, view.textViewMeditationDuration, view.textViewNumberOfPeriods,
                        view.textViewPatternOfPeriods)
            }
        })
        view.imageViewExplanationPatternOfPeriods.setOnClickListener {
            AlertDialog.Builder(this@MainActivity) //
                    .setTitle(R.string.prefsPatternOfPeriods) //
                    .setMessage(R.string.explanationPatternOfPeriods) //
                    .setPositiveButton(android.R.string.ok, null) //
                    .show()
        }
        view.checkBoxKeepScreenOn.isChecked = prefs.isKeepScreenOn
        view.checkBoxStopMeditationAutomatically.isChecked = prefs.isStopMeditationAutomatically
        val meditationDialog = AlertDialog.Builder(this) //
                .setTitle(R.string.title_meditation_dialog) //
                .setView(view) //
                .setPositiveButton(R.string.buttonStartMeditation, null) // avoid implementation that dismisses the dialog
                .setNeutralButton(R.string.buttonStartMeditationDirectly, null) // avoid implementation that dismisses the dialog
                .setNegativeButton(android.R.string.cancel, null) //
                .show()
        // Ensure dialog is dismissed if input has been successfully validated
        meditationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            onClickStartMeditation(prefs, meditationDialog, view.textViewPatternOfPeriods, view.textViewMeditationDuration,
                    view.textViewNumberOfPeriods, view.textViewRampUpTime, view.checkBoxKeepScreenOn, view.checkBoxStopMeditationAutomatically,
                    false)
        }
        meditationDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            onClickStartMeditation(prefs, meditationDialog, view.textViewPatternOfPeriods, view.textViewMeditationDuration,
                    view.textViewNumberOfPeriods, view.textViewRampUpTime, view.checkBoxKeepScreenOn, view.checkBoxStopMeditationAutomatically,
                    true)
        }
    }

    /**
     * Validate chosen meditation dialog values, if ok store them in preferences and start meditation.
     */
    private fun onClickStartMeditation(prefs: Prefs?, meditationDialog: AlertDialog, textViewPatternOfPeriods: TextView,
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
     * Toggle active/inactive and set UI switch to the resulting state.
     */
    private fun onClickActive(activeSwitch: Switch): Boolean {
        prefs.isActive = !prefs.isActive
        activeSwitch.isChecked = prefs.isActive
        scheduler.updateBellScheduleForReminder(true)
        val feedback = getText(if (prefs.isActive) R.string.summaryActive else R.string.summaryNotActive)
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
            val timePicker = TimePicker(this@MainActivity)
            timePicker.setIs24HourView(true)
            val time = MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(textView.text.toString())
            @Suppress("DEPRECATION") // setCurrent*() deprecated now but not for older API levels < 23
            timePicker.currentHour = time.hour
            @Suppress("DEPRECATION") // setCurrent*() deprecated now but not for older API levels < 23
            timePicker.currentMinute = time.minute
            AlertDialog.Builder(this@MainActivity) //
                    .setTitle(residTitle) //
                    .setView(timePicker) //
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        @Suppress("DEPRECATION") // getCurrent*() deprecated now but not for older API levels < 23
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
            val periodMillis = Prefs.derivePeriodMillis(patternOfPeriods, meditationDuration, i)
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
            val numberPicker = NumberPicker(this@MainActivity)
            numberPicker.minValue = min
            numberPicker.maxValue = max
            numberPicker.value = Integer.valueOf(textView.text.toString())!!
            AlertDialog.Builder(this@MainActivity) //
                    .setTitle(residTitle) //
                    .setView(numberPicker) //
                    .setPositiveButton(android.R.string.ok) { _, _ ->
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
            val editText = EditText(this@MainActivity)
            editText.setText(textView.text)
            AlertDialog.Builder(this@MainActivity) //
                    .setTitle(residTitle) //
                    .setView(editText) //
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        Utils.hideKeyboard(this@MainActivity, editText)
                        var newValue = editText.text.toString()
                        if (normalizer != null) {
                            newValue = normalizer.normalize(newValue)
                        }
                        textView.text = newValue
                        onEnterListener?.onEnter(newValue)
                    } //
                    .setNegativeButton(android.R.string.cancel) { _, _ -> Utils.hideKeyboard(this@MainActivity, editText) } //
                    .show()
        }
        textViewLabel.setOnClickListener(onClickListener)
        textView.setOnClickListener(onClickListener)
    }

    /**
     * Update UI items in the app bar according to inner state. This gets called because {@link onResume} calls {@link invalidateOptionsMenu}.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        // Sync active switch in UI with inner state
        val activeItem = menu.findItem(R.id.active)
        val activeSwitch = activeItem.actionView as Switch
        activeSwitch.isChecked = prefs.isActive

        // Sync meditation start/stop icon in UI with inner state and prohibit other actions but stopping meditation while meditating
        val isMeditating = prefs.isMeditating
        val meditatingItem = menu.findItem(R.id.meditating)
        meditatingItem.setIcon(if (isMeditating) R.drawable.ic_action_meditating_off else R.drawable.ic_action_meditating_on)
        meditatingItem.setTitle(if (isMeditating) R.string.prefsMeditatingOff else R.string.prefsMeditatingOn)
        menu.findItem(R.id.active).isVisible = !isMeditating
        menu.findItem(R.id.settings).isVisible = !isMeditating
        menu.findItem(R.id.muteFor).isVisible = !isMeditating
        menu.findItem(R.id.about).isVisible = !isMeditating
        menu.findItem(R.id.help).isVisible = !isMeditating

        return true
    }

    override fun onResume() {
        if (intent.getBooleanExtra(Prefs.EXTRA_STOP_MEDITATION, false)) {
            Log.d(TAG, "MainActivity received stop meditation intent")
            // If the activity has once been opened from the InterruptService by automatically stopping meditation further screen
            // rotations will stop meditation, too, because getIntent() always returns the intent that initially opened the
            // activity. Hence the extra information must be removed to avoid stopping medtiation in these other cases.
            intent.removeExtra(Prefs.EXTRA_STOP_MEDITATION)
            // InterruptService detected meditation to be over and sent intent to leave meditation mode. To be sure user has not stopped
            // meditation in the meantime (between sending and receiving intent) we check that meditation is still running.
            if (prefs.isMeditating) {
                toggleMeditating() // as meditation is still running this means to stop the meditation mode
            }
        } else {
            flipToAppropriateView(false)
            if (prefs.isMeditating) {
                countdownView.startDisplayUpdateTimer()
            }
            invalidateOptionsMenu() // re-call onPrepareOptionsMenu(), maybe active setting has been changed via preferences
        }
        super.onResume()
    }

    /**
     * Toggle meditating state, update view if requested and show information to user.
     */
    private fun toggleMeditating() {
        prefs.isMeditating = !prefs.isMeditating // toggle active/inactive
        if (prefs.isMeditating) {
            val rampUpStartingTimeMillis = System.currentTimeMillis()
            val rampUpTimeMillis = if (prefs.isStartMeditationDirectly) 0L else prefs.rampUpTimeMillis
            val meditationStartingTimeMillis = rampUpStartingTimeMillis + rampUpTimeMillis
            val meditationEndingTimeMillis = meditationStartingTimeMillis + prefs.meditationDurationMillis
            // put values into preferences to make them survive an app termination because alarm goes on anyway
            prefs.rampUpStartingTimeMillis = rampUpStartingTimeMillis
            prefs.meditationStartingTimeMillis = meditationStartingTimeMillis
            prefs.meditationEndingTimeMillis = meditationEndingTimeMillis
            scheduler.updateBellScheduleForMeditation(rampUpStartingTimeMillis, if (prefs.isStartMeditationDirectly) 1 else 0)
            countdownView.startDisplayUpdateTimer()
            if (prefs.isKeepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d(TAG, "Keep screen on activated")
            }
        } else {
            countdownView.stopDisplayUpdateTimer()
            val actionsExecutor = ActionsExecutor.getInstance(this)
            actionsExecutor.finishBellSound()
            scheduler.updateBellScheduleForReminder(false)
            if (prefs.isKeepScreenOn) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d(TAG, "Keep screen on deactivated")
            }
        }
        flipToAppropriateView(false)
        invalidateOptionsMenu() // re-call onPrepareOptionsMenu()
        val feedback = getText(if (prefs.isMeditating) R.string.summaryMeditating else R.string.summaryNotMeditating)
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        // Stop meditation when screen is rotated, otherwise timer states had to be saved
        if (prefs.isMeditating) {
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
        val versionCodePopupShownFor = prefs.popup
        return versionCode == versionCodePopupShownFor
    }

    private fun setPopupShown(shown: Boolean) {
        if (shown) {
            val versionCode = Utils.getApplicationVersionCode(packageManager, packageName)
            prefs.popup = versionCode
        } else {
            prefs.resetPopup()
        }
    }

    private fun onMenuItemClickHelp(): Boolean {
        val popupView = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null)
        val versionName = Utils.getApplicationVersionName(packageManager, packageName)
        val builder = AlertDialog.Builder(this) //
                .setTitle("${getText(R.string.app_name).toString()} $versionName") //
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
