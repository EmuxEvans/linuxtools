/**********************************************************************
 * Copyright (c) 2011 Ericsson.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Bernd Hufmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.linuxtools.tmf.ui.tests.views.uml2sd.load;

import junit.framework.TestCase;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.linuxtools.internal.tmf.ui.Activator;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.SDView;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.load.IUml2SDLoader;
import org.eclipse.linuxtools.tmf.ui.views.uml2sd.load.LoadersManager;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class LoadersManagerTest extends TestCase {

    private final static String SDVIEW_WITH_ONE_LOADER = "org.eclipse.linuxtools.tmf.ui.tests.testSDView1Loader"; //$NON-NLS-1$
    private final static String SDVIEW_WITH_MULTIPLE_LOADER = "org.eclipse.linuxtools.tmf.ui.tests.testSDView2Loaders"; //$NON-NLS-1$
    private final static String TEST_LOADER_CLASS_NAME = "org.eclipse.linuxtools.tmf.ui.tests.uml2sd.load.TestLoaders"; //$NON-NLS-1$
    private final static String TMF_UML2SD_LOADER_CLASS_NAME = "org.eclipse.linuxtools.tmf.ui.views.uml2sd.loader.TmfUml2SDSyncLoader"; //$NON-NLS-1$

    private static final String LOADER_TAG = "uml2SDLoader"; //$NON-NLS-1$
    private static final String LOADER_PREFIX = LOADER_TAG + "."; //$NON-NLS-1$

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @SuppressWarnings("nls")
    public void testLoaderManager() {

        SDView view = null;
        try {

            /*
             * Test creation of a loader (one per SD view)
             */

            // Open view
            // Note this will create the default loader!
            view = (SDView)PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow()
                            .getActivePage()
                            .showView(SDVIEW_WITH_ONE_LOADER);

            IUml2SDLoader defaultLoader = LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_ONE_LOADER, view);
            assertNotNull("testLoaderManager", defaultLoader);

            // Test createLoader where loader doesn't exist
            assertNull("testLoaderManager", LoadersManager.getInstance().createLoader("blabla", view));

            // Test createLoader
            IUml2SDLoader loader = LoadersManager.getInstance().createLoader(TEST_LOADER_CLASS_NAME, view);

            assertNotNull("testLoaderManager", loader);
            assertEquals("testLoaderManager", "Test Loader", loader.getTitleString());
            assertEquals("testLoaderManager", loader, LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_ONE_LOADER));

            // compare loader and default loader. Even if they are the same class, they are different instances
            assertFalse("testLoaderManager", loader==defaultLoader);

            // test getCurrentLoader(viewId, view)
            IUml2SDLoader loader2 = LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_ONE_LOADER, view);
            assertEquals("testLoaderManager", loader, loader2);

            // Hide the view
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(view);

            // test that view restores the previous associated loader
            view = (SDView)PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow()
                            .getActivePage()
                            .showView(SDVIEW_WITH_ONE_LOADER);

            // Well, this is the only way test which loader is set
            assertEquals("testLoaderManager", "Sequence Diagram - First Page", view.getFrame().getName());

            // Test view == null
            assertNull("testLoaderManager", LoadersManager.getInstance().createLoader(TEST_LOADER_CLASS_NAME, null));

            // Hide the view
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(view);

            /*
             * Test creation of loaders with re-uses the same SD view
             */

            // test that view restores the previous associated loader
            view = (SDView)PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow()
                            .getActivePage()
                            .showView(SDVIEW_WITH_MULTIPLE_LOADER);

            // Test that default loader is set. Note that 2 default loaders are define in the plugin.xml and the
            // the first one should be selected.

            // Well, this is the only way test which loader is set
            assertEquals("testLoaderManager", "Sequence Diagram - First Page", view.getFrame().getName());

            loader = LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_MULTIPLE_LOADER, view);
            assertNotNull("testLoaderManager", loader);
            assertEquals("testLoaderManager", "Test Loader", loader.getTitleString());
            assertEquals("testLoaderManager", loader, LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_MULTIPLE_LOADER));

            // Test createLoader for loader with class name TMF_UML2SD_LOADER_CLASS_NAME
            loader = LoadersManager.getInstance().createLoader(TMF_UML2SD_LOADER_CLASS_NAME, view);

            assertNotNull("testLoaderManager", loader);
            assertEquals("testLoaderManager", "Component Interactions", loader.getTitleString());
            assertEquals("testLoaderManager", loader, LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_MULTIPLE_LOADER));

            // Verify if the correct loader is stored in the preferences as the current loader for this view
            assertEquals("testLoaderManager", TMF_UML2SD_LOADER_CLASS_NAME, getSavedLoader(SDVIEW_WITH_MULTIPLE_LOADER));

            // Test createLoader for loader with class name TEST_LOADER_CLASS_NAME
            loader = LoadersManager.getInstance().createLoader(TEST_LOADER_CLASS_NAME, view);

            assertNotNull("testLoaderManager", loader);
            assertEquals("testLoaderManager", "Test Loader", loader.getTitleString());
            assertEquals("testLoaderManager", loader, LoadersManager.getInstance().getCurrentLoader(SDVIEW_WITH_MULTIPLE_LOADER));

            // Verify if the correct loader is stored in the preferences as the current loader for this view
            assertEquals("testLoaderManager", TEST_LOADER_CLASS_NAME, getSavedLoader(SDVIEW_WITH_MULTIPLE_LOADER));
            assertEquals("testLoaderManager", TEST_LOADER_CLASS_NAME, LoadersManager.getInstance().getSavedLoader(SDVIEW_WITH_MULTIPLE_LOADER));

            // Hide the view
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().hideView(view);

            // Test reset loader
            LoadersManager.getInstance().resetLoader(SDVIEW_WITH_MULTIPLE_LOADER);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @SuppressWarnings("unused")
    private static void delay(long waitTimeMillis) {
        Display display = Display.getCurrent();
        if (display != null) {
            long endTimeMillis = System.currentTimeMillis() + waitTimeMillis;
            while(System.currentTimeMillis() < endTimeMillis) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
                display.update();
            }
        } else {
            try {
                Thread.sleep(waitTimeMillis);
            } catch (InterruptedException e) {
                // Ignored
            }
        }
    }

    private static String getSavedLoader(String viewId) {
        IPreferenceStore p = Activator.getDefault().getPreferenceStore();
        return p.getString(LOADER_PREFIX + viewId);
    }
}