package org.bluemarble.util;

import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

public class LayerSet extends RenderableLayer 
{
	LayerList list;
	
	public LayerSet(String name, Layer[] layers) {
		super();
		list = new LayerList(layers);
		setName(name);
	}

	public LayerSet(String name, LayerList layers) {
		super();
		list = layers;
		setName(name);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		for (Layer layer : list) {
			layer.setEnabled(enabled);
		}
	}
	
	public void addToModel (WorldWindowGLCanvas canvas){
		for (Layer layer : list) {
			if ( layer instanceof TimeLoopGroundOverlay)
				((TimeLoopGroundOverlay)layer).addToModel(canvas);
			else
				canvas.getModel().getLayers().add(layer);
		}
	}
}
