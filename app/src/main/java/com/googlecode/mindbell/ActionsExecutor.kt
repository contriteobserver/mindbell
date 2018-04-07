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
package com.googlecode.mindbell

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.os.Build
import android.os.Vibrator
import com.googlecode.mindbell.Prefs.Companion.EXTRA_KEEP
import com.googlecode.mindbell.Prefs.Companion.WAITING_TIME
import java.io.IOException

/**
 * This singleton class executes all actions which in fact is everything around interrupt actions.
 */
class ActionsExecutor private constructor(val context: Context) : AudioManager.OnAudioFocusChangeListener {

    private var prefs = Prefs.getInstance(context)

    val isBellSoundPlaying: Boolean
        get() = mediaPlayer != null // if we hold a reference we haven't finished bell sound completely

    val alarmMaxVolume: Int
        get() {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioMan.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        }

    var alarmVolume: Int
        get() {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            return audioMan.getStreamVolume(AudioManager.STREAM_ALARM)
        }
        set(volume) {
            val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioMan.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)

        }

    /**
     * Start interrupt actions (show/sound/vibrate) and setup handler to finish the actions after sound or timer ends.
     */
    fun startInterruptActions(interruptSettings: InterruptSettings, meditationStopper: Runnable?, callingService: Service? = null) {

        // Stop an already ongoing sound, this isn't wrong when phone and bell are muted, too
        finishBellSound()

        // Runnable that finishes all reminder actions and stops the meditation if requested. It is either executed after the
        // sound has been played or after a timer has expired.
        val reminderActionsFinisher = Runnable { finishInterruptActions(interruptSettings, meditationStopper, callingService) }

        // Show bell if wanted
        if (interruptSettings.isShow) {
            showBell()
        }

        // Raise alarm volume to the max but keep the original volume for reset by finishBellSound() and start playing sound if
        // requested by preferences
        var playingSoundStarted = false
        if (interruptSettings.isSound) {
            playingSoundStarted = startPlayingSound(interruptSettings, reminderActionsFinisher)
        }

        // Explicitly start vibration if not already done by ring notification
        if (interruptSettings.isVibrate && !interruptSettings.isNotification) {
            startVibration()
        }

        // If no sound has been started (as requested or due to a failure starting it) a timer is used to finish the reminder
        // actions when it has expired.
        if (!playingSoundStarted) {
            startWaiting(reminderActionsFinisher)
        }

    }

    fun finishBellSound() {
        if (isBellSoundPlaying) { // do we hold a reference to a MediaPlayer?
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.stop()
                ReminderShowActivity.logDebug("Ongoing MediaPlayer stopped")
            }
            mediaPlayer!!.reset() // get rid of "mediaplayer went away with unhandled events" log entries
            mediaPlayer!!.release()
            mediaPlayer = null
            ReminderShowActivity.logDebug("Reference to MediaPlayer released")
            if (prefs.isPauseAudioOnSound) {
                if (audioManager!!.abandonAudioFocus(this) == AUDIOFOCUS_REQUEST_FAILED) {
                    ReminderShowActivity.logDebug("Abandon of audio focus failed")
                } else {
                    ReminderShowActivity.logDebug("Audio focus successfully abandoned")
                }
            }
        }
        // Reset volume to originalVolume if it has been set before (does not equal -1)
        if (prefs.isUseAudioStreamVolumeSetting) { // we don't care about setting the volume
            ReminderShowActivity.logDebug("Finish bell sound found without touching audio stream volume")
        } else {
            val originalVolume = prefs.originalVolume
            if (originalVolume < 0) {
                ReminderShowActivity.logDebug("Finish bell sound found originalVolume $originalVolume, alarm volume left untouched")
            } else {
                val alarmMaxVolume = alarmMaxVolume
                if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
                    ReminderShowActivity.logDebug(
                            "Finish bell sound found originalVolume $originalVolume to be max, alarm volume left untouched")
                } else {
                    ReminderShowActivity.logDebug("Finish bell sound found originalVolume $originalVolume, setting alarm volume to it")
                    alarmVolume = originalVolume
                }
                prefs.resetOriginalVolume() // no longer needed therefore invalidate it
            }
        }
    }

    /**
     * Start playing bell sound and call runWhenDone when playing finishes but only if bell is not muted - returns true when
     * sound has been started, false otherwise.
     */
    private fun startPlayingSound(interruptSettings: InterruptSettings, reminderActionsFinisher: Runnable): Boolean {
        val bellUri = interruptSettings.getSoundUri()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (prefs.isNoSoundOnMusic && audioManager!!.isMusicActive) {
            ReminderShowActivity.logDebug("Sound suppressed because setting is no sound on music and music is playing")
            return false
        } else if (bellUri == null) {
            ReminderShowActivity.logDebug("Sound suppressed because no sound has been set")
            return false
        } else if (prefs.isPauseAudioOnSound) {
            val requestResult = audioManager!!.requestAudioFocus(this, prefs.audioStream, retrieveDurationHint())
            if (requestResult == AUDIOFOCUS_REQUEST_FAILED) {
                ReminderShowActivity.logDebug("Sound suppressed because setting is pause audio on sound and request of audio focus failed")
                return false
            }
            ReminderShowActivity.logDebug("Audio focus successfully requested")
        }
        if (prefs.isUseAudioStreamVolumeSetting) { // we don't care about setting the volume
            ReminderShowActivity.logDebug("Start playing sound without touching audio stream volume")
        } else {
            val originalVolume = alarmVolume
            val alarmMaxVolume = alarmMaxVolume
            if (originalVolume == alarmMaxVolume) { // "someone" else set it to max, so we don't touch it
                ReminderShowActivity.logDebug(
                        "Start playing sound found originalVolume $originalVolume to be max, alarm volume left untouched")
            } else {
                ReminderShowActivity.logDebug(
                        "Start playing sound found and stored originalVolume $originalVolume, setting alarm volume to max")
                alarmVolume = alarmMaxVolume
                prefs.originalVolume = originalVolume
            }
        }
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setAudioStreamType(prefs.audioStream)
        if (!prefs.isUseAudioStreamVolumeSetting) { // care about setting the volume
            val bellVolume = interruptSettings.volume
            mediaPlayer!!.setVolume(bellVolume, bellVolume)
        }
        try {
            try {
                mediaPlayer!!.setDataSource(context, bellUri)
            } catch (e: IOException) { // probably because of withdrawn permissions, hence use default bell
                mediaPlayer!!.setDataSource(context, prefs.getDefaultReminderBellSoundUri()!!)
            }

            mediaPlayer!!.prepare()
            mediaPlayer!!.setOnCompletionListener {
                reminderActionsFinisher.run()
            }
            mediaPlayer!!.start()
            return true
        } catch (e: IOException) {
            ReminderShowActivity.logError("Could not start playing sound: " + e.message, e)
            reminderActionsFinisher.run()
            return false
        }

    }

    /**
     * Finishes interrupt actions (show/sound/vibrate) after sound or timer has ended.
     */
    private fun finishInterruptActions(interruptSettings: InterruptSettings, runWhenDone: Runnable?, callingService: Service?) {
        // nothing to do to finish vibration
        finishBellSound()
        // interrupt notfication is cancelled automatically because foreground service is attached to it
        if (interruptSettings.isShow) {
            hideBell()
        }
        runWhenDone?.run()
        callingService?.stopSelf()
    }

    /**
     * Vibrate with the requested vibration pattern.
     */
    private fun startVibration() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(prefs.vibrationPattern, -1)
    }

    /**
     * Start waiting for a specific time period and call runWhenDone when time is over.
     */
    private fun startWaiting(reminderActionsFinisher: Runnable) {
        Thread(Runnable {
            try {
                Thread.sleep(WAITING_TIME)
            } catch (e: InterruptedException) {
                // doesn't care if sleep was interrupted, just move on
            }

            reminderActionsFinisher.run()
        }).start()
    }

    /**
     * Returns duration hint for requesting audio focus depending on the version of Android.
     */
    private fun retrieveDurationHint(): Int {
        return if (Build.VERSION.SDK_INT >= 19) {
            AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
        } else {
            AUDIOFOCUS_GAIN_TRANSIENT
        }
    }

    /**
     * Shows bell by starting activity MindBell
     */
    private fun showBell() {
        ReminderShowActivity.logDebug("Starting ReminderShowActivity to show bell")
        showOrHideBell(true)
    }

    /**
     * Hides bell by starting activity MindBell
     */
    private fun hideBell() {
        ReminderShowActivity.logDebug("Starting ReminderShowActivity to hide bell")
        showOrHideBell(false)
    }

    /**
     * Show or hide bell by starting activity MindBell following this idea: https://stackoverflow.com/a/14411650
     */
    private fun showOrHideBell(show: Boolean) {
        val intent = Intent(context, ReminderShowActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // context may be service context only, not an activity context
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) // MindBell becomes the new root to let back button return to other apps
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) // don't launch activity a second time when hiding "on top" of showing
        intent.putExtra(EXTRA_KEEP, show)
        context.startActivity(intent)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        ReminderShowActivity.logDebug("Callback onAudioFocusChange() received focusChange=" + focusChange)
        when (focusChange) {
            AUDIOFOCUS_LOSS, AUDIOFOCUS_LOSS_TRANSIENT // could be handled by only pausing playback (not useful for bell sound)
                , AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK // could also be handled by lowering volume (not useful for bell sound)
            -> finishBellSound()
            else -> {
            }
        }
    }

    /**
     * Send an intent to MainActivity to finally stop meditation (change status, stop countdown) automatically instead of
     * pressing the stop meditation button manually.
     */
    fun stopMeditation() {
        ReminderShowActivity.logDebug("Starting activity MainActivity to stop meditation")
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = (Intent.FLAG_ACTIVITY_NEW_TASK // context may be service context only, not an activity context
                or Intent.FLAG_ACTIVITY_CLEAR_TASK) // MainActivity becomes the new root to let back button return to other apps
        intent.putExtra(Prefs.EXTRA_STOP_MEDITATION, true)
        context.startActivity(intent)
    }

    companion object {

        // Keep MediaPlayer to finish a started sound explicitly, reclaimed when app gets destroyed: http://stackoverflow.com/a/2476171
        private var mediaPlayer: MediaPlayer? = null
        private var audioManager: AudioManager? = null

        /*
         * The one and only instance of this class.
         */
        var instance: ActionsExecutor? = null

        /*
         * Returns the one and only instance of this class.
         */
        @Synchronized
        fun getInstance(context: Context): ActionsExecutor {
            if (instance == null) {
                instance = ActionsExecutor(context)
            }
            return instance!!
        }

    }
}
