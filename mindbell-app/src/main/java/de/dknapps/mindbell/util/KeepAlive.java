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
package de.dknapps.mindbell.util;

import de.dknapps.mindbell.accessors.ContextAccessor;
import de.dknapps.mindbell.logic.RingingLogic;

public class KeepAlive {

    private boolean               isDone = false;
    private final long            timeout;
    private final long            sleepDuration;

    private final ContextAccessor ca;

    public KeepAlive(ContextAccessor ca, long timeout) {
        this.ca = ca;
        this.timeout = timeout;
        this.sleepDuration = timeout / 10;
    }

    public void ringBell() {
        RingingLogic.ringBell(ca, new Runnable() {
            public void run() {
                setDone();
            }
        });
        long totalSlept = 0;
        while (!isDone && totalSlept < timeout) {
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
