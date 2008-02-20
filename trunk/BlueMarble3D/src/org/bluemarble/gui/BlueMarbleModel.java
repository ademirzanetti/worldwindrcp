package org.bluemarble.gui;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

public class BlueMarbleModel extends BasicModel 
{
	
	public BlueMarbleModel(WorldWindowGLCanvas canvas)   
	{
		// WW default layers
		super();

		//getLayers().removeAll(getLayers());
		
		// Toolbar layer
        MasterUILayer toolbar = new MasterUILayer(canvas); 
        toolbar.setEnabled(true);
        
        getLayers().add(toolbar);
	}
	
}
