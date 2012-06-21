/*******************************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.ctfadaptor.headless;

import java.util.Vector;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfIterator;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;

/**
 * Test and benchmark reading a CTF LTTng kernel trace.
 *
 * @author Matthew Khouzam
 */
public class Benchmark {

    /**
     * Run the benchmark.
     *
     * @param args The command-line arguments
     */
    public static void main(final String[] args) {
        final String TRACE_PATH = "testfiles/kernel"; //$NON-NLS-1$
        final int NUM_LOOPS = 100;

        // Change this to enable text output
        final boolean USE_TEXT = true;

//        try {
//            System.in.read();
//        } catch (final IOException e1) {
//            e1.printStackTrace();
//        }
        // Work variables
        Long nbEvent = 0L;
        final Vector<Double> benchs = new Vector<Double>();
        CtfTmfTrace trace = null;
        long start, stop;
        for (int loops = 0; loops < NUM_LOOPS; loops++) {
            nbEvent = 0L;
            trace = new CtfTmfTrace();
            try {
                trace.initTrace(null, TRACE_PATH, CtfTmfEvent.class);
            } catch (final TmfTraceException e) {
                loops = NUM_LOOPS +1;
                break;
            }

            start = System.nanoTime();
            if (nbEvent != -1) {
                final CtfIterator traceReader = (CtfIterator) trace.seekEvent(0);

                start = System.nanoTime();
                CtfTmfEvent current = traceReader.getCurrentEvent();
                while (current != null) {
                    nbEvent++;
                    if (USE_TEXT) {

                        System.out.println("Event " + traceReader.getRank() + " Time " //$NON-NLS-1$ //$NON-NLS-2$
                                + current.getTimestamp().toString() + " type " + current.getEventName() //$NON-NLS-1$
                                + " on CPU " + current.getSource() + " " + current.getContent().toString()) ; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    traceReader.advance();
                    current = traceReader.getCurrentEvent();
                }
            }
            stop = System.nanoTime();
            System.out.print('.');
            final double time = (stop - start) / (double) nbEvent;
            benchs.add(time);
        }
        System.out.println(""); //$NON-NLS-1$
        double avg = 0;
        for (final Double val : benchs) {
            avg += val;
        }
        avg /= benchs.size();
        System.out.println("Time to read = " + avg + " events/ns"); //$NON-NLS-1$ //$NON-NLS-2$
        for (final Double val : benchs) {
            System.out.print(val);
            System.out.print(", "); //$NON-NLS-1$
        }

    }

//    /**
//     * @param timestamp
//     *            the timestamp in UTC to convert to nanoseconds.
//     * @return formatted string.
//     */
//    private static String formatDate(final long timestamp) {
//        final Date d = new Date(timestamp / 1000000);
//        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss."); //$NON-NLS-1$
//        final String output = df.format(d) + (timestamp % 1000000000);
//        return output;
//    }

}