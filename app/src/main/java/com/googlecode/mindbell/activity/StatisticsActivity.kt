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

import android.app.ListActivity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.googlecode.mindbell.R
import com.googlecode.mindbell.mission.Prefs
import com.googlecode.mindbell.mission.model.Statistics.StatisticsEntry
import kotlinx.android.synthetic.main.activity_statistics.*
import kotlinx.android.synthetic.main.activity_statistics_item.view.*

/**
 * Show about dialog to display e.g. the license.
 */
class StatisticsActivity : ListActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)
        val prefs = Prefs.getInstance(applicationContext)
        list.adapter = StatisticsEntryListAdapter(applicationContext, R.layout.activity_statistics_item, prefs
                .getStatisticsEntryList())
    }

    private inner class StatisticsEntryListAdapter(context: Context, private val resource: Int, objects: List<StatisticsEntry>)
        : ArrayAdapter<StatisticsEntry>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val itemView = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
            val statisticsEntry = getItem(position)
            itemView.now.text = statisticsEntry.now
            itemView.comment.text = statisticsEntry.toString()
            // itemView.judgment ...
            return itemView
        }
    }

}
