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
package com.googlecode.mindbell.accessors;

import android.Manifest;
import android.app.AlarmManager;
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

import com.googlecode.mindbell.MindBell;
import com.googlecode.mindbell.MindBellMain;
import com.googlecode.mindbell.R;
import com.googlecode.mindbell.Scheduler;
import com.googlecode.mindbell.logic.RingingLogic;
import com.googlecode.mindbell.util.AlarmManagerCompat;
import com.googlecode.mindbell.util.TimeOfDay;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;

import static com.googlecode.mindbell.MindBellPreferences.TAG;

public class AndroidContextAccessor extends ContextAccessor {

    private static final int uniqueNotificationID = R.layout.bell;

    // Keep MediaPlayer to finish a started sound explicitly but avoid producing a memory leak
    private static WeakReference<MediaPlayer> mediaPlayerWeakReference = null;

    // ApplicationContext of MindBell
    private final Context context;

    /**
     * Returns an accessor for the given context, this call also validates the preferences.
     */
    public static AndroidContextAccessor getInstance(Context context) {
        AndroidContextAccessor instance = new AndroidContextAccessor(context);
        ((AndroidPrefsAccessor) instance.getPrefs()).checkSettings(context, false);
        return instance;
    }

    /**
     * Returns an accessor for the given context, this call also validates the preferences.
     */
    public static AndroidContextAccessor getInstanceAndLogPreferences(Context context) {
        AndroidContextAccessor instance = new AndroidContextAccessor(context);
        ((AndroidPrefsAccessor) instance.getPrefs()).checkSettings(context, true);
        return instance;
    }

    /**
     * Constructor is private just in case we want to make this a singleton.
     *
     * @param context
     */
    private AndroidContextAccessor(Context context) {
        this.context = context.getApplicationContext();
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
        Log.d(TAG, "Can settings be satisfied? -> " + result);
        return result;
    }

    @Override
    public void finishBellSound() {
        if (isBellSoundPlaying()) { // do we hold a reference to a MediaPlayer?
            MediaPlayer mediaPlayer = mediaPlayerWeakReference.get();
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                MindBell.logDebug("Ongoing MediaPlayer stopped");
            }
            mediaPlayer.release();
            mediaPlayerWeakReference.clear();
            mediaPlayerWeakReference = null;
            MindBell.logDebug("Weak reference to MediaPlayer released");
        }
        int alarmMaxVolume = getAlarmMaxVolume();
        if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
            MindBell.logDebug(
                    "Finish bell sound found originalVolume " + originalVolume + " to be max, alarm volume left untouched");
        } else {
            MindBell.logDebug("Finish bell sound found originalVolume " + originalVolume + ", setting alarm volume to it");
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
        // if we hold a reference we haven't finished bell sound completely so only the reference is checked
        return mediaPlayerWeakReference != null && mediaPlayerWeakReference.get() != null;
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
    public void startPlayingSoundAndVibrate(ActivityPrefsAccessor activityPrefs, final Runnable runWhenDone) {

        // Start playing sound if requested by preferences
        if (activityPrefs.isSound()) {
            startPlayingSound(activityPrefs, runWhenDone);
        }

        // Vibrate if requested by preferences
        if (activityPrefs.isVibrate()) {
            startVibration();
        }

        // If displaying the bell is requested by the preferences but playing a sound is not, then
        // we are currently in MindBell.onStart(), so the bell has not been displayed yet. So this
        // method must end to bring the bell to front. But after a while someone has to send the
        // bell to the back again. So a new thread is created that waits and calls the runWhenDone
        // which sends the bell to background. As it's a new thread this method ends after starting
        // the thread which leads to the end of MindBell.onStart() which shows the bell.
        if (activityPrefs.isShow() && !activityPrefs.isSound() && runWhenDone != null) {
            startWaiting(runWhenDone);
        }

    }

    /**
     * Start playing bell sound and call runWhenDone when playing finishes.
     *
     * @param activityPrefs
     * @param runWhenDone
     */
    @Override
    public void startPlayingSound(ActivityPrefsAccessor activityPrefs, final Runnable runWhenDone) {
        originalVolume = getAlarmVolume();
        int alarmMaxVolume = getAlarmMaxVolume();
        if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
            MindBell.logDebug(
                    "Start playing sound found originalVolume " + originalVolume + " to be max, alarm volume left untouched");
        } else {
            MindBell.logDebug("Start playing sound found originalVolume " + originalVolume + ", setting alarm volume to max");
            setAlarmVolume(alarmMaxVolume);
        }
        float bellVolume = activityPrefs.getVolume();
        Uri bellUri = activityPrefs.getSoundUri();
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayerWeakReference = new WeakReference<MediaPlayer>(mediaPlayer); // store it for finishBellSound
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
            Log.e(TAG, "Could not start playing sound: " + e.getMessage(), e);
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

    /**
     * Send a newly created intent to Scheduler to update notification and setup a new bell schedule.
     */
    @Override
    public void updateBellSchedule() {
        Log.d(TAG, "Update bell schedule requested");
        Intent intent = createSchedulerIntent(false, null, null);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            sender.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Could not update bell schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Send a newly created intent to Scheduler to update notification and setup a new bell schedule for meditation.
     *
     * @param nextTargetTimeMillis Millis to be given to Scheduler as now (or nextTargetTimeMillis from the perspective of the previous call)
     */
    @Override
    public void updateBellSchedule(long nextTargetTimeMillis) {
        Log.d(TAG, "Update bell schedule requested, nextTargetTimeMillis=" + nextTargetTimeMillis);
        Intent intent = createSchedulerIntent(false, nextTargetTimeMillis, 0);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            sender.send();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Could not update bell schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Reschedule the bell by letting AlarmManager send an intent to Scheduler.
     *
     * @param nextTargetTimeMillis Millis to be given to Scheduler as now (or nextTargetTimeMillis from the perspective of the previous call)
     * @param nextMeditationPeriod null if not meditating, otherwise 0: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    @Override
    public void reschedule(long nextTargetTimeMillis, Integer nextMeditationPeriod) {
        Intent nextIntent = createSchedulerIntent(true, nextTargetTimeMillis, nextMeditationPeriod);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManagerCompat alarmManager = new AlarmManagerCompat(context);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTargetTimeMillis, sender);
        TimeOfDay nextBellTime = new TimeOfDay(nextTargetTimeMillis);
        Log.d(TAG, "Scheduled next bell alarm for " + nextBellTime.getDisplayString());
    }

    /**
     * Create an intent to be send to Scheduler to update notification and to (re-)schedule the bell.
     *
     * @param isRescheduling True if the intents is meant for rescheduling instead of updating bell schedule.
     * @param nowTimeMillis If not null millis to be given to Scheduler as now (or nextTargetTimeMillis from the perspective of the previous call)
     * @param meditationPeriod Zero: ramp-up, 1-(n-1): intermediate period, n: last period, n+1: beyond end
     */
    private Intent createSchedulerIntent(boolean isRescheduling, Long nowTimeMillis, Integer meditationPeriod) {
        Log.d(TAG, "Creating scheduler intent: isRescheduling=" + isRescheduling + ", nowTimeMillis=" + nowTimeMillis + ", meditationPeriod=" + meditationPeriod);
        Intent intent = new Intent(context, Scheduler.class);
        if (isRescheduling) {
            final String extraIsRescheduling = context.getText(R.string.extraIsRescheduling).toString();
            intent.putExtra(extraIsRescheduling, true);
        }
        if (nowTimeMillis != null) {
            final String extraNowTimeMillis = context.getText(R.string.extraNowTimeMillis).toString();
            intent.putExtra(extraNowTimeMillis, nowTimeMillis);
        }
        if (meditationPeriod != null) {
            final String extraMeditationPeriod = context.getText(R.string.extraMeditationPeriod).toString();
            intent.putExtra(extraMeditationPeriod, meditationPeriod);
        }
        return intent;
    }

    /**
     * Shows bell by bringing activity MindBell to the front
     */
    @Override
    public void showBell() {
        Intent ringBell = new Intent(context, MindBell.class);
        PendingIntent bellIntent = PendingIntent.getActivity(context, -1, ringBell, PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            bellIntent.send(); // show MindBell activity and call RingingLogic.ringBellAndWait()
        } catch (PendingIntent.CanceledException e) {
            Log.d(TAG, "Cannot show bell, play sound and vibrate: " + e.getMessage(), e);
        }
    }

    /**
     * This is about updating status notifcation on changes in system settings or when ringing the bell.
     */
    @Override
    public void updateStatusNotification() {
        if ((!prefs.isActive() && !prefs.isMeditating()) || !prefs.isStatus()) {// bell inactive or no notification wanted?
            Log.i(TAG, "Remove status notification because of inactive and non-meditating bell or unwanted notification");
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
        String contentText;
        String muteRequestReason = getMuteRequestReason(false);
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
        } else if (prefs.isMeditating()) {// Bell meditation => override icon and notification text
            statusDrawable = R.drawable.ic_stat_bell_meditating;
            contentTitle = context.getText(R.string.statusTitleBellMeditating);
            contentText = MessageFormat.format(context.getText(R.string.statusTextBellMeditating).toString(), //
                    Integer.valueOf(prefs.getMeditationDuration()), //
                    new TimeOfDay(prefs.getMeditationEndingTimeMillis()).getShortDisplayString());
        } else if (muteRequestReason != null) { // Bell muted => override icon and notification text
            statusDrawable = bellActiveButMutedDrawable;
            contentText = muteRequestReason;
        } else { // enrich standard notification by times and days
            contentText = MessageFormat.format(context.getText(R.string.statusTextBellActive).toString(), //
                    prefs.getDaytimeStartString(), //
                    prefs.getDaytimeEndString(), //
                    prefs.getActiveOnDaysOfWeekString());
        }
        // Now do the notification update
        Log.i(TAG, "Update status notification: " + contentText);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent openAppIntent = PendingIntent.getActivity(context, 0, new Intent(context, MindBellMain.class), PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent refreshIntent = PendingIntent.getBroadcast(context, 1, new Intent("com.googlecode.mindbell.UPDATE_STATUS_NOTIFICATION"), PendingIntent.FLAG_UPDATE_CURRENT);
        int visibility = (prefs.isStatusNotificationVisibilityPublic()) ? NotificationCompat.VISIBILITY_PUBLIC
                : NotificationCompat.VISIBILITY_PRIVATE;
        Notification notification = new NotificationCompat.Builder(context.getApplicationContext()) //
                .setCategory(NotificationCompat.CATEGORY_ALARM) //
                .setColor(context.getResources().getColor(R.color.notificationBackground)) //
                .setContentTitle(contentTitle) //
                .setContentText(contentText) //
                .setContentIntent(openAppIntent) //
                .setOngoing(true) //
                .setSmallIcon(statusDrawable) //
                .setVisibility(visibility) //
                .addAction(R.drawable.ic_action_refresh_status, context.getText(R.string.statusActionRefreshStatus), refreshIntent) //
                .build();
        notificationManager.notify(uniqueNotificationID, notification);
    }

}
