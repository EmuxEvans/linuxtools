/*******************************************************************************
 * Copyright (c) 2010 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.linuxtools.changelog.ui.tests.swtbot;

import org.junit.runners.Suite;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Run as SWTBot test.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
        AddChangelogEntrySWTBotTest.class,
        PrepareChangelogSWTBotTest.class,
        DisabledPrepareChangelogSWTBotTest.class,
        CreateChangeLogFromHistorySWTBotTest.class,
        FormatChangeLogSWTBotTest.class
    }
)

public class AllSWTBotTests {
    // empty
}
