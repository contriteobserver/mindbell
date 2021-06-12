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
package com.googlecode.mindbell.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.googlecode.mindbell.R
import com.googlecode.mindbell.databinding.ActivityAboutBinding
import com.googlecode.mindbell.databinding.BellBinding
import com.googlecode.mindbell.mission.Prefs.Companion.EXTRA_KEEP
import com.googlecode.mindbell.mission.Prefs.Companion.TAG


class ReminderShowActivity : Activity() {
    private lateinit var binding: BellBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BellBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
    }

    override fun onStart() {
        super.onStart()
        val keep = intent.extras?.getBoolean(EXTRA_KEEP)
        Log.d(TAG, "ReminderShowActivity.onStart() called EXTRA_KEEP='$keep'")
        if (keep != null && !keep) {
            finish()
        }
    }

}