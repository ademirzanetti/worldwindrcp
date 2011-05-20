/*******************************************************************************
 * Copyright (c) 2006 Vladimir Silva and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Silva - initial API and implementation
 *******************************************************************************/
package org.eclipse.plugin.worldwind.actions;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.plugin.worldwind.views.NetCDFView;
import org.eclipse.plugin.worldwind.views.WebBrowserView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * Action to activate a {@link WebBrowserView} or {@link NetCDFView} view
 * @author Owner
 *
 */
public class OpenViewAction extends Action 
{
	private static final Logger logger = Logger.getLogger(OpenViewAction.class);
	
    private IWorkbenchWindow window;
	private String ViewID;
	
	private static int instanceNum = 0;
	private boolean multiInstance = false;
	
	// view type: WebBrowser or NetCDF
	public enum VIEW_TYPE { WEB_BROWSER, NETCDF};
	
	private VIEW_TYPE type;
	
	// if null it will open a file selection dialog
	// This is used to fire this action within a we browser
	private String netCDFUri;
	
	public OpenViewAction(String text, IWorkbenchWindow window, String ViewID
			, boolean multiInstance, VIEW_TYPE type) 
	{
        super(text);
        this.window 		= window;
        this.ViewID 		= ViewID;
        this.multiInstance 	= multiInstance;
        this.type 			= type;
        
        logger.debug("View ID:" + ViewID + " type=" + type);
	}

	/**
	 * Menu initialization
	 * @param cmdID
	 * @param im
	 */
	public void initMenu (String cmdID, ImageDescriptor im) {
        // The id is used to refer to the action in a menu or toolbar
        setId(cmdID);
        
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(cmdID);
        setImageDescriptor(im); 
		
	}
	
	@Override
	public void run() {
        if ( window != null) {
        	try {
        		if ( type == VIEW_TYPE.NETCDF) 
        			handleNetCDF();
        		else
        			handleWebBrowser();
			} 
        	catch (PartInitException e) {
        		MessageDialog.openError(window.getShell()
						, "Open View"
						, e.getMessage()
						);
			}
        }
	}

	private void handleWebBrowser () throws PartInitException
	{
		// multi instance view
		window.getActivePage().showView(WebBrowserView.ID
				, Integer.toString(instanceNum++)
				, IWorkbenchPage.VIEW_ACTIVATE);
		
	}
	
	/**
	 * Used to fire this action within a web browser without firing the file selection
	 * dialog
	 * @param uri
	 */
	public void setNetCDFUri (String uri) {
		netCDFUri = uri;
	}
	
	private void handleNetCDF () throws PartInitException
	{
    	String[] extensions 	= new String[] { "*.*"};
    	String[] filterNames 	= new String[] { "All NetCDF files (*.nc, *.ncf, *.*)"};
    	
    	String path 		= netCDFUri;

    	if ( path == null) {
			FileDialog dialog = new FileDialog(window.getShell(), SWT.OPEN);
			
			dialog.setFilterExtensions(extensions);
			dialog.setFilterNames(filterNames);
			
			path 		= dialog.open();
    	}

    	logger.debug("NetCDF Uri:" + path);
    	
		if ( path == null ) return; 
		
		NetCDFView view;
		
		if ( multiInstance) {
			view = (NetCDFView)window.getActivePage().showView(ViewID
    				, Integer.toString(instanceNum++)
    				, IWorkbenchPage.VIEW_ACTIVATE);
		}
		else
			view = (NetCDFView)window.getActivePage().showView(ViewID);
		
		view.loadDataset(path);
	}
}
