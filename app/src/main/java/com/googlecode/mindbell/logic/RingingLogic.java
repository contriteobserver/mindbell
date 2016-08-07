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

import com.googlecode.mindbell.accessors.ContextAccessor;

/**
 * This class decides wether to ring the bell and in case kicks off the bell ring.
 */
public class RingingLogic {

    /** Time to wait for bell sound to finish or for displayed bell to be send back */
    public static final long WAITING_TIME = 15000L;

    /**
     * Instance to start the bell ring and wait for the end of ringing.
     */
    public static class KeepAlive {

        private boolean isDone = false;

        private final long time;

        private final long sleepDuration;

        private final ContextAccessor ca;

        public KeepAlive(ContextAccessor ca, long timeout) {
            this.ca = ca;
            this.time = timeout;
            this.sleepDuration = timeout / 10;
        }

        public void ringBell() {
            RingingLogic.ringBell(ca, new Runnable() {
                public void run() {
                    setDone();
                }
            });
            long totalSlept = 0;
            while (!isDone && totalSlept < time) {
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException ie) {
                }
                totalSlept += sleepDuration;
            }
        }

        private void setDone() {
            isDone = true;
        }

    }

    /**
     * Start playing bell sound and vibration if requested.
     *
     * @param context
     *            the context in which to play the sound.
     * @param runWhenDone
     *            an optional Runnable to call on completion of the sound, or null.
     * @return true if bell started ringing, false otherwise
     */
    public static boolean ringBell(ContextAccessor ca, final Runnable runWhenDone) {

        // 1. Verify if we should be muted
        if (ca.isMuteRequested(true)) {
            if (runWhenDone != null) {
                runWhenDone.run();
            }
            return false;
        }
        // 2. Stop any ongoing ring, and manually reset volume to original.
        if (ca.isBellSoundPlaying()) { // probably false, as context is (probably) different from that for startPlayingSoundAndVibrate()
            ca.finishBellSound();
        }

        // 3. Kick off the playback of the bell sound, with an automatic volume
        // reset built-in if not stopped.
        ca.startPlayingSoundAndVibrate(runWhenDone);
        return true;
    }

    /**
     * Play sound and vibrate if requested and wait till it's done or time reached.
     *
     * @param context
     *            the context in which to play the sound.
     */
    public static void ringBellAndWait(ContextAccessor ca) {
        new KeepAlive(ca, WAITING_TIME).ringBell();
    }

}
