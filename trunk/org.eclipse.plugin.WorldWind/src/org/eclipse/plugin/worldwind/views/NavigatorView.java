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
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.operation.AnimationJob;
import org.eclipse.plugin.worldwind.operation.UpdatesCheckJob;
import org.eclipse.plugin.worldwind.operation.LayerLoaderJob;
import org.eclipse.plugin.worldwind.utils.LayerControlsDialog;
import org.eclipse.plugin.worldwind.utils.LayersToolTipSupport;
import org.eclipse.plugin.worldwind.utils.StatusLine;
import org.eclipse.plugin.worldwind.utils.YGeoSearch;
import org.eclipse.plugin.worldwind.utils.YGeoSearch.YResult;
import org.eclipse.plugin.worldwind.views.tree.TreeObject;
import org.eclipse.plugin.worldwind.views.tree.TreeParent;
import org.eclipse.plugin.worldwind.views.tree.LayersTreeViewer;
import org.eclipse.plugin.worldwind.views.tree.LayersTreeViewer.LayersLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import worldwind.contrib.LayerUtils;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay.OverlayLoopListener;
import worldwind.contrib.layers.quadkey.VirtualEarthLayer;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;

/**
 * Layer Navigator View using eclipse forms + Yahoo Geo Search
 * 
 * The view has the following sections:
 * 	1) A places search text box using yahoo geo services
 * 	2) A layers tree for built-in and user defined layers
 * 
 * The Y! GeoSearch works as follows:
 * 	http://local.yahooapis.com/MapsService/V1/geocode?appid=YahooDemo&location=durham
 * 
 * Returns
 * <pre>
&lt;ResultSet xsi:schemaLocation="urn:yahoo:maps http://api.local.yahoo.com/MapsService/V1/GeocodeResponse.xsd"&gt;
&lt;Result precision="city"&gt;
	&lt;Latitude&gt;35.949417&lt;/Latitude&gt;
	&lt;Longitude&gt;-93.980476&lt;/Longitude&gt;
	&lt;Address/&gt;
	&lt;City&gt;DURHAM&lt;/City&gt;
	&lt;State&gt;AR&lt;/State&gt;
	&lt;Zip/&gt;
	&lt;Country&gt;US&lt;/Country&gt;
&lt;/Result&gt;
&lt;/ResultSet&gt; * </pre> 
 * @author Vladimir Silva
 *
 */
public class NavigatorView extends ViewPart 
	implements Listener, OverlayLoopListener 
{
	private static final Logger logger = Logger.getLogger(NavigatorView.class);
	
	static public final String ID = NavigatorView.class.getName();

	// Save layer as... icon
	private final ImageDescriptor ICON_SAVE = Activator.getImageDescriptor("icons/16x16-saveas.png");
	
	// Eclipse forms support
	private FormToolkit toolkit;
	private ScrolledForm scrolledForm;
	
	// User defined layers (must be saved on exit) & loaded at startup
	private LayerList myLayers = new LayerList();
	
	// Layers tree has: WWJ built in layers + real time sat
	private LayersTreeViewer layersViewer;
	
	// Yahoo location search table
	private TableViewer searchViewer;
	
	// search text
	private Text searchText;
	
	// Y! results
	private YResult[] results;

	// Tooltip support	
	private LayersToolTipSupport tipSupport;
	
	// Status line
	private StatusLine statusLine;
	
	// Layer controls
	private Action actionLayerControls;
	
	// remove a node from the tree
	private Action actionRemoveNode;
	
	// Save layer: KMZ only
	private Action actionSaveLayer;
	
	// Fly to a layer
	private Action actionFlyToLayer;
	
	// A hash table to track animations
	private ConcurrentHashMap<String, AnimationJob> animatedJobs 
		= new ConcurrentHashMap<String, AnimationJob>();
	
	
	@Override
	/**
	 * Create view's controls
	 */
	public void createPartControl(Composite parent) 
	{
		// When the parent is disposed, save tree layers on WW cache
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				saveLayers();
			}
		});
		
		// initialize status line
		statusLine 	= ApplicationActionBarAdvisor.getDefaultStatusLine();
		
		toolkit 		= new FormToolkit(parent.getDisplay());
		scrolledForm 	= toolkit.createScrolledForm(parent);

		scrolledForm.setText(Messages.getString("view.navigator.hdr"));
		//scrolledForm.setImage(Activator.ICON_NASA);

		toolkit.decorateFormHeading(scrolledForm.getForm());	
		
		// Form Toolbar setup
		makeActions();
		contributeToActionBars();
		
		// Create UI
		TableWrapLayout layout 	= new TableWrapLayout();
		layout.numColumns 		= 2;

		scrolledForm.getBody().setLayout(layout);

		int expanded 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED;
		int collapsed 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE;
		
		// create UI elements
		searchViewer 	= createSearchSection(Messages.getString("view.navigator.places"), null, collapsed, 2);
		
		layersViewer 	= createTreeSection(Messages.getString("view.navigator.layers"), null, expanded, 2, 300);

		initLayers();	
		
		// load local layers from cache
		loadLayers();
		
		// check 4 updates
		UpdatesCheckJob job = new UpdatesCheckJob(getViewSite().getWorkbenchWindow());
		job.schedule(120000);
	}

	
	/**
	 * Initialize the layers tree with WWJ built in layers and some 
	 * real-time satellite layers from the Navy Labs
	 */
	private void initLayers()
	{
		// Tooltip support
		LayersLabelProvider labelProvider = new LayersLabelProvider();
		
		tipSupport = LayersToolTipSupport.enablefor(layersViewer
				, ToolTip.NO_RECREATE
				, getViewSite().getWorkbenchWindow());
		
		labelProvider.setTipSupport(tipSupport);

		// Init weather w/ WW built in layers
		layersViewer.setContentProvider(new LayersTreeViewer.LayersContentProvider());
		layersViewer.setLabelProvider(labelProvider);
		layersViewer.initialize();
		
		// When user checks a checkbox in the tree, check all its children
	    layersViewer.addCheckStateListener(new ICheckStateListener() {
	      public void checkStateChanged(CheckStateChangedEvent event) 
	      {
	    	  boolean checked = event.getChecked(); 
	    	  
	    	  // check node
	    	  TreeObject to = (TreeObject)event.getElement();
	    	  
	    	  // handle state
	    	  handleCheckState(checked, to, (LayersTreeViewer)event.getSource());
	      }
	    });
		
	    // on double click fly to the selected layer
		hookClickAction(layersViewer);
		
		// Add a tree ctx menu
		hookContextMenu();
		
		// Set initially checked layers
		layersViewer.updateCheckState();
		
		// load remote real time weather layers 
		LayerLoaderJob job = new LayerLoaderJob(getViewSite().getShell().getDisplay()
				, layersViewer
				, statusLine );
		
		job.setProperty(IProgressConstants.ICON_PROPERTY, Activator.ICON_NOAA);
		//job.setUser(false);
		job.schedule(5000);
	}
	
	/**
	 * Create an expandable section with a checked tree inside 
	 * @param title
	 * @param description
	 * @param style
	 * @return
	 */
	private LayersTreeViewer createTreeSection(String title
			, String description
			, int style
			, int colSpan
			, int heightHint) 
	{
		Section section = createSection(title, description, style, colSpan); 

		Composite sectionClient = toolkit.createComposite(section);
		sectionClient.setLayout(new GridLayout());

		// use flat borders		
		sectionClient.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(sectionClient);
		
		GridData gd = new GridData();
		sectionClient.setLayoutData(gd);
		
		// ad a tree widget
		Tree tree = toolkit.createTree(sectionClient, SWT.FILL | SWT.CHECK);
		
		tree.addListener(SWT.Selection, this);
		
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.grabExcessVerticalSpace 	= true;
		gd.heightHint = heightHint;

		tree.setLayoutData(gd);
		
		section.setClient(sectionClient);
		
		return 	new LayersTreeViewer(tree);
	}
	
	/**
	 * Create a search section with a text box button and results table
	 * @param title
	 * @param description
	 * @param style
	 * @return
	 */
	private TableViewer createSearchSection(String title
			, String description
			, int style, int colSpan) 
	{
		Section section 		= createSection(title, description, style, colSpan);
		Composite sectionClient = toolkit.createComposite(section);
		
		sectionClient.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(sectionClient);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;

		sectionClient.setLayout(layout); 

		// Text box & search button
		searchText = toolkit.createText(sectionClient, "", SWT.FILL);
		
		TableWrapData td = new TableWrapData(TableWrapData.FILL_GRAB);
		searchText.setLayoutData(td);
		
		Button button = toolkit.createButton(sectionClient, "Search", SWT.NONE);
		button.setLayoutData(new TableWrapData());

		// Search results table
		Table table = toolkit.createTable(sectionClient, SWT.FILL);
		
		td 				= new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan 		= 2;
		td.heightHint 	= 100;
		
		table.setLayoutData(td);
		
		section.setClient(sectionClient);
		
		// evnt listeners
		button.addListener(SWT.Selection, this);
		button.addListener(SWT.DefaultSelection, this);
	
		table.addListener(SWT.Selection, this);
		table.addListener(SWT.DefaultSelection, this);
		
		return new TableViewer(table);
	}

	/*
	 * createSection  utility
	 */
	private Section createSection (String title
			, String description
			, int style, int colSpan)
	{
		Section section = toolkit.createSection(scrolledForm.getBody(), style);
		
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				   scrolledForm.reflow(true);
			}
		});
		
		if ( title != null )		
			section.setText(title);
		
		if ( description != null )  
			section.setDescription(description);
		 		
		TableWrapData td 	= new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan 			= colSpan;
		section.setLayoutData(td);
		
		return section;
	}
	
	@Override
	public void setFocus() {
		scrolledForm.setFocus();
	}

	/**
	 * Status error message
	 * @param message
	 */
	public void setStatusErrorMessage (String message) {
		statusLine.setErrorMessage(message);
	}
	

	/**
	 * Disposes the toolkit
	 */
	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}
	
	/**
	 * Load layers from WW default cache. All layers stored as KML.
	 * Layers are added to the myPlaces tree.
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
			
				//logger.debug("Got KML of size=" + kml.toKML()); //.length());
				addKMLSource(kml,  false);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
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
		
		TreeParent parent = new TreeParent(layer, icon);
		
		layersViewer.addTreeObject( parent
				, null
				, true		// Make layer renderable
				, enabled); // checked
		
		// for caching
		myLayers.add(layer);
	}
	
	/**
	 * Add a kml/kmz source. All layers regardless of protocol are cached as KML in
	 * WW cache.
	 * @param kml The {@link KMLSource} object to store
	 */
	public void addKMLSource (KMLSource kml,  boolean enabled) 
	{
		LayersTreeViewer nodeViewer = layersViewer; // myPlacesViewer;
		
		String displayName 		= kml.getDocument().getName();
		LayerList list 			= kml.toLayerList();

		logger.debug("KML display name " + displayName + " enabled=" + enabled 
				+ " # of elements=" + list.size());
		
		// If the doc has more than 1 ground overlay, the layer will be
		// displayed as a TimeLoop overlay. 
		// There should be a better way of doing this...
		if ( kml.groundOverlaysSize() > 1) 
		{
			final TimeLoopGroundOverlay lov = KMLSource.toTimeLoopGroundOverlay(kml.getDocument());
			
			nodeViewer.addTimeLoopGroundOverlays( 
					new TimeLoopGroundOverlay[] { lov } 
					);
			
			// for caching
			myLayers.add(lov);
		}
		// Doc has a single element 
		else if ( list.size() == 1) 
		{
			final Layer child = list.iterator().next();
			
			nodeViewer.addTreeObject(new TreeParent(child, LayersTreeViewer.guessIcon(child.getName()))
					, null
					, true
					, enabled);
			
			// for caching
			myLayers.add(child);
		}
		else {
			// Otherwise other elements: Screen Ovs & placemarks will be composed
			// as children of a top element
			RenderableLayer layer = new RenderableLayer();
			
			layer.setName(displayName);
			layer.setValue("KMLSOURCE", kml.toKML());

			TreeParent top = new TreeParent(layer
					, LayersTreeViewer.guessIcon(displayName)
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
						 , LayersTreeViewer.guessIcon(child.getName())
						 );
				 
			}
			nodeViewer.addTreeObject(top, children, true, enabled);

			// for caching
			myLayers.add(layer);
		}
	}
	
	/*
	 * Process a check state event
	 */
	private void handleCheckState (boolean checked, final TreeObject to, final LayersTreeViewer treeViewer)
	{
		to.setEnabled(checked);

		// WW Layer
		Layer layer = to.getLayer();
	  
		logger.debug("Layer " + layer.getName() + " type=" + layer.getClass().getName() );

		// The real time weather node has too many layers. it cannot be checked
		// Only its children can be checked
		if ( layer.getName().equalsIgnoreCase(Messages.getString("remote.layers.tree.lbl")))
		{
			Messages.showErrorMessage(getViewSite().getShell()
					, Messages.getString("err.msg.realtime.sat", new Object[] { layer.getName()} ));

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
				
				// Listen for loop updates
				//overlay.addLoopListener(this);
				//overlay.play(); 

				final Display display = getViewSite().getShell().getDisplay();
				
				// Use an eclipse animation job for smoothness
				// It won't hang the UI
				AnimationJob job = new AnimationJob(display, overlay, statusLine);
				
				// listen for job change events
				job.addJobChangeListener(new JobChangeAdapter()
				{
					@Override
					public void done(IJobChangeEvent event) 
					{
						// Has the job been canceled?
						if ( !event.getResult().isOK() ) 
						{
							logger.debug("Job for layer ID " + to.getID() + " canceled.");
							
							display.syncExec(new Runnable() {
								public void run() 
								{
									handleCheckState(false, to, treeViewer);
								}
							});
							
						}
					}
				});
				
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
			
			parent.setEnabled(checked);
			
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
							handleCheckState(checked, children[i], treeViewer);
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
		treeViewer.checkNode(to, checked);
		
	}

	/**
	 * On click move globe to the centroid of the BBOX dataset
	 */
	private void hookClickAction (final LayersTreeViewer treeViewer) 
	{
		treeViewer.getTree().addKeyListener(new KeyAdapter(){


			public void keyReleased(KeyEvent e) {
				if ( e.character == SWT.DEL)
					removeNode();
			}
		});
		
		//treeViewer.getTree().addSelectionListener(new SelectionListener() 
		treeViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent arg0) {
				flyOnClickAction(treeViewer);
			}
		});
	}
	
	/**
	 * Fly to a tree layer center on click.
	 */
	private void flyOnClickAction (LayersTreeViewer treeViewer)  
	{
			ISelection selection = treeViewer.getSelection();
			Object obj = ((IStructuredSelection)selection).getFirstElement();
			
			// move globe position to layer centroid
			LayerUtils.moveViewTo(EarthView.world.getView()
					, EarthView.world.getModel().getGlobe()
					, ((TreeObject)obj).getLayer());
	}

	/**
	 * Add actions to the form Toolbar 
	 */
	private void contributeToActionBars()
	{
		Form form = scrolledForm.getForm(); 
		form.getToolBarManager().add(actionLayerControls);
		form.getToolBarManager().add(actionSaveLayer);
		form.getToolBarManager().add(actionFlyToLayer);
		
		form.getToolBarManager().add(new Separator());
		
		form.getToolBarManager().add(actionRemoveNode);
		form.getToolBarManager().update(true);
		form.setToolBarVerticalAlignment(SWT.LEFT);
	}
	
	
	/* View local actions */
	private void makeActions() 
	{
		// Fly to the selected layer
		actionFlyToLayer = new Action() {
			public void run() 
			{
				flyOnClickAction(layersViewer);
			}
		};
		actionFlyToLayer.setText(Messages.getString("layer.action.fly"));
		actionFlyToLayer.setToolTipText(Messages.getString("layer.action.fly"));
		actionFlyToLayer.setImageDescriptor(Activator.ICON_WEB_BROWSER );
		
		// Remove layer from tree node: All layers in this view can be removed
		actionRemoveNode = new Action() {
			public void run() 
			{
				removeNode();
			}
		};
		
		actionRemoveNode.setToolTipText(Messages.getString("layer.action.remove"));
		actionRemoveNode.setText(Messages.getString("layer.action.remove"));
		actionRemoveNode.setImageDescriptor(
				Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
		// Save layer as KMZ 
		actionSaveLayer = new Action() {
			public void run() 
			{
				final LayersTreeViewer nodeViewer = layersViewer; // getSelectedViewer();
				
				if ( nodeViewer == null ) return;
				
				ISelection selection 	= nodeViewer.getSelection();
				Object obj 				= ((IStructuredSelection)selection).getFirstElement();
				
				if ( obj == null ) return;
					
				Layer layer = ((TreeObject)obj).getLayer();

				// output file
				File kmzFile = null;
				
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

					kmzFile = new File(path);
					KMLSource.buildKMZ(kmzFile, layer);
					
				} 
				catch (Exception e)
				{
					e.printStackTrace();
					
					// delete file if error
					if ( kmzFile!= null && kmzFile.exists())
						kmzFile.delete();
					
					Messages.showErrorMessage(getViewSite().getShell()
							, "Error saving " + layer + ": " + e.getMessage() );
				}
			}
		};
		
		actionSaveLayer.setToolTipText(Messages.getString("layer.action.save"));
		actionSaveLayer.setText(Messages.getString("layer.action.save"));
		actionSaveLayer.setImageDescriptor(ICON_SAVE);
		
		// Layer controls: opacity, speed, etc.
		actionLayerControls = new Action() {
			public void run() 
			{
				try {
					showLayerControls();
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		};
		
		actionLayerControls.setToolTipText(Messages.getString("layer.action.controls"));
		actionLayerControls.setText(Messages.getString("layer.action.controls"));
		actionLayerControls.setImageDescriptor(Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		
	}
	
	/*
	 * Remove a node from the tree
	 */
	private void removeNode ()
	{
		IStructuredSelection selection = (IStructuredSelection)layersViewer.getSelection();
		
		TreeObject to = (TreeObject) selection.getFirstElement();

		if ( to == null ) return ;

		// built-in layers cannot be removed
		if ( ! myLayers.contains(to.getLayer()))
		{
			MessageDialog.openInformation(getViewSite().getShell()
					, Messages.getString("info.dialog.title")
					, Messages.getString("err.msg.builtin.layer"
							, new Object[] { to.getLayer().getName()}) );
			return;
		}
		
		try {
			// remove
			if ( MessageDialog.openQuestion(getViewSite().getShell()
					, Messages.getString("question.dialog.title")
					, Messages.getString("layer.action.remove") + " " + to.getLayer().getName() + "?"))
			{
				// Stop/Hide
				handleCheckState(false, to, layersViewer);
	
				// remove node from tree & dispose resources
				layersViewer.removeTreeObject(selection.toArray());
				
				// remove from cached layers
				myLayers.remove(to.getLayer());
			}
		} 
		catch (Exception e) {
			//e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
	
	/*
	 * Show selected layer controls
	 */
	private void showLayerControls() 
	{
		LayersTreeViewer nodeViewer = layersViewer; 
		
		if ( nodeViewer == null ) return;
		
		ISelection selection 	= nodeViewer.getSelection();
		
		if ( selection == null ) return;
		
		Object obj 				= ((IStructuredSelection)selection).getFirstElement();
		TreeObject to 			= (TreeObject)obj;
		
		if ( obj == null ) return;

		Shell shell = getViewSite().getShell();
		Layer layer = to.getLayer();

		// Only ground overlays & custom layers like MS VE support controls
		if ( ! (layer instanceof TimeLoopGroundOverlay)
				&& ! (layer instanceof GroundOverlayLayer) 
				&& ! (layer instanceof VirtualEarthLayer)
				) 
		{
			MessageDialog.openInformation(shell
					, Messages.getString("info.dialog.title")
					, Messages.getString("ctl.dlg.invalid.layer"
							, new Object[] {layer.getName()} ));
			return;
		}
		
		
		LayerControlsDialog dialog = new LayerControlsDialog(shell, layer);
		
		if ( layer instanceof TimeLoopGroundOverlay)
			dialog.setAnimationJob(animatedJobs.get(to.getID()));
		
		dialog.open();
	}
	
	/**
	 * Handle GUI events: Mouse clicks, button presses, etc
	 */
	public void handleEvent(Event event) 
	{
		Widget w = event.widget;
		try 
		{
			// The search button has been pressed
			if ( w instanceof Button ) 
			{
				String location = searchText.getText(); // ((Text)w).getText();
				
				// Search for text using Yahoo
				YGeoSearch yahoo 	= new YGeoSearch(location);
				results 			=  yahoo.getLocations();
				
				searchViewer.getTable().removeAll();
				
				// Add results to the table
				for (YResult result : results) {
					searchViewer.add(result);
				}
				
			}
			else if ( w instanceof Table ) {
				// A search result has been clicked within the table
				int idx = ((Table)w).getSelectionIndex();

				// grab earth view
				EarthView view = (EarthView)Activator.getView(
						getViewSite().getWorkbenchWindow()
						, EarthView.ID);
				
				// lat/lon cannot be null
				double lat = Double.parseDouble(results[idx].latitude);
				double lon = Double.parseDouble(results[idx].longitude);
				
				// Fly to the lat lon of the yahoo result
				LatLon latlon = new LatLon(Angle.fromDegrees(lat)
						, Angle.fromDegrees(lon));
				
				view.flyTo(latlon);
			}
			// Which tree is selected?
			else if ( w instanceof Tree ) 
			{
				updateActions();
			}
		} 
		catch (Exception e) {
			// show error in status line
			e.printStackTrace();
			statusLine.setErrorMessage(e.getMessage());
		}
	}
	
	/**
	 * Cache tree layers to WW default cache location. All layers are saved as KML.
	 * Only {@link TimeLoopGroundOverlay} and {@link GroundOverlayLayer}
	 * are cached
	 */
	private void saveLayers() 
	{
		//TreeObject[] topLayers = ((TreeParent)myPlacesViewer.getInput()).getChildren();
		try {
			StringBuffer buf = new StringBuffer("<xml>" + Messages.NL);
			
			//getKML(topLayers, buf);
			getKML(myLayers, buf);
			
			buf.append("</xml>");

			// save XML in WW cache folder
			File file = WorldWind.getDataFileCache().newFile("layers.xml");
			
			worldwind.contrib.Messages.writeToFile(file, buf.toString().getBytes());
		} 
		catch (Exception e) 
		{
			// Unable to save file!....Why?
			e.printStackTrace();
		}
	}
	
	/**
	 * Recurse tree layers extracting kml. All layers (local remote) are
	 * stored as KML.
	 * @param topLayers
	 * @param buf
	 */
	//private void getKML ( TreeObject[] topLayers, StringBuffer buf)
	private void getKML ( LayerList topLayers, StringBuffer buf)
	{
		//for (TreeObject treeObject : topLayers) 
		for (Layer layer : topLayers)
		{
			//final Layer layer = treeObject.getLayer();
	
			// Layers generated from a KML URL store the XML internally
			final String kml = (String)layer.getValue("KMLSOURCE");
			
			String buffer = null;
			
			// This is a layer created from a remote KML doc
			if ( kml != null ) {
				buffer = "<kml>" + Messages.NL
						+ kml
						+ "</kml>" + Messages.NL;
			}
			else if ( layer instanceof TimeLoopGroundOverlay) {
				buffer = "<kml>" + Messages.NL 
						+ ((TimeLoopGroundOverlay)layer).toKML() + Messages.NL
						+ "</kml>" + Messages.NL;
			}
			else if (layer instanceof GroundOverlayLayer )
					//&& ! (treeObject.getParent().getLayer() instanceof TimeLoopGroundOverlay) ) 
			{
				buffer = "<kml>" + Messages.NL
						+ ((GroundOverlayLayer)layer).toKML() + Messages.NL
						+ "</kml>" + Messages.NL;
			}
			
			if ( buffer != null) {
				buf.append(buffer);
			}
			else
				logger.error("Unable to get KML for layer " + layer 
						+ " of type " + layer.getClass().getName() );
			
			// if parent w/ children and not built in
//			if ( treeObject instanceof TreeParent 
//					&& ((TreeParent)treeObject).hasChildren()
//					&&  treeObject.isRemovable() )  
//			{
//				getKML(((TreeParent)treeObject).getChildren(), buf);
//			}
		}
	}
	
	/**
	 * Add an array of {@link TiledWMSLayer} usually WMS 1.1.x with no time dimension
	 * to the view
	 * @param parentName Name of the tree parent node
	 * @param layers Tiled WMS layers
	 */
	public void addTiledWMSLayers( String parentName, TiledWMSLayer[] layers, boolean enabled) 
	{
		RenderableLayer top = new RenderableLayer();
		top.setName(parentName);
		
		// Parent
		TreeParent parent = new TreeParent (top
				, LayersTreeViewer.guessIcon(parentName) );
		
		// Children
		TreeObject[] children = new TreeObject[layers.length];
		
		for (int i = 0; i < children.length; i++) 
		{
			layers[i].setEnabled(enabled);
			
			children[i] = new TreeObject( layers[i] , null);
		}
		
		// Add to the view
		layersViewer.addTreeObject(parent, children, true, enabled);
		
	}
	
	/**
	 * Add an array World Wind {@link Layer} to the view 
	 */
	public void addLayers (Layer[] layers, boolean enabled) 
	{
		if ( layers == null ) return;
		
		for (Layer layer : layers) 
		{
			// Time Loop layer
			if ( layer instanceof TimeLoopGroundOverlay) 
			{
				layersViewer.addTimeLoopGroundOverlays( (TimeLoopGroundOverlay[])layers);
				
				// for caching
					myLayers.add(layer);
				
				return;
			}
			// Ground overlays
			else if ( layer instanceof GroundOverlayLayer) 
			{
					addGroundOverlay((GroundOverlayLayer)layer, enabled);
			}
			// regular layer (Not a Ground Overlay or Time Loop)
			else {
				addLayer(layer, null, true);
				//statusLine.setErrorMessage("Invalid layer type: " + layer.getClass().getName());
			}
		}
	}
	
//	public void addOverlays (Layer[] layers, boolean enabled) 
//	{
//		if ( layers == null ) return;
//		
//		Layer first = layers[0];
//		
//		// Time Loop layer
//		if ( first instanceof TimeLoopGroundOverlay) 
//		{
//			layersViewer.addTimeLoopGroundOverlays( (TimeLoopGroundOverlay[])layers);
//			
//			// for caching
//			for (Layer layer : layers) {
//				myLayers.add(layer);
//			}
//			
//			return;
//		}
//		// Ground overlays
//		else if ( first instanceof GroundOverlayLayer) 
//		{
//			for (Layer layer : layers) {
//				addGroundOverlay((GroundOverlayLayer)layer, enabled);
//			}
//		}
//		// regular layer (Not a Ground Overlay or Time Loop) - don't save
//		else {
//			statusLine.setErrorMessage("Invalid layer type: " + first.getClass().getName());
//		}
//	}
	
	/**
	 * Add a {@link GroundOverlayLayer} to the View tree
	 * @param overlay {@link GroundOverlayLayer}
	 */
	private void addGroundOverlay (GroundOverlayLayer overlay, boolean enabled) 
	{
		TreeParent parent = 
			new TreeParent(overlay
					, LayersTreeViewer.guessIcon(overlay.getName())
		            );
		
		
		layersViewer.addTreeObject(parent, null, true, enabled);
		
		// Add to my layers so it will be saved
		myLayers.add(overlay);
	}
	
	/******************************************************
	 * Ground Overlay listeners
	 ******************************************************/
	
	/**
	 * Fires on ground overlay error
	 */
	public void onError(Layer layer, Exception ex) 
	{
		final String message = layer.getName() + ": " 
			+ ex.getClass() + " " + ex.getMessage();
		
		Display display = getViewSite().getShell().getDisplay();
		
		display.syncExec(new Runnable() {
			public void run() {
				statusLine.setErrorMessage(message);
			}
		});

		// stop rendering layer
		layer.setEnabled(false);
	}

	/**
	 * fires when an animated ground overlay loops thru
	 */
	public synchronized void statusChanged(final int current, final int total, final GroundOverlayLayer layer) 
	{
		if ( getViewSite() == null || getViewSite().getShell() == null ) return;
		
//		Display display = getViewSite().getShell().getDisplay();
//		
//        display.syncExec(new Runnable() {
//        	public void run() {
//        		String message = layer + " ("  + current + "/" + total + ")" ;
//        		statusLine.setLoopStatusMessage(message);
//        	}
//        });
	}

	/******************************************************
	 * End Ground Overlay listeners
	 ******************************************************/

	/*
	 * Create a PopUp or context menu
	 */
	private void hookContextMenu() 
	{
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(layersViewer.getControl());

		// Add a menu listener to enable/disable menu items
		menu.addMenuListener(new MenuListener(){

			public void menuHidden(MenuEvent e) {
			}

			public void menuShown(MenuEvent e) {
				updateActions();
			}});
		
		layersViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, layersViewer);
	}

	/*
	 * Add actions to the context menu
	 */
	private void fillContextMenu(IMenuManager manager) 
	{
		manager.add(actionLayerControls);
		manager.add(actionSaveLayer);
		manager.add(actionFlyToLayer);
		manager.add(new Separator());
		manager.add(actionRemoveNode);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	/*
	 * Enable/disable actions according to the selected layer
	 */
	private void updateActions ()
	{
		TreeObject to = (TreeObject)((IStructuredSelection)layersViewer.getSelection()).getFirstElement();

		if ( to == null ) 
			return;
		
		
		Layer layer = to.getLayer();
		
		// only user defined layers can be removed
		boolean bool =  myLayers.contains(layer);
		
		actionRemoveNode.setEnabled(bool);

		// Ground overlays can be flown to...
		bool =  (layer instanceof GroundOverlayLayer)
				|| (layer instanceof TimeLoopGroundOverlay);
		
		actionFlyToLayer.setEnabled(bool);
	}
}
