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
package org.eclipse.plugin.worldwind.views.tree;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.Earth.BMNGOneImage;
import gov.nasa.worldwind.layers.Earth.BMNGSurfaceLayer;
import gov.nasa.worldwind.layers.Earth.LandsatI3;
import gov.nasa.worldwind.layers.TerrainProfileLayer;
import gov.nasa.worldwind.layers.Earth.USGSDigitalOrtho;
import gov.nasa.worldwind.layers.Earth.USGSTopoHighRes;
import gov.nasa.worldwind.layers.Earth.USGSTopographicMaps;
import gov.nasa.worldwind.layers.Earth.USGSUrbanAreaOrtho;

import java.util.Vector;

import javax.media.opengl.GLException;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.utils.LayersToolTipSupport;
import org.eclipse.plugin.worldwind.views.EarthView;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISharedImages;

import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.ScreenOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import org.eclipse.plugin.worldwind.contrib.layers.quadkey.VirtualEarthLayer;

/**
 * CheckedBoxTreeViewer with logic to add/remove nodes + track the
 * check state of each node
 * @author Owner
 *
 */
public class LayersTreeViewer extends CheckboxTreeViewer
{
	private static final Logger logger = Logger.getLogger(LayersTreeViewer.class);
	
	// Nodes checked by default
	private Vector<TreeObject> checkedTreeNodes = new Vector<TreeObject>();

	
	/*
	 * Provides content to the layers tree
	 */
	public static class LayersContentProvider 
		implements IStructuredContentProvider, ITreeContentProvider 
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject)child).getParent();
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent)parent).getChildren();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent)parent).hasChildren();
			return false;
		}

	}
	
	/*
	 * Provides Labels/images to the tree
	 */
	public static class LayersLabelProvider extends CellLabelProvider 
	{
		LayersToolTipSupport tipSupport;

		public void setTipSupport (LayersToolTipSupport tipSupport ) {
			this.tipSupport = tipSupport;
		}
		
		@Override
		public Image getToolTipImage(Object object) {
			return ((TreeObject)object).getImage();
		}

		public void update(ViewerCell cell) {
			cell.setText(cell.getElement().toString());
		}
		
		@Override
		public String getToolTipText(Object element) 
		{
			final Layer layer 	= ((TreeObject)element).getLayer();
			
			// Layer cannot be NULL
			if ( layer == null ) 
				return null;
			
			String description 	= layer.getName(); // ((TreeObject)element).toString();
			
			// Get layer description: only in GroundOverlay & TimeLoop Ovs
			if ( layer instanceof TimeLoopGroundOverlay )
				description = ((TimeLoopGroundOverlay)layer).getDescription();

			if ( layer instanceof GroundOverlayLayer )
				description = ((GroundOverlayLayer)layer).getDescription();
				
			// if null use layer name
			if (description == null ) 
				description = layer.getName();
			
			final String toolTip = "<html>" 
				+ "<style>body, table {font-family:Arial;font-size=12px;background-color='#FFFFCC'}"
				+ "</style>"
				+ "<body bgcolor='#FFFFCC'>" 
				+ description 
				+ "</body></html>";

			if ( tipSupport != null) 
			{
				// very crappy tip size logic
				final int len = description.length();
				int w = 200 , h = 100;
				
				if ( len > 30  && len < 100) { w = 300; h = 200;} 
				else if ( len > 100 && len < 200 ){ w = 300; h = 300;}
				else if ( len > 200 ){ w = 600; h = 400;}
				
				tipSupport.setTipSize(w, h);
			}
			return toolTip;
		}
		
	}
	
	public LayersTreeViewer(Composite parent, int style) {
		super(parent, style);
	}

	public LayersTreeViewer(Tree tree) {
		super(tree);
	}
	
	/**
	 * Initialize tree contents
	 */
	public void initialize() {
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
		// NASA WMS Server http://worldwind21.arc.nasa.gov/ off line since Feb 2008
		// invisibleRoot.addChild(buildNASAWMSLayers());
		invisibleRoot.addChild(buildOtherLayers());
		
		return invisibleRoot;
	}
	
	/**
	 * Used to preserve the check state of a node when the tree is refreshed 
	 * @param to
	 * @param checked
	 */
	public void checkNode (TreeObject to, boolean checked)
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
	
	public void updateCheckState () {
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
		checkNode(parent, checked);
		
		if ( children != null ) 
		{
			for (TreeObject child : children) 
			{
				// set child visibility & check state
				child.setEnabled(checked);
				checkNode(child, checked);
				
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
	
	/**
	 * Build Layers from NASA's WMS Server
	 */
//	private TreeParent buildNASAWMSLayers()
//	{
//		NASAWMSLayerList nasaWmsLayerList = new NASAWMSLayerList();
//    	nasaWmsLayerList.setAllEnabled(false);
//		
//		// NASA WMS Layers nasaWmsLayerList. Must be added to the model
//		RenderableLayer topLayer = new RenderableLayer();
//		topLayer.setName( Messages.getString("layer.nasa.wms.name"));
//		
//		TreeParent top = new TreeParent(topLayer, Activator.ICON_NASA);
//		
//	    for (Layer layer : nasaWmsLayerList)
//	    {
//	    	// Must add layer to both tree & world wind model
//	    	EarthView.world.getModel().getLayers().add(layer);
//	    	
//	    	final TreeObject to = new TreeObject(layer, guessIcon(layer.getName()));
//	    	
//	    	top.addChild(to);
//	    }
//	    
//	    // self + children
//	    top.setRemovable(false);
//	    return top;
//	}
	
	/*
	 * Others such as MS Virtual Earth (disabled by default)
	 */
	private TreeParent buildOtherLayers() 
	{
		VirtualEarthLayer layer =  new VirtualEarthLayer();
		
		// MS VE Logo
//		ScreenOverlayLayer logo = new ScreenOverlayLayer("MS VE Logo"
//				, "worldwind/contrib/layers/quadkey/logo_msve.png"
//				, ScreenOverlayLayer.SOUTHWEST);
//		
//		logo.setIconScale(0.5);
//		layer.setLogo(logo);
		layer.setEnabled(false);
		
		// add to ww model (so it will render)
		EarthView.world.getModel().getLayers().add(layer);
		
		TreeParent top = new TreeParent(layer, null);
	    top.setRemovable(false);
	    return top;
	}
	
	/**
	 * Add layers from the WW model to the tree. 
	 * These are built-in (cannot be removed) and enabled by default
	 */
	private TreeParent buildDefaultWorldWindLayers()
	{
		
        // Add World Wind built-in layers (all enabled)
		LayerList layerList = EarthView.world.getModel().getLayers();

        // WW parent layer for Model layers
        RenderableLayer topLayer = new RenderableLayer();
		topLayer.setName(Messages.getString("layer.nasa.builtin.name"));
		
        // Tree parent that contains 1 TreeObject for each WW layer
		TreeParent parent = new TreeParent(topLayer, Activator.ICON_NASA);
		
		// 1 TreeObject for each WW Layer
		for (Layer layer : layerList) 
		{
			final TreeObject to = new TreeObject(layer
					, guessIcon(layer.getName())	// Tree icon
					);
			
			parent.addChild(to);

			checkNode(to, true);
			
			/**
			 * Turn off BM One image on Win/Mac 
			 */
			if ( layer instanceof BMNGOneImage ) {
				if ( Messages.isWindows() || Messages.isMacOSX()) {
					layer.setEnabled(false);
					checkNode(to, false);
				}
				continue;
			}
			
			/**
			 * Turn off BMNG on Linux (No support for compressed textures by default) 
			 */
			if ( layer instanceof BMNGSurfaceLayer ) {
				if ( !Messages.isWindows() && !Messages.isMacOSX()) {
					layer.setEnabled(false);
					checkNode(to, false);
				}
				continue;
			}
			
			/**
			 * These layers use compressed textures which will crash old linuxes.
			 * Thus disabled by default
			 */
			if ( layer instanceof USGSDigitalOrtho 
					|| layer instanceof LandsatI3
					|| layer instanceof USGSUrbanAreaOrtho
					|| layer instanceof USGSTopographicMaps
					|| layer instanceof USGSTopoHighRes
					|| layer instanceof TerrainProfileLayer)
			{
				layer.setEnabled(false);
				checkNode(to, false);
			}
//			else
//				checkNode(to, true);
		}
		
		// self + children
		parent.setRemovable(false);
		
		// Check parent 
		checkedTreeNodes.add(parent);
		// enable all by default
		//checkNode(parent, true); 
		
		return parent;
	}
	
	/**
	 * Add a rime loop overlay to the tree
	 */
	public void addTimeLoopGroundOverlays(TimeLoopGroundOverlay[] ovs) 
	{
		for (TimeLoopGroundOverlay aov : ovs) 
		{
			TreeParent parent = new TreeParent(aov, guessIcon(aov.getName()));
			
			// # of frames in loop
			int overlaySize 			= aov.getOverlays().size();
			ScreenOverlayLayer legend 	= aov.getLegend();
			
			logger.debug("Adding time loop ov " + aov.getName() 
					+ " # of ground ovs=" + overlaySize 
					+ " Legend=" + legend);
			
			// Array of children to be added to the tree
			int childSize = (legend != null ) ? overlaySize + 1 : overlaySize;
			TreeObject[] children = new TreeObject[childSize];
			
			// Convert loop overlays to an array of objects
			GroundOverlayLayer[] childOvs = new GroundOverlayLayer[overlaySize];
			aov.getOverlays().toArray(childOvs);
			
			// Add loop frames to the tree array
			for (int i = 0; i < childOvs.length; i++) {
				children[i] = new TreeObject(childOvs[i]
						, guessIcon(childOvs[i].getName())
						);
			}
			
			// Add legend
			if ( legend != null)
				children[childSize - 1] = new TreeObject(legend);
				
			// Add to view
			addTreeObject(parent, children, false, false);
		}
	}
	
	/*
	 * Very simple logic to load an icon from a layer name
	 */
	static public final Image guessIcon(String layerName) 
	{
    	Image icon = null;
    	String name = layerName.toLowerCase();
    	
    	// set some icons
    	if ( name.indexOf("usgs") != -1 )
    		icon = Activator.ICON_USGS;
    	else if ( name.indexOf("us ") != -1)
    		icon = Activator.ICON_FLAG_US;
    	else if ( name.indexOf("nasa") != -1
    			|| name.indexOf("blue marble") != -1 
    			|| name.indexOf("compass") != -1)
    		icon = Activator.ICON_NASA;
    	else
    		icon = Activator.getSharedImage(ISharedImages.IMG_DEF_VIEW);
    	return icon;
	}

	
}
