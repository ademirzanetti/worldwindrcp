package org.eclipse.plugin.worldwind.views.tree;

import gov.nasa.worldwind.layers.Layer;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;

/*
 * The content provider class is responsible for
 * providing objects to the view. It can wrap
 * existing objects in adapters or simply return
 * objects as-is. These objects may be sensitive
 * to the current input of the view, or ignore
 * it and always show the same content 
 * (like Task List, for example).
 */
public class TreeObject implements IAdaptable {
	private Layer  layer;
	private TreeParent parent;
	private boolean checked;
	private Image image;
	
	// Unique node ID
	private String id;
	
	// All layers are removable by default from the tree
	// built-in layers cannot be removed.
	private boolean removable = true;
	
	public TreeObject( Layer layer) { 
		this.layer 	= layer;
		checked 	= layer.isEnabled();
		id 			= layer.getName() + "-" + System.currentTimeMillis();
	}
	
	public TreeObject( Layer layer, Image image) 
	{ 
		this.layer 	= layer;
		this.checked =layer.isEnabled(); 
		this.image 	= image;
		id 			= layer.getName() + "-" + System.currentTimeMillis();
	}
	
	public String getName() {
		return layer.getName();
	}
	public void setParent(TreeParent parent) {
		this.parent = parent;
	}
	public TreeParent getParent() {
		return parent;
	}
	public String toString() {
		return getName();
	}
	@SuppressWarnings("unchecked")
	public Object getAdapter(Class key) {
		return null;
	}
	public void setEnabled(boolean enabled) {
		layer.setEnabled(enabled);
	}
	public Image getImage () {
		return image;
	}
	public void setImage (Image image) {
		this.image = image;
	}
	public boolean getChecked () {
		return checked;
	}
	public Layer getLayer() {
		return layer;
	}
	public void setRemovable(boolean removable) {
		this.removable = removable;
	}
	public boolean isRemovable() {
		return removable;
	}
	public String getID () {
		return id;
	}
}

