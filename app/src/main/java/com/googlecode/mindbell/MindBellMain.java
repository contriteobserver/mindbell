/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 * for remembering what really counts
 * <p/>
 * Copyright (C) 2010-2014 Marc Schroeder
 * Copyright (C) 2014-2016 Uwe Damken
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
package com.googlecode.mindbell;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.preference.MinutesIntervalPickerPreference;
import com.googlecode.mindbell.util.TimeOfDay;
import com.googlecode.mindbell.util.Utils;

import static com.googlecode.mindbell.accessors.PrefsAccessor.MIN_MEDITATION_DURATION;
import static com.googlecode.mindbell.accessors.PrefsAccessor.MIN_RAMP_UP_TIME;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_NEGATIVE_PERIOD;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT;
import static com.googlecode.mindbell.accessors.PrefsAccessor.ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING;

public class MindBellMain extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private ContextAccessor contextAccessor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MindBell.logDebug("Main activity is being created");
        contextAccessor = AndroidContextAccessor.getInstance(this);
        // Use the following line to show popup dialog on every start
        // setPopupShown(false);
        setContentView(R.layout.main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Save some place and do not show an icon on action bar
        getActionBar().setIcon(android.R.color.transparent);
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        MenuItem settingsItem = menu.findItem(R.id.settings);
        settingsItem.setIntent(new Intent(this, MindBellPreferences.class));
        MenuItem muteForItem = menu.findItem(R.id.muteFor);
        muteForItem.setIntent(new Intent(this, MuteActivity.class));
        MenuItem aboutItem = menu.findItem(R.id.about);
        aboutItem.setIntent(new Intent(this, AboutActivity.class));
        MenuItem helpItem = menu.findItem(R.id.help);
        helpItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickHelp();
            }

        });
        MenuItem meditatingItem = menu.findItem(R.id.meditating);
        meditatingItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickMeditating();
            }
        });
        MenuItem activeItem = menu.findItem(R.id.active);
        Switch activeSwitch = (Switch) activeItem.getActionView();
        activeSwitch.setChecked(contextAccessor.getPrefs().isActive());
        activeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onCheckedChangedActive(isChecked);
            }

        });
        return true;
    }

    private boolean onMenuItemClickHelp() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null);
        String versionName = Utils.getApplicationVersionName(getPackageManager(), getPackageName());
        AlertDialog.Builder builder = new AlertDialog.Builder(this) //
                .setTitle(getText(R.string.app_name) + " " + versionName) //
                .setIcon(R.mipmap.ic_launcher) //
                .setView(popupView) //
                .setPositiveButton(android.R.string.ok, null) //
                .setNegativeButton(R.string.sendMail, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickSendInfo();
                    }
                });
        if (Build.VERSION.SDK_INT >= 23) {
            builder.setNeutralButton(R.string.batterySettings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onMenuItemClickBatteryOptimizationSettings();
                }
            });
        }
        builder.show();
        return true;
    }

    /**
     * Handles click on menu item active.
     */
    private boolean onMenuItemClickMeditating() {
        final PrefsAccessor prefs = contextAccessor.getPrefs();
        if (!prefs.isMeditating()) {
            View view = getLayoutInflater().inflate(R.layout.meditation_dialog, null);
            final TextView textViewRampUpTime = (TextView) view.findViewById(R.id.rampUpTime);
            final TextView textViewMeditationDuration = (TextView) view.findViewById(R.id.meditationDuration);
            final TextView textViewNumberOfPeriods = (TextView) view.findViewById(R.id.numberOfPeriods);
            final TextView textViewExplanationNumberOfPeriods = (TextView) view.findViewById(R.id.explanationNumberOfPeriods);
            final TextView textViewPatternOfPeriods = (TextView) view.findViewById(R.id.patternOfPeriods);
            final TextView textViewExplanationPatternOfPeriods = (TextView) view.findViewById(R.id.explanationPatternOfPeriods);
            final CheckBox checkBoxKeepScreenOn = (CheckBox) view.findViewById(R.id.keepScreenOn);
            textViewRampUpTime.setText(MinutesIntervalPickerPreference.deriveSummary(prefs.getRampUpTime(), false));
            attachIntervalPickerDialog(textViewRampUpTime, R.string.prefsRampUpTime, MIN_RAMP_UP_TIME, false, null);
            textViewMeditationDuration.setText(MinutesIntervalPickerPreference.deriveSummary(prefs.getMeditationDuration(), true));
            attachIntervalPickerDialog(textViewMeditationDuration, R.string.prefsMeditationDuration, MIN_MEDITATION_DURATION, true,
                    new OnPickListener() {
                        @Override
                        public boolean onPick() {
                            return isValidMeditationSetup(textViewMeditationDuration, textViewMeditationDuration,
                                    textViewNumberOfPeriods, textViewPatternOfPeriods);
                        }
                    });
            textViewNumberOfPeriods.setText(String.valueOf(prefs.getNumberOfPeriods()));
            attachNumberPickerDialog(textViewNumberOfPeriods, R.string.prefsNumberOfPeriods, 1, 99, new OnPickListener() {
                @Override
                public boolean onPick() {
                    int numberOfPeriods = Integer.valueOf(textViewNumberOfPeriods.getText().toString());
                    textViewPatternOfPeriods.setText(PrefsAccessor.derivePatternOfPeriods(numberOfPeriods));
                    return isValidMeditationSetup(textViewNumberOfPeriods, textViewMeditationDuration, textViewNumberOfPeriods,
                            textViewPatternOfPeriods);
                }
            });
            textViewExplanationNumberOfPeriods.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MindBellMain.this) //
                            .setTitle(R.string.prefsNumberOfPeriods) //
                            .setMessage(R.string.explanationNumberOfPeriods) //
                            .setPositiveButton(android.R.string.ok, null) //
                            .show();
                }
            });
            textViewPatternOfPeriods.setText(prefs.getPatternOfPeriods());
            attachEditTextDialog(textViewPatternOfPeriods, R.string.prefsPatternOfPeriods, new Normalizer() {
                @Override
                public String normalize(String value) {
                    return value
                            .trim()
                            .replaceAll(PrefsAccessor.PERIOD_SEPARATOR_WITH_BLANKS_REGEX,
                                    PrefsAccessor.PERIOD_SEPARATOR_WITH_BLANK);
                }
            }, new OnEnterListener() {
                @Override
                public boolean onEnter(String value) {
                    textViewNumberOfPeriods.setText(String.valueOf(PrefsAccessor.deriveNumberOfPeriods(value)));
                    return isValidMeditationSetup(textViewPatternOfPeriods, textViewMeditationDuration, textViewNumberOfPeriods,
                            textViewPatternOfPeriods);
                }
            });
            textViewExplanationPatternOfPeriods.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(MindBellMain.this) //
                            .setTitle(R.string.prefsPatternOfPeriods) //
                            .setMessage(R.string.explanationPatternOfPeriods) //
                            .setPositiveButton(android.R.string.ok, null) //
                            .show();
                }
            });
            checkBoxKeepScreenOn.setChecked(prefs.isKeepScreenOn());
            final AlertDialog meditationDialog = new AlertDialog.Builder(this) //
                    .setTitle(R.string.title_meditation_dialog) //
                    .setView(view) //
                    .setPositiveButton(R.string.buttonStartMeditation, null) // avoid implementation that dismisses the dialog
                    .setNegativeButton(android.R.string.cancel, null) //
                    .show();
            // Ensure dialog is dismissed if input has been successfully validated
            meditationDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isValidMeditationSetup(textViewPatternOfPeriods, textViewMeditationDuration, textViewNumberOfPeriods,
                            textViewPatternOfPeriods)) {
                        prefs.setRampUpTime(
                                MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(textViewRampUpTime.getText().toString()));
                        prefs.setMeditationDuration(MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(
                                textViewMeditationDuration.getText().toString()));
                        prefs.setPatternOfPeriods(textViewPatternOfPeriods.getText().toString());
                        prefs.setKeepScreenOn(checkBoxKeepScreenOn.isChecked());
                        meditationDialog.dismiss();
                        toggleMeditating();
                    }
                }
            });
            meditationDialog.show();
        } else {
            toggleMeditating();
        }
        return true;
    }

    /**
     * Handles change in checked state of active switch.
     */
    private boolean onCheckedChangedActive(boolean isChecked) {
        PrefsAccessor prefsAccessor = contextAccessor.getPrefs();
        prefsAccessor.setActive(isChecked); // toggle active/inactive
        contextAccessor.updateBellSchedule();
        CharSequence feedback = getText((prefsAccessor.isActive()) ? R.string.summaryActive : R.string.summaryNotActive);
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Handles click on send info button.
     */
    private void onClickSendInfo() {
        new AlertDialog.Builder(this) //
                .setTitle(R.string.sendMail) //
                .setMessage(R.string.mailInfo2) //
                .setIcon(R.mipmap.ic_launcher) //
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onClickReallySendInfo();
                    }
                }) //
                .setNegativeButton(android.R.string.cancel, null) //
                .show();
    }

    /**
     * Handles click on menu item battery optimization settings.
     *
     * Warning: Caller must ensure SDK >= 23.
     */
    private void onMenuItemClickBatteryOptimizationSettings() {
        if (Utils.isAppWhitelisted(this)) {
            Toast.makeText(this, getText(R.string.alreadyWhitelisted), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getText(R.string.shouldGetWhitelisted), Toast.LENGTH_LONG).show();
        }
        startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
    }

    /**
     * Sets an OnClickListener upon the text view to open a time picker dialog when it is clicked.
     */
    private void attachIntervalPickerDialog(final TextView textView, final int residTitle, final TimeOfDay min,
                                            final boolean isMinutesInterval, final OnPickListener onPickListener) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TimePicker timePicker = new TimePicker(MindBellMain.this);
                timePicker.setIs24HourView(true);
                TimeOfDay time = MinutesIntervalPickerPreference.parseTimeOfDayFromSummary(textView.getText().toString());
                timePicker.setCurrentHour(time.getHour());
                timePicker.setCurrentMinute(time.getMinute());
                new AlertDialog.Builder(MindBellMain.this) //
                        .setTitle(residTitle) //
                        .setView(timePicker) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TimeOfDay newTime = new TimeOfDay(timePicker.getCurrentHour(), timePicker.getCurrentMinute());
                                if (newTime.getInterval() < min.getInterval()) {
                                    newTime = min;
                                }
                                textView.setText(MinutesIntervalPickerPreference.deriveSummary(newTime, isMinutesInterval));
                                if (onPickListener != null) {
                                    onPickListener.onPick();
                                }
                            }
                        }) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .show();
            }
        });
    }

    /**
     * Returns true if meditationDuration, numberOfPeriods and patternOfPeriods can be used together to setup a meditation,
     * otherwise it returns false and sets an error messages to the view specified as edited view. The latter gets the focus
     * otherwise the error message would not be displayed.
     */
    private boolean isValidMeditationSetup(TextView editedTextView, TextView textViewMeditationDuration,
                                           TextView textViewNumberOfPeriods, TextView textViewPatternOfPeriods) {
        textViewMeditationDuration.setError(null);
        textViewNumberOfPeriods.setError(null);
        textViewPatternOfPeriods.setError(null);
        int meditationDuration = MinutesIntervalPickerPreference
                .parseTimeOfDayFromSummary(textViewMeditationDuration.getText().toString())
                .getInterval();
        int numberOfPeriods = Integer.valueOf(textViewNumberOfPeriods.getText().toString());
        String patternOfPeriods = textViewPatternOfPeriods.getText().toString();
        // validate by validating every period, this is not very efficient but all is reduced to a single implementation
        for (int i = 1; i <= numberOfPeriods; i++) {
            long periodMillis = PrefsAccessor.derivePeriodMillis(patternOfPeriods, meditationDuration, i);
            Integer message = null;
            if (periodMillis == ONE_MINUTE_MILLIS_INVALID_PERIOD_SPECIFICATION ||
                    periodMillis == ONE_MINUTE_MILLIS_PERIOD_NOT_EXISTING) {
                message = R.string.invalidPeriodSpecification;
            } else if (periodMillis == ONE_MINUTE_MILLIS_VARIABLE_PERIOD_MISSING) {
                message = R.string.variablePeriodMissing;
            } else if (periodMillis == ONE_MINUTE_MILLIS_PERIOD_TOO_SHORT) {
                message = R.string.periodTooShort;
            } else if (periodMillis == ONE_MINUTE_MILLIS_NEGATIVE_PERIOD) {
                message = R.string.negativePeriod;
            }
            if (message != null) {
                editedTextView.setError(getText(message).toString());
                editedTextView.requestFocus();
                return false;
            }
        }
        return true;
    }

    /**
     * Sets an OnClickListener upon the text view to open a number picker dialog when it is clicked.
     */
    private void attachNumberPickerDialog(final TextView textView, final int residTitle, final int min, final int max,
                                          final OnPickListener onPickListener) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final NumberPicker numberPicker = new NumberPicker(MindBellMain.this);
                numberPicker.setMinValue(min);
                numberPicker.setMaxValue(max);
                numberPicker.setValue(Integer.valueOf(textView.getText().toString()));
                new AlertDialog.Builder(MindBellMain.this) //
                        .setTitle(residTitle) //
                        .setView(numberPicker) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int newValue = numberPicker.getValue();
                                textView.setText(String.valueOf(newValue));
                                if (onPickListener != null) {
                                    onPickListener.onPick();
                                }
                            }
                        }) //
                        .setNegativeButton(android.R.string.cancel, null) //
                        .show();
            }
        });
    }

    /**
     * Sets an OnClickListener upon the text view to open a edit text dialog when it is clicked.
     */
    private void attachEditTextDialog(final TextView textView, final int residTitle, final Normalizer normalizer,
                                      final OnEnterListener onEnterListener) {
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final EditText editText = new EditText(MindBellMain.this);
                editText.setText(textView.getText());
                new AlertDialog.Builder(MindBellMain.this) //
                        .setTitle(residTitle) //
                        .setView(editText) //
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Utils.hideKeyboard(MindBellMain.this, editText);
                                String newValue = editText.getText().toString();
                                if (normalizer != null) {
                                    newValue = normalizer.normalize(newValue);
                                }
                                textView.setText(newValue);
                                if (onEnterListener != null) {
                                    onEnterListener.onEnter(newValue);
                                }
                            }
                        }) //
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Utils.hideKeyboard(MindBellMain.this, editText);
                            }
                        }) //
                        .show();
            }
        });
    }

    /**
     * Toggle meditating state, update view if requested and show information to user.
     */
    private void toggleMeditating() {
        PrefsAccessor prefs = contextAccessor.getPrefs();
        prefs.setMeditating(!prefs.isMeditating()); // toggle active/inactive
        CountdownView countdownView = (CountdownView) findViewById(R.id.countdown);
        if (prefs.isMeditating()) {
            long rampUpStartingTimeMillis = System.currentTimeMillis();
            long meditationStartingTimeMillis = rampUpStartingTimeMillis + prefs.getRampUpTimeMillis();
            long meditationEndingTimeMillis = meditationStartingTimeMillis + prefs.getMeditationDurationMillis();
            // put values into preferences to make them survive an app termination because alarm goes on anyway
            prefs.setRampUpStartingTimeMillis(rampUpStartingTimeMillis);
            prefs.setMeditationStartingTimeMillis(meditationStartingTimeMillis);
            prefs.setMeditationEndingTimeMillis(meditationEndingTimeMillis);
            contextAccessor.updateBellSchedule(rampUpStartingTimeMillis);
            countdownView.startDisplayUpdateTimer(contextAccessor);
            if (prefs.isKeepScreenOn()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                MindBell.logDebug("Keep screen on activated");
            }
        } else {
            countdownView.stopDisplayUpdateTimer();
            contextAccessor.finishBellSound();
            contextAccessor.updateBellSchedule();
            if (prefs.isKeepScreenOn()) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                MindBell.logDebug("Keep screen on deactivated");
            }
        }
        flipToAppropriateView();
        invalidateOptionsMenu(); // re-call onPrepareOptionsMenu()
        CharSequence feedback = getText((prefs.isMeditating()) ? R.string.summaryMeditating : R.string.summaryNotMeditating);
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
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
     * Flip to meditation view if isMeditating is true, to bell view otherwise.
     */
    private void flipToAppropriateView() {
        ViewFlipper viewFlipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        viewFlipper.setDisplayedChild(contextAccessor.getPrefs().isMeditating() ? 1 : 2);
    }

    /**
     * Return information to be sent by mail.
     */
    private String getInfoMailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
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
        return sb.toString();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem meditatingItem = menu.findItem(R.id.meditating);
        meditatingItem.setIcon((contextAccessor.getPrefs().isMeditating()) ?
                R.drawable.ic_action_meditating_off :
                R.drawable.ic_action_meditating_on);
        meditatingItem.setTitle(
                (contextAccessor.getPrefs().isMeditating()) ? R.string.prefsMeditatingOff : R.string.prefsMeditatingOn);
        return true;
    }

    @Override
    protected void onResume() {
        flipToAppropriateView();
        if (contextAccessor.getPrefs().isMeditating()) {
            CountdownView countdownView = (CountdownView) findViewById(R.id.countdown);
            countdownView.startDisplayUpdateTimer(contextAccessor);
        }
        invalidateOptionsMenu(); // Maybe active setting has been changed via MindBellPreferences
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Stop meditation when screen is rotated, otherwise timer states had to be saved
        if (contextAccessor.getPrefs().isMeditating()) {
            CountdownView countdownView = (CountdownView) findViewById(R.id.countdown);
            countdownView.stopDisplayUpdateTimer();
        }
        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            MindBell.logDebug("Bell tapped");
            notifyIfNotActive();
            contextAccessor.updateStatusNotification();
            contextAccessor.startPlayingSoundAndVibrate(contextAccessor.getPrefs().forTapping(), null);
        }
        return true;
    }

    /**
     * Show hint how to activate the bell.
     */
    private void notifyIfNotActive() {
        if (!contextAccessor.getPrefs().isActive()) {
            Toast.makeText(this, R.string.howToSet, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            checkWhetherToShowPopup();
        }
    }

    private void checkWhetherToShowPopup() {
        if (!hasShownPopup()) {
            setPopupShown(true);
            requestPermissionsAndOpenHelpDialog(); // calls onMenuItemClickHelp() afterwards
        }
    }

    private boolean hasShownPopup() {
        int versionCode = Utils.getApplicationVersionCode(getPackageManager(), getPackageName());
        int versionCodePopupShownFor = contextAccessor.getPrefs().getPopup();
        return versionCode == versionCodePopupShownFor;
    }

    private void setPopupShown(boolean shown) {
        if (shown) {
            int versionCode = Utils.getApplicationVersionCode(getPackageManager(), getPackageName());
            contextAccessor.getPrefs().setPopup(versionCode);
        } else {
            contextAccessor.getPrefs().resetPopup();
        }
    }

    private void requestPermissionsAndOpenHelpDialog() {
        if (!contextAccessor.getPrefs().isStatus() || !contextAccessor.getPrefs().isMuteOffHook() ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED) {
            onMenuItemClickHelp(); // Permission not needed or already granted => go directly to the next dialog
        } else {
            new AlertDialog.Builder(this) //
                    .setTitle(R.string.requestReadPhoneStateTitle) //
                    .setMessage(R.string.requestReadPhoneStateText) //
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ActivityCompat.requestPermissions(MindBellMain.this, new String[]{Manifest.permission.READ_PHONE_STATE},
                                    0);
                        }
                    }) //
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            onMenuItemClickHelp();
                        }
                    }) //
                    .create() //
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // Request permission without handling the result but calling the popup dialog. If permissions are not sufficient for
        // the settings the user will get a warning notification and can grant permission via settings.
        onMenuItemClickHelp();
    }

    /**
     * Callback interface to normalize a string value.
     */
    private interface Normalizer {

        String normalize(String value);

    }

    /**
     * Listener that is called when an item has been picked in dialog a created by attach*PickerDialog().
     */
    private interface OnPickListener {

        boolean onPick();

    }

    /**
     * Listener that is called when an edit text has been entered in a dialog created by attachEditTextDialog().
     */
    private interface OnEnterListener {

        boolean onEnter(String value);

    }

}
