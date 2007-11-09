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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.view.View;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.operation.AnimationJob;
import org.eclipse.plugin.worldwind.utils.LayersToolTipSupport;
import org.eclipse.plugin.worldwind.views.LayersView.LayersContentProvider;
import org.eclipse.plugin.worldwind.views.LayersView.LayersLabelProvider;
import org.eclipse.plugin.worldwind.views.LayersView.TreeObject;
import org.eclipse.plugin.worldwind.views.LayersView.TreeParent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.part.ViewPart;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import worldwind.contrib.LayerUtils;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;

/**
 * Places View
 * @author Vladimir Silva
 *
 */
public class PlacesView extends ViewPart
//	implements GroundOverlayLoopListener
{
	private static final Logger logger	= Logger.getLogger(PlacesView.class);
	static public String ID 			= PlacesView.class.getName();
	
	private LayersTree treeViewer;
	private LayersToolTipSupport tipSupport;

	// Status line
	private StatusLine statusLine;
	
	// A hashtable to track animations
	private ConcurrentHashMap<String, AnimationJob> animatedJobs 
		= new ConcurrentHashMap<String, AnimationJob>();

	/** Local Actions */
	
	// Local Actions for TimeLoopGrounOverlay player
	private Action actionPlay;
	private Action actionStop;

	// remove a node from the tree
	private Action actionRemoveNode;
	
	// Save layer: KMZ only
	private Action actionSaveLayer;
	
	// node double click
	private Action doubleClickAction;
	
	private ImageDescriptor ICON_PLAY = Activator.getImageDescriptor("icons/16x16-play.png");
	private ImageDescriptor ICON_STOP = Activator.getImageDescriptor("icons/16x16-stop.png");
	private ImageDescriptor ICON_SAVE = Activator.getImageDescriptor("icons/16x16-saveas.png");
	
	@Override
	public void createPartControl(Composite parent) 
	{
		// When the parent is disposed, save tree layers on WW cache
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				saveLayers();
			}
		});
		
		treeViewer = new LayersTree(parent, SWT.H_SCROLL | SWT.V_SCROLL);

		// Tooltip support
		LayersLabelProvider labelProvider = new LayersLabelProvider();
		
		// Tooltip support
		tipSupport = LayersToolTipSupport.enablefor(treeViewer
				, ToolTip.NO_RECREATE
				, getViewSite().getWorkbenchWindow());

		labelProvider.setTipSupport(tipSupport);
		
		treeViewer.setContentProvider(new LayersContentProvider());
		treeViewer.setLabelProvider(labelProvider);

		// init tree data
		treeViewer.setInput(getInitialInput());
		
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
		
		// initialize status line
		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine();
		
		makeActions();
		hookClickAction();
		contributeToActionBars();
		
		// load local layers from cache
		loadLayers();
		
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

			logger.debug("Loading local layers from " + url);
			
			// loop thru layers
			for (int i = 0; i < nl.getLength(); i++) 
			{
				// convert kml to a WW layer list
				KMLSource kml 	= new KMLSource("doc.kml", (Element) nl.item(i));
			
				logger.debug("Got KML source " + kml.toKML());
				addKMLSource(kml, false);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
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

		logger.debug("KML display name " + displayName + " enabled=" + enabled 
				+ " # of elements=" + list.size());
		
		// If the doc has more than 1 ground overlay, the layer will be
		// displayed as a TimeLoop overlay. 
		// There should be a better way of doing this...
		if ( kml.groundOverlaysSize() > 1) {
			treeViewer.addTimeLoopGroundOverlays( 
					new TimeLoopGroundOverlay[] { 
							KMLSource.toTimeLoopGroundOverlay(kml.getDocument()) 
							} 
					);
		}
		else if ( list.size() == 1) {
			final Layer child = list.iterator().next();
			treeViewer.addTreeObject(new TreeParent(child, LayersView.guessIcon(child.getName()))
					, null
					, true
					, enabled);
		}
		else {
			// Otherwise other elements: Screen Ovs & placemarks will be composed
			// as children of a top element
			RenderableLayer layer = new RenderableLayer();
			
			layer.setName(displayName);
			layer.setValue("KMLSOURCE", kml.toKML());
			
			TreeParent top = new TreeParent(layer
					, LayersView.guessIcon(displayName)
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
						 , LayersView.guessIcon(child.getName())
						 );
				 
			}
			treeViewer.addTreeObject(top, children, true, enabled);
		}
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


		return invisibleRoot;
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
				, LayersView.guessIcon(parentName) );
		
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
	 * Add a World Wind Layer to the view 
	 */
	public void addOverlays (Layer[] layers, boolean enabled) 
	{
		if ( layers == null ) return;
		
		Layer first = layers[0];
		
		// Time Loop layer
		if ( first instanceof TimeLoopGroundOverlay) {
			treeViewer.addTimeLoopGroundOverlays( (TimeLoopGroundOverlay[])layers);
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
	 * Add a {@link GroundOverlayLayer} to the View tree
	 * @param overlay {@link GroundOverlayLayer}
	 */
	private void addGroundOverlay (GroundOverlayLayer overlay, boolean enabled) 
	{
		TreeParent parent = 
			new TreeParent(overlay
					, LayersView.guessIcon(overlay.getName())
		            );
		
		treeViewer.addTreeObject(parent, null, true, enabled);
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
	
	/*
	 * Process a check state event
	 */
	private void handleCheckState (boolean checked, TreeObject to)
	{
		to.setEnabled(checked);

		// WW Layer
		Layer layer = to.getLayer();
	  
		logger.debug("Layer " + layer.getName() + " type=" + layer.getClass().getName() );

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
				//overlay.addLoopListener(this);
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
				//overlay.removeLoopListener(this);
				
				// Get job from pool
				AnimationJob job = animatedJobs.get(to.getID());
				if ( job != null) {
					logger.debug("Stopping animated job " + job + " id=" + to.getID());
					
					job.stop();
					animatedJobs.remove(to.getID());
				}
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
			
//			if ( layer instanceof GroundOverlayLayer)
//				((GroundOverlayLayer)layer).addOverlayListener(this);
			
			// set tree check state
			to.setEnabled(checked);
		}

		// repaint WW
		EarthView.world.repaint();
		
		// check state not preserved when adding/removing nodes
		treeViewer.trackCheckState(to, checked);
	}
	
	@Override
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	public void setStatusErrorMessage (String message) {
		statusLine.setErrorMessage(message);
	}
	
	/******************************************************
	 * Ground Overlay listeners
	 ******************************************************/
	
	/**
	 * Fires on ground overlay error
	 */
	public void onError(GroundOverlayLayer layer, Exception ex) 
	{
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
//	public synchronized void statusChanged(final int current, final int total, final GroundOverlayLayer layer) 
//	{
//		if ( getViewSite() == null || getViewSite().getShell() == null ) return;
//		
//		Display display = getViewSite().getShell().getDisplay();
//		
//        display.syncExec(new Runnable() {
//        	public void run() {
//        		String message = layer + " ("  + current + "/" + total + ")" ;
//        		statusLine.setLoopStatusMessage(message);
//        	}
//        });
//	}

	/******************************************************
	 * End Ground Overlay listeners
	 ******************************************************/

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionPlay);
		// manager.add(actionPause);
		manager.add(actionStop);
		manager.add(new Separator());
		manager.add(actionSaveLayer);
		manager.add(new Separator());
		manager.add(actionRemoveNode);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		//fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
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
		
		// Remove layer from tree node: All layers in this view can be removed
		actionRemoveNode = new Action() {
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)treeViewer.getSelection();
				
				TreeObject to = (TreeObject) selection.getFirstElement();

				try {
					// Stop/Hide
					handleCheckState(false, to);

					// remove node from tree & dispose resources
					treeViewer.removeTreeObject(selection.toArray());
					
				} catch (Exception e) {
					logger.error(e);
				}
			}
		};
		
		actionRemoveNode.setToolTipText(Messages.getText("layer.action.remove"));
		actionRemoveNode.setImageDescriptor(
				Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_DELETE));

		// Tree double click action: Navigate to layer centroid
		doubleClickAction = new Action() {
			public void run() 
			{
				ISelection selection = treeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				
				View view 	= EarthView.world.getView();
				Globe globe = EarthView.world.getModel().getGlobe();
				
				if ( view.getEyePoint() == null)
					return;
				
				// move globe position to layer centroid
				LayerUtils.moveViewTo(view
						, globe
						, ((TreeObject)obj).getLayer());
			}
		};

		// Save layer as KMZ 
		actionSaveLayer = new Action() {
			public void run() 
			{
				ISelection selection 	= treeViewer.getSelection();
				Object obj 				= ((IStructuredSelection)selection).getFirstElement();
				
				if ( obj == null ) return;
					
				Layer layer = ((TreeObject)obj).getLayer();

				try {
					// Save
					Shell shell = getViewSite().getShell();
					
			    	String[] extensions 	= new String[] { "*.kmz" };
			    	String[] filterNames 	= new String[] { "Google Earth KMZ (*.kmz)" };
					
					FileDialog dialog 	= new FileDialog(shell, SWT.SAVE);

					dialog.setFilterExtensions(extensions);
					dialog.setFilterNames(filterNames);
					
					String path 		= dialog.open();

					if ( path == null) return;
					
					logger.debug("Saving " + layer + " as " + path);

					KMLSource.buildKMZ(new File(path), layer);
					
				} catch (Exception e) {
					Messages.showErrorMessage(getViewSite().getShell()
							, Messages.getText("err.dialog.title")
							, "Error saving " + layer + ": " + e.getMessage() );
				}
			}
		};
		actionSaveLayer.setToolTipText(Messages.getText("layer.action.save"));
		actionSaveLayer.setImageDescriptor(ICON_SAVE);
		
	}
	

	/**
	 * On click move globe to the centroid of the BBOX dataset
	 */
	private void hookClickAction () {
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
	
}
