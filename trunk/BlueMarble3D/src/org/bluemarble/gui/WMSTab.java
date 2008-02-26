package org.bluemarble.gui;

import gov.nasa.worldwind.WorldWind;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.bluemarble.BlueMarble3D;
import org.bluemarble.util.BlueMarbeUtils;
import org.fenggui.Button;
import org.fenggui.CheckBox;
import org.fenggui.ComboBox;
import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.FengGUI;
import org.fenggui.Label;
import org.fenggui.LayoutManager;
import org.fenggui.List;
import org.fenggui.ListItem;
import org.fenggui.ScrollContainer;
import org.fenggui.TextEditor;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.event.ISelectionChangedListener;
import org.fenggui.event.SelectionChangedEvent;
import org.fenggui.event.mouse.MousePressedEvent;
import org.fenggui.event.mouse.MouseReleasedEvent;
import org.fenggui.layout.BorderLayout;
import org.fenggui.layout.BorderLayoutData;
import org.fenggui.layout.FormAttachment;
import org.fenggui.layout.FormData;
import org.fenggui.layout.FormLayout;
import org.fenggui.layout.GridLayout;
import org.fenggui.table.Table;
import org.fenggui.util.Color;
import org.fenggui.util.Spacing;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.SimpleWMSParser;
import worldwind.contrib.parsers.WMS_Capabilities;
import worldwind.contrib.parsers.ParserUtils.PublicWMSServer;
import worldwind.contrib.parsers.WMS_Capabilities.Layer;

/**
 * WMS Tab
 * @author Owner
 *
 */
public class WMSTab extends Container
{
	private static final Logger logger = Logger.getLogger(WMSTab.class);
	
	// WMS servers caps
	private WMS_Capabilities capabilities;

	// Server names combo
	private ComboBox<ParserUtils.PublicWMSServer> cmbServers;
	
	private Display display;
	
	// Layers table within a scroll container
	private final ScrollContainer listSC 	= new ScrollContainer();
	private final WMSLayersTable layersList = new WMSLayersTable();
	
	private Label lStatus;
	
	// Used to cache capabilities for increased performance
	private static Hashtable<String, WMS_Capabilities> capabilitiesCache
		= new Hashtable<String, WMS_Capabilities>();
	
	// bbox
	private TextEditor latMin, latMax, lonMin, lonMax, tmin, tmax;

	// request formats
	private ComboBox<String> formats;

	// Date labels (may be hidden dynamically) 
	private Label l3,l4;

	@SuppressWarnings("unchecked")
	private CheckBox chkUseTiles;
	
	/**
	 * WMS Layers Table class
	 * @author Owner
	 *
	 */
	@SuppressWarnings("unchecked")
	class WMSLayersTable extends Table 
	{
		public WMSLayersTable() {
			super();
			setupTheme(WMSLayersTable.class);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void mouseReleased(MouseReleasedEvent mr) 
		{
			//List list 	= (List)mr.getSource();
			Table list 	= (Table)mr.getSource();
			if (list.getModel() == null ) return;
			
			int row 	= list.getSelection(); //getMouseOverRow();
			if ( row < 0 ) return;
			
			handleListSelection(row);
		}
		
		@Override
		public void mousePressed(MousePressedEvent mp) {
			try {
				super.mousePressed(mp);
			} catch (Exception e) {	}
		}
		
		/**
		 * Get all selected indices
		 */
		public int[] getSelectionIndices()
		{
			int[] indices = new int[getSelectionCount()];
			int j = 0;
			
			if (getModel() == null)
				throw new IllegalStateException("No table model set!");

			for (int i = 0; i < selected.length; i++)
			{
				if (selected[i] == true)
					indices[j++] = i;
			}
			return indices;
		}
	}
	
	
	/**
	 * Default constructor
	 */
	public WMSTab(Display display) {
		this(display, new FormLayout());
	}
	
	/**
	 * Constructor 
	 * @param lm
	 */
	public WMSTab(Display display, LayoutManager lm) {
		super(lm);
		this.display = display;
		
		Container c1 = buildServersSection();
		Container c2 = buildLayersSection(c1);
		Container c3 = buildCoverageSection(c2);
		
		addWidget(c1);
		addWidget(c2);
		addWidget(c3);
		
		try {
			initialize();
			//loadServers();
		} catch (Exception e) {
			// A bug!
			e.printStackTrace();
			BlueMarbeUtils.MessageBox(display, e.getMessage());
		}
	}
	
	/**
	 * WMS LayersA A label plus Layers list
	 * @param prev
	 * @return
	 */
	private Container buildLayersSection (Container prev)
	{
		Container c 			= new Container(new BorderLayout()); 
		//ScrollContainer listSC 	= new ScrollContainer();

		c.getAppearance().setPadding(new Spacing(5,5));
		
		lStatus = new Label("Layers");
		lStatus.setLayoutData(BorderLayoutData.NORTH);
		
		//layersList = new WMSLayersTable();
		layersList.getAppearance().setHeaderVisible(false);
		layersList.getAppearance().setGridVisible(false);
		layersList.setMultipleSelection(true);
		layersList.setHeight(150);
		
		listSC.setInnerWidget(layersList);
		listSC.setLayoutData(BorderLayoutData.CENTER);
		
		c.addWidget(lStatus);
		c.addWidget(listSC);
		
		FormData fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(prev,0);
 		fd.bottom = new FormAttachment(30,0);

		c.setLayoutData(fd);
		return c;
	}
	
	/**
	 * Coverage
	 * @param prev
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Container buildCoverageSection (Container prev)
	{
		Container c = new Container(new GridLayout(7, 3));
		c.getAppearance().setPadding(new Spacing(5,5));

		// row 1 : Lat
		FengGUI.createLabel(c, "Latitude");
		latMin = FengGUI.createTextField(c);
		latMin.setShrinkable(false);
		latMin.getAppearance().getCursorPainter().setCursorColor(Color.WHITE);
		
		latMax = FengGUI.createTextField(c);
		latMax.setShrinkable(false);
		latMax.getAppearance().getCursorPainter().setCursorColor(Color.WHITE);
		
		// row 2
		c.addWidget(new Label("Longitude"));
		lonMin = FengGUI.createTextField(c); 
		lonMax = FengGUI.createTextField(c); 

		lonMin.getAppearance().getCursorPainter().setCursorColor(Color.WHITE);
		lonMax.getAppearance().getCursorPainter().setCursorColor(Color.WHITE);
		
		// row 3
		c.addWidget(new Label("Format"));
		formats = FengGUI.createComboBox(c); 
		c.addWidget(new Label(""));

		// row 4
		l3 = FengGUI.createLabel(c, "Start time"); 
		
		tmin = FengGUI.createTextField(c); 
		tmin.getAppearance().getCursorPainter().setCursorColor(Color.WHITE);
		
		c.addWidget(new Label(""));

		// row 5
		l4 = FengGUI.createLabel(c, "End time"); 
		
		tmax = FengGUI.createTextField(c); 
		tmax.getAppearance().getCursorPainter().setCursorColor(Color.WHITE);
		
		c.addWidget(new Label(""));

		// row 6
		c.addWidget(new Label(""));
		chkUseTiles = FengGUI.createCheckBox(c, "Tile images");
		c.addWidget(new Label(""));
		
		// row 7 - Go button
		Button goButton = FengGUI.createButton(c, "Go");
		goButton.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				performFinish();
			}
		});	

		c.addWidget(new Label(""));
		c.addWidget(new Label(""));
		
		// Layout stuff
		FormData fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(prev,0);
//		fd.bottom = new FormAttachment(10,0);

		c.setLayoutData(fd);
		
		return c;
	}
	
	/**
	 * WMS Servers section: Label (Servers), A Servers combo, and a 'New' button
	 * @return
	 */
	private Container buildServersSection ()
	{
		Container c = new Container(new GridLayout(1, 3));
		c.getAppearance().setPadding(new Spacing(5,5));
		
		cmbServers = new ComboBox<ParserUtils.PublicWMSServer>();
		cmbServers.addSelectionChangedListener(new ISelectionChangedListener()
		{
			// This method fires twice...
			public void selectionChanged(SelectionChangedEvent e) 
			{
				int row = cmbServers.getList().getMouseOverRow();
				if ( row < 0 ) return;
				
				handleComboSeletion(cmbServers.getList(), row);
			}
			
		});
		
		c.addWidget(new Label("Server"));
		c.addWidget(cmbServers);
		c.addWidget(new Button("New"));
		
		FormData fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(100,0);
		
		c.setLayoutData(fd);
		return c;
	}
	
	// Used to avoid duplicate calls to handleComboSelection
	int currRow = -1;
	
	/**
	 * Handle combo selection. This method fires twice per selection
	 * @param servers
	 * @param row
	 */
	private void handleComboSeletion ( List<ParserUtils.PublicWMSServer> servers, int row) 
	{
		final PublicWMSServer server = servers.getItem(row).getValue();
		
		// Avoid duplicate calls
		if ( currRow != row)
		{
			currRow = row;
			
			URL url = server.capabilitiesURL;
			
			// Search CAPS in cache first
			if ( capabilitiesCache.containsKey(url.toString())) 
			{
				// load from cache
				capabilities = capabilitiesCache.get(url.toString());
				
				// Populate list: remove any previous elements
				//layersList.clear();
				
				// Ad layers to the list box
//				for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
//					layersList.addItem(layer.Title);
//				}
				layersList.setModel(new WMSLayersTableModel(capabilities.getLayers()));
				listSC.layout();
				
				// Status
				lStatus.setText(server.name + " has " 
						+ capabilities.getLayers().size() + " layers." );
				
				return;
			}
			
			// WMS CAPS not in cache. Use a thread to load the WMS TOC
			new Thread(new Runnable() 
			{
				public void run() 
				{
					try 
					{
						lStatus.setText("Loading " + server.name + "...");
						
						// Long operation: load WMS Caps XML via HTTP
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						SimpleHTTPClient client = new SimpleHTTPClient(server.capabilitiesURL);
						client.doGet(bos); 
						bos.close();
						
						// parse WMS XML
						SimpleWMSParser parser = new SimpleWMSParser();
						parser.parse(server.name, new ByteArrayInputStream(bos.toString().getBytes()));

						capabilities 	= parser.getCapabilities();
						
						// cache WMS Caps for repeated use
						capabilitiesCache.put(server.capabilitiesURL.toString(), capabilities);

						// clear table
						//layersList.clear();
						
						// Load table with layer names
//						for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
//							layersList.addItem(layer.Title);
//						}
						layersList.setModel(new WMSLayersTableModel(capabilities.getLayers()));
						listSC.layout();
						
						// Done. Show status
						lStatus.setText(server.name + " has " 
								+ capabilities.getLayers().size() + " layers." );
					}
					catch ( NullPointerException e0){
						e0.printStackTrace();
					}
					catch (Exception e) {
						BlueMarbeUtils.MessageBox(display, e.getMessage());
						setStatusMessage(null); // reset
					}
				}
			}).start();
		}
//		else {
//			System.out.println("duplicate call");
//		}
	}
	
	/**
	 * Handle List selection
	 * @param row
	 */
	private void handleListSelection ( int row )  
	{
		try {
			
			// Save indices to extract Layers from Caps obj later on
//			selectedIndices = indices;

			if ( capabilities != null ) 
			{
				// extract date range/bbox of the selected layer
				Layer layer = capabilities.getLayers().get(row);
				
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
				
				// Use a TiledWMSLayer if the 1st layer has no time span
				//chkUseTiles.setSelection(layer.ISOTimeSpan == null);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Update the coverage depending on the selected layer
	 */
	private void loadCoverage(boolean showDates, boolean showLatLon
			, String[] dates 
			, Object bbox
			, String[] formats)  
	{
		// Used to control pageComplete
//		this.showDates = showDates;
		
		updateVisibility(showDates, showLatLon, formats != null);
		
		if ( showDates ) 
		{
			// clear combos
//			tmin.removeAll();
//			tmax.removeAll();
	
			// Load date combos if less than 20000 steps
			// otherwise response is too slow
//			if ( dates.length < 20000) {
//				for (int i = 0; i < dates.length; i++) {
//					tmin.add(dates[i]);
//					tmax.add(dates[i]);
//				}
//			}
//			else {
				// Let user enter dates  (add the 1st only)
				tmin.setText(dates[0]);
				tmax.setText(dates[dates.length - 1]);
//			}
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
			this.formats.getList().clear();
			for (String format : formats) {
				this.formats.addItem(format);
			}
		}
		
		// Use WW tiles for layers with no time dimension (usually wms 1.1.x)
		chkUseTiles.setSelected(! tmin.isVisible());
	}
	
	/**
	 * Parse WMS URLs from a local HTML list
	 */
	private void initialize() throws Exception
	{
		// load public cmbServers from config folder
		String list = "config/skylab_public_wmslist.html";
		
	    InputStream buffer  = new BufferedInputStream(BlueMarbeUtils.getInputStream(WMSTab.class, list));   
	    OutputStream os = new ByteArrayOutputStream();
		
	    ParserUtils.readStream(buffer, os);
	    os.close();
	    
	    // parse WMS server
	    Vector<ParserUtils.PublicWMSServer> servers = ParserUtils.parsePublicWmsHTTPPage( os.toString() );
	    
	    loadServers(servers);
	}

	/*
	 * Load WMS servers into serverCombo box
	 */
	private void loadServers(Vector<ParserUtils.PublicWMSServer> servers)
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
		
		// load servers from vector
		for (ParserUtils.PublicWMSServer server : servers) 
		{
			if (server.name != null ) {
				ListItem<ParserUtils.PublicWMSServer> li = 
					new ListItem<ParserUtils.PublicWMSServer>(server.name);
				
				li.setValue(server);
				cmbServers.addItem(li);
			}
		}
	}
	
	
	private void  setStatusMessage (String text){
		if ( text == null ) lStatus.setText("Layers"); // reset
		else lStatus.setText(text);
	}

	/**
	 * load WMS book marks
	 * @return
	 */
	private String[] loadBookmarks() {
		try {
			File file = WorldWind.getDataFileCache().newFile("wms-bookmarks.txt"); //$NON-NLS-1$
			RandomAccessFile raf = new RandomAccessFile(file,"r"); //$NON-NLS-1$
			
			byte[] bytes = new byte[(int)raf.length()];
			raf.readFully(bytes);
			raf.close();
			return new String(bytes).split(BlueMarble3D.NL);
		} 
		catch (Exception e) {
			return null;
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
	}

	/**
	 * Submit routine
	 */
	private void performFinish() 
	{
		try {
			if ( layersList.getModel() == null) 
				return;
			
			// Grab Nav window
			NavigatorWindow view = (NavigatorWindow)getParent().getParent().getParent();
			
			// selected table indices
			int[] indices = layersList.getSelectionIndices();
			
			// image format
			final String format = formats.getSelectedValue();
			
			if ( indices == null || indices.length == 0) {
				BlueMarbeUtils.MessageBox(display, "Select one or more layers.");
				return ;
			}
			
			// User selected WMS layers
			WMS_Capabilities.Layer[] selectedLayers = getSelectedLayers(indices);
			
			logger.debug("WMS Capabilities ver=" + capabilities.getVersion()); //$NON-NLS-1$
			logger.debug("Number of selected layers=" + indices.length  //$NON-NLS-1$
					+ " selected fmt=" + format);
			
			/**
			 * Use TiledWMSLayes, commonly for WMS Caps < 1.3.0 
			 */
			if ( chkUseTiles.isSelected() )  
			{
				handleTiledWMS(selectedLayers, format, view);
			}
			/**
			 *  Use ground overlays: TimeLoopGrounOverlays | GroundOverlay.  
			 */
			else 
			{
				// Use GroundOverlay or AnimatedGroundOverlay
				logger.debug("Using Overlays."); 
				
				//boolean noTimeSteps = tmin.getItems().length == 1;
				boolean isKML = format.equals(SimpleHTTPClient.CT_KML) 
								|| format.equals(SimpleHTTPClient.CT_KMZ);

				if ( isKML )
				{
					logger.debug("KML/KMZ detected.");
					
					handleKmlKmz(selectedLayers, format, view);
				}
				// Use GroundOverlayLayer: Each GroundOverlay is a different layer
				else if ( !tmin.isVisible() ) // ! showDates )   
				{
					logger.debug("Using GroundOverlays.");
					
					GroundOverlayLayer[] ovs = 
						ParserUtils.newGroundOverlay(selectedLayers, format);
					
					// add to the layers view
					view.addLayers(capabilities.getService().Title, ovs); //, false);
					
				}
				// Convert selected layers to TimeLoopGroundOverlay(s) 
				else {
					handleTimeLoop(selectedLayers, format, view);
				}
			}
			
			// Show layers
			view.showLayers();
		} 
		catch (NullPointerException e) {
			e.printStackTrace(); // bug
		}
		catch ( Exception e) {
			BlueMarbeUtils.MessageBox(display, e.getMessage());
		}
	}
	
	/**
	 * Get user selected WMS layers
	 */
	private WMS_Capabilities.Layer[] getSelectedLayers(int[] indices) 
	{
		WMS_Capabilities.Layer[] layers = new WMS_Capabilities.Layer[indices.length];
		
		for (int i = 0; i < layers.length; i++) {
			layers[i] = capabilities.getLayers().get(indices[i]);
		}
		return layers;
	}
	
	/**
	 * Tiled WMS 
	 * @param selectedLayers
	 * @param format
	 * @param view
	 */
	private void handleTiledWMS (WMS_Capabilities.Layer[] selectedLayers
			, String format, NavigatorWindow view )
	{
		// Convert WMS Caps layers to TiledWMSLayer
		TiledWMSLayer[] wwLayers = 
			ParserUtils.newWMSTiledLayer(selectedLayers, format);
		
		// Add to Layers View
		String nodeName = (capabilities.getService().Title != null )
			? capabilities.getService().Title
			: capabilities.getService().Name;
		
		logger.debug("Using tiled WMS layers. Parent node name=" 
				+ nodeName + " Num layers=" + wwLayers.length);
		
		// All layers are disabled by default
		view.addLayers(nodeName, wwLayers); //, false);
		
	}	
	
	/**
	 * Handle Time series WMS layers
	 * @param selectedLayers
	 * @param format
	 * @param view
	 */
	private void handleTimeLoop (WMS_Capabilities.Layer[] selectedLayers, String format, NavigatorWindow view )
		throws MalformedURLException
	{
		// dates[] should be the same for all layers
		String[] dates = getSelectedTimes();
		
		// Mzx # of allowed time steps
		int MAX_DATE_LEN = 100;

		logger.debug("Using TimeLoopOverlays. Dates size=" + dates.length); //$NON-NLS-1$
		
		if ( dates.length > MAX_DATE_LEN) {
			BlueMarbeUtils.MessageBox(display, "Time span too large: " 
					+ dates.length + " . Max =" + MAX_DATE_LEN);
			return;
		}
		
		TimeLoopGroundOverlay loopLayer;
		
		for (int i = 0; i < selectedLayers.length; i++) 
		{
			loopLayer = //s[i] = 
				ParserUtils.newTimeLoopGroundOverlay (
						selectedLayers[i]
						, dates
						, format);
			
			loopLayer.setEnabled(false);
			
			logger.debug("Submitting loop layer: " + loopLayer);//$NON-NLS-1$
			view.addLayer(loopLayer);
		}
	}
	
	/**
	 * Handle Google Earth Docs
	 * @param selectedLayers
	 * @param format
	 * @param view
	 */
	private void handleKmlKmz (WMS_Capabilities.Layer[] selectedLayers, final String format, final NavigatorWindow view )
		throws Exception
	{
		// Build overlay from selected layers
		final GroundOverlayLayer[] overlays = 
			ParserUtils.newGroundOverlay(selectedLayers, format);

		// Reformat single file GroundOverlay URLs with ...&time=T1/T2
		for (GroundOverlayLayer groundOverlay : overlays) 
		{
			String newURL = groundOverlay.getTextureURL().toString()
				+ "&time=" + tmin.getText()  //$NON-NLS-1$
				+ "/" + tmax.getText(); 	//$NON-NLS-1$
			
			logger.debug("Changing URL for " //$NON-NLS-1$
					+ groundOverlay + " to " + newURL); //$NON-NLS-1$
			
			groundOverlay.setTextureURL(new URL(newURL));
		}
		
		setStatusMessage("Fetching " + selectedLayers.length + " layers...");
		
		// Pre-fetch overlays
		new Thread(new Runnable() 
		{
			public void run() 
			{
				for (GroundOverlayLayer overlay : overlays) 
				{
					// fetch synchronously
					overlay.fetchOverlay(true);
					
					try {
						// Add to the view
						view.addKMLSource(new KMLSource(overlay.getFileFromCache(), format) );
					} 
					catch (Exception e) {
						BlueMarbeUtils.MessageBox(display, "Unable to load KML: " + e.getMessage());
						e.printStackTrace();
					}
				}
				setStatusMessage(null);
			}
		}).start();
	}
	
	/**
	 * Build an array of ISO dates for the selected layers
	 * @return String array of dates for the user selected time span
	 */
	private String[] getSelectedTimes () 
	{
		// selected table indices
		int[] selectedIndices = layersList.getSelectionIndices();
		
		// WMS time dimension of the 1st selected layer
		String isoTime 	= capabilities.getLayers().get(selectedIndices[0]).ISOTimeSpan;
		String[] tmp 	= isoTime.split("/");
		String period 	= (tmp != null && tmp.length == 3 ) ? tmp[2] : null;
		
		logger.debug("Manual input for time span? Layer iso time=" + isoTime + " Period=" +period);

		if ( period != null) {
			String iso = tmin.getText() + "/" + tmax.getText() + "/" + period;
			try {
				return WMS_Capabilities.buildWMSTimeList(iso).split(",");
			} catch (Exception e) {
				return new String[] { tmin.getText(), tmax.getText() };
			}
		}
		else
			return new String[] { tmin.getText(), tmax.getText() };
		
	}
}
