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

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.plugin.worldwind.actions.ICommandIds;
import org.eclipse.plugin.worldwind.views.NavigatorView;
import org.eclipse.plugin.worldwind.views.WebBrowserView;

import org.eclipse.plugin.worldwind.contrib.layers.GeoRSSLayer;
import org.eclipse.plugin.worldwind.contrib.parsers.KMLSource;
import org.eclipse.plugin.worldwind.contrib.parsers.SimpleHTTPClient;


public class OpenFileAction extends Action 
{
	private static final Logger logger = Logger.getLogger(OpenFileAction.class);
	
    private final IWorkbenchWindow window;
	
	public OpenFileAction(String text, IWorkbenchWindow window) {
        super(text);
        this.window = window;
        
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_OPEN_FILE);
        
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_OPEN_FILE);
	}

	/*
	 * Run the file/url wiz
	 * @see org.eclipse.jface.action.Action#run()
	 */
    public void run() 
    {
    	String[] extensions 	= new String[] { "*.kml", "*.kmz", "*.xml"};
    	String[] filterNames 	= new String[] { "Google Earth KML (*.kml)"
    			, "Google Earth KMZ (*.kmz)", "GeoRSS XML files (*.xml)"};
    	
    	
		FileDialog dialog = new FileDialog(window.getShell(), SWT.OPEN);
		
		dialog.setFilterExtensions(extensions);
		dialog.setFilterNames(filterNames);
		
		// Files can be: kml, kmz or xml
		String path 		= dialog.open();
		String fileName		= dialog.getFileName();
		
		// Grab layers view (where kml objects will be added)
		//PlacesView view = (PlacesView)Activator.getView(window, PlacesView.ID);
		NavigatorView view = (NavigatorView)Activator.getView(window, NavigatorView.ID);
		
		try {
			if (  fileName.indexOf(".kml") != -1
					|| fileName.indexOf(".kmz") != -1)
			{
				// KML/KMZ
				File file = new File(path);
				
				KMLSource kml = (fileName.indexOf(".kml") != -1)
					? new KMLSource(file, SimpleHTTPClient.CT_KML)
					: new KMLSource(file, SimpleHTTPClient.CT_KMZ);
	
				logger.debug("Open kml file " + file + " as " + kml);
				
				WebBrowserView.handleKmlKmz(kml, view);
			}
			else {
				// GeoRSS
				GeoRSSLayer layer = new GeoRSSLayer(fileName, new File(path));
				
				view.addLayer(layer, Activator.ICON_GEORSS.createImage(), true);
				
			}
			
		} catch (Exception e) {
			view.setStatusErrorMessage(e.getMessage());
		}
    }
}
