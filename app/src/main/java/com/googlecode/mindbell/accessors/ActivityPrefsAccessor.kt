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
package com.googlecode.mindbell.accessors

import android.content.Context
import android.net.Uri

/**
 * All preference getter methods that influence the activities when the bell rings if active or meditating. It is used to enable
 * using different tones for different periods of meditation and for regular activity.
 */
interface ActivityPrefsAccessor {

    val isShow: Boolean

    val isSound: Boolean

    val isVibrate: Boolean

    val volume: Float

    val isNotification: Boolean

    val isDismissNotification: Boolean

    fun getSoundUri(context: Context): Uri?

}
