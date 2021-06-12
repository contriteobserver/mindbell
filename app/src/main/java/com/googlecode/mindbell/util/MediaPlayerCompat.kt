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
package com.googlecode.mindbell.util

import android.media.AudioAttributes
import android.media.AudioAttributes.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build

/**
 * Hide Android API level differences from application code.
 */
class MediaPlayerCompat : MediaPlayer() {

    /**
     * Set audio stream type for API level < 26, otherwise audio attributes with the corresponding usage.
     */
    fun setAudioStream(streamtype: Int) {
        if (Build.VERSION.SDK_INT < 26) {
            @Suppress("DEPRECATION")
            setAudioStreamType(streamtype)
        } else {
            val usage = when (streamtype) {
                AudioManager.STREAM_NOTIFICATION -> USAGE_NOTIFICATION
                AudioManager.STREAM_MUSIC -> USAGE_MEDIA
                else -> USAGE_ALARM
            }
            val audioAttributes = Builder() //
                    .setContentType(CONTENT_TYPE_UNKNOWN) // it's neither MOVIE nor MUSIC nor SONIFICATION nor SPEECH
                    .setUsage(usage) //
                    .build()
            setAudioAttributes(audioAttributes)
        }
    }

}