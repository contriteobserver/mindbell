/*
 * MindBell - Aims to give you a support for staying mindful in a busy life -
 *            for remembering what really counts
 *
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
 */
package com.googlecode.mindbell;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * AndroidStudio does not run all JUnit4 test classes automatically probably not those implementing
 * ActivityInstrumentationTestCase2. Therefore a suite class is needed for them.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({MindBellTest.class})
public class AllNotAutomaticallRunTests {
    //nothing
}
