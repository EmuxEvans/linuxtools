/*******************************************************************************
 * Copyright (c) 2009 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   William Bourque <wbourque@gmail.com> - Initial API and implementation
 *   Matthew Khouzam - Update to CtfTmf trace and events
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.tests.ctfadaptor.headless;

import java.util.Vector;

import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.core.ctfadaptor.CtfTmfTrace;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.core.trace.TmfExperiment;

/**
 * Benchmark the event request subsystem of TMF.
 */
public class RequestBenchmark extends TmfEventRequest<CtfTmfEvent> {

    @SuppressWarnings("unchecked")
    private RequestBenchmark(final Class<? extends ITmfEvent> dataType,
            final TmfTimeRange range, final int nbRequested) {
        super((Class<CtfTmfEvent>) dataType, range, nbRequested, 1);
    }

    // Path of the trace
    private static final String TRACE_PATH = "../org.eclipse.linuxtools.ctf.core.tests/traces/kernel"; //$NON-NLS-1$

    // Change this to run several time over the same trace
    private static final int NB_OF_PASS = 100;

    // Work variables
    private static int nbEvent = 0;
    private static TmfExperiment<CtfTmfEvent> fExperiment = null;
    private static Vector<Double> benchs = new Vector<Double>();

    /**
     * Run the benchmark
     *
     * @param args
     *            The command-line arguments
     */
    public static void main(final String[] args) {

        try {
            /* Our experiment will contains ONE trace */
            @SuppressWarnings("unchecked")
            final ITmfTrace<CtfTmfEvent>[] traces = new ITmfTrace[1];
            traces[0] = new CtfTmfTrace();
            traces[0].initTrace(null, TRACE_PATH, CtfTmfEvent.class);
            /* Create our new experiment */
            fExperiment = new TmfExperiment<CtfTmfEvent>(CtfTmfEvent.class, "Headless", traces); //$NON-NLS-1$

            /*
             * We will issue a request for each "pass". TMF will then process
             * them synchronously.
             */
            RequestBenchmark request = null;
            for (int x = 0; x < NB_OF_PASS; x++) {
                request = new RequestBenchmark(CtfTmfEvent.class,
                        TmfTimeRange.ETERNITY, Integer.MAX_VALUE);
                fExperiment.sendRequest(request);
            }
            prev = System.nanoTime();
        } catch (final NullPointerException e) {
            /*
             * Silently dismiss Null pointer exception The only way to "finish"
             * the threads in TMF is by crashing them with null.
             */
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void handleData(final CtfTmfEvent event) {
        super.handleData(event);
        nbEvent++;

    }

    static long prev;
    static long done = 0;
    @Override
    public void handleCompleted() {
        final long next = System.nanoTime();
        double val = next - prev;
        final int nbEvent2 = nbEvent;
        val /= nbEvent2;

        nbEvent = 0;
        prev = next;
        benchs.add(val);
        if (benchs.size() == NB_OF_PASS) {
            try {
                System.out.println("Nb events : " + nbEvent2); //$NON-NLS-1$

                for (final double value : benchs) {
                    System.out.print(value + ", "); //$NON-NLS-1$
                }
                fExperiment.sendRequest(null);

            } catch (final Exception e) {
            }
        }
    }

    @Override
    public void handleSuccess() {
    }

    @Override
    public void handleFailure() {
    }

    @Override
    public void handleCancel() {
    }

}