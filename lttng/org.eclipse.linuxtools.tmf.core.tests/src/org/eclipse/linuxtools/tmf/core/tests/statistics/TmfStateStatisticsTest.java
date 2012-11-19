/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.statistics;

import java.io.File;
import java.io.IOException;

import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statistics.TmfStateStatistics;
import org.junit.BeforeClass;

/**
 * Unit tests for the {@link TmfStateStatistics}
 *
 * @author Alexandre Montplaisir
 */
public class TmfStateStatisticsTest extends TmfStatisticsTest {

    /**
     * Set up the fixture (build the state history, etc.) once for all tests.
     */
    @BeforeClass
    public static void setUpClass() {
        try {
            File htFile = File.createTempFile("stats-test", ".ht"); //$NON-NLS-1$ //$NON-NLS-2$
            htFile.deleteOnExit();
            backend = new TmfStateStatistics(TestParams.createTrace(), htFile);
        } catch (TmfTraceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}