/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
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
package com.googlecode.mindbell.util

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Converts volume setting from linear to decibel and vice versa.
 */
class VolumeConverter(private val mDynamicRangeDB: Int, val mMaxProgress: Int) {

    fun progress2volume(progress: Int): Float {
        if (progress == 0) {
            return 0f
        }
        val minusDB = (mMaxProgress - progress).toFloat() / mMaxProgress * mDynamicRangeDB
        return 10.0.pow(-minusDB / 20.0).toFloat()
    }

    fun volume2progress(aVolume: Float): Int {
        if (aVolume < 0.0001) {
            return 0
        }
        var minusDB = 20 * log10(aVolume / 1.0).toFloat()
        // limit our resolution to the dynamic range
        if (minusDB < -mDynamicRangeDB) {
            minusDB = (-mDynamicRangeDB).toFloat()
        }
        return ((mDynamicRangeDB + minusDB) / mDynamicRangeDB * mMaxProgress).roundToInt()
    }

}
