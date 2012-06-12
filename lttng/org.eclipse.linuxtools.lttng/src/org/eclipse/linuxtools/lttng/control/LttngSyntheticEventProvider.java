/*******************************************************************************
 * Copyright (c) 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Alvaro Sanchez-Leon (alvsan09@gmail.com) - Initial API and implementation
 *   Marc Dumais (marc.dumais@ericsson.com) - Fix for 316455 (first part)
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.control;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.linuxtools.lttng.TraceDebug;
import org.eclipse.linuxtools.lttng.event.LttngEvent;
import org.eclipse.linuxtools.lttng.event.LttngEventType;
import org.eclipse.linuxtools.lttng.event.LttngSyntheticEvent;
import org.eclipse.linuxtools.lttng.event.LttngSyntheticEvent.SequenceInd;
import org.eclipse.linuxtools.lttng.event.LttngTimestamp;
import org.eclipse.linuxtools.lttng.model.LTTngTreeNode;
import org.eclipse.linuxtools.lttng.request.LttngBaseEventRequest;
import org.eclipse.linuxtools.lttng.state.evProcessor.ITransEventProcessor;
import org.eclipse.linuxtools.lttng.state.evProcessor.state.StateEventToHandlerFactory;
import org.eclipse.linuxtools.lttng.state.model.LttngTraceState;
import org.eclipse.linuxtools.lttng.state.trace.IStateTraceManager;
import org.eclipse.linuxtools.tmf.component.TmfEventProvider;
import org.eclipse.linuxtools.tmf.event.TmfEventSource;
import org.eclipse.linuxtools.tmf.event.TmfTimeRange;
import org.eclipse.linuxtools.tmf.event.TmfTimestamp;
import org.eclipse.linuxtools.tmf.experiment.TmfExperiment;
import org.eclipse.linuxtools.tmf.request.ITmfDataRequest;
import org.eclipse.linuxtools.tmf.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.signal.TmfEndSynchSignal;
import org.eclipse.linuxtools.tmf.signal.TmfStartSynchSignal;
import org.eclipse.linuxtools.tmf.trace.ITmfContext;
import org.eclipse.linuxtools.tmf.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.trace.TmfContext;
import org.eclipse.linuxtools.tmf.trace.TmfTrace;

/**
 * @author alvaro
 * 
 */
public class LttngSyntheticEventProvider extends TmfEventProvider<LttngSyntheticEvent> {

	// ========================================================================
	// Data
	// ========================================================================
	public static final int BLOCK_SIZE = 1;
	public static final int NB_EVENTS  = 1;
	public static final int QUEUE_SIZE = 1; // lttng specific, one event at a time

	private ITmfDataRequest<LttngSyntheticEvent> fmainRequest = null;
	private final List<IStateTraceManager> fEventProviderRequests = new Vector<IStateTraceManager>();

	private final LttngSyntheticEvent fStatusEvent;
	private int fMainReqEventCount = 0;
	volatile boolean startIndSent = false;
	private LTTngTreeNode fExperiment = null;
	private ITransEventProcessor fstateUpdateProcessor = StateEventToHandlerFactory.getInstance();
	private boolean waitForRequest = false;
	long dispatchTime = 0L;
	private final Map<ITmfTrace, LttngTraceState> traceToTraceStateModel = new HashMap<ITmfTrace, LttngTraceState>();

	// ========================================================================
	// Constructor
	// ========================================================================
	/**
	 * Accessibility to package - use factory instead of this constructor
	 * 
	 * @param type
	 */
	LttngSyntheticEventProvider(Class<LttngSyntheticEvent> type) {
		super("LttngSyntheticEventProvider", type, QUEUE_SIZE);

		// prepare empty instance status indicators and allow them to travel via
		// the framework
		TmfEventSource source = new TmfEventSource(this);
		LttngEventType dtype = new LttngEventType();
		LttngTimestamp statusTimeStamp = new LttngTimestamp(
				TmfTimestamp.Zero);

		fStatusEvent = new LttngSyntheticEvent(null, statusTimeStamp, source,
				dtype, null, null, null);
		fStatusEvent.setSequenceInd(SequenceInd.STARTREQ);
	}

	// ========================================================================
	// Methods
	// ========================================================================

	@SuppressWarnings("unchecked")
	@Override
	public ITmfContext armRequest(final ITmfDataRequest<LttngSyntheticEvent> request) {
		// validate
		// make sure we have the right type of request
		if (!(request instanceof ITmfEventRequest<?>)) {
			request.cancel();
			TraceDebug.debug("Request is not an instance of ITmfEventRequest");
			return null;
		}

		if (fExperiment == null) {
			TraceDebug.debug("Experiment is null");
			request.cancel();
			return null;
		}

		// get ready to start processing
		reset(fExperiment);

		// At least one base provider shall be available
		if (fEventProviderRequests.size() < 1) {
			request.cancel();
			TraceDebug.debug("No Base event providers available");
			return null;
		}

		fmainRequest = request;
		// define event data handling
		ITmfEventRequest<LttngSyntheticEvent> eventRequest = (ITmfEventRequest<LttngSyntheticEvent>) fmainRequest;
		TmfTimeRange reqWindow = eventRequest.getRange();

		TraceDebug.debug("Main Synthethic event request started on thread:  " + Thread.currentThread().getName());

		boolean subRequestQueued = false;
		TmfExperiment<LttngEvent> experiment = (TmfExperiment<LttngEvent>) fExperiment.getValue();
		experiment.startSynch(new TmfStartSynchSignal(0));
		
		TmfTimeRange adjustedRange = reqWindow;
				
		// Figure-out if we need to increase the range of the request:  if some
		// checkpoints are before the beginning of the range, increase the 
		// range to catch them.   We will then exercise the state system of 
		// those traces until the requested beginning time range, discarding
		// the unrequested data.   		
		IStateTraceManager traceManager;
		Iterator<IStateTraceManager> iter = fEventProviderRequests.iterator();
		// For each traceManager in the current experiment...
		while(iter.hasNext()) {
			traceManager = iter.next();
			// restore trace state system to nearest check point
			TmfTimestamp checkPoint = traceManager
					.restoreCheckPointByTimestamp(reqWindow.getStartTime());

			// validate that the checkpoint restored is within requested bounds
			// (not outside the current trace's range or after the end of requested range)
			TmfTimeRange traceRange = traceManager.getTrace().getTimeRange();
			if ((checkPoint != null) && !(
					checkPoint.getValue() >= traceRange.getStartTime().getValue() &&
					checkPoint.getValue() <= traceRange.getEndTime().getValue() && 
					checkPoint.getValue() < reqWindow.getEndTime().getValue())
					) {
				// checkpoint is out of trace bounds; no need to adjust request for this
				// trace
			}
			else {
				// use checkpoint time as new startTime for request if it's earlier than
				// current startTime
				if (checkPoint != null && adjustedRange.getStartTime().getValue() > checkPoint.getValue()) {
					adjustedRange = new TmfTimeRange(checkPoint, reqWindow.getEndTime());
				}	
			}		
			// Save which trace state model corresponds to current trace
			traceToTraceStateModel.put(traceManager.getTrace(), traceManager.getStateModel());
		}
		
		dispatchTime = reqWindow.getStartTime().getValue();
		// Create a single request for all traces in the experiment, with coalesced time range.
		final LttngBaseEventRequest subRequest = new LttngBaseEventRequest(adjustedRange, reqWindow.getStartTime(),
				0, TmfEventRequest.ALL_DATA, BLOCK_SIZE, ITmfDataRequest.ExecutionType.FOREGROUND) {

			private LttngSyntheticEvent syntheticEvent = null;
			private LttngSyntheticEvent syntheticAckIndicator = null;
			long subEventCount = 0L;

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.linuxtools.lttng.control.LttngEventRequest#handleData()
			 */
			@Override
			public void handleData(LttngEvent event) {
				super.handleData(event);
				if (event != null) {
					handleIncomingData(event);
				} else {
					TraceDebug.debug("handle data received with no data");
				}
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.linuxtools.tmf.request.TmfDataRequest#done()
			 */
			@Override
			public void done() {
				// mark this sub-request as completed
				super.done();
				handleProviderDone(/*getTraceModel()*/); // mcds
			}

			/**
			 * Trigger the Analysis and sequential control of the events.
			 * 
			 * @param e
			 */
			private void handleIncomingData(LttngEvent e) {
				long eventTime = e.getTimestamp().getValue();

				TmfTrace<LttngEvent> inTrace = e.getParentTrace();
				LttngTraceState traceModel = traceToTraceStateModel.get(inTrace);
				
				// queue the new event data and an ACK
				updateSynEvent(e);
				
				// If time at or above requested time, update application
				if (eventTime >= dispatchTime) {
					// Before update
					syntheticEvent.setSequenceInd(SequenceInd.BEFORE);
					fmainRequest.handleData(syntheticEvent);
					fmainRequest.handleData(syntheticAckIndicator);

					// Update state locally
					syntheticEvent.setSequenceInd(SequenceInd.UPDATE);
					fstateUpdateProcessor.process(syntheticEvent, traceModel);

					// After Update
					syntheticEvent.setSequenceInd(SequenceInd.AFTER);
					fmainRequest.handleData(syntheticEvent);
					fmainRequest.handleData(syntheticAckIndicator);

					// increment once per dispatch
					incrementSynEvenCount();
					subEventCount++;
				} else {
					// event time is between checkpoint adjusted time and
					// requested time i.e. application does not expect the
					// event, however the state system needs to be re-built
					// to the dispatch point
					syntheticEvent.setSequenceInd(SequenceInd.UPDATE);
					fstateUpdateProcessor.process(syntheticEvent, traceModel);
				}
			}

			/**
			 * Create a synthetic event from the received new reference, if
			 * the reference is the same there is no need for a new instance
			 * 
			 * if this is the first event for this request, call start
			 * handler
			 * 
			 * @param e
			 * @return
			 */
			private LttngSyntheticEvent updateSynEvent(LttngEvent e) {
				if (syntheticEvent == null
						|| syntheticEvent.getBaseEvent() != e) {
					syntheticEvent = new LttngSyntheticEvent(e);
				}

				TmfTrace<LttngEvent> inTrace = e.getParentTrace();
				LttngTraceState traceModel = traceToTraceStateModel.get(inTrace);
				
				// Trace model needed by application handlers
				syntheticEvent.setTraceModel(traceModel);

				// send the start request indication once per request thread
				if (!startIndSent) {
					TraceDebug.debug("Thread started: " + Thread.currentThread().getName());
					handleProviderStarted(traceModel);
					startIndSent = true;
				}

				return syntheticEvent;
			}
		};

		// start request
		TmfExperiment<LttngEvent> provider = (TmfExperiment<LttngEvent>) fExperiment.getValue();
		provider.sendRequest(subRequest);

		// provider.sendRequest(subRequest, ExecutionType.LONG);
		subRequestQueued = true;

		experiment.endSynch(new TmfEndSynchSignal(0));

		// Return a dummy context, not used for relay provider
		return (subRequestQueued) ? new TmfContext() : null;
	}

	/**
	 * Notify listeners to prepare to receive data e.g. clean previous data etc.
	 */
	public void handleProviderStarted(LttngTraceState traceModel) {
		LttngSyntheticEvent startIndEvent = new LttngSyntheticEvent(fStatusEvent);
		startIndEvent.setSequenceInd(SequenceInd.STARTREQ);

		// Notify application
		fmainRequest.handleData(startIndEvent);

		// Notify state event processor
		fstateUpdateProcessor.process(startIndEvent, null);
	}

	/**
	 * Notify listeners, no more events for the current request will be
	 * distributed e.g. update view.
	 */
	public synchronized void handleProviderDone() {
		// Notify application. One notification per trace so the last state of each trace can be
		// drawn
		LttngTraceState traceModel;
		Iterator<IStateTraceManager> iter = fEventProviderRequests.iterator();
		while(iter.hasNext()) {
			traceModel = iter.next().getStateModel();

			LttngSyntheticEvent finishEvent = new LttngSyntheticEvent(fStatusEvent);
			finishEvent.setSequenceInd(SequenceInd.ENDREQ);
			finishEvent.setTraceModel(traceModel);

			fmainRequest.handleData(finishEvent);
			fmainRequest.done();
		}
		
	}

	/**
	 * Increment the global event counter i.e. events from any sub requests
	 */
	private synchronized void incrementSynEvenCount() {
		fMainReqEventCount++;
	}

	/**
	 * @return
	 */
	public synchronized int getSynEvenCount() {
		return fMainReqEventCount;
	}

	/**
	 * Reset provider to a state ready to begin thread execution
	 * 
	 * @param experimentNode
	 */
	public synchronized void reset(LTTngTreeNode experimentNode) {
		fmainRequest = null;

		fEventProviderRequests.clear();
		fMainReqEventCount = 0;
		startIndSent = false;

		// set of base event providers
		if (fExperiment != null) {
			LTTngTreeNode[] traces = fExperiment.getChildren();
			for (LTTngTreeNode trace : traces) {
				IStateTraceManager traceBaseEventProvider = (IStateTraceManager) trace;
				fEventProviderRequests.add(traceBaseEventProvider);
			}
		}

		if (fExperiment != experimentNode) {
			updateExperimentNode(experimentNode);
		}
	}

	/**
	 * Point to a new experiment reference
	 * 
	 * @param experiment
	 */
	private synchronized void updateExperimentNode(LTTngTreeNode experiment) {
		if (experiment != null
				&& experiment.getValue() instanceof TmfExperiment<?>) {
			fExperiment = experiment;
		} else {
			TraceDebug
					.debug("Experiment received is not instance of TmfExperiment: "
							+ experiment.getClass().getName());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.linuxtools.tmf.component.TmfDataProvider#sendRequest(org.
	 * eclipse.linuxtools.tmf.request.TmfDataRequest)
	 */
	@Override
	public void sendRequest(final ITmfDataRequest<LttngSyntheticEvent> request) {
		super.sendRequest(request);
		if (waitForRequest) {
			try {
				request.waitForCompletion();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @return the waitForRequest
	 */
	public boolean isWaitForRequest() {
		return waitForRequest;
	}

	/**
	 * @param waitForRequest
	 *            configures the provider to wait for the request completion
	 */
	public void setWaitForRequest(boolean waitForRequest) {
		this.waitForRequest = waitForRequest;
	}

	@Override
	public LttngSyntheticEvent getNext(ITmfContext context) {
		try {
			fmainRequest.waitForCompletion();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

}