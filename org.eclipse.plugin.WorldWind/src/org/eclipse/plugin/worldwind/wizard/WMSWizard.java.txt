package org.eclipse.plugin.worldwind.wizard;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.operation.GroundOverlayFetchOperation;
import org.eclipse.plugin.worldwind.views.NavigatorView;
import org.eclipse.ui.IWorkbenchWindow;

import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.WMS_Capabilities;

public class WMSWizard extends Wizard 
{
	private static final Logger logger = Logger.getLogger(WMSWizard.class);
	
	private IWorkbenchWindow window;

	private WMSWizardPage page1;
	private DimSelectionPage page2;
	
	private Vector<ParserUtils.PublicWMSServer> servers;
	
	public WMSWizard(IWorkbenchWindow window) 
		throws Exception
	{
        super();
        setWindowTitle(Messages.getString("wiz.wms.title"));
        setNeedsProgressMonitor(true);
        
        this.window = window; 
        
        initialize();
	}

	/*
	 * Load WMS URLs from a local list
	 */
	private void initialize() throws Exception
	{
		// load public servers from config folder
		String list = Messages.getString("wiz.wms.servers.local.list");
		//String list = Messages.getText("wiz.wms.servers.list");
		
	    InputStream buffer  = new BufferedInputStream(Messages.getInputStream(ParserUtils.class, list));   
	    OutputStream os = new ByteArrayOutputStream();
		
	    ParserUtils.readStream(buffer, os);
	    os.close();
	    
		servers = ParserUtils.parsePublicWmsHTTPPage( os.toString() ); // new URL(list));
	}
	
	@Override
	public void addPages() {
        page1 = new WMSWizardPage("page1");
		page2 = new DimSelectionPage("page2");

		page1.setServers(servers);
		
		addPage(page1);
		addPage(page2);
	}
	
	@Override
	public boolean canFinish() {
		boolean p1Complete = page1.isPageComplete();
		boolean p2Complete = page2.isPageComplete();

		return p1Complete && p2Complete;
	}
	
	@Override
	public boolean performFinish() 
	{
		try {
			// Grab layers view
			//PlacesView view = (PlacesView)Activator.getView(window, PlacesView.ID);
			NavigatorView view = (NavigatorView)Activator.getView(window, NavigatorView.ID); 
					
			// selected date indices
			int[] indices 	= page1.getSelectedIndices();

			// image format
			final String format = page2.getFormat();

			// WMS caps + selected layers
			WMS_Capabilities capabilities 			=  page1.getCapabilities();
			WMS_Capabilities.Layer[] selectedLayers = getSelectedLayers(capabilities, indices);
			
			logger.debug("WMS Capabilities=" + capabilities.getVersion());
			logger.debug("# of selected layers=" + indices.length + " fmt:" + format);
			
			/**
			 * WMS Caps > 1.1.0 < 1.3.0 map to TiledWMSLayes
			 */
			if ( capabilities.getVersion().mid < 3) 
			{
				// Convert WMS Caps layers to TiledWMSLayer
				TiledWMSLayer[] wwLayers = 
					ParserUtils.newWMSTiledLayer(selectedLayers, format);
				
				// Add to Layers View
				String nodeName = (capabilities.getService().Title != null )
					? capabilities.getService().Title
					: capabilities.getService().Name;
					
				// All layers are disabled by default
				view.addTiledWMSLayers(nodeName, wwLayers, false);
			}
			/* WMS Caps ver 1.3.0 map to AnimatedGrounOverlays.  */
			else {
				// Use GroundOverlay or AnimatedGroundOverlay
				logger.debug("WMS version=" + capabilities.getVersion() 
						+ " Show dates=" + page2.showDates 
						+ " dates size=" + page2.startDate.getItems().length);
				
				boolean noTimeSteps = page2.startDate.getItems().length == 1;
				
				// Use GroundOverlayLayer: Each GroundOverlay is a different layer
				if ( !page2.showDates || ( page2.showDates && noTimeSteps))  
				{
					
					GroundOverlayLayer[] ovs = 
						ParserUtils.newGroundOverlay(selectedLayers, format);
					
					// Loop Overlay w/ too many time steps
					// Append "&time=T1/T2/PERIOD" to texture URL
					if ( page2.showDates  ) {
						for (GroundOverlayLayer groundOverlay : ovs) 
						{
							String newURL = groundOverlay.getTextureURL().toString()
								+ "&time=" + page2.startDate.getText() 
								+ "/" + page2.endDate.getText();
							
							logger.debug("Too many time steps for "
									+ groundOverlay + " using new url " + newURL);
							
							groundOverlay.setTextureURL(new URL(newURL));
						}
					}
					
					// Pre-fetch ground overlays to WW cache.
					// Layers are fetched when rendered thus large files will hang the gui
					getContainer().run(true, true, new GroundOverlayFetchOperation(ovs));
					
					// KML/KMZ?
					if ( format.equals(SimpleHTTPClient.CT_KML) 
							|| format.equals(SimpleHTTPClient.CT_KMZ)) 
					{
						// process each kml/kmz overlay (only
						for (GroundOverlayLayer groundOverlay : ovs) 
						{
							KMLSource kml = new KMLSource(groundOverlay.getTextureURL());
							logger.debug("Adding kml " + kml.getDocument().getName());
							
							view.addKMLSource(kml, false);
						}
					}
					else {
						// add to the layers view
						addGroundOverlays(ovs, view);
					}
				}
				// Convert selected layers to TimeLoopGroundOverlay 
				else {
					// dates[] should be the same for all layers
					String[] dates = page2.getSelectedTimes();
					 
					TimeLoopGroundOverlay[] loopLayers = new TimeLoopGroundOverlay[selectedLayers.length];
					
					for (int i = 0; i < loopLayers.length; i++) {
						loopLayers[i] = 
							ParserUtils.newTimeLoopGroundOverlay (
									selectedLayers[i]
									, dates
									, format);
						
						logger.debug("Adding loop layer: " + loopLayers[i]);
					}
					
					view.addOverlays(loopLayers, false);
				}
			}
			
			// show places view
			window.getActivePage().showView(NavigatorView.ID);
			
			return true;
		} 
		catch (Exception e) {
			e.printStackTrace();
			page2.setErrorMessage(e.getMessage());
			return false;
		}
	}

	
	/*
	 * Add ground overlays to view
	 */
	void addGroundOverlays(GroundOverlayLayer[] ovs, NavigatorView view) 
	{
		view.addOverlays(ovs, false);
	}
	
	
	/*
	 * Get user selected
	 */
	private WMS_Capabilities.Layer[] getSelectedLayers(WMS_Capabilities capabilities,  int[] indices) 
	{
		WMS_Capabilities.Layer[] layers = new WMS_Capabilities.Layer[indices.length];
		
		for (int i = 0; i < layers.length; i++) {
			layers[i] = capabilities.getLayers().get(indices[i]);
		}
		return layers;
	}
}
