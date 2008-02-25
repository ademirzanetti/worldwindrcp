package org.bluemarble.util;

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
		for (Layer layer : list) {
			layer.setEnabled(enabled);
		}
	}
}
