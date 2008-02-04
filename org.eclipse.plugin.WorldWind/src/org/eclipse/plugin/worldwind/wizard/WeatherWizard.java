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
package org.eclipse.plugin.worldwind.wizard;

import org.apache.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.views.NavigatorView;
import org.eclipse.ui.IWorkbenchWindow;


import worldwind.contrib.layers.loop.HTTPLoopFileLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.ParserUtils;

/**
 * Navy Research Labs near real time weather access wizard:
 * see http://www.nrlmry.navy.mil/sat_products.html
 * 
 * This data uses the NRL XML file from WW .NET
 * 
 * @author vsilva
 *
 */
public class WeatherWizard extends Wizard 
{
	private static final Logger logger = Logger.getLogger(WeatherWizard.class);
	
	private WeatherWizardPage page1;
	private DimSelectionPage page2;
	
	private IWorkbenchWindow window;
	
	public WeatherWizard(IWorkbenchWindow window)  
	{
        super();
        setWindowTitle(Messages.getString("wiz.weather.title"));
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
			//PlacesView view = (PlacesView)Activator.getView(window, PlacesView.ID);
			NavigatorView view = (NavigatorView)Activator.getView(window, NavigatorView.ID);
			
			if ( view != null ) 
			{
				
				logger.debug("# of dataset frames=" +( (HTTPLoopFileLayer)layer).getFrames().size() );
				
				// build only the latest MAX_FRAMES overlays
				//((HTTPLoopFileLayer)layer).buildOverlays(HTTPSatLoopLayerList.MAX_FRAMES);
				((HTTPLoopFileLayer)layer).buildOverlays(i, j);
				
				addLayers(layer, view);
				
				// show places view
				window.getActivePage().showView(NavigatorView.ID);
			}
			
			return true;
		} 
		catch (Exception e) {
			//e.printStackTrace();
			Messages.showErrorMessage(window.getShell()
					, e.getClass() + ": " +  e.getMessage());
			return false;
		}
	}

	/*
	 * Add time steps (as children) of the layer into the Layers View
	 */
	private void addLayers(TimeLoopGroundOverlay layer , NavigatorView view) 
	{
		view.addOverlays(new TimeLoopGroundOverlay[]{layer}, false);
	}
	
}
