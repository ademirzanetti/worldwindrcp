package org.eclipse.plugin.worldwind.wizard;


import org.apache.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.views.LayersView;
import org.eclipse.ui.IWorkbenchWindow;



import worldwind.contrib.layers.loop.HTTPLoopFileLayer;
import worldwind.contrib.layers.loop.HTTPSatLoopLayerList;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
//import worldwind.contrib.layers.loop.HTTPGOESLoopFileLayer;
//import worldwind.contrib.layers.loop.HTTPNavyWeatherLayer;
import worldwind.contrib.parsers.ParserUtils;

public class WeatherWizard extends Wizard 
{
	private static final Logger logger = Logger.getLogger(WeatherWizard.class);
	
	private WeatherWizardPage page1;
	private DimSelectionPage page2;
	
	private IWorkbenchWindow window;
//	private WorldWindowGLCanvas canvas;
	
	public WeatherWizard(IWorkbenchWindow window)  
	{
        super();
        setWindowTitle(Messages.getText("wiz.weather.title"));
        setNeedsProgressMonitor(true);
        
        this.window = window; 
	}

	@Override
	public void addPages() {
        page1 = new WeatherWizardPage("page1");
		page2 = new DimSelectionPage("page2");
		
		addPage(page1);
		addPage(page2);
	}
	
	@Override
	public boolean performFinish() 
	{
		ParserUtils.HTTPDataSet data = page1.getDataSets()[0];
		
		// 0 == NRL (Navy Labs), 1 == GOES
		
		// time span indexes
		int i = page2.getStartIndex();
		int j = page2.getEndIndex() + 1;
		
		TimeLoopGroundOverlay layer = page1.getlayer();
		
		logger.debug("Dataset=" + data.name 
				+ " range[" + i + "," + j + "] AGO:" + layer 
				);
		try 
		{
			LayersView view = (LayersView)Activator.getView(window, LayersView.ID);
			
			if ( view != null ) 
			{
				
				logger.debug("# of dataset frames=" +( (HTTPLoopFileLayer)layer).getFrames().size() );
				
				// build only the latest MAX_FRAMES overlays
				((HTTPLoopFileLayer)layer).buildOverlays(HTTPSatLoopLayerList.MAX_FRAMES);
				
/*				
				// Load Layers + time steps into the layers view
				if ( layer instanceof HTTPNavyWeatherLayer) { 
					logger.debug("# of NRL frames=" + ((HTTPNavyWeatherLayer)layer).getFrames().size());
					
					// NRL
					((HTTPNavyWeatherLayer)layer).buildOverlays(i, j);
				}
				else {
					logger.debug("# of GOES frames=" + ((HTTPGOESLoopFileLayer)layer).getFrames().size());
					
					// GOES
					((HTTPGOESLoopFileLayer)layer).buildOverlays(i, j);
				}
*/				
				addLayers(layer, view);
				//canvas.repaint();
			}
			
			return true;
		} 
		catch (Exception e) {
			//e.printStackTrace();
			Messages.showErrorMessage(window.getShell()
					, Messages.getText("wiz.err.dlg.title")
					, e.getClass() + ": " +  e.getMessage());
			return false;
		}
	}

	/*
	 * Add time steps (as children) of the layer into the Layers View
	 */
	private void addLayers(TimeLoopGroundOverlay layer , LayersView view) 
	{
		view.addOverlays(new TimeLoopGroundOverlay[]{layer}, false);
	}
	
}
