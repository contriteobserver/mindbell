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
package com.googlecode.mindbell.accessors;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

import java.io.IOException;

import com.googlecode.mindbell.MindBell;
import com.googlecode.mindbell.MindBellMain;
import com.googlecode.mindbell.R;
import com.googlecode.mindbell.Scheduler;
import com.googlecode.mindbell.logic.RingingLogic;
import com.googlecode.mindbell.util.Utils;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class AndroidContextAccessor extends ContextAccessor {

    public static final int KEYMUTEINFLIGHTMODE = R.string.keyMuteInFlightMode;

    public static final int KEYMUTEOFFHOOK = R.string.keyMuteOffHook;

    public static final int KEYMUTEWITHPHONE = R.string.keyMuteWithPhone;

    private static final int uniqueNotificationID = R.layout.bell;

    /**
     * Returns an accessor for the given context, just in case we want to make this a Singleton.
     */
    public static AndroidContextAccessor getInstance(Context context) {
        return new AndroidContextAccessor(context);
    }

    private final Context context;

    private MediaPlayer mediaPlayer = null;

    private AndroidContextAccessor(Context context) {
        this.context = context;
        this.prefs = new AndroidPrefsAccessor(context);
    }

    /**
     * Returns true if mute bell with phone isn't requested or if the app has the permission to be informed in case of incoming or
     * outgoing calls. Notification bell could not be turned over correctly if muting with phone were requested without permission
     * granted.
     */
    private boolean canSettingsBeSatisfied(PrefsAccessor prefs) {
        boolean result = !prefs.isMuteOffHook() || ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "canSettingsBeSatisfied() -> " + result);
        return result;
    }

    @Override
    public void finishBellSound() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            MindBell.logDebug("Stopped ongoing player.");
        }
        mediaPlayer.release();
        mediaPlayer = null;
        int alarmMaxVolume = getAlarmMaxVolume();
        if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
            MindBell.logDebug(
                    "finishBellSound() found originalVolume " + originalVolume + " to be max, alarm volume left untouched");
        } else {
            MindBell.logDebug("finishBellSound() found originalVolume " + originalVolume + ", setting alarm volume to it");
            setAlarmVolume(originalVolume);
        }
    }

    @Override
    public int getAlarmMaxVolume() {
        AudioManager audioMan = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioMan.getStreamMaxVolume(AudioManager.STREAM_ALARM);
    }

    @Override
    public int getAlarmVolume() {
        AudioManager audioMan = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int alarmVolume = audioMan.getStreamVolume(AudioManager.STREAM_ALARM);
        return alarmVolume;
    }

    @Override
    public float getBellVolume() {
        float bellVolume = prefs.getVolume(getBellDefaultVolume());
        return bellVolume;
    }

    @Override
    protected String getReasonMutedInFlightMode() {
        return context.getText(R.string.reasonMutedInFlightMode).toString();
    }

    @Override
    protected String getReasonMutedOffHook() {
        return context.getText(R.string.reasonMutedOffHook).toString();
    }

    @Override
    protected String getReasonMutedWithPhone() {
        return context.getText(R.string.reasonMutedWithPhone).toString();
    }

    @Override
    public boolean isBellSoundPlaying() {
        return mediaPlayer != null;
    }

    @Override
    public boolean isPhoneInFlightMode() {
        return Settings.System.getInt(context.getContentResolver(), Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    @Override
    public boolean isPhoneMuted() {
        final AudioManager audioMan = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return audioMan.getStreamVolume(AudioManager.STREAM_RING) == 0;
    }

    @Override
    public boolean isPhoneOffHook() {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getCallState() != TelephonyManager.CALL_STATE_IDLE;
    }

    private void removeStatusNotification() {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(uniqueNotificationID);
    }

    @Override
    public void setAlarmVolume(int volume) {
        AudioManager audioMan = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioMan.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);

    }

    @Override
    public void showMessage(String message) {
        MindBell.logDebug(message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startPlayingSoundAndVibrate(final Runnable runWhenDone) {
        // Start playing sound if requested by preferences
        if (prefs.isSound()) {
            startPlayingSound(runWhenDone);
        }
        // Vibrate if requested by preferences
        if (prefs.isVibrate()) {
            startVibration();
        }
        // If displaying the bell is requested by the preferences but playing a sound is not, then
        // we are currently in MindBell.onStart(), so the bell has not been displayed yet. So this
        // method must end to bring the bell to front. But after a while someone has to send the
        // bell to the back again. So a new thread is created that waits and calls the runWhenDone
        // which sends the bell to background. As it's a new thread this method ends after starting
        // the thread which leads to the end of MindBell.onStart() which shows the bell.
        if (prefs.isShow() && !prefs.isSound()) {
            startWaiting(runWhenDone);
        }
    }

    /**
     * Start playing bell sound and call runWhenDone when playing finishes.
     *
     * @param runWhenDone
     */
    private void startPlayingSound(final Runnable runWhenDone) {
        originalVolume = getAlarmVolume();
        int alarmMaxVolume = getAlarmMaxVolume();
        if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
            MindBell.logDebug(
                    "startPlayingSound() found originalVolume " + originalVolume + " to be max, alarm volume left untouched");
        } else {
            MindBell.logDebug("startPlayingSound() found originalVolume " + originalVolume + ", setting alarm volume to max");
            setAlarmVolume(alarmMaxVolume);
        }
        float bellVolume = getBellVolume();
        Uri bellUri = Utils.getResourceUri(context, R.raw.bell10s);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mediaPlayer.setVolume(bellVolume, bellVolume);
        try {
            mediaPlayer.setDataSource(context, bellUri);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    finishBellSound();
                    if (runWhenDone != null) {
                        runWhenDone.run();
                    }
                }
            });
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "could not set up bell sound: " + e.getMessage(), e);
            if (runWhenDone != null) {
                runWhenDone.run();
            }
        }
    }

    /**
     * Vibrate with the requested vibration pattern.
     */
    private void startVibration() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(prefs.getVibrationPattern(), -1);
    }

    /**
     * Start waiting for a specific time period and call runWhenDone when time is over.
     *
     * @param runWhenDone
     */
    private void startWaiting(final Runnable runWhenDone) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(RingingLogic.WAITING_TIME);
                } catch (InterruptedException e) {
                    // doesn't care if sleep was interrupted, just move on
                }
                runWhenDone.run();
            }
        }).start();
    }

    public void updateBellSchedule() {
        updateStatusNotification(prefs, true);
        if (prefs.isActive()) {
            Log.d(TAG, "Update bell schedule for active bell");
            Intent intent = new Intent(context, Scheduler.class);
            PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
                sender.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "Could not send: " + e.getMessage(), e);
            }
        }
    }

    /**
     * This is about updating status notifcation on changes in system settings, removal of notification is done by
     * udpateBellSchedule().
     */
    public void updateStatusNotification() {
        updateStatusNotification(prefs, false);
    }

    private void updateStatusNotification(PrefsAccessor prefs, boolean shouldShowMessage) {
        if (!prefs.isActive() || !prefs.isStatus()) {// bell inactive or no notification wanted?
            Log.i(TAG, "remove status notification because of inactive bell or unwanted notification");
            removeStatusNotification();
            return;
        }
        // Choose material design or pre material design status icons
        int bellActiveDrawable;
        int bellActiveButMutedDrawable;
        if (prefs.useStatusIconMaterialDesign()) {
            bellActiveDrawable = R.drawable.ic_stat_bell_active;
            bellActiveButMutedDrawable = R.drawable.ic_stat_bell_active_but_muted;
        } else {
            bellActiveDrawable = R.drawable.golden_bell_status_active;
            bellActiveButMutedDrawable = R.drawable.golden_bell_status_active_but_muted;
        }
        // Suppose bell is active and not muted and all settings can be satisfied
        int statusDrawable = bellActiveDrawable;
        CharSequence contentTitle = context.getText(R.string.statusTitleBellActive);
        String contentText = context.getText(R.string.statusTextBellActive).toString();
        String muteRequestReason = getMuteRequestReason(shouldShowMessage);
        // Override icon and notification text if bell is muted or permissions are insufficient
        if (!canSettingsBeSatisfied(prefs)) { // Insufficient permissions => override icon/text, switch notifications off
            statusDrawable = R.drawable.ic_stat_warning_white_24px;
            contentTitle = context.getText(R.string.statusTitleNotificationsDisabled);
            contentText = context.getText(R.string.statusTextNotificationsDisabled).toString();
            // Status Notification would not be correct during incoming or outgoing calls because of the missing permission to
            // listen to phone state changes. Therefore we switch off notification and ask user for permission when he tries
            // to enable notification again. In this very moment we cannot ask for permission to avoid an ANR in receiver
            // UpdateStatusNotification.
            prefs.setStatus(false);
        } else if (muteRequestReason != null) { // Bell muted => override icon and notification text
            statusDrawable = bellActiveButMutedDrawable;
            contentText = muteRequestReason;
        } else { // enrich standard notification by times and days
            contentText = contentText.replace("_STARTTIME_", prefs.getDaytimeStartString())
                    .replace("_ENDTIME_", prefs.getDaytimeEndString()).replace("_WEEKDAYS_", prefs.getActiveOnDaysOfWeekString());
        }
        // Now do the notification update
        Log.i(TAG, "Update status notification: " + contentText);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, MindBellMain.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        int visibility = (prefs.isStatusNotificationVisibilityPublic()) ? NotificationCompat.VISIBILITY_PUBLIC
                : NotificationCompat.VISIBILITY_PRIVATE;
        Notification notif = new NotificationCompat.Builder(context.getApplicationContext()) //
                .setCategory(NotificationCompat.CATEGORY_ALARM) //
                .setColor(context.getResources().getColor(R.color.notificationBackground)) //
                .setContentTitle(contentTitle) //
                .setContentText(contentText) //
                .setContentIntent(contentIntent) //
                .setOngoing(true) //
                .setSmallIcon(statusDrawable) //
                .setVisibility(visibility) //
                .setWhen(System.currentTimeMillis()) //
                .build();
        notificationManager.notify(uniqueNotificationID, notif);
    }

}
