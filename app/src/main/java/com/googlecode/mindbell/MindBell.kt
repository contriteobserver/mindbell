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
package com.googlecode.mindbell

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.googlecode.mindbell.accessors.PrefsAccessor.Companion.EXTRA_KEEP


class MindBell : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bell)
    }

    override fun onStart() {
        super.onStart()
        val keep = intent.extras?.getBoolean(EXTRA_KEEP)
        MindBell.logDebug("MindBell.onStart() called EXTRA_KEEP='$keep'")
        if (keep != null && !keep) {
            finish()
        }
    }

    companion object {

        val TAG = "MindBell"

        fun logDebug(message: String) {
            Log.d(TAG, message)
        }

        fun logInfo(message: String) {
            Log.i(TAG, message)
        }

        fun logWarn(message: String) {
            Log.w(TAG, message)
        }

        fun logWarn(message: String, e: Exception) {
            Log.w(TAG, message, e)
        }

        fun logError(message: String) {
            Log.e(TAG, message)
        }

        fun logError(message: String, e: Exception) {
            Log.e(TAG, message, e)
        }

    }

}