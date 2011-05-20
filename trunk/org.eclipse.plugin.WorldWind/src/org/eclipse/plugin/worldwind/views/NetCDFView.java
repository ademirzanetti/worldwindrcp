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
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.contrib.LayerUtils;
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
import org.eclipse.ui.PartInitException;
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
import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.ScreenOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

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
	enum UI_STATE { IDLE, PLOTTING, SUBSETTING};
	
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
	
	private final String VIEW_TITLE = Messages.getString("NetCDFView.0"); //$NON-NLS-1$

	private boolean cancelPlot = false;
	
	private Display display; 

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) 
	{
		display 		= getViewSite().getShell().getDisplay();
		
		toolkit 		= new FormToolkit(parent.getDisplay());
		scrolledForm 	= toolkit.createScrolledForm(parent);

		scrolledForm.setText(VIEW_TITLE);
		toolkit.decorateFormHeading(scrolledForm.getForm());
		
		// Create UI
		TableWrapLayout layout 	= new TableWrapLayout();
		layout.numColumns 		= 2;

		scrolledForm.getBody().setLayout(layout);

		int expanded 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED;
		int collapsed 	=  Section.DESCRIPTION | Section.TITLE_BAR | Section.TWISTIE;

		// create UI elements: Metadata
		createMetaSection(Messages.getString("NetCDFView.1"), null, collapsed, 2); //$NON-NLS-1$

		// Grids list box section
		viewer 	= createGridsSection(Messages.getString("NetCDFView.2"), null, expanded, 2); //$NON-NLS-1$
		
		// dataset dimensions
		createDimsSection(Messages.getString("NetCDFView.3"), null, expanded, 2); //$NON-NLS-1$
		
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
		toolkit.createLabel(sectionClient, Messages.getString("NetCDFView.4"), SWT.NONE);   //$NON-NLS-1$
		
		latMin = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		latMin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		latMax = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		latMax.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// Lon
		toolkit.createLabel(sectionClient, Messages.getString("NetCDFView.7"), SWT.NONE);   //$NON-NLS-1$
		
		lonMin = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		lonMin.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		lonMax = toolkit.createText(sectionClient, "", SWT.FILL | SWT.READ_ONLY); //$NON-NLS-1$
		lonMax.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		// Z(level)  
		toolkit.createLabel(sectionClient, Messages.getString("NetCDFView.10"), SWT.NONE);   //$NON-NLS-1$

		TableWrapData td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		lev = new Combo(sectionClient, SWT.READ_ONLY);
		lev.setLayoutData(td); 
		lev.addListener(SWT.Selection, this);
		//lev.addListener(SWT.DefaultSelection, this);

		// tmin
		toolkit.createLabel(sectionClient, Messages.getString("NetCDFView.11"), SWT.NONE);   //$NON-NLS-1$
		
		
		tmin = new Combo(sectionClient, SWT.READ_ONLY);
		
		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		tmin.setLayoutData(td); 
		tmin.addListener(SWT.Selection, this);
		tmin.addListener(SWT.DefaultSelection, this);
		
		// tmax
		toolkit.createLabel(sectionClient, Messages.getString("NetCDFView.12"), SWT.NONE);   //$NON-NLS-1$
		
		tmax = new Combo(sectionClient, SWT.READ_ONLY);
		
		td = new TableWrapData(TableWrapData.LEFT);
		td.colspan = 2;
		
		tmax.setLayoutData(td); 
		tmax.addListener(SWT.Selection, this);
		tmax.addListener(SWT.DefaultSelection, this);

		// progress bar
		statusMessage = toolkit.createLabel(sectionClient, "", SWT.NONE); //$NON-NLS-1$

		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.colspan = 3;
		
		statusMessage.setLayoutData(td);
		
		// plot btn
		plot = toolkit.createButton(sectionClient, Messages.getString("NetCDFView.14"), SWT.NONE); //$NON-NLS-1$
		plot.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		plot.addListener(SWT.Selection, this);
		
		// subset btn
		subset = toolkit.createButton(sectionClient, Messages.getString("NetCDFView.15"), SWT.NONE); //$NON-NLS-1$
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
		metaData= toolkit.createLabel(sectionClient, "", SWT.WRAP);  //$NON-NLS-1$
		
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
				final String label =((Button)w).getText();
				
				// Is the btn label Cancel?
				if (label.equalsIgnoreCase(Messages.getString("NetCDFView.5"))) {
					cancelPlot();
				}
				else {
					// Plot
					cancelPlot = false;
					firePlotSubsetThread(true);
				}
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
	
	/**
	 * Plot Grid
	 * @param t1 Start time idx
	 * @param t2 End time idx
	 * @param z Vertical level index
	 * @param grid {@link GeoGrid} to plot
	 * @param overlayNames Names for the {@link GroundOverlayLayer} frames.
	 * 		  Time steps can be used. 
	 */
	private void plotGrid(int t1 , int t2, int z
			, final GeoGrid grid, String[] overlayNames) 
		throws Exception
	{
		logger.debug("t=[" + t1 + "," + t2 + "] z=" + z); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		// z size can be 0, t should be > 0
		if (( t1 == -1 || t2 == -1) ) {
			display.syncExec(new Runnable() {
				public void run() {
					MessageDialog.openInformation(getSite().getShell()
							, VIEW_TITLE
							, Messages.getString("NetCDFView.20")); //$NON-NLS-1$
				}
			});
			return;
		}
		
		if ( z == -1 ) z = 0;
		
		// plotter
		Plot plotter = new Plot();
		
		// loop overlay
		final TimeLoopGroundOverlay loop = new TimeLoopGroundOverlay((new File(dataset.getLocationURI())).getName());

		
		// bbox (not always +-180 longitude)
		final LatLonRect bbox = dataset.getBoundingBox();
		
		for (int i = t1; i <= t2; i++) 
		{
			final int tStep = i; // for SWT threads
			
			// canceled by user?
			if ( cancelPlot ) {
				logger.debug("Breaking out of plot loop at t=" + i);
				return;
			}
			
			// WW cache name
			final String cacheName = "Earth"  //$NON-NLS-1$
				+ "/" + (new File(dataset.getLocationURI())).getName()  //$NON-NLS-1$
				+ "/" + grid.getName() + ".t" + i + ".z" + z; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

			// look for frame in cache
			// ww 0.5 URL frame = WorldWind.getDataFileCache().findFile(cacheName + ".png", false); //$NON-NLS-1$
			URL frame = LayerUtils.getWWFileStore().findFile(cacheName + ".png", false); //$NON-NLS-1$
			
			// if file not in cache plot
			if (  frame == null) 
			{
//				final File imF 		= WorldWind.getDataFileCache().newFile(cacheName + ".png"); //$NON-NLS-1$
//				final File legendF 	= WorldWind.getDataFileCache().newFile(cacheName + ".legend.png"); //$NON-NLS-1$
				final File imF 		= LayerUtils.getWWFileStore().newFile(cacheName + ".png"); //$NON-NLS-1$
				final File legendF 	= LayerUtils.getWWFileStore().newFile(cacheName + ".legend.png"); //$NON-NLS-1$
				
				logger.debug("Plotting:" + cacheName + " to " + imF); //$NON-NLS-1$ //$NON-NLS-2$
				
				display.syncExec(new Runnable() {
					public void run() {
						statusMessage.setText("t=" + tStep + " z=" +  lev.getText() + " to " + cacheName); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				});
				
				
				try {
					// plot: returns a buf img + legend
					BufferedImage im = plotter.plotGrid(dataset, grid
							, grid.readYXData(i, z)
							, new Color(223, 223, 223));
					
					// write png: im + legend
					ImageIO.write(im, "PNG", imF ); //$NON-NLS-1$
					ImageIO.write(plotter.getLegend(), "PNG", legendF ); //$NON-NLS-1$
					
					frame = imF.toURL();
				} 
				catch ( IllegalArgumentException iae) {
					// something's wrong...abort
					throw new Exception(iae);
				}
				catch (final IOException e) 
				{
					e.printStackTrace();
			
					display.syncExec(new Runnable() {
						public void run() {
							statusMessage.setText(grid.getName() 
									+ " t=" + tStep + " z=" + lev.getText()  //$NON-NLS-1$ //$NON-NLS-2$
									+ " " + e.getMessage()); //$NON-NLS-1$
						}
					});
					
					// Current plot has failed. Continue to the next frame
					continue;
				}
			}
			else {
				logger.debug(cacheName + " already in cache"); //$NON-NLS-1$
			}

			// Overlay sector (lon must always be +-180)
			final Sector sector = normalizeBBox(bbox); 
			
			// Overlay image URL
			final URL url = new URL(frame.toString().replaceAll(" ", "%20")); //$NON-NLS-1$ //$NON-NLS-2$
			
			//logger.debug("Groung overlay " + overlayNames[i] + " Sector:" + sector + " Url:" + url); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			// add the ground overlay to the loop
			loop.add(new GroundOverlayLayer(
					overlayNames[i] //tmin.getItem(i)	// time step is the overlay name
					, sector
					, url		// URL
					, ".png" 	// format //$NON-NLS-1$
					));
			
			// grab 1st legend
			if ( i  == t1 ) 
			{
				// ww 0.5 final URL legendU = WorldWind.getDataFileCache().findFile(cacheName + ".legend.png", false); //$NON-NLS-1$
				URL legendU = LayerUtils.getWWFileStore().findFile(cacheName + ".legend.png", false); //$NON-NLS-1$
				
				if ( legendU != null)
					loop.setLegend(new ScreenOverlayLayer("legend" //$NON-NLS-1$
						, legendU
						, ScreenOverlayLayer.SOUTHWEST));
			}
		}
		
		// description as HTML
		loop.setDescription("<pre>" + dataset.getDetailInfo() + "</pre>"); //$NON-NLS-1$ //$NON-NLS-2$
		

		display.syncExec(new Runnable() {
			public void run() {
				// Add loop to the earth view
				// show view if hidden
				try {
					getViewSite().getWorkbenchWindow().getActivePage().showView(NavigatorView.ID);
				} catch (PartInitException e) {
					// shouldn't happen
				}

				// Get Navigator and add the loop overlay
				NavigatorView view = (NavigatorView)Activator.getView(getViewSite().getWorkbenchWindow()
						, NavigatorView.ID);
				
				//view.addOverlays(new TimeLoopGroundOverlay[] { loop }, false);
				view.addLayers(new TimeLoopGroundOverlay[] { loop }, false);
			}
		});
	}

	/**
	 * Convert a NetCDF bbox {@link LatLonRect} to WWJ {@link Sector}
	 * Longitude must be +-180
	 * @return
	 */
	private Sector normalizeBBox (LatLonRect bbox)
	{
		double lonMin = bbox.getLonMin();
		double lonMax = bbox.getLonMax();
		
		if ( lonMax > 180 ) {
			lonMin = -bbox.getCenterLon();
			lonMax = bbox.getCenterLon();
		}
		
		return new Sector(Angle.fromDegrees(bbox.getLatMin())
				, Angle.fromDegrees(bbox.getLatMax())
				, Angle.fromDegrees(lonMin)
				, Angle.fromDegrees(lonMax)
				);		
	}
	
	/*
	 * Fires when the subset btn is pressed.
	 * Subset dataset by Grid and Time range.
	 */
	private void subsetGrid ()
	{
		try {
			// get grid
			IStructuredSelection sel 	= (IStructuredSelection) viewer.getSelection();
			final GeoGrid grid 			= (GeoGrid)sel.getFirstElement();

			if ( tmin.getSelectionIndex() == -1 ) {
				MessageDialog.openInformation(getSite().getShell()
						, VIEW_TITLE
						, Messages.getString("NetCDFView.50")); //$NON-NLS-1$
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
			
			dialog.setFilterExtensions(new String[] { "*.*" } ); //$NON-NLS-1$
			dialog.setFilterNames(new String[] { "All files (*.*)"}); //$NON-NLS-1$
			
			// User defined path
			String fileName 		= dialog.open();

			if ( fileName == null)
				return;

			// Does file exist?
			if ( new File(fileName).exists()) 
				if ( ! MessageDialog.openQuestion(getSite().getShell()
						, VIEW_TITLE
						, fileName  + " " + Messages.getString("NetCDFView.53"))) //$NON-NLS-1$
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

			logger.debug("grid=" + grid.getName() 
					+ " date:" + start + "," + end  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ " out=" + fileName); //$NON-NLS-1$
		    
			statusMessage.setText(Messages.getString("NetCDFView.76") + start + " "
					+ Messages.getString("NetCDFView.59") 
					+ end + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
		    writer.makeFile(fileName, dataset, gridList,
		          dataset.getBoundingBox(),
		          new DateRange(start,end),
		          true,
		          1, 1, 1);
		    
			MessageDialog.openInformation(getSite().getShell()
					, VIEW_TITLE
					, Messages.getString("NetCDFView.61") + fileName); //$NON-NLS-1$
		    
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Load netcdf dataset URI
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	public void loadDataset( String uri) 
	{
		logger.debug("Uri " + uri); //$NON-NLS-1$
		
		setPartName(uri);
		
		try {
			dataset = GridDataset.open(uri);
			
			List<GeoGrid> grids = dataset.getGrids();

			clearForm();

			// Warn if not in LatLon projection
			if (!grids.isEmpty() && ! grids.get(0).getProjection().isLatLon())
				MessageDialog.openWarning(getSite().getShell()
						, VIEW_TITLE
						, Messages.getString("NetCDFView.64") + uri   //$NON-NLS-1$
							+ "\n" + Messages.getString("NetCDFView.66") //$NON-NLS-1$ //$NON-NLS-2$
							+ grids.get(0).getProjection().getProjectionTypeLabel() 
							+ "\n" + Messages.getString("NetCDFView.68"));				 //$NON-NLS-1$ //$NON-NLS-2$
				
			// load grid names into table
			for (GeoGrid grid : grids) {
				viewer.add(grid);
				
			}
			
			// ds information
			metaData.setText(dataset.getInfo());
		} 
		catch (Exception e) 
		{
			logger.error(e.getMessage());
			MessageDialog.openError(getSite().getShell()
					, VIEW_TITLE
					, e.getMessage());
			
		}
	}
	
	/*
	 * Update dims when viewer is selected
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	private void updateDimensions (GeoGrid grid ) 
	{
		// clear dimensions first: Vertical levl + times
		clearDimensions();
		
		// vertical levs
		List<NamedObject> levs = grid.getLevels();
		
		for (NamedObject namedObject : levs) {
			lev.add(namedObject.getName());
		}

		// bbox
		LatLonRect bbox = dataset.getBoundingBox();

		latMin.setText("" + bbox.getLatMin()); //$NON-NLS-1$
		latMax.setText("" + bbox.getLatMax()); //$NON-NLS-1$
		
		lonMin.setText("" + bbox.getLonMin()); //$NON-NLS-1$
		lonMax.setText("" + bbox.getLonMax()); //$NON-NLS-1$
		
		// times
		List<NamedObject> times = grid.getTimes();
		
		for (NamedObject time : times) {
			tmin.add(time.getName());
			tmax.add(time.getName());
		}
	}
	
	private void clearForm() {
		viewer.getTable().removeAll();
		clearDimensions();
	}
	
	private void clearDimensions () {
		lev.removeAll();
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
				plot.setText(Messages.getString("NetCDFView.14"));
				subset.setEnabled(true);
				//statusMessage.setText(""); //$NON-NLS-1$
				break;

			case PLOTTING:
				plot.setText(Messages.getString("NetCDFView.5"));
				subset.setEnabled(false);
				break;
				
			case SUBSETTING:
				plot.setEnabled(false);
				subset.setEnabled(false);
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
		// start/end time idices
		final int t1 = tmin.getSelectionIndex();
		final int t2 = tmax.getSelectionIndex();
		
		// vertical level index
		final int z = lev.getSelectionIndex();

		// GeoGrid to plot
		IStructuredSelection sel 	= (IStructuredSelection) viewer.getSelection();
		final GeoGrid grid 			= (GeoGrid)sel.getFirstElement();
		
		// Overlay names
		final String[] names = tmin.getItems();
		
		// Grid Validation
		// Must have a 1D X and Y coordinate axes.
		// Be lat/lon or Lambert Conformal Projection
		// Equally spaced
		if ( doPlot) {
			if (! grid.getCoordinateSystem().isRegularSpatial()) {
				MessageDialog.openError(getSite().getShell()
						, VIEW_TITLE
						, grid.getName() 
							+  " " + Messages.getString("NetCDFView.8")
						); 
				return;
			}
		}
		
		// Set UI state
		if (doPlot) 
			setUIState(UI_STATE.PLOTTING);
		else
			setUIState(UI_STATE.SUBSETTING);
	
		new Thread(new Runnable() 
		{
			public void run() 
			{
				try {
					// Wrapping all this stuff in display.syncExec won't cancel
					if (doPlot) {
						plotGrid(t1, t2, z, grid, names);
					}
					else {
						display.syncExec(new Runnable() {
							public void run() {
								subsetGrid();
							}
						});
					}
					
					// done
					display.syncExec(new Runnable() {
						public void run() {
							setUIState(UI_STATE.IDLE);
						}
					});
				} 
				catch (final Exception e) 
				{
					//e.printStackTrace();
					
					display.syncExec(new Runnable() {
						public void run() {
							statusMessage.setText(Messages.getString("NetCDFView.75") 
									+ e.getMessage()); //$NON-NLS-1$
						}
					});
				}
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
	
	/*
	 * Cancel a long runing plot 
	 */
	private void cancelPlot () {
		logger.debug("Cancelling plot.");
		cancelPlot = true;
		setUIState(UI_STATE.IDLE);
	}
}