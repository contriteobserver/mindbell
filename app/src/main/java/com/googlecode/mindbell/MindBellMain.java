/*******************************************************************************
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
 *******************************************************************************/
package com.googlecode.mindbell;

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.logic.RingingLogic;
import com.googlecode.mindbell.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

public class MindBellMain extends Activity {

    private static final String POPUP_PREFS_FILE = "popup-prefs";

    private static final String KEY_POPUP = "popup";

    private SharedPreferences popupPrefs;

    private void checkWhetherToShowPopup() {
        if (!hasShownPopup()) {
            setPopupShown(true);
            onMenuItemClickHelp();
        }
    }

    private boolean hasShownPopup() {
        try {
            int versionCode = Utils.getApplicationVersionCode(getPackageManager(), getPackageName());
            int versionCodePopupShownFor = popupPrefs.getInt(KEY_POPUP, 0);
            return versionCode == versionCodePopupShownFor;
        } catch (ClassCastException e) {
            setPopupShown(false);
            Log.w(TAG, "Removed setting '" + KEY_POPUP + "' since it had wrong type");
            return false;
        }
    }

    /**
     * Return information to be sent by mail.
     */
    private String getInfoMailText(boolean withLog) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(getText(R.string.main_message6_popup));
        sb.append("\n\n");
        sb.append(getText(R.string.main_message7_popup));
        sb.append("\n\n");
        sb.append(Utils.getApplicationInformation(getPackageManager(), getPackageName()));
        sb.append("\n");
        sb.append(Utils.getSystemInformation());
        if (withLog) {
            sb.append("\n");
            sb.append(Utils.getLimitedLogEntriesAsString());
        }
        return sb.toString();
    }

    /**
     * Show hint how to activate the bell.
     */
    private void notifyIfNotActive() {
        if (!AndroidContextAccessor.getInstance(this).getPrefs().isActive()) {
            Toast.makeText(this, R.string.howToSet, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        popupPrefs = getSharedPreferences(POPUP_PREFS_FILE, MODE_PRIVATE);
        // Use the following line to show popup dialog on every start
        // setPopupShown(false);
        setContentView(R.layout.main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        MenuItem settingsItem = menu.findItem(R.id.settings);
        settingsItem.setIntent(new Intent(this, MindBellPreferences.class));
        MenuItem aboutItem = menu.findItem(R.id.about);
        aboutItem.setIntent(new Intent(this, AboutActivity.class));
        MenuItem helpItem = menu.findItem(R.id.help);
        helpItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickHelp();
            }

        });
        MenuItem activeItem = menu.findItem(R.id.active);
        activeItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickActive();
            }

        });
        return true;
    }

    /**
     * Handles click on menu item active.
     */
    private boolean onMenuItemClickActive() {
        ContextAccessor contextAccessor = AndroidContextAccessor.getInstance(MindBellMain.this);
        PrefsAccessor prefsAccessor = contextAccessor.getPrefs();
        prefsAccessor.isActive(!prefsAccessor.isActive()); // toggle active/inactive
        contextAccessor.updateBellSchedule();
        invalidateOptionsMenu(); // re-call onPrepareOptionsMenu()
        CharSequence feedback = getText((prefsAccessor.isActive()) ? R.string.summaryActive : R.string.summaryNotActive);
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Handles click on menu item send info.
     */
    private boolean onMenuItemClickSendInfo(boolean withLog) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", getText(R.string.emailAddress).toString(), null));
        i.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.emailSubject));
        i.putExtra(Intent.EXTRA_TEXT, getInfoMailText(withLog));
        try {
            startActivity(Intent.createChooser(i, getText(R.string.emailChooseApp)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PrefsAccessor prefs = AndroidContextAccessor.getInstance(MindBellMain.this).getPrefs();
        MenuItem activeItem = menu.findItem(R.id.active);
        activeItem.setIcon((prefs.isActive()) ? R.drawable.ic_action_bell_off : R.drawable.ic_action_bell_on);
        return true;
    }

    @Override
    protected void onResume() {
        invalidateOptionsMenu(); // Maybe active setting has been changed via MindBellPreferences
        super.onResume();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            notifyIfNotActive();
            ContextAccessor contextAccessor = AndroidContextAccessor.getInstance(this);
            contextAccessor.updateStatusNotification();
            RingingLogic.ringBell(contextAccessor, null);
        }
        return true;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            checkWhetherToShowPopup();
        }
    }

    private void setPopupShown(boolean shown) {
        if (shown) {
            int versionCode = Utils.getApplicationVersionCode(getPackageManager(), getPackageName());
            popupPrefs.edit().putInt(KEY_POPUP, versionCode).commit();
        } else {
            popupPrefs.edit().remove(KEY_POPUP).apply();
        }
    }

    private boolean onMenuItemClickHelp() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null);
        String versionName = Utils.getApplicationVersionName(getPackageManager(), getPackageName());
        new AlertDialog.Builder(this) //
                .setTitle(getText(R.string.app_name) + " " + versionName) //
                .setIcon(R.drawable.icon) //
                .setView(popupView) //
                .setPositiveButton(R.string.main_yes_popup, null) //
                .setNegativeButton(R.string.main_no_popup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onMenuItemClickSendInfo(true);
                    }
                }) //
                .show();
        return true;
    }

}
