package org.bm3d.gui;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

public class WWJFengModel extends BasicModel 
{
	
	public WWJFengModel(WorldWindowGLCanvas canvas)   
	{
		// WW default layers
		super();

		// Toolbar layer
        ToolbarLayer toolbar = new ToolbarLayer(canvas); 
        toolbar.setEnabled(true);
        
        getLayers().add(toolbar);
	}
	
}
