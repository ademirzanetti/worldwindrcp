package org.eclipse.plugin.worldwind.actions;

import java.io.File;

import org.eclipse.jface.action.Action;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.plugin.worldwind.actions.ICommandIds;
import org.eclipse.plugin.worldwind.views.LayersView;
import org.eclipse.plugin.worldwind.views.WebBrowserView;

import worldwind.contrib.layers.GeoRSSLayer;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.SimpleHTTPClient;


public class OpenFileAction extends Action 
{
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
		String fileName	= dialog.getFileName();
		
		// Grab layers view (where kml objects will be added)
		LayersView view = (LayersView)Activator.getView(window, LayersView.ID);
		
		try {
			if (  fileName.indexOf(".kml") != -1
					|| fileName.indexOf(".kmz") != -1)
			{
				// KML/KMZ
				File file = new File(path);
				
				KMLSource kml = (fileName.indexOf(".kml") != -1)
					? new KMLSource(file, SimpleHTTPClient.CT_KML)
					: new KMLSource(file, SimpleHTTPClient.CT_KMZ);
					
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
