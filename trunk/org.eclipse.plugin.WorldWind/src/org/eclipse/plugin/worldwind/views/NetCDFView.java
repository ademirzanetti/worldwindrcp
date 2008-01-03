package org.eclipse.plugin.worldwind.views;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import netcdf.grid.plot.Plot;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;

import thredds.datatype.DateRange;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.NamedObject;
import ucar.unidata.geoloc.LatLonRect;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.ScreenOverlayLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

/**
 * NetCDF View: 
 * 
 * NetCDF (network Common Data Form) is a set of software libraries 
 * and machine-independent data formats that support the creation, access, and 
 * sharing of array-oriented scientific data.<p/>
 * 
 *  http://www.unidata.ucar.edu/software/netcdf/
 *  
 * NetCDF Java Library 
 * 	http://www.unidata.ucar.edu/software/netcdf-java/
 * 
 * <p/>
 * Uses a custom Plot library for NetCDF (netcdf-plot.jar). For example:
 * <pre>
 * 	String uri = "/path/CCTM_J3a_b313.12km.20010101";
 *
 *	// NetCDF Georef dataset
 *	GridDataset gds = GridDataset.open(uri);
 *	GeoGrid grid 	= gds.findGridByName("PM25");
 *
 *	// Data slice t = 0, lev = 0
 *	Array aYX 		= grid.readYXData(0, 0);
 *
 *	Plot plot = new Plot();
 *
 *	// plot slice, transp color (223, 223, 223)
 *	BufferedImage img = plot.plotGrid(gds, grid, aYX
 *			, new Color(223, 223, 223) );
 * </pre>
 * @author Vladimir Silva
 *
 */
public class NetCDFView extends ViewPart
	implements Listener 
{
	private static final Logger logger = Logger.getLogger(NetCDFView.class);
	
	public static final String ID = NetCDFView.class.getName();

	// state of the user interface
	enum UI_STATE { IDLE, PLOTTING_SUBSETTING};
	
	private TableViewer viewer;

	// Netcdf grid dataset
	private GridDataset dataset;
	
	//Widgets: bbox
	private Text latMin, latMax, lonMin, lonMax;
	
	// vertical level (z)
	private Combo lev;
	
	// time range
	private Combo tmin, tmax;
	
	// Action btos
	private Button plot, subset;
	
	// staus message
	private Label statusMessage;

	// metadata
	private Label metaData;
	
	// Eclipse forms support
	private FormToolkit toolkit;
	private ScrolledForm scrolledForm;
	
	

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) {
		toolkit 		= new FormToolkit(parent.getDisplay());
		scrolledForm 	= toolkit.createScrolledForm(parent);

		scrolledForm.setText("NetCDF Viewer");
		toolkit.decorateFormHeading(scrolledForm.getForm());
		
		// Create UI
		TableWrapLayout layout 	= new TableWrapLayout();
		layout.numColumns 		= 2;

		scrolledForm.getBody().setLayout(layout);

		int expanded 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED;
		int collapsed 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE;

		// create UI elements
		createMetaSection("Information", null, collapsed, 2);

		viewer 	= createGridsSection("Grids", null, expanded, 2);
		
		createDimsSection("Dimensions", null, expanded, 2);
		
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		scrolledForm.setFocus();
	}
	
	
	/*
	 * Grids section
	 */
	private TableViewer createGridsSection(String title
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
		
		// grids table
		TableWrapData td ;
		Table table = toolkit.createTable(sectionClient, SWT.FILL);
		
		td 				= new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan 		= 2;
		td.heightHint 	= 100;
		
		table.setLayoutData(td);
		
		section.setClient(sectionClient);
	
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
	private void createDimsSection(String title
			, String description
			, int style, int colSpan) 
	{
		Section section 		= createSection(title, description, style, colSpan);
		Composite sectionClient = toolkit.createComposite(section);
		
		sectionClient.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TEXT_BORDER);
		toolkit.paintBordersFor(sectionClient);
		
		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 3;

		sectionClient.setLayout(layout); 
		
		// Lat
		toolkit.createLabel(sectionClient, "Latitude", SWT.NONE); //Label l1 = 
		
		latMin = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY);
		latMin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		latMax = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY);
		latMax.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// Lon
		toolkit.createLabel(sectionClient, "Longitude", SWT.NONE); // Label l2 = 
		//l2.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		lonMin = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY);
		lonMin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		lonMax = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY);
		lonMax.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// Z(level)  
		toolkit.createLabel(sectionClient, "Level", SWT.NONE); //Label l3 = 
		//l3.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		TableWrapData td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		lev = new Combo(sectionClient, SWT.READ_ONLY);
		lev.setLayoutData(td); //new TableWrapData(TableWrapData.LEFT));
		lev.addListener(SWT.Selection, this);
		//lev.addListener(SWT.DefaultSelection, this);

		// tmin
		toolkit.createLabel(sectionClient, "Time min", SWT.NONE); //Label l4 = 
		//l4.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		tmin = new Combo(sectionClient, SWT.READ_ONLY);
		
		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		tmin.setLayoutData(td); //new TableWrapData(TableWrapData.FILL_GRAB));
		tmin.addListener(SWT.Selection, this);
		tmin.addListener(SWT.DefaultSelection, this);
		
		// tmax
		toolkit.createLabel(sectionClient, "Time max", SWT.NONE); // Label l5 = 
		//l5.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		tmax = new Combo(sectionClient, SWT.READ_ONLY);
		
		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		tmax.setLayoutData(td); //new TableWrapData(TableWrapData.FILL_GRAB));
		tmax.addListener(SWT.Selection, this);
		tmax.addListener(SWT.DefaultSelection, this);

		// progress bar
		statusMessage = toolkit.createLabel(sectionClient, "", SWT.NONE);
//		pb = new ProgressBar(sectionClient, SWT.HORIZONTAL);
//		pb.setVisible(false);

		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 3;
		
//		pb.setLayoutData(td);
		statusMessage.setLayoutData(td);
		
		// plot btn
		plot = toolkit.createButton(sectionClient, "Plot", SWT.NONE);
		plot.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		plot.addListener(SWT.Selection, this);
		
		// subset btn
		subset = toolkit.createButton(sectionClient, "Subset", SWT.NONE);
		subset.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		subset.addListener(SWT.Selection, this);
		
		section.setClient(sectionClient);
	}

	/*
	 * Dataset meta data section
	 */
	private void createMetaSection(String title
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
		
		// Metadata label
		metaData= toolkit.createLabel(sectionClient, "", SWT.WRAP); 
		
		TableWrapData td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;

		metaData.setLayoutData(td);
		
		section.setClient(sectionClient);
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
	
	/**
	 * Handle GUI events: Mouse clicks, button presses, etc
	 */
	public void handleEvent(Event event) 
	{
		Widget w = event.widget;
		
		try {
			if ( w instanceof Table ) 
			{
				IStructuredSelection sel = (IStructuredSelection) viewer.getSelection();
				
				final GeoGrid grid = (GeoGrid)sel.getFirstElement();
				
				updateDimensions(grid);
			}
			else if ( w == plot) 
			{
				// Plot
				firePlotSubsetThread(true);
			}
			else if ( w == subset) 
			{
				// subset
				firePlotSubsetThread(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Plot grid
	 */
	private void plotGrid() //throws IOException //, URISyntaxException
	{
		int t1 = tmin.getSelectionIndex();
		int t2 = tmax.getSelectionIndex();
		
		int z = lev.getSelectionIndex();
		
		logger.debug("t=[" + t1 + "," + t2 + "] z=" + z);
		
		// z size can be 0, t should be > 0
		if (( t1 == -1 || t2 == -1) ) { // && tmin.getItemCount() > 0 ) {
			MessageDialog.openInformation(getSite().getShell()
					, "NetCDFView"
					, "Select a time range");
			return;
		}
		
		if ( z == -1 ) z = 0;
		
		// plotter
		Plot plotter = new Plot();
		
		// loop ov
		TimeLoopGroundOverlay loop = new TimeLoopGroundOverlay((new File(dataset.getLocationURI())).getName());
		
		// bbox
		LatLonRect bbox = dataset.getBoundingBox();
		
		for (int i = t1; i <= t2; i++) 
		{
			// get grid
			IStructuredSelection sel 	= (IStructuredSelection) viewer.getSelection();
			final GeoGrid grid 			= (GeoGrid)sel.getFirstElement();

			// WW cache name
			final String cacheName = "Earth" 
				+ "/" + (new File(dataset.getLocationURI())).getName() 
				+ "/" + grid.getName() + ".t" + i + ".z" + z;

			URL frame = WorldWind.getDataFileCache().findFile(cacheName + ".png", false);
			
			// if file not in cache plot
			if (  frame == null) 
			{
				final File imF 		= WorldWind.getDataFileCache().newFile(cacheName + ".png");
				final File legendF 	= WorldWind.getDataFileCache().newFile(cacheName + ".legend.png");
				
				logger.debug("Plotting:" + cacheName + " to " + imF);
				
				statusMessage.setText("t=" + i + " z=" +  lev.getText() + " to " + cacheName);
				
				try {
					// plot: returns a buf img + legend
					BufferedImage im = plotter.plotGrid(dataset, grid
							, grid.readYXData(i, z)
							, new Color(223, 223, 223));
					
					// write png: im + legend
					ImageIO.write(im, "PNG", imF );
					ImageIO.write(plotter.getLegend(), "PNG", legendF );
					
					frame = imF.toURL();
				} 
				catch (IOException e) {
					statusMessage.setText(grid.getName() + " t=" + i + " z=" + lev.getText() 
							+ " " + e.getMessage());
				}
			}
			else {
				logger.debug(cacheName + " already in cache");
			}
			
			// add the ground ov to the loop
			loop.add(new GroundOverlayLayer(
					tmin.getItem(i)	// time step is the overlay name
					, new Sector(Angle.fromDegrees(bbox.getLatMin())
							, Angle.fromDegrees(bbox.getLatMax())
							, Angle.fromDegrees(bbox.getLonMin())
							, Angle.fromDegrees(bbox.getLonMax())
							)
					, frame, ".png"
					));
			
			// grab 1st legend
			if ( i  == t1 ) 
			{
				final URL legendU = WorldWind.getDataFileCache().findFile(cacheName + ".legend.png", false);
				if ( legendU != null)
					loop.setLegend(new ScreenOverlayLayer("legend"
						, legendU
						, ScreenOverlayLayer.SOUTHEAST));
			}
		}
		
		logger.debug(loop.toKML());
		
		// Add loop to the earth view
	}

	/*
	 * Fires when the subset btn is pressed
	 */
	private void subsetGrid ()
	{
		try {
			// get grid
			IStructuredSelection sel 	= (IStructuredSelection) viewer.getSelection();
			final GeoGrid grid 			= (GeoGrid)sel.getFirstElement();

			if ( tmin.getSelectionIndex() == -1 ) {
				MessageDialog.openInformation(getSite().getShell()
						, "NetCDFView"
						, "Select a time range");
				return;
			}
/*			
			Range t_range = new Range(tmin.getSelectionIndex()
					, tmax.getSelectionIndex());

			// Use selected vertical level
			int z = lev.getSelectionIndex() == -1 ? 0 : lev.getSelectionIndex(); 
				
			Range z_range = new Range(z, z);

			// subset by t & z ranges (all Y, X values)
			GeoGrid newGrid = grid.subset(t_range, z_range, null, null);
*/
			// get a file name from the user
			FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN);
			
			dialog.setFilterExtensions(new String[] { "*.*" } );
			dialog.setFilterNames(new String[] { "All files (*.*)"});
			
			// User defined path
			String fileName 		= dialog.open();

			if ( fileName == null)
				return;

			// Does file exist?
			if ( new File(fileName).exists()) 
				if ( ! MessageDialog.openQuestion(getSite().getShell()
						, "NetCDFView"
						, fileName +  " already exists. Replace?"))
					return;
			
			//logger.debug("t range=" + t_range + " z range=" + z_range + " out=" + fileOut);
			
			/*
			 * Subset by date range only: All vertical levels (Z), All Y, X
			 */
		    NetcdfCFWriter writer = new NetcdfCFWriter();

		    List<String> gridList = new ArrayList<String>();
		    gridList.add(grid.getName());
		    //gridList.add(newGrid.getName());

		    DateFormatter format 	= new DateFormatter();
		    Date start 				= format.stdDateTimeFormat(tmin.getText());
		    Date end 				= format.stdDateTimeFormat(tmax.getText());

			logger.debug("grid=" + grid.getName() + " date:" + start + "," + end 
					+ " out=" + fileName);
		    
			statusMessage.setText("Subsetting from " + start + " to " + end + " ...");
			
		    writer.makeFile(fileName, dataset, gridList,
		          dataset.getBoundingBox(),
		          new DateRange(start,end),
		          true,
		          1, 1, 1);
		    
			MessageDialog.openInformation(getSite().getShell()
					, "NetCDFView"
					, "Output file is " + fileName);
		    
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Load netcdf dataset URI
	 */
	@SuppressWarnings("unchecked")
	public void loadDataset( String uri) {
		logger.debug("Uri " + uri);
		
		try {
			dataset = GridDataset.open(uri);

			List<GeoGrid> grids = dataset.getGrids();

			clearForm();
			
			// load grid names into table
			for (GeoGrid grid : grids) {
				viewer.add(grid);
				
			}
			
			// ds information
			metaData.setText(dataset.getInfo());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Update dims when viewer is selected
	 */
	@SuppressWarnings("unchecked")
	private void updateDimensions (GeoGrid grid ) 
	{
		// vertical levs
		List<NamedObject> levs = grid.getLevels();
		
		for (NamedObject namedObject : levs) {
			lev.add(namedObject.getName());
		}

		// bbox
		LatLonRect bbox = dataset.getBoundingBox();

		latMin.setText("" + bbox.getLatMin());
		latMax.setText("" + bbox.getLatMax());
		
		lonMin.setText("" + bbox.getLonMin());
		lonMax.setText("" + bbox.getLonMax());
		
		// times
		List<NamedObject> times = grid.getTimes();
		
		for (NamedObject time : times) {
			tmin.add(time.getName());
			tmax.add(time.getName());
		}
	}
	
	private void clearForm() {
		lev.removeAll();
		viewer.getTable().clearAll();
		tmin.removeAll();
		tmax.removeAll();
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
				plot.setEnabled(true);
				subset.setEnabled(true);
				statusMessage.setText("");
				//pb.setVisible(false);
				break;
				
			case PLOTTING_SUBSETTING:
				plot.setEnabled(false);
				subset.setEnabled(false);
				//pb.setVisible(true);
				break;
			default:
				break;
		}
	}
	
	/**
	 * Use threads for time consuming plots/subset operations
	 * @param doPlot if true plot else subset grid
	 */
	private void firePlotSubsetThread (final boolean doPlot)
	{
		final Display display = getViewSite().getShell().getDisplay();
	
		setUIState(UI_STATE.PLOTTING_SUBSETTING);
		
		new Thread(new Runnable() 
		{
			public void run() 
			{
				display.syncExec(new Runnable() {
					public void run() 
					{
//						try {
							if (doPlot)
								plotGrid();
							else
								subsetGrid();
//						} 
//						catch (Exception e) {
//							e.printStackTrace();
//						}
//						finally {
							setUIState(UI_STATE.IDLE);
//						}
					}
				});
			}
		}).start();
	}
	
	@Override
	public void dispose() {
		try {
			if ( dataset != null)
				dataset.close();
			
		} catch (Exception e) {}
	}
	
}