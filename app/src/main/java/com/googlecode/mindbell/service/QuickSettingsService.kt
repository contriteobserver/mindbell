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
        // no longer need to filter TelephonyManager.ACTION_PHONE_STATE_CHANGED as quick settings are close on calls
        intentFilter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        registerReceiver(broadcastReceiver, intentFilter)
        updateTile()
    }

    /**
     * Called when the user taps the tile.
     */
    override fun onClick() {
        if (isLocked) { // is phone on lock screen?
            // TODO If active and manually muted then un-mute
            // TODO If active and not manually muted then mute for an hour (or a new default from prefs)
            // TODO I not active then do nothing
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

        val newIcon: Icon
        val newLabel: String
        val newState: Int

        val prefs = Prefs.getInstance(applicationContext)
        val statusDetector = StatusDetector.getInstance(applicationContext)

        if (statusDetector.isMuteRequested(false)) {
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_stat_bell_active_but_muted)
        } else {
            newIcon = Icon.createWithResource(applicationContext, R.drawable.ic_stat_bell_active)
        }

        if (prefs.isActive) {
            newLabel = getString(R.string.summaryActive)
            newState = Tile.STATE_ACTIVE
        } else {
            newLabel = getString(R.string.summaryNotActive)
            newState = Tile.STATE_INACTIVE
        }

        // Change icon, label and state of the quick settings tile
        val tile = qsTile
        tile.icon = newIcon
        tile.label = newLabel
        tile.state = newState
        tile.updateTile() // let UI show the changes
    }

}
