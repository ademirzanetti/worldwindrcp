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
package org.eclipse.plugin.worldwind.views;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.util.Vector;

import javax.media.opengl.GLException;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.views.LayersView.TreeObject;
import org.eclipse.plugin.worldwind.views.LayersView.TreeParent;
import org.eclipse.swt.widgets.Composite;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.NASAWMSLayerList;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

/**
 * CheckedBoxTreeViewer with logic to add/remove nodes + track the
 * check state of each node
 * @author Owner
 *
 */
public class LayersTree extends CheckboxTreeViewer
{
	private static final Logger logger = Logger.getLogger(LayersTree.class);
	
	// Nodes checked by default
	private Vector<TreeObject> checkedTreeNodes = new Vector<TreeObject>();

	
	public LayersTree(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * Initialize tree contents
	 */
	void initialize() {
		setInput(getInitialInput());
	}
	
    /*
     * Load tree data
     */
	private TreeParent getInitialInput () 
	{
		// the root tree node (invisible)
		RenderableLayer rl = new RenderableLayer();
		rl.setName("invisible");
		
		TreeParent invisibleRoot = new TreeParent(rl, null);

		// Add layers
		invisibleRoot.addChild(buildDefaultWorldWindLayers());
		invisibleRoot.addChild(buildNASAWMSLayers());

		return invisibleRoot;
	}
	
	/**
	 * Used to preserve the check state of a node when the tree is refreshed 
	 * @param to
	 * @param checked
	 */
	void trackCheckState (TreeObject to, boolean checked)
	{
		if ( checked ) 
			checkedTreeNodes.add(to);
		else
			checkedTreeNodes.remove(to);
		
		// children?
		if  (to instanceof TreeParent && ((TreeParent)to).hasChildren()) 
		{
			TreeObject[] children = ((TreeParent)to).getChildren();
			
			for (TreeObject treeObject : children) 
			{
				if ( checked )
					checkedTreeNodes.add(treeObject);
				else
					checkedTreeNodes.remove(treeObject);
			}
		}
	}

	/*
	 * Convert checkedTreeNodes to an array of TreeObject[] to set checked items on the
	 * tree viewer
	 */
	private TreeObject[] checkedNodesToArray() {
		return checkedTreeNodes.toArray(new TreeObject[checkedTreeNodes.size()]); 
	}
	
	void updateCheckState () {
		setCheckedElements(checkedNodesToArray());
	}

	
	/**
	 * Add a layer to the view
	 * @param layer {@link NavigatorLayer} parent object to add to the view
	 * @param children Array of {@link NavigatorLayer} children
	 * @param addToModel Add layers to the World Wind model so they will render
	 */
	public void addTreeObject(TreeParent parent
			, TreeObject[] children
			, boolean addToModel
			, boolean checked) 
	{
		// set parent visibility
		parent.setEnabled(checked);
		
		// track parent check state on the tree
		trackCheckState(parent, checked);
		
		if ( children != null ) 
		{
			for (TreeObject child : children) 
			{
				// set child visibility & check state
				child.setEnabled(checked);
				trackCheckState(child, checked);
				
				logger.debug("Adding child:" + child.getName() + " to the tree");
				parent.addChild(child);
				
				// Render? The parent should not be added cuz it has nothing to render
				if ( addToModel ) {
					logger.debug("Adding child to WW render model " + child.getName() );
					EarthView.world.getModel().getLayers().add(child.getLayer());					
				}
			}
		}
		
		// Add only single Layers
		// AnimatedGroundOverlay(s) are not added to the model for performance
		if ( ! (parent.getLayer() instanceof TimeLoopGroundOverlay) 
				&& addToModel && children == null)
		{
			logger.debug("Adding top layer to WW render model " + parent.getName() );
			EarthView.world.getModel().getLayers().add(parent.getLayer());					
		}
		
		// Add the TreeObject to the Tree Viewer
		// Get the root object
		TreeParent root = (TreeParent)getInput();
		
		// Add the new object as a child of root 
		root.addChild(parent);

		// Set the input back
		setInput(root);
		
		// Refresh the tree. NOTE: the checked state of items is not preserved!
		refresh(root);
		
		// refresh the checked state of nodes
		setCheckedElements(checkedNodesToArray());
	}
	
	/**
	 * Remove a layer from the view
	 * @param nodes Array of Object[]. Instances of either {@link TreeObject}
	 * or {@link TreeParent} 
	 */
	public void removeTreeObject(Object[] nodes) 
	{
		try {
			for (Object object : nodes) 
			{
				logger.debug("Removing object " + object );
				
				// if single node
				if ( object instanceof TreeObject) {
					((TreeObject)object).getLayer().setEnabled(false);
					((TreeObject)object).getLayer().dispose();
				}
				
				// If parent, dispose of children
				if ( object instanceof TreeParent) 
				{
					TreeObject[] children = ((TreeParent)object).getChildren();
					
					logger.debug("Removing " + children.length + " children");
					
					for (TreeObject treeObject : children) {
						try {
							logger.debug("Disposing " + treeObject);
							treeObject.getLayer().setEnabled(false);
							treeObject.getLayer().dispose();
							
						} catch (GLException e) 
						{
							// GLException: No OpenGL context current on this thread?
							// Don't know what to do...
						}
					}
				}

				// remove from tree
				TreeParent root = (TreeParent)getInput();
				root.removeChild((TreeObject)object);
				
				setInput(root);
				refresh(root);

				// refresh the checked state of nodes
				setCheckedElements(checkedNodesToArray());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	/*
	 * Build Layers from NASA's WMS Server
	 */
	private TreeParent buildNASAWMSLayers()
	{
		NASAWMSLayerList nasaWmsLayerList = new NASAWMSLayerList();
    	nasaWmsLayerList.setAllEnabled(false);
		
		// NASA WMS Layers nasaWmsLayerList. Must be added to the model
		RenderableLayer topLayer = new RenderableLayer();
		topLayer.setName( Messages.getText("layer.nasa.wms.name"));
		
		TreeParent top = new TreeParent(topLayer, Activator.ICON_NASA);
		
	    for (Layer layer : nasaWmsLayerList)
	    {
	    	// Must add layer to both tree & world wind model
	    	EarthView.world.getModel().getLayers().add(layer);
	    	
	    	final TreeObject to = new TreeObject(layer, LayersView.guessIcon(layer.getName()));
	    	
	    	top.addChild(to);
	    }
	    
	    // self + children
	    top.setRemovable(false);
	    return top;
	}
	
	/*
	 * Add layers from the WW model to the tree. T
	 * These are built-in (cannot be removed) and enabled by default
	 */
	private TreeParent buildDefaultWorldWindLayers()
	{
		// Initialize model
		//EarthView.initWorldWindLayerModel();
		
        // Add World Wind built-in layers (all enabled)
		LayerList layerList = EarthView.world.getModel().getLayers();
       

        // WW parent layer for Model layers
        RenderableLayer topLayer = new RenderableLayer();
		topLayer.setName(Messages.getText("layer.nasa.builtin.name"));
		
        // Tree parent that contains 1 TreeObject for each WW layer
		TreeParent parent = new TreeParent(topLayer, Activator.ICON_NASA);
		
		// 1 TreeObject for each WW Layer
		//for ( TreeObject to : defaultLayers)
		for (Layer layer : layerList) 
		{
			final TreeObject to = new TreeObject(layer
					, LayersView.guessIcon(layer.getName())	// Tree icon
					);
			parent.addChild(to);
		}
		
		// self + children
		parent.setRemovable(false);
		
		trackCheckState(parent, true); 
		
		return parent;
	}
	
	/**
	 * Add a rime loop overlay to the tree
	 */
	void addTimeLoopGroundOverlays(TimeLoopGroundOverlay[] ovs) 
	{
		for (TimeLoopGroundOverlay aov : ovs) 
		{
			TreeParent parent = new TreeParent(aov, LayersView.guessIcon(aov.getName()));
			
			// View children
			TreeObject[] children = new TreeObject[aov.getOverlays().size()];
			
			// Children as ground overlays
			GroundOverlayLayer[] childOvs = new GroundOverlayLayer[aov.getOverlays().size()];
			
			aov.getOverlays().toArray(childOvs);
			
			for (int i = 0; i < children.length; i++) {
				children[i] = new TreeObject(childOvs[i]
						, LayersView.guessIcon(childOvs[i].getName())
						);
			}
			
			// Add to view
			addTreeObject(parent, children, false, false);
		}
	}
	
}
