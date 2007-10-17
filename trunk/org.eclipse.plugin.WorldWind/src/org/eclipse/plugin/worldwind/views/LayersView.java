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


import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;


import org.apache.log4j.Logger;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.core.runtime.IAdaptable;

import worldwind.contrib.LayerUtils;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay.GroundOverlayLoopListener;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;

import org.eclipse.plugin.worldwind.operation.AnimationJob;
import org.eclipse.plugin.worldwind.operation.LayerLoaderJob;
import org.eclipse.plugin.worldwind.utils.LayersToolTipSupport;
import org.eclipse.plugin.worldwind.views.EarthView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Layer Navigator View 
 * @author Vladimir Silva
 *
 */
public class LayersView extends ViewPart
	implements GroundOverlayLoopListener
{
	private static final Logger logger = Logger.getLogger(LayersView.class);
	
	static public String ID = LayersView.class.getName();
	
	private LayersTree treeViewer;

	// Actions for TimeLoopGrounOverlay player
	private Action actionPlay;
	private Action actionStop;
//	private Action actionPause;

	// remove a node from the tree
	private Action actionRemoveNode;
	
	// node double click
	private Action doubleClickAction;
	
	private ImageDescriptor ICON_PLAY = Activator.getImageDescriptor("icons/16x16-play.png");
	private ImageDescriptor ICON_STOP = Activator.getImageDescriptor("icons/16x16-stop.png");
	
	// Status line
	private StatusLine statusLine;
	
	private LayersToolTipSupport tipSupport;
	
	// A hashtable to track animations
	private ConcurrentHashMap<String, AnimationJob> animatedJobs 
		= new ConcurrentHashMap<String, AnimationJob>();
	
	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	public static class TreeObject implements IAdaptable {
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
	
	public static class TreeParent extends TreeObject 
	{
		private ArrayList<TreeObject> children;
		
		public TreeParent(Layer layer) { 
			super(layer);
			children = new ArrayList<TreeObject>();
		}
		public TreeParent(Layer layer, Image image) { 
			super(layer, image);
			children = new ArrayList<TreeObject>();
		}
		
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}
		public void clearChildren() {
			children.clear();
		}
		public TreeObject [] getChildren() {
			return (TreeObject [])children.toArray(new TreeObject[children.size()]);
		}
		public boolean hasChildren() {
			return children.size()>0;
		}
		
		public void setRemovable(boolean removable) {
			super.setRemovable(removable);
			for (TreeObject to : children) {
				to.setRemovable(removable);
			}
		}
	}

	/*
	 * Provides content to the layers tree
	 */
	private class LayersContentProvider 
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
	class LayersLabelProvider extends CellLabelProvider {

//		public String getText(Object obj) {
//			return  obj.toString();
//		}
		
//		public Image getImage(Object obj) {
//			Image image = ((TreeObject)obj).getImage();
//			return image;  
//		}
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
			final Layer layer = ((TreeObject)element).getLayer();
			final String description = (layer instanceof TimeLoopGroundOverlay)
				? ((TimeLoopGroundOverlay)layer).getDescription()
				: ((TreeObject)element).toString(); //layer.toString();
			
				
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
	
//	class NameSorter extends ViewerSorter {
//	}

	/**
	 * The constructor.
	 */
	public LayersView() {
	}

	
	/*
	 * Process a check state event
	 */
	private void handleCheckState (boolean checked, TreeObject to)
	{
		to.setEnabled(checked);

		// WW Layer
		Layer layer = to.getLayer();
	  
		logger.debug("Layer " + layer.getName() + " type=" + layer.getClass().getName() );

		// The real time weather node has too many layers. it cannot be checked
		// Only its children can be checked
		if ( layer.getName().equalsIgnoreCase(Messages.getText("remote.layers.tree.lbl")))
		{
			Messages.showErrorMessage(getViewSite().getShell()
					, Messages.getText("err.dialog.title")
					, Messages.getText("err.msg.realtime.sat", new Object[] { layer.getName()} ));

			treeViewer.setChecked(to, false);
			return;
		}
		
		// check all node children
		treeViewer.setSubtreeChecked(to, checked);
	  
		// Animated Overlay, play it
		if ( layer instanceof TimeLoopGroundOverlay ) 
		{
			final TimeLoopGroundOverlay overlay = (TimeLoopGroundOverlay)layer;

			if ( checked ) 
			{
				overlay.addToModel(EarthView.world);
				//overlay.asynchFetchSome();
				
				// Listen for loop updates
				overlay.addLoopListener(this);
				//overlay.play(); 

				Display display = getViewSite().getShell().getDisplay();
				
				// Use an eclipse animation job for smoothness
				// It won't hang the UI
				AnimationJob job = new AnimationJob(display, overlay, statusLine);
				
				// Save the job, so it can be stopped
				animatedJobs.put(to.getID(), job);
				
				logger.debug("Starting animated job " + job + " id=" + to.getID());
				job.play();
			}
			else { 
				//overlay.stop();
				overlay.removeLoopListener(this);
				
				// Get job from pool
				AnimationJob job = animatedJobs.get(to.getID());
				job.stop();

				logger.debug("Stopped animated job " + job + " id=" + to.getID());
				animatedJobs.remove(to.getID());
			}
		}
		// If layer has children
		else if ( to instanceof TreeParent && ((TreeParent)to).hasChildren() )
		{
			logger.debug("Layer w/ children parent=" + to.getParent() );
			
			// Grab parent layer. 
			Layer parent = (to.getParent() != null ) 
				? to.getParent().getLayer()
				: null ;
			
			// if parent is an animated overlay (set child visibility)
			if ( parent != null && parent instanceof TimeLoopGroundOverlay ) 
			{
				((TimeLoopGroundOverlay)parent).setVisible(layer, checked);
			}
			// enable/disable respective children layers
			else  
			{
				TreeObject[] children = ((TreeParent)to).getChildren();
				
				if ( children != null) 
				{
					logger.debug("Children size=" + children.length);
					for (int i = 0; i < children.length; i++) 
					{
						logger.debug("Setting visibility for " + children[i] + " to " + checked);
						
						children[i].setEnabled(checked);
						
						// is the child a TimeLoopGroundOverlay?
						if ( children[i].getLayer() instanceof TimeLoopGroundOverlay)
							handleCheckState(checked, children[i]);
					}
				}
			}
		}
		// leaf layer
		else 
		{
			logger.debug("Leaf layer "+ layer.getName());
			
			if ( layer instanceof GroundOverlayLayer)
				((GroundOverlayLayer)layer).addOverlayListener(this);
			
			// set tree check state
			to.setEnabled(checked);
		}

		// repaint WW
		EarthView.world.repaint();
		
		// check state not preserved when adding/removing nodes
		treeViewer.trackCheckState(to, checked);
	}
	
	
	/**
	 * This is a callback that will allow us
	 * to create the treeViewer and initialize it.
	 */
	public void createPartControl(Composite parent) 
	{
		// When the parent is disposed, save tree layers on WW cache
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				saveLayers();
			}
		});
		
		//treeViewer = new CheckboxTreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer = new LayersTree(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		//drillDownAdapter = new DrillDownAdapter(treeViewer);
		
		treeViewer.setContentProvider(new LayersContentProvider());
		treeViewer.setLabelProvider(new LayersLabelProvider());
		//treeViewer.setSorter(new NameSorter());
		//treeViewer.setInput(getInitialInput());
		treeViewer.initialize();
		
		// Tooltip support
		tipSupport = LayersToolTipSupport.enablefor(treeViewer
				, ToolTip.NO_RECREATE
				, getViewSite().getWorkbenchWindow());
		
		// When user checks a checkbox in the tree, check all its children
	    treeViewer.addCheckStateListener(new ICheckStateListener() {
	      public void checkStateChanged(CheckStateChangedEvent event) 
	      {
	    	  boolean checked = event.getChecked(); 
	    	  
	    	  // check node
	    	  TreeObject to = (TreeObject)event.getElement();
	    	  
	    	  // handle state
	    	  handleCheckState(checked, to);
	      }
	    });
		
		makeActions();
//		hookContextMenu();
		hookClickAction();
		contributeToActionBars();
		
		// load local layers from cache
		loadLayers();

		// Set initially checked layers
		treeViewer.updateCheckState();
		
		// initialize status line
		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine();
		
		// load remote real time weather layers 
		LayerLoaderJob job = new LayerLoaderJob(getViewSite().getShell().getDisplay()
				, treeViewer
				, statusLine );
		
		job.setUser(true);
		job.schedule();
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

	
/*	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				LayersView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}
*/
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		//fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}
	
/*
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(actionPlay);
		manager.add(new Separator());
		manager.add(actionStop);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(actionPlay);
		manager.add(actionStop);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
*/
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionPlay);
		// manager.add(actionPause);
		manager.add(actionStop);
		manager.add(new Separator());
		manager.add(actionRemoveNode);
		
		//drillDownAdapter.addNavigationActions(manager);
	}


	/* View local actions */
	private void makeActions() 
	{
		// Play an animated overlay
		actionPlay = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection )treeViewer.getSelection();
				TreeObject to = (TreeObject) selection.getFirstElement();
				
				// only TimeLoopGroundOverlay support animation
				if ( ! (to.getLayer() instanceof TimeLoopGroundOverlay) ) 
				{
					Messages.showErrorMessage(getViewSite().getShell()
							, Messages.getText("err.dialog.title")
							, Messages.getText("err.msg.no.ago", new Object[] {to.toString()}));
				}
				else {
					handleCheckState(true, to);
				}
			}
		};
		actionPlay.setToolTipText(Messages.getText("layer.action.play"));
		actionPlay.setImageDescriptor(ICON_PLAY);
		
		/*
		actionPause = new Action() {
			public void run() {
				System.out.println("Pause");
			}
		};
		actionPause.setToolTipText(Messages.getText("layer.action.pause"));
		actionPause.setImageDescriptor(
				Activator.getImageDescriptor("icons/player_pause.png")
			);
		
		*/
		
		// Stop animated overlay
		actionStop = new Action() {
			public void run() 
			{
				TreeObject to = (TreeObject)((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
				
				if ( ! (to.getLayer() instanceof TimeLoopGroundOverlay) ) {
					Messages.showErrorMessage(getViewSite().getShell()
							, Messages.getText("err.dialog.title")
							, Messages.getText("err.msg.no.ago", new Object[] {to.toString()}));
				}
				else {
					handleCheckState(false, to);
				}
			}
		};
		actionStop.setToolTipText(Messages.getText("layer.action.stop"));
		actionStop.setImageDescriptor(ICON_STOP);
		
		// Remove a not built-in tree node
		actionRemoveNode = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				
				TreeObject to = (TreeObject) selection.getFirstElement();
				
				final String name = to.getLayer().getName();

				// built-in layers cannot be removed from the tree
				if ( ! to.isRemovable()) { // LayersTree.isBuiltinLayer(name)) {
					Messages.showErrorMessage(getViewSite().getShell()
							, Messages.getText("err.dialog.title")
							, Messages.getText("err.msg.builtin.layer", new Object[] {name}));
					return;
				}
				
				// remove node
				treeViewer.removeTreeObject(selection.toArray());
			}
		};
		
		actionRemoveNode.setToolTipText(Messages.getText("layer.action.remove"));
		actionRemoveNode.setImageDescriptor(
				Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_DELETE));

		// Tree double click action
		doubleClickAction = new Action() {
			public void run() 
			{
				ISelection selection = treeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				
				// move globe position to layer centroid
				LayerUtils.moveViewTo(EarthView.world.getView()
						, EarthView.world.getModel().getGlobe()
						, ((TreeObject)obj).getLayer());
			}
		};
		
	}
	

	/**
	 * On click move globe to the centroid of the BBOX dataset
	 */
	private void hookClickAction () {
//		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				doubleClickAction.run();
//			}
//		});
		
		treeViewer.getTree().addSelectionListener(new SelectionListener() 
		{
			public void widgetDefaultSelected(SelectionEvent e) {
				doubleClickAction.run();
			}

			public void widgetSelected(SelectionEvent e) {
				doubleClickAction.run();
			}
		});
	}
	
	/**
	 * Passing the focus request to the treeViewer's control.
	 */
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	/******************************************************
	 * Ground Overlay listeners
	 ******************************************************/
	
	/**
	 * Fires on ground overlay error
	 */
	public void onError(GroundOverlayLayer layer, Exception ex) 
	{
		//ex.printStackTrace();
		
		final String message = layer.getName() + ": " 
			+ ex.getClass() + " " + ex.getMessage();
		
		Display display = getViewSite().getShell().getDisplay();
		
		display.syncExec(new Runnable() {
			public void run() {
				statusLine.setErrorMessage(message);
			}
		});
		
	}

	/**
	 * fires when an animated ground overlay loops thru
	 */
	public synchronized void statusChanged(final int current, final int total, final GroundOverlayLayer layer) 
	{
		if ( getViewSite() == null || getViewSite().getShell() == null ) return;
		
		Display display = getViewSite().getShell().getDisplay();
		
        display.syncExec(new Runnable() {
        	public void run() {
        		String message = layer + " ("  + current + "/" + total + ")" ;
        		statusLine.setLoopStatusMessage(message);
        	}
        });
	}
	
	/**
	 * Add a World Wind Layer to the view 
	 */
	public void addOverlays (Layer[] layers, boolean enabled) 
	{
		if ( layers == null ) return;
		
		Layer first = layers[0];
		
		// Time Loop layer
		if ( first instanceof TimeLoopGroundOverlay) {
			addTimeLoopGroundOverlays((TimeLoopGroundOverlay[])layers);
			return;
		}
		// Ground overlays
		else if ( first instanceof GroundOverlayLayer) {
			for (Layer layer : layers) {
				addGroundOverlay((GroundOverlayLayer)layer, enabled);
			}
		}
		// regular layer
		else {
			statusLine.setErrorMessage("Invalid layer type: " + first.getClass().getName());
		}
	}

	/**
	 * Add a World Wind {@link Layer} to the tree
	 * @param layer
	 * @param icon
	 * @param enabled
	 */
	public void addLayer (Layer layer, Image icon, boolean enabled) {
		layer.setEnabled(enabled);
		treeViewer.addTreeObject(new TreeParent(layer, icon	)
				, null
				, true		// Make lare renderable
				, enabled); // checked
		
	}
	
	/**
	 * Add a rime loop overlay to the tree
	 */
	private void addTimeLoopGroundOverlays(TimeLoopGroundOverlay[] ovs) 
	{
		for (TimeLoopGroundOverlay aov : ovs) 
		{
			TreeParent parent = new TreeParent(aov, guessIcon(aov.getName()));
			
			// View children
			TreeObject[] children = new TreeObject[aov.getOverlays().size()];
			
			// Children as ground overlays
			GroundOverlayLayer[] childOvs = new GroundOverlayLayer[aov.getOverlays().size()];
			
			aov.getOverlays().toArray(childOvs);
			
			for (int i = 0; i < children.length; i++) {
				children[i] = new TreeObject(childOvs[i]
						, guessIcon(childOvs[i].getName())
						);
			}
			
			// Add to view
			treeViewer.addTreeObject(parent, children, false, false);
		}
	}

	/**
	 * Add a {@link GroundOverlayLayer} to the View tree
	 * @param overlay {@link GroundOverlayLayer}
	 */
	private void addGroundOverlay (GroundOverlayLayer overlay, boolean enabled) 
	{
		TreeParent parent = 
			new TreeParent(overlay
					, guessIcon(overlay.getName())
		            );
		
		treeViewer.addTreeObject(parent, null, true, enabled);
	}
	
	/**
	 * Add a tiled WMS (usually 1.1.x - no time dimension) layer to the view
	 * @param parentName Name of the tree parent node
	 * @param layers Tiles WMS layers
	 */
	public void addTiledWMSLayers( String parentName, TiledWMSLayer[] layers, boolean enabled) 
	{
		RenderableLayer top = new RenderableLayer();
		top.setName(parentName);
		
		// Parent
		TreeParent parent = new TreeParent (top
				, guessIcon(parentName) );
		
		// Children
		TreeObject[] children = new TreeObject[layers.length];
		
		for (int i = 0; i < children.length; i++) 
		{
			layers[i].setEnabled(enabled);
			
			children[i] = new TreeObject( layers[i] , null);
		}
		
		// Add to view
		treeViewer.addTreeObject(parent, children, true, enabled);
	}

	/**
	 * Add a kml/kmz source. All layers regardless of protocol are cached as KML in
	 * WW cache.
	 * @param kml The {@link KMLSource} object to store
	 */
	public void addKMLSource (KMLSource kml, boolean enabled) 
	{
		String displayName 	= kml.getDocument().getName();
		LayerList list 		= kml.toLayerList();

		// If the doc has more than 1 ground overlay, the layer will be
		// displayed as a TimeLoop overlay. 
		// There should be a better way of doing this...
		if ( kml.groundOverlaysSize() > 1) {
			addTimeLoopGroundOverlays(
					new TimeLoopGroundOverlay[] { 
							KMLSource.toTimeLoopGroundOverlay(kml.getDocument()) 
							} 
					);
		}
		else {
			// Otherwise other elements: Screen Ovs & placemarks will be composed
			// as children of a top element
			RenderableLayer layer = new RenderableLayer();
			
			layer.setName(displayName);
			layer.setValue("KMLSOURCE", kml.toKML());
			
			TreeParent top = new TreeParent(layer
					, guessIcon(displayName)
					);
			
			// Add list layers to children (of top)
			int childrenSize = list.size(); 
			
			TreeObject[] children = new TreeObject[childrenSize];
	
			Iterator<Layer> iter =  list.iterator();
	
			int i = 0;
			while ( iter.hasNext()) 
			{
				 final Layer child = iter.next();
				 
				 child.setEnabled(enabled);
				 children[i++] = new TreeObject(child
						 , guessIcon(child.getName())
						 );
				 
			}
			treeViewer.addTreeObject(top, children, true, enabled);
		}
	}
	
	/**
	 * Recurse tree layers extracting kml. All layers (local remote) are
	 * stored as KML.
	 * @param topLayers
	 * @param buf
	 */
	private void getKML ( TreeObject[] topLayers, StringBuffer buf)
	{
		for (TreeObject treeObject : topLayers) 
		{
			final Layer layer = treeObject.getLayer();
	
			// Layers generated from KML URL store the XML internally
			final String kml = (String)layer.getValue("KMLSOURCE");
			
			
			// This is a layer created from a remote KML doc
			if ( kml != null ) {
				buf.append("<kml>" + Messages.NL
						+ kml
						+ "</kml>" + Messages.NL);
			}
			else if ( layer instanceof TimeLoopGroundOverlay) {
				buf.append("<kml>" + Messages.NL 
						+ ((TimeLoopGroundOverlay)layer).toKML() + Messages.NL
						+ "</kml>" + Messages.NL);
			}
			else if (layer instanceof GroundOverlayLayer 
					&& ! (treeObject.getParent().getLayer() instanceof TimeLoopGroundOverlay) ) 
			{
				buf.append("<kml>" + Messages.NL
						+ ((GroundOverlayLayer)layer).toKML() + Messages.NL
						+ "</kml>" + Messages.NL);
			}
			
			// if parent w/ children and not built in
			if ( treeObject instanceof TreeParent 
					&& ((TreeParent)treeObject).hasChildren()
					&&  treeObject.isRemovable() ) // ! LayersTree.isBuiltinLayer(layer.getName())) 
			{
				getKML(((TreeParent)treeObject).getChildren(), buf);
			}
		}
	}
	
	/**
	 * Cache tree layers to WW default cache location. All layers are saved as KML.
	 * Only {@link TimeLoopGroundOverlay} and {@link GroundOverlayLayer}
	 * are cached
	 */
	private void saveLayers() 
	{
		TreeObject[] topLayers = ((TreeParent)treeViewer.getInput()).getChildren();
		try {
			StringBuffer buf = new StringBuffer("<xml>" + Messages.NL);
			
			getKML(topLayers, buf);
			
			buf.append("</xml>");

			// save XML in WW cache folder
			File file = WorldWind.getDataFileCache().newFile("layers.xml");
			
			worldwind.contrib.Messages.writeToFile(file, buf.toString().getBytes());
		} catch (Exception e) {
			// Unable to save file!....Why?
			e.printStackTrace();
		}
	}
	
	/**
	 * Load layers from WW default cache. All layers stored as KML
	 */
	private void loadLayers() {
		try {
			// Read kml from cache
			URL url 	= WorldWind.getDataFileCache().findFile("layers.xml", false);
			
			if ( url == null ) return;
			
			File file 	= new File(url.toURI());
			String xml 	= new String(worldwind.contrib.Messages.readFile(file));
			
			// Each <kml> element represents a WW layer
			Document doc 	= ParserUtils.parse(new ByteArrayInputStream(xml.getBytes()));
			NodeList nl 	= doc.getElementsByTagName("kml");

			// loop thru layers
			for (int i = 0; i < nl.getLength(); i++) 
			{
				// convert kml to a WW layer list
				KMLSource kml 	= new KMLSource("doc.kml", (Element) nl.item(i));
				
				addKMLSource(kml, false);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setStausMessage(String message){
		statusLine.setMessage(message);
	}
	
	public void setStatusErrorMessage (String message) {
		statusLine.setErrorMessage(message);
	}
}

