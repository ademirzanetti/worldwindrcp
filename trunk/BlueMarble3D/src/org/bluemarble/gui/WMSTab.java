package org.bluemarble.gui;

import gov.nasa.worldwind.WorldWind;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.bluemarble.BlueMarble3D;
import org.bluemarble.util.BM3DUtils;
import org.fenggui.Button;
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
import org.fenggui.event.mouse.MouseReleasedEvent;
import org.fenggui.layout.BorderLayout;
import org.fenggui.layout.BorderLayoutData;
import org.fenggui.layout.FormAttachment;
import org.fenggui.layout.FormData;
import org.fenggui.layout.FormLayout;
import org.fenggui.layout.GridLayout;
import org.fenggui.util.Spacing;

import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.SimpleWMSParser;
import worldwind.contrib.parsers.WMS_Capabilities;
import worldwind.contrib.parsers.ParserUtils.PublicWMSServer;
import worldwind.contrib.parsers.WMS_Capabilities.Layer;

public class WMSTab extends Container
{
	private static final Logger logger = Logger.getLogger(WMSTab.class);
	
	// WMS servers caps
	private WMS_Capabilities capabilities;

	// Server names combo
	private ComboBox<ParserUtils.PublicWMSServer> cmbServers;
	
	private Display display;
	
	private LayersList layersList;
	
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

	/**
	 * Layers List class
	 * @author Owner
	 *
	 */
	@SuppressWarnings("unchecked")
	class LayersList extends List {
		public LayersList() {
			super();
			setupTheme(LayersList.class);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void mouseReleased(MouseReleasedEvent mr) 
		{
			List list 	= (List)mr.getSource();
			int row 	= list.getMouseOverRow();
			
			if ( row < 0 ) return;
			
			handleListSelection(row);
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
		
		Container c1 = buildWMServersSection();
		Container c2 = buildWMSLayersSection(c1);
		Container c3 = buildWMSCoverageSection(c2);
		
		addWidget(c1);
		addWidget(c2);
		addWidget(c3);
		
		try {
			initialize();
			//loadServers();
		} catch (Exception e) {
			// A bug!
			e.printStackTrace();
			BM3DUtils.MessageBox(display, e.getMessage());
		}
	}
	
	/**
	 * WMS LayersA A label plus Layers list
	 * @param prev
	 * @return
	 */
	private Container buildWMSLayersSection (Container prev)
	{
		Container c 			= new Container(new BorderLayout()); 
		ScrollContainer listSC 	= new ScrollContainer();

		c.getAppearance().setPadding(new Spacing(5,5));
		
		lStatus = new Label("Layers");
		lStatus.setLayoutData(BorderLayoutData.NORTH);
		
		layersList = new LayersList();
		layersList.setHeight(150);
		
		listSC.addWidget(layersList);
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
	private Container buildWMSCoverageSection (Container prev)
	{
		Container c = new Container(new GridLayout(6, 3));
		c.getAppearance().setPadding(new Spacing(5,5));

		// row 1 : Lat
		FengGUI.createLabel(c, "Latitude");
		latMin = FengGUI.createTextField(c);
		latMin.setShrinkable(false);

		latMax = FengGUI.createTextField(c);
		latMax.setShrinkable(false);
		
		// row 2
		c.addWidget(new Label("Longitude"));
		lonMin = FengGUI.createTextField(c); 
		lonMax = FengGUI.createTextField(c); 

		// row 3
		c.addWidget(new Label("Format"));
		formats = FengGUI.createComboBox(c); 
		c.addWidget(new Label(""));

		// row 4
		l3 = FengGUI.createLabel(c, "Start time"); 
		
		tmin = FengGUI.createTextField(c); 
		c.addWidget(new Label(""));

		// row 5
		l4 = FengGUI.createLabel(c, "End time"); 
		
		tmax = FengGUI.createTextField(c); 
		c.addWidget(new Label(""));

		// row 6 - Go button
		Button goButton = FengGUI.createButton(c, "Go");
		goButton.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				System.out.println("Finish.");
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
	private Container buildWMServersSection ()
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
				layersList.clear();
				
				// Ad layers to the list box
				for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
					layersList.addItem(layer.Title);
				}

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
						layersList.clear();
						
						// Load table with layer names
						for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
							layersList.addItem(layer.Title);
						}
						
						// Done. Show status
						lStatus.setText(server.name + " has " 
								+ capabilities.getLayers().size() + " layers." );
					}
					catch ( NullPointerException e0){
						e0.printStackTrace();
					}
					catch (Exception e) {
						BM3DUtils.MessageBox(display, e.getMessage());
						lStatus.setText("Layers");
					}
				}
			}).start();
		}
		else {
			System.out.println("duplicate call");
		}
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
					//logger.debug("Building ISO time list for " + layer.ISOTimeSpan); //$NON-NLS-1$
						
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
	}
	
	/**
	 * Parse WMS URLs from a local HTML list
	 */
	private void initialize() throws Exception
	{
		// load public cmbServers from config folder
		String list = "config/skylab_public_wmslist.html";
		
	    InputStream buffer  = new BufferedInputStream(BM3DUtils.getInputStream(WMSTab.class, list));   
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
		lStatus.setText(text);
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
	
}
