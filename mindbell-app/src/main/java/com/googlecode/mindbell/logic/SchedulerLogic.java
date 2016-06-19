/*******************************************************************************
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
 *     Copyright (C) 2010-2014 Marc Schroeder
 *     Copyright (C) 2014-2016 Uwe Damken
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
 *******************************************************************************/
package com.googlecode.mindbell.logic;

import java.util.Random;

import com.googlecode.mindbell.accessors.PrefsAccessor;
import com.googlecode.mindbell.util.TimeOfDay;

import android.util.Log;

public class SchedulerLogic {

    private static Random random = new Random();

/**
     * Return next time to bell after the given "now".
 *
     * @param nowTimeMillis
     * @param prefs
     * @return
 */
    public static long getNextTargetTimeMillis(long nowTimeMillis, PrefsAccessor prefs) {
        final long meanInterval = prefs.getInterval();
        final boolean randomize = prefs.isRandomize();
        long randomizedInterval = randomize ? getRandomInterval(meanInterval) : meanInterval;
        long targetTimeMillis = nowTimeMillis + randomizedInterval;
        if (!prefs.isDaytime(new TimeOfDay(targetTimeMillis))) { // inactive time?
            long dayStartMillis = prefs.getNextDaytimeStartInMillis(targetTimeMillis);
            targetTimeMillis = dayStartMillis + (randomize ? randomizedInterval - meanInterval / 2 : 0);
            assert targetTimeMillis >= dayStartMillis;
        }
        return targetTimeMillis;
    }

    /**
     * Compute a random value following a Gaussian distribution around the given mean. The value is guaranteed not to fall below
     * 0.5 * mean and not above 1.5 * mean.
     *
     * @param mean
     * @return
     */
    private static long getRandomInterval(long mean) {
        long value = (long) (mean * (1.0 + 0.3 * random.nextGaussian()));
        if (value < mean / 2) {
            value = mean / 2;
        }
        if (value > 3 * mean / 2) {
            value = 3 * mean / 2;
        }
        Log.d(com.googlecode.mindbell.MindBellPreferences.TAG, "value: " + (new TimeOfDay(value)).toString());
        return value;
    }
}
