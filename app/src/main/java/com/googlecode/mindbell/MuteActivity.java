/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
 *     Copyright (C) 2014-2017 Uwe Damken
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.NumberPicker;

import com.googlecode.mindbell.accessors.ContextAccessor;

/**
 * Activity to ask for the time period to mute the bell for.
 */
public class MuteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ContextAccessor contextAccessor = ContextAccessor.getInstance(this);
        final NumberPicker numberPicker = new NumberPicker(this);
        final int hours = 24;
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(hours);
        numberPicker.setDisplayedValues(createDisplayedHourValues(hours));
        new AlertDialog.Builder(this) //
                .setTitle(R.string.statusActionMuteFor) //
                .setView(numberPicker) //
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int newValue = numberPicker.getValue();
                        long nextTargetTimeMillis = System.currentTimeMillis() + newValue * 3600000L;
                        contextAccessor.getPrefs().setMutedTill(nextTargetTimeMillis);
                        contextAccessor.updateStatusNotification();
                        contextAccessor.scheduleUpdateStatusNotificationMutedTill(nextTargetTimeMillis);
                        MuteActivity.this.finish();
                    }
                }) //
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MuteActivity.this.finish();
                    }
                }) //
                .show();
    }

    @NonNull
    private String[] createDisplayedHourValues(int hours) {
        String[] displayedValues = new String[hours + 1];
        displayedValues[0] = getText(R.string.prefsMutedTillOff).toString();
        for (int i = 1; i <= hours; i++) {
            displayedValues[i] = String.valueOf(i) + " h";
        }
        return displayedValues;
    }

}
