/*******************************************************************************
 * Copyright (c) 2009 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Francois Chouinard - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.core.signal;

import org.eclipse.linuxtools.tmf.core.event.ITmfTimestamp;

/**
 * A new current time is selected (for component synchronization)
  * 
 * @version 1.0
 * @author Francois Chouinard
*/
public class TmfTimeSynchSignal extends TmfSignal {

	private final ITmfTimestamp fCurrentTime;

	public TmfTimeSynchSignal(Object source, ITmfTimestamp ts) {
		super(source);
		fCurrentTime = ts;
	}

	public ITmfTimestamp getCurrentTime() {
		return fCurrentTime;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
    @SuppressWarnings("nls")
	public String toString() {
		return "[TmfTimeSynchSignal (" + fCurrentTime.toString() + ")]";
	}

}