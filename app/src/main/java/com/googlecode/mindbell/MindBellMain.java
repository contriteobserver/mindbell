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

import com.googlecode.mindbell.accessors.AndroidContextAccessor;
import com.googlecode.mindbell.accessors.ContextAccessor;
import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.logic.RingingLogic;
import com.googlecode.mindbell.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.Toast;

public class MindBellMain extends Activity {

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private static final String POPUP_PREFS_FILE = "popup-prefs";

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private static final String KEY_POPUP = "popup";

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private SharedPreferences popupPrefs;

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private void checkWhetherToShowPopup() {
    // if (!hasShownPopup()) {
    // setPopupShown(true);
    // showPopup();
    // }
    // }

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private boolean hasShownPopup() {
    // return popupPrefs.getBoolean(KEY_POPUP, false);
    // }

    /**
     * Return information to be sent by mail.
     */
    private String getInfoMailText(boolean withLog) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append(Utils.getApplicationInformation(getPackageManager(), getPackageName()));
        sb.append("\n");
        sb.append(Utils.getSystemInformation());
        if (withLog) { // too much log entries may produce FAILED BINDER TRANSACTION => users choice
            sb.append("\n");
            sb.append(Utils.getLimitedLogEntriesAsString());
        }
        return sb.toString();
    }

    /**
     * Show hint how to activate the bell.
     */
    private void notifyIfNotActive() {
        if (!AndroidContextAccessor.getInstance(this).getPrefs().isBellActive()) {
            Toast.makeText(this, R.string.howToSet, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO dkn For the time being there is no need to show a popup on first startup
        // popupPrefs = getSharedPreferences(POPUP_PREFS_FILE, MODE_PRIVATE);
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
        MenuItem activeItem = menu.findItem(R.id.active);
        activeItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickActive();
            }

        });
        MenuItem sendLogItem = menu.findItem(R.id.sendInfoMail);
        sendLogItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickSendInfo(false);
            }

        });
        MenuItem sendLogAndLogItem = menu.findItem(R.id.sendInfoAndLogMail);
        sendLogAndLogItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClickSendInfo(true);
            }

        });
        return true;
    }

    /**
     * Handles click on menu item active.
     */
    private boolean onMenuItemClickActive() {
        PrefsAccessor prefsAccessor = AndroidContextAccessor.getInstance(MindBellMain.this).getPrefs();
        prefsAccessor.setBellActive(!prefsAccessor.isBellActive()); // toggle active/inactive
        Utils.updateBellSchedule(MindBellMain.this);
        invalidateOptionsMenu(); // re-call onPrepareOptionsMenu()
        CharSequence feedback = getText((prefsAccessor.isBellActive()) ? R.string.summaryActive : R.string.summaryNotActive);
        Toast.makeText(this, feedback, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Handles click on menu item send info.
     */
    private boolean onMenuItemClickSendInfo(boolean withLog) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[] { getText(R.string.emailAddress).toString() });
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
        activeItem.setIcon((prefs.isBellActive()) ? R.drawable.ic_action_bell_off : R.drawable.ic_action_bell_on);
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
            ContextAccessor ca = AndroidContextAccessor.getInstance(this);
            RingingLogic.ringBell(ca, null);
        }
        return true;
    }

    // TODO dkn For the time being there is no need to show a popup on first startup
    // @Override
    // public void onWindowFocusChanged(boolean hasFocus) {
    // super.onWindowFocusChanged(hasFocus);
    // if (hasFocus) {
    // checkWhetherToShowPopup();
    // }
    // }

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private void setPopupShown(boolean shown) {
    // popupPrefs.edit().putBoolean(KEY_POPUP, shown).commit();
    // }

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private void showPopup() {
    // DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
    // public void onClick(DialogInterface dialog, int which) {
    // dialog.dismiss();
    // takeUserToOffer();
    // }
    // };
    //
    // View popupView = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null);
    // new AlertDialog.Builder(this).setTitle(R.string.main_title_popup).setIcon(R.drawable.alarm_natural_icon)
    // .setView(popupView).setPositiveButton(R.string.main_yes_popup, yesListener)
    // .setNegativeButton(R.string.main_no_popup, null).show();
    // }

    // TODO dkn For the time being there is no need to show a popup on first startup
    // private void takeUserToOffer() {
    // startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.main_uri_popup))));
    // }
}
