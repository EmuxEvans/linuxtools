/**********************************************************************
 * Copyright (c) 2012 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.internal.lttng2.ui.views.control.handlers;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.linuxtools.internal.lttng2.core.control.model.TraceSessionState;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.ControlView;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceDomainComponent;
import org.eclipse.linuxtools.internal.lttng2.ui.views.control.model.impl.TraceSessionComponent;
import org.eclipse.ui.IWorkbenchPage;

/**
 * <p>
 * Command handler implementation to add contexts to all channels and all events.
 * </p>
 *
 * @author Bernd Hufmann
 */
public class AddContextOnDomainHandler extends BaseAddContextHandler {

    // ------------------------------------------------------------------------
    // Attributes
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------
    /*
     * (non-Javadoc)
     * @see org.eclipse.linuxtools.internal.lttng2.ui.views.control.handlers.BaseAddContextHandler#addContexts(org.eclipse.linuxtools.internal.lttng2.ui.views.control.handlers.CommandParameter, java.util.List, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void addContexts(CommandParameter param, List<String> contextNames, IProgressMonitor monitor) throws ExecutionException {
        if (param instanceof DomainCommandParameter) {
            TraceDomainComponent domain = ((DomainCommandParameter)param).getDomain();
            domain.addContexts(contextNames, monitor);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.core.commands.AbstractHandler#isEnabled()
     */
    @Override
    public boolean isEnabled() {

        // Get workbench page for the Control View
        IWorkbenchPage page = getWorkbenchPage();
        if (page == null) {
            return false;
        }

        TraceDomainComponent domain = null;
        TraceSessionComponent session = null;

        // Check if one domain is selected
        ISelection selection = page.getSelection(ControlView.ID);
        if (selection instanceof StructuredSelection) {
            StructuredSelection structered = ((StructuredSelection) selection);
            for (Iterator<?> iterator = structered.iterator(); iterator.hasNext();) {
                Object element = iterator.next();
                if (element instanceof TraceDomainComponent) {
                    TraceDomainComponent tmpDomain = (TraceDomainComponent) element;
                    session = (TraceSessionComponent) tmpDomain.getParent();

                    // Add only TraceDomainComponent whose TraceSessionComponent parent is inactive and not destroyed
                    if ((session.getSessionState() == TraceSessionState.INACTIVE) && (!session.isDestroyed())) {
                        domain = tmpDomain;
                    }
                }
            }
        }

        boolean isEnabled = domain != null;

        fLock.lock();
        try {
            fParam = null;
            if (isEnabled) {
                fParam = new DomainCommandParameter(session, domain);
            }
        } finally {
            fLock.unlock();
        }

        return isEnabled;
    }


}