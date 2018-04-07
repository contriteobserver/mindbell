/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2007 The Android Open Source Project
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

package com.googlecode.mindbell.preference

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.googlecode.mindbell.ReminderShowActivity
import com.googlecode.mindbell.util.Utils
import com.googlecode.mindbell.util.VolumeConverter
import kotlinx.android.synthetic.main.seekbar_dialog.view.*
import java.io.IOException

/**
 * @hide
 */
class MediaVolumePreference(context: Context, attrs: AttributeSet) : SeekBarPreference(context, attrs), View.OnKeyListener {
    private var mStreamType: Int = 0
    private var mSoundUri: Uri? = null
    /**
     * May be null if the dialog isn't visible.
     */
    private var mSeekBarVolumizer: SeekBarVolumizer? = null

    init {
        // MindBell.logDebug("Attributes: " + attrs.getAttributeCount());
        // for (int i = 0; i < attrs.getAttributeCount(); i++) {
        // MindBell.logDebug("Attr " + i + ": " + attrs.getAttributeName(i) + "=" + attrs.getAttributeValue(i));
        // }
        mStreamType = attrs.getAttributeIntValue(mindfulns, "streamType", AudioManager.STREAM_NOTIFICATION)
        val mRingtoneResId = attrs.getAttributeResourceValue(mindfulns, "ringtone", -1)
        if (mRingtoneResId != -1) {
            mSoundUri = Utils.getResourceUri(context, mRingtoneResId)
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        mSeekBarVolumizer = SeekBarVolumizer(context, view.seekBarVolume, mStreamType)

        // getPreferenceManager().registerOnActivityStopListener(this);

        // grab focus and key events so that pressing the volume buttons in the
        // dialog doesn't also show the normal volume adjust toast.
        view.setOnKeyListener(this)
        view.isFocusableInTouchMode = true
        view.requestFocus()
    }

    // public void onActivityStop() {
    // cleanup();
    // }

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)

        if (!positiveResult && mSeekBarVolumizer != null) {
            mSeekBarVolumizer!!.revertVolume()
        }

        if (positiveResult && mSeekBarVolumizer != null) {
            ReminderShowActivity.logDebug("Persisting volume as " + mSeekBarVolumizer!!.volume)
            persistFloat(mSeekBarVolumizer!!.volume)
            ReminderShowActivity.logDebug("And reverting volume to " + mSeekBarVolumizer!!.mOriginalStreamVolume)
            mSeekBarVolumizer!!.revertVolume()

        }

        cleanup()
    }

    /**
     * Do clean up. This can be called multiple times!
     */
    private fun cleanup() {
        // getPreferenceManager().unregisterOnActivityStopListener(this);

        if (mSeekBarVolumizer != null) {
            val dialog = dialog
            if (dialog != null && dialog.isShowing) {
                val view = dialog.window!!.decorView.seekBarVolume
                view?.setOnKeyListener(null)
                // Stopped while dialog was showing, revert changes
                mSeekBarVolumizer!!.revertVolume()
            }
            mSeekBarVolumizer!!.stop()
            mSeekBarVolumizer = null
        }

    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        // If key arrives immediately after the activity has been cleaned up.
        if (mSeekBarVolumizer == null) {
            return true
        }
        val isdown = event.action == KeyEvent.ACTION_DOWN
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (isdown) {
                    mSeekBarVolumizer!!.changeVolumeBy(-1)
                }
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (isdown) {
                    mSeekBarVolumizer!!.changeVolumeBy(1)
                }
                return true
            }
            else -> return false
        }
    }

    protected fun onSampleStarting(volumizer: SeekBarVolumizer) {
        if (mSeekBarVolumizer != null && volumizer !== mSeekBarVolumizer) {
            mSeekBarVolumizer!!.stopSample()
        }
    }

    fun setStreamType(streamType: Int) {
        mStreamType = streamType
    }

    fun setSoundUri(soundUri: Uri) {
        this.mSoundUri = soundUri
    }

    /**
     * Turns a [SeekBar] into a volume control.
     */
    inner class SeekBarVolumizer(private val mContext: Context, val seekBar: SeekBar, private val mStreamType: Int) : OnSeekBarChangeListener {

        private val mAudioManager: AudioManager
        private val converter: VolumeConverter
        var mOriginalStreamVolume: Int = 0
        private var mPlayer: MediaPlayer? = null
        var volume: Float = 0.toFloat()

        init {
            mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            converter = VolumeConverter(DYNAMIC_RANGE_DB, MAX_PROGRESS)
            initSeekBar(seekBar)
        }

        private fun initSeekBar(seekBar: SeekBar) {
            mOriginalStreamVolume = mAudioManager.getStreamVolume(mStreamType)
            mAudioManager.setStreamVolume(mStreamType, mAudioManager.getStreamMaxVolume(mStreamType), 0)
            seekBar.max = MAX_PROGRESS
            volume = getPersistedFloat(0.5f)
            seekBar.progress = converter.volume2progress(volume)
            seekBar.setOnSeekBarChangeListener(this)

            if (mSoundUri != null) {
                mPlayer = MediaPlayer()
                mPlayer!!.setAudioStreamType(mStreamType)
                try {
                    mPlayer!!.setDataSource(mContext, mSoundUri!!)
                    mPlayer!!.prepare()
                } catch (e: IOException) {
                    ReminderShowActivity.logError("Cannot load ringtone", e)
                    mPlayer = null
                }

            }
            if (mPlayer != null) {
                sample()
            }
        }

        private fun sample() {
            onSampleStarting(this)
            if (mPlayer != null) {
                mPlayer!!.setVolume(volume, volume)
                mPlayer!!.seekTo(0)
                mPlayer!!.start()
            }
        }

        fun changeVolumeBy(amount: Int) {
            seekBar.incrementProgressBy(amount)
            stopSample()
            sample()
            // if (mRingtone != null && !mRingtone.isPlaying()) {
            // sample();
            // }
            // if (mRingtone != null && mRingtone.isPlaying()) {
            // stopSample();
            // }
            // sample();
        }

        fun stopSample() {
            if (mPlayer != null) {
                mPlayer!!.pause()
            }
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromTouch: Boolean) {
            volume = converter.progress2volume(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            stopSample()
            sample()
        }

        fun revertVolume() {
            mAudioManager.setStreamVolume(mStreamType, mOriginalStreamVolume, 0)
        }

        fun stop() {
            stopSample()
            if (mPlayer != null) {
                mPlayer!!.release()
                mPlayer = null
            }
            seekBar.setOnSeekBarChangeListener(null)
        }

    }

    companion object {

        val DYNAMIC_RANGE_DB = 50
        val MAX_PROGRESS = 50
        private val TAG = "MediaVolumePreference"
        private val mindfulns = "http://dknapps.de/ns"
    }

}