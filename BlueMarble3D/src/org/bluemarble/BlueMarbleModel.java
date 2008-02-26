package org.bluemarble;

import org.bluemarble.gui.MasterUILayer;

import worldwind.contrib.layers.PositionLayer;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.Earth.LandsatI3;
import gov.nasa.worldwind.layers.Earth.USGSTopographicMaps;

public class BlueMarbleModel extends BasicModel 
{
	private MasterUILayer uiLayer;
	
	public BlueMarbleModel(WorldWindowGLCanvas canvas)   
	{
		// WW default layers
		super();

		// disable some layers: LandSat, USGS Topo
		LayerList layers = getLayers();
		
		for (Layer layer : layers) {
			if ( layer instanceof USGSTopographicMaps 
					|| layer instanceof LandsatI3)
				layer.setEnabled(false);
		}
		
		// Add LatLon pointer layer
		layers.add(new PositionLayer(canvas));
		
		//getLayers().removeAll(getLayers());
		
		// Feng GUI user interface layer
		uiLayer = new MasterUILayer(canvas); 
		uiLayer.setEnabled(true);
        
        getLayers().add(uiLayer);
	}
	
	public void toogleGUI() 
	{
		uiLayer.setEnabled(!uiLayer.isEnabled());
	}
}
