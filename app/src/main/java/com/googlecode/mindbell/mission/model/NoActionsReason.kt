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

package com.googlecode.mindbell.mission.model

enum class NoActionsReason {

    INACTIVE, // Bell is neither meditating nor active -- not reminding, not rescheduling
    MEDITATION_RAMP_UP, // Bell is in ramp-up phase of meditation
    BUTTON_OR_PREFS_OR_REBOOT, // Bell called by activate bell button or preferences or when boot completed or after updating
    NIGHT_TIME, // Bell called during night time
    MUTED, // Bell called but is muted
    NONE // Only used to allow default constructor for StatisticsEntry object

}