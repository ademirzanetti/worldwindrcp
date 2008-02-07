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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.operation.GroundOverlayFetchOperation;
import org.eclipse.plugin.worldwind.operation.WMSParseOperation;
import org.eclipse.plugin.worldwind.utils.NewRemoteServerDialog;
import org.eclipse.plugin.worldwind.utils.StatusLine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.WMS_Capabilities;
import worldwind.contrib.parsers.WMS_Capabilities.Layer;

/**
 * Web Map Service (WMS) Viewer: Populates a local server list from a local html file and
 * allows the user to add new servers, which are cached to wwj wms-bookmarks.txt
 * 
 * WMS layers are converted to wwj {@link GroundOverlayLayer} or {@link TimeLoopGroundOverlay}
 * depending on the WMS version.
 * 
 * Supports WMS 1.1.x and 1.3.x
 * @author Vladimir Silva
 *
 */
public class WMSView extends ViewPart
	implements Listener 
{
	private static final Logger logger = Logger.getLogger(WMSView.class);
	
	public static final String ID = WMSView.class.getName();

	// state of the user interface
	private enum UI_STATE { IDLE, BUSY};
	
	// WMS layers
	private TableViewer layers;
	
	// Eclipse forms support
	private FormToolkit toolkit;
	private ScrolledForm form;

	//Widgets: bbox
	private Text latMin, latMax, lonMin, lonMax;
	
	// Servers
	private Combo cmbServers;
	
	// new server
	private Button newSrv;
	
	// time range
	private Combo tmin, tmax;
	
	// Action btos
	private Button submit;

	private Display display; 

	// Date combos
	private Combo formats;

	// status message
	private Label statusMessage;

	// Status line
	private StatusLine statusLine;
	
	// Vector of WMS server objects
	private Vector<ParserUtils.PublicWMSServer> servers;

	// WMS servers caps
	private WMS_Capabilities capabilities;
	
	// Indices of the selected layers from caps above
	private int[] selectedIndices;
	
	// Used to cache capabilities for increased performance
	private static Hashtable<String, WMS_Capabilities> capabilitiesCache
		= new Hashtable<String, WMS_Capabilities>();

	// bbox/ date labels (may be hidden dynamically) 
	private Label l3,l4;

	private boolean showDates;

	@Override
	public void createPartControl(Composite parent) 
	{
		display 		= getViewSite().getShell().getDisplay();
		toolkit 		= new FormToolkit(parent.getDisplay());
		form 			= toolkit.createScrolledForm(parent);

		form.setText(Messages.getString("WMSView.0")); //$NON-NLS-1$
		toolkit.decorateFormHeading(form.getForm());
		
		// initialize status line
		statusLine 	= ApplicationActionBarAdvisor.getDefaultStatusLine();
		
		// Create UI
		TableWrapLayout layout 	= new TableWrapLayout();
		layout.numColumns 		= 2;

		form.getBody().setLayout(layout);

		int expanded 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED;
		int collapsed 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE;
		
		// Layers section
		layers 	= createLayersSection(Messages.getString("WMSView.1"), null, expanded, 2); //$NON-NLS-1$
		
		// coverage
		createCoverageSection(Messages.getString("WMSView.2"), null, collapsed, 2); //$NON-NLS-1$
		
		// display options
		createOptionsSection("Display Options", null, collapsed, 2);
		
		// status message
		statusMessage = toolkit.createLabel(form.getBody(), "", SWT.NONE); //$NON-NLS-1$
		statusMessage.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB, 1, 2));
		
		// submit btn
		submit = toolkit.createButton(form.getBody(), Messages.getString("WMSView.4"), SWT.NONE); //$NON-NLS-1$
		//submit.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.FILL_GRAB, 1, 1));
		submit.addListener(SWT.Selection, this);
		
		// set flat look
		toolkit.paintBordersFor(form.getBody());
		
		try {
			initialize();
			loadServers();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
		form.setFocus();
	}

	
	/*
	 * Grids section
	 */
	private TableViewer createLayersSection(String title
			, String description
			, int style, int colSpan) 
	{
		Section section 		= createSection(title, description, style, colSpan);
		Composite client = toolkit.createComposite(section);
		
		client.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(client);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;

		client.setLayout(layout); 
		
		TableWrapData td ;
		td 				= new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan 		= 2;

		// lbl cmbServers
		Label l1 		= toolkit.createLabel(client, Messages.getString("WMSView.5")); //$NON-NLS-1$
		l1.setLayoutData(td);

		cmbServers			= new Combo(client, SWT.READ_ONLY);
		cmbServers.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		cmbServers.addListener(SWT.Selection, this);
		
		// new srv btn
		newSrv	= toolkit.createButton(client, Messages.getString("WMSView.6"), SWT.PUSH); //$NON-NLS-1$
		newSrv.addListener(SWT.Selection, this);

		td 				= new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan 		= 2;
		
		Label l2 		= toolkit.createLabel(client, Messages.getString("WMSView.7")); //$NON-NLS-1$
		l2.setLayoutData(td);
		
		td 				= new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan 		= 2;
		td.heightHint	= 200;
		
		Table table = toolkit.createTable(client, SWT.MULTI);
		table.setLayoutData(td);
		
		table.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		// or myControl.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
		
		section.setClient(client);
	
		table.addListener(SWT.Selection, this);
		table.addListener(SWT.DefaultSelection, this);
		
		return new TableViewer(table);
	}

	/**
	 * Dimensions section
	 * @param title
	 * @param description
	 * @param style
	 * @param colSpan
	 */
	private void createCoverageSection(String title
			, String description
			, int style, int colSpan) 
	{
		Section section 		= createSection(title, description, style, colSpan);
		Composite sectionClient = toolkit.createComposite(section);
		
		sectionClient.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(sectionClient);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 3;
		layout.makeColumnsEqualWidth = true;
		
		sectionClient.setLayout(layout); 
		
		// Lat
		toolkit.createLabel(sectionClient, Messages.getString("WMSView.8"), SWT.NONE);  //$NON-NLS-1$
		
		latMin = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		latMin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		latMax = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		latMax.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// Lon
		toolkit.createLabel(sectionClient, Messages.getString("WMSView.11"), SWT.NONE);   //$NON-NLS-1$
		
		lonMin = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		lonMin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		lonMax = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		lonMax.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		

		TableWrapData td; // = new TableWrapData(TableWrapData.LEFT);

		// formats: lbl + combo
		toolkit.createLabel(sectionClient, Messages.getString("WMSView.14"), SWT.NONE);   //$NON-NLS-1$

		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		formats = new Combo(sectionClient, SWT.READ_ONLY);
		formats.setLayoutData(td);
		
		// tmin
		l3 = toolkit.createLabel(sectionClient, Messages.getString("WMSView.15"), SWT.NONE);   //$NON-NLS-1$
		
		tmin = new Combo(sectionClient, SWT.BORDER);
		
		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		tmin.setLayoutData(td); 
		tmin.addListener(SWT.Selection, this);
		tmin.addListener(SWT.DefaultSelection, this);
		
		// tmax
		l4 = toolkit.createLabel(sectionClient, Messages.getString("WMSView.16"), SWT.NONE);   //$NON-NLS-1$
		
		tmax = new Combo(sectionClient, SWT.BORDER);
		
		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		tmax.setLayoutData(td); //new TableWrapData(TableWrapData.FILL_GRAB));
		tmax.addListener(SWT.Selection, this);
		tmax.addListener(SWT.DefaultSelection, this);



		section.setClient(sectionClient);
	}
	
	private void createOptionsSection(String title
			, String description
			, int style, int colSpan) 
	{
		Section section 		= createSection(title, description, style, colSpan);
		Composite sectionClient = toolkit.createComposite(section);
		
		sectionClient.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(sectionClient);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		
		sectionClient.setLayout(layout); 
	}
	
	/*
	 * createSection  utility
	 */
	private Section createSection (String title
			, String description
			, int style, int colSpan)
	{
		Section section = toolkit.createSection(form.getBody(), style);
		
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				   form.reflow(true);
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
	

	/**
	 * Enable/disable controls according to the UI state
	 * @param state
	 */
	private void setUIState ( UI_STATE state) 
	{
		switch (state) 
		{
			case IDLE:
				submit.setEnabled(true);
				break;
	
			case BUSY:
				submit.setEnabled(false);
				break;
			default:
					break;
		}
	}
	
	/**
	 * Handle GUI events: Mouse clicks, button presses, etc
	 */
	public void handleEvent(Event event) 
	{
		Widget w = event.widget;
		
		try {
			if ( w instanceof Table ) 
			{
				IStructuredSelection sel = (IStructuredSelection) layers.getSelection();

				System.out.println(sel);
				
				int[] indices = ((Table)w).getSelectionIndices();
				handleListSelection(indices);
			}
			else if ( w == cmbServers) 
			{
				handleComboSelection((Combo)w);
			}
			else if ( w == newSrv) 
			{
				addServer();
			}
			else if ( w == submit) 
			{
				setUIState(UI_STATE.BUSY);
				performFinish();
				setUIState(UI_STATE.IDLE);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Parse WMS URLs from a local HTML list
	 */
	private void initialize() throws Exception
	{
		// load public cmbServers from config folder
		String list = Messages.getString("wiz.wms.servers.local.list"); //$NON-NLS-1$
		
	    InputStream buffer  = new BufferedInputStream(Messages.getInputStream(ParserUtils.class, list));   
	    OutputStream os = new ByteArrayOutputStream();
		
	    ParserUtils.readStream(buffer, os);
	    os.close();
	    
	    // parse WMS server
		servers = ParserUtils.parsePublicWmsHTTPPage( os.toString() ); 
	}

	/*
	 * Load WMS servers into serverCombo box
	 */
	private void loadServers()
	{
		// add servers from wms-bookmarks.txt
		String[] bookmarks = loadBookmarks();
		
		try {
			if ( bookmarks != null) 
			{
				logger.debug("Loading " + bookmarks.length + " WMS srvs from bookmarks."); //$NON-NLS-1$ //$NON-NLS-2$
				
				for (String bm : bookmarks) {
					final String[] tmp = bm.split("\\|"); // field sep (|) //$NON-NLS-1$
					
					final String name 	= tmp[0];
					final String url 	= tmp[1];

					logger.debug("Adding WMS bookmark: " + name + " " + url); //$NON-NLS-1$ //$NON-NLS-2$
					ParserUtils.PublicWMSServer wms = new ParserUtils.PublicWMSServer(name, new URL(url));

					// Add to the WMS servers vector
					servers.add(0, wms);
				}
			}
		} 
		catch (Exception e) {	}
		
		// Populate the server combo from the WMS servers vector
		for (ParserUtils.PublicWMSServer server : servers) {
			if (server.name != null )
				cmbServers.add(server.name.trim());
		}
	}
	
	/* fires when the list is clicked */
	private void handleListSelection ( int[] indices )  
	{
		try {
			
			// Save indices to extract Layers from Caps obj later on
			selectedIndices = indices;

			if ( capabilities != null ) 
			{
				// extract date range/bbox of the 1st layer only
				int idx = selectedIndices[0];
				
				Layer layer = capabilities.getLayers().get(idx);
				
				boolean showDates = false;
				String[] dates = null;
				
				if ( layer.ISOTimeSpan != null ) {
					showDates = true;
					
					// Comma sep string of ISO dates for this layer time span
					logger.debug("Building ISO time list for " + layer.ISOTimeSpan); //$NON-NLS-1$
						
					final String csvDates = WMS_Capabilities.buildWMSTimeList(layer.ISOTimeSpan);
					
					// split csv string to generate dates
					dates =  csvDates.split(","); //$NON-NLS-1$
				}
				
				// Layer formats
				String[] formats = new String[capabilities.getMapRequest().formats.size()];
				
				capabilities.getMapRequest().formats.toArray(formats);
				
				logger.debug("Using WMS layer "  //$NON-NLS-1$
						+ layer.Name + " " + ((dates != null) ? dates.length: 0) + " time steps " //$NON-NLS-1$ //$NON-NLS-2$
						+ " formats size=" + formats.length  //$NON-NLS-1$
						+ " Show dates=" + showDates); //$NON-NLS-1$
				
				setStatusMessage("Using " + layer.Name + " with " + ((dates != null) ? dates.length: 0) + " time steps " );
				
				loadCoverage(showDates
						, layer.bbox.isValid() 	// show latlon
						, dates					// time steps
						, layer.bbox			// bbox
						, formats);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* fires when a serverCombo item is selected */
	private void handleComboSelection ( Combo combo ) 
	{
		String server 	= combo.getText();
		int idx 		= combo.getSelectionIndex();
		URL url 		= null;

		if ( idx == -1 ) return;
		
		
		try {
			// Load from public list
			url = servers.get(idx).capabilitiesURL;

			logger.debug("Server=" + server + " idx=" + idx + " url:" + url); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			if ( capabilitiesCache.containsKey(url.toString())) 
			{
				// load from cache
				capabilities = capabilitiesCache.get(url.toString());
				
				// Populate list: remove any previous elements
				layers.getTable().removeAll();
				
				// Ad layers to the list box
				for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
					layers.add(layer.Title);
				}
				
			}
			else 
			{
				WMSParseOperation operation = new WMSParseOperation(server, url
					, layers, display);
				
				// run a Jface operation (it will populate the layers list)
				try {
					getViewSite().getWorkbenchWindow().run(true, true, operation);
				} 
				catch (InvocationTargetException e) {
					// operation canceled
					statusLine.setErrorMessage(e.getMessage());
					return;
				}
				catch ( InterruptedException e) {
					statusLine.setErrorMessage(e.getMessage());
					return;
				}
				
				capabilities = operation.getCapabilities();

				// cache for repeated use
				capabilitiesCache.put(url.toString(), capabilities);
			}
			
			final WMS_Capabilities.Service service = capabilities.getService();
			
			String name = (service.Title != null )
				? service.Name + ": " + service.Title //$NON-NLS-1$
				: service.Name;
			
			name += Messages.getString("WMSView.34")  //$NON-NLS-1$
				+ capabilities.getLayers().size() + "/" //$NON-NLS-1$
				+ capabilities.getTotalLayers() 
				+ Messages.getString("WMSView.36"); //$NON-NLS-1$
				
			
			setStatusMessage(name);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void  setStatusMessage (String text){
		statusMessage.setText(text);
	}
	
	/*
	 * Update the coverage depending on the selected layer
	 */
	private void loadCoverage(boolean showDates, boolean showLatLon
			, String[] dates 
			, Object bbox
			, String[] formats)  
	{
		// Used to control pageComplete
		this.showDates = showDates;
		
		updateVisibility(showDates, showLatLon, formats != null);
		
		if ( showDates ) 
		{
			// clear combos
			tmin.removeAll();
			tmax.removeAll();
	
			// Load date combos if less than 20000 steps
			// otherwise response is too slow
			if ( dates.length < 20000) {
				for (int i = 0; i < dates.length; i++) {
					tmin.add(dates[i]);
					tmax.add(dates[i]);
				}
			}
			else {
				// Let user enter dates  (add the 1st only)
				tmin.add(dates[0]);
				tmax.add(dates[0]);
			}
		}
		
		if ( showLatLon) 
		{
			// Insert lat/lon vals
			if ( bbox != null ) {
				if ( bbox instanceof WMS_Capabilities.BBox ) {
					WMS_Capabilities.BBox box = (WMS_Capabilities.BBox)bbox;
					latMax.setText(box.north);
					latMin.setText(box.south);
					lonMax.setText(box.east);
					lonMin.setText(box.west);
				}
			}
		}
		
		// formats are optional 
		if ( formats != null ) {
			this.formats.removeAll();
			for (String format : formats) {
				this.formats.add(format);
			}
		}
	}

	/*
	 * Update GUI visibility: for layers without a time dimension
	 */
	private void updateVisibility (boolean showDates, boolean showLatLon
			, boolean showFormats) 
	{
		// date combos
		tmin.setVisible(showDates);
		tmax.setVisible(showDates);
		l3.setVisible(showDates);
		l4.setVisible(showDates);
		
		// bbox always visible
//		latMax.setVisible(showLatLon); latMin.setVisible(showLatLon);
//		lonMax.setVisible(showLatLon); lonMin.setVisible(showLatLon);
//		l1.setVisible(showLatLon); l2.setVisible(showLatLon);
		
		// formats always visible
//		formats.setVisible(showFormats);
//		l5.setVisible(showLatLon);
	}
	
	/*
	 * Show the add WMS server dialog
	 */
	private void addServer() 
	{
		final Shell shell = getViewSite().getShell();
		
		// Get Server name, WMS Caps URL from user
		NewRemoteServerDialog dialog = new NewRemoteServerDialog(shell);
		
		final int rc = dialog.open();
		
		if ( rc == Dialog.OK ) 
		{
			try {
				String name = dialog.getName();
				String url 	= dialog.getUrl();

				// Add WMS caps to combo box
				ParserUtils.PublicWMSServer wms = new ParserUtils.PublicWMSServer(name, new URL(url));
				
				logger.debug("Adding user defined WMS: " + name + " Url=" + url); //$NON-NLS-1$ //$NON-NLS-2$
				
				cmbServers.add(name, 0);
				servers.add(0, wms);
				
				// cache
				saveServer(name, url);
			} 
			catch (Exception ex) {
				Messages.showErrorMessage(shell, ex.getMessage());
			}
		}
	}
	
	/**
	 * Build WWJ layers from the users selection when the Submit btn is pressed.
	 * @return
	 */
	private boolean performFinish() 
	{
		try {
			// Grab layers view
			IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
			NavigatorView view = (NavigatorView)Activator.getView(window, NavigatorView.ID); 
					
			// selected layer indices
			int[] indices 	= selectedIndices;

			// image format
			final String format = formats.getText();

			if ( format == null || format.length() == 0 ) {
				Messages.showErrorMessage(getViewSite().getShell(), Messages.getString("WMSView.64")); //$NON-NLS-1$
				return false;
			}
			
			// User selected layers
			WMS_Capabilities.Layer[] selectedLayers = getSelectedLayers(capabilities, indices);
			
			logger.debug("WMS Capabilities ver=" + capabilities.getVersion()); //$NON-NLS-1$
			logger.debug("# of selected layers=" + indices.length  //$NON-NLS-1$
					+ " selected fmt=" + format + " selected layer incides=" + indices ); //$NON-NLS-1$ //$NON-NLS-2$
			
			/**
			 * WMS Caps > 1.1.0 < 1.3.0 map to TiledWMSLayes
			 */
			//if ( selectedLayers[0].ISOTimeSpan == null )  
			if ( capabilities.getVersion().mid < 3)
			{
				// Convert WMS Caps layers to TiledWMSLayer
				TiledWMSLayer[] wwLayers = 
					ParserUtils.newWMSTiledLayer(selectedLayers, format);
				
				// Add to Layers View
				String nodeName = (capabilities.getService().Title != null )
					? capabilities.getService().Title
					: capabilities.getService().Name;
					
				// All layers are disabled by default
				view.addTiledWMSLayers(nodeName, wwLayers, false);
			}
			/* WMS Caps version 1.3.0 map to AnimatedGrounOverlays.  */
			else 
			{
				// Use GroundOverlay or AnimatedGroundOverlay
				logger.debug("WMS version=" + capabilities.getVersion()  //$NON-NLS-1$
						+ " Show dates=" + showDates  //$NON-NLS-1$
						+ " dates size=" + tmin.getItems().length); //$NON-NLS-1$
				
				boolean noTimeSteps = tmin.getItems().length == 1;
				
				// Use GroundOverlayLayer: Each GroundOverlay is a different layer
				if ( !showDates || ( showDates && noTimeSteps))  
				{
					
					GroundOverlayLayer[] ovs = 
						ParserUtils.newGroundOverlay(selectedLayers, format);
					
					// Loop Overlay w/ too many time steps
					// Append "&time=T1/T2/PERIOD" to texture URL
					if ( showDates  ) {
						for (GroundOverlayLayer groundOverlay : ovs) 
						{
							String newURL = groundOverlay.getTextureURL().toString()
								+ "&time=" + tmin.getText()  //$NON-NLS-1$
								+ "/" + tmax.getText(); //$NON-NLS-1$
							
							logger.debug("Too many time steps for " //$NON-NLS-1$
									+ groundOverlay + " using new url " + newURL); //$NON-NLS-1$
							
							groundOverlay.setTextureURL(new URL(newURL));
						}
					}
					
					// Fetch ground overlays to WW cache.
					// This is to speed up response time
					getViewSite().getWorkbenchWindow().run(true, true, new GroundOverlayFetchOperation(ovs));
					
					// KML/KMZ?
					if ( format.equals(SimpleHTTPClient.CT_KML) 
							|| format.equals(SimpleHTTPClient.CT_KMZ)) 
					{
						// process each overlay as a KML object
						for (GroundOverlayLayer groundOverlay : ovs) 
						{
							KMLSource kml = new KMLSource(groundOverlay.getTextureURL());
							logger.debug("Adding kml " + kml.getDocument().getName()); //$NON-NLS-1$
							
							view.addKMLSource(kml, false);
						}
					}
					else {
						// add to the layers view
						//view.addOverlays(ovs, false);
						view.addLayers(ovs, false);
					}
				}
				// Convert selected layers to TimeLoopGroundOverlay 
				else {
					// dates[] should be the same for all layers
					String[] dates = getSelectedTimes();

					
					if ( dates == null) {
						Messages.showErrorMessage(getViewSite().getShell(), Messages.getString("WMSView.63")); //$NON-NLS-1$
						return false;
					}

					logger.debug("User selected dates size=" + dates.length); //$NON-NLS-1$
					
					TimeLoopGroundOverlay[] loopLayers = new TimeLoopGroundOverlay[selectedLayers.length];
					
					for (int i = 0; i < loopLayers.length; i++) {
						loopLayers[i] = 
							ParserUtils.newTimeLoopGroundOverlay (
									selectedLayers[i]
									, dates
									, format);
						
						logger.debug("Adding loop layer: " + loopLayers[i]); //$NON-NLS-1$
					}
					
					//view.addOverlays(loopLayers, false);
					view.addLayers(loopLayers, false);
				}
			}
			
			// show places view
			window.getActivePage().showView(NavigatorView.ID);
			
			return true;
		} 
		catch (Exception e) {
			e.printStackTrace();
			setStatusMessage(e.getMessage());
			return false;
		}
	}
	
	/**
	 * Get selected time span values
	 * @return Array of string, each value represents a date/time value from
	 * 			the layer time span.
	 */
	private String[] getSelectedTimes () {
		int i = tmin.getSelectionIndex();
		int j = tmax.getSelectionIndex();

		if ( i < 0 || j < 0 ) return null;
		
		String[] times = new String[j - i + 1];
		
		for (int k = 0; k < times.length; k++) {
			times[k] = tmin.getItem(i + k);
		}
		return times;
	}
	
	/*
	 * Get user selected WMS layers
	 */
	private WMS_Capabilities.Layer[] getSelectedLayers(WMS_Capabilities capabilities,  int[] indices) 
	{
		WMS_Capabilities.Layer[] layers = new WMS_Capabilities.Layer[indices.length];
		
		for (int i = 0; i < layers.length; i++) {
			layers[i] = capabilities.getLayers().get(indices[i]);
		}
		return layers;
	}
	
	/**
	 * Save user added WMS server name/url to cache 
	 */
	private void saveServer (String name, String wmsUrl) {
		try {
			URL url 	= WorldWind.getDataFileCache().findFile("wms-bookmarks.txt", false); //$NON-NLS-1$
			File file	= null;
			
			if ( url == null)
				file = WorldWind.getDataFileCache().newFile("wms-bookmarks.txt"); //$NON-NLS-1$
			else
				file = new File(url.toURI());
					
			FileWriter fw = new FileWriter(file, true);
			
			logger.debug("Saving wms: " + name + " Url=" + wmsUrl + " to " + file); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			fw.write(name + "|" + wmsUrl + Messages.NL ); //$NON-NLS-1$
			
			fw.close();
		} 
		catch (Exception e) {
		}
	}
	
	/**
	 * load bookmarks
	 * @return
	 */
	private String[] loadBookmarks() {
		try {
			File file = WorldWind.getDataFileCache().newFile("wms-bookmarks.txt"); //$NON-NLS-1$
			RandomAccessFile raf = new RandomAccessFile(file,"r"); //$NON-NLS-1$
			
			byte[] bytes = new byte[(int)raf.length()];
			raf.readFully(bytes);
			raf.close();
			return new String(bytes).split(Messages.NL);
		} 
		catch (Exception e) {
			return null;
		}
	}
	
}
