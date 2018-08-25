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
package com.googlecode.mindbell.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.telephony.TelephonyManager
import android.util.Log
import com.googlecode.mindbell.R
import com.googlecode.mindbell.activity.MuteActivity
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.mission.Prefs.Companion.TAG
import com.googlecode.mindbell.mission.StatusDetector


@SuppressLint("Override")
@TargetApi(24)
class QuickSettingsService : TileService() {

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "QuickSettingsService received intent with action ${intent.action}")
            updateTile()
        }
    }

    /**
     * Called when this tile begins listening for events.
     */
    override fun onStartListening() {
        Log.d(TAG, "QuickSettingsService starts listening to implicit broadcasts")
        val intentFilter = IntentFilter()
        intentFilter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(broadcastReceiver, intentFilter)
        updateTile()
    }

    /**
     * Called when the user taps the tile.
     */
    override fun onClick() {
        if (isLocked) { // is phone on lock screen?
            // TODO I not active or meditating then do nothing
            // TODO If active and manually muted then un-mute
            // TODO If active and not manually muted then mute for an hour (or a new default from prefs)
        } else {
            // TODO Show status in MuteActivity and rename
            startActivityAndCollapse(Intent(applicationContext, MuteActivity::class.java))
        }
    }

    /**
     * Called when this tile moves out of the listening state.
     */
    override fun onStopListening() {
        Log.d(TAG, "QuickSettingsService stops listening to implicit broadcasts")
        unregisterReceiver(broadcastReceiver)
    }

    /**
     * Update the quick settings tile according to the current state of MindBell.
     */
    private fun updateTile() {

        val tile = qsTile
        val prefs = Prefs.getInstance(applicationContext)
        val statusDetector = StatusDetector.getInstance(applicationContext)
        val muteRequestReason = statusDetector.getMuteRequestReason(false)

        val drawable = if (prefs.isMeditating) {
            R.drawable.ic_stat_meditating
        } else if (prefs.isActive && muteRequestReason == null) {
            R.drawable.ic_stat_active
        } else if (prefs.isActive && muteRequestReason != null) {
            muteRequestReason.muteReasonType.drawable
        } else {
            R.drawable.ic_stat_inactive
        }
        tile.icon = Icon.createWithResource(applicationContext, drawable)

        if (prefs.isActive) {
            tile.label = if (muteRequestReason != null) muteRequestReason.message else getString(R.string.summaryActive)
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.label = getString(R.string.summaryNotActive)
            tile.state = Tile.STATE_INACTIVE
        }

        tile.updateTile() // let UI show the changes
    }

}
