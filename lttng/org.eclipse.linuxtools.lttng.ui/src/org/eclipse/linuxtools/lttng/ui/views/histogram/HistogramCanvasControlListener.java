/*******************************************************************************
 * Copyright (c) 2009, 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Modifications:
 * 2010-07-16 Yuriy Vashchuk - Base class simplification
 *******************************************************************************/

package org.eclipse.linuxtools.lttng.ui.views.histogram;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;

/**
 * <b><u>HistogramCanvasControlListener</u></b>
 * <p>
 * Implementation of a ControlListener for the need of the HistogramCanvas
 * <p> 
 */
public class HistogramCanvasControlListener implements ControlListener {
	
	private HistogramCanvas ourCanvas = null;

	/**
	 * HistogramCanvasControlListener default constructor
	 */
	public HistogramCanvasControlListener() {
	}	
	
	/**
	 * HistogramCanvasControlListener constructor
	 * 
	 * @param newCanvas Related canvas
	 */
	public HistogramCanvasControlListener(HistogramCanvas newCanvas) {
		ourCanvas = newCanvas;
	}
	
	
	/**
	 * Method called when the canvas is moved.<p>
	 * 
	 * Just redraw the canvas...
	 * 
	 * @param event 	The controle event generated by the move.
	 */
	public void controlMoved(ControlEvent event) {
		if (ourCanvas != null)
			ourCanvas.redraw();
	}
	
	/**
	 * Method called when the canvas is resized.<p>
	 * 
	 * We need to tell the content that the canvas size changed
	 * 
	 * @param event 	The control event generated by the resize.
	 */
	public void controlResized(ControlEvent event) {
		
		if ( (ourCanvas != null) && (ourCanvas.getHistogramContent() != null) ) {
			// Set the new canvas size
			ourCanvas.getHistogramContent().setCanvasWindowSize(ourCanvas.getSize().x);
		}
	}
}