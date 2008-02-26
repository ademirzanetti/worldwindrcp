package org.bluemarble.gui;


import java.awt.Point;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;


import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;

import org.bluemarble.BlueMarble3D;
import org.bluemarble.util.BlueMarbeUtils;
import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.FengGUI;
import org.fenggui.GameMenuButton;
import org.fenggui.IWidget;
import org.fenggui.background.PlainBackground;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.RowLayout;
import org.fenggui.render.Binding;
import org.fenggui.render.jogl.EventBinding;
import org.fenggui.render.jogl.JOGLBinding;
import org.fenggui.theme.DefaultTheme;
import org.fenggui.theme.ITheme;
import org.fenggui.theme.XMLTheme;
import org.fenggui.util.Color;

import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.SimpleHTTPClient;

public class MasterUILayer extends AbstractLayer 
{
	// the canvas on which the OpenGL will draw his stuff. We keep
	// it as a field because we need the canvas to instantiate the
	// JOGL binding.
    private WorldWindowGLCanvas canvas = null;
    
	// The root of the Widget tree.
    private Display display = null;	
    
    private final String THEME_PATH = "data/themes/QtCurveBlack/QtCurveBlack.xml";
    
    // Toolbar buttons
    private GameMenuButton nav, open, search, www, quit;
    
    // Search Window
    private Window wSearch; 
    
    // Navigator
    private Window wNavigator;
    
    // Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
    private OrderedIcon orderedImage = new OrderedIcon();

//    private final PickSupport pickSupport = new PickSupport();
    
    private class OrderedIcon implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
        	//MasterUILayer.this.draw(dc);
        	dc.addPickedObject(new PickedObject(dc.getUniquePickColor().getRGB(), this));
//        	pickSupport.clearPickList();
//            pickSupport.beginPicking(dc);
//            java.awt.Color color = dc.getUniquePickColor();
//            pickSupport.addPickableObject(color.getRGB(), MasterUILayer.this);
//            pickSupport.endPicking(dc);
//            pickSupport.resolvePick(dc, pickPoint, MasterUILayer.this);            
        }

        public void render(DrawContext dc)
        {
            MasterUILayer.this.draw(dc);
        }
    }
    
    /**
     * Constructor
     * @param canvas
     */
    public MasterUILayer(WorldWindowGLCanvas canvas) {
    	super();
    	setName("User Interface");
		this.canvas = canvas;
	}
    
    /**
     * Load theme
     */
    private void loadTheme() 
    {
    	ITheme theme;
    	try {
    		theme =  new XMLTheme(THEME_PATH);
		} catch (Exception e) {
			e.printStackTrace();
			theme = new DefaultTheme();
		}
		FengGUI.setTheme(theme);
    }
    
    /**
     * Build the GUI.
     */
    public void buildGUI() 
    {
    	
        display = FengGUI.createDisplay(new JOGLBinding(canvas));
        display.setDepthTestEnabled(true);
        
        new EventBinding(canvas, display);

    	loadTheme();
        
     	createWindows();
     	display.addWidget(createToolbar());
    }

    /**
     * Create all windows using the selected theme
     */
    private void createWindows() 
    {
    	// Places Search
    	wSearch = new SearchWindow(canvas); 
    	wNavigator	= new NavigatorWindow(canvas, display);
    	
    	//display.addWidget(wSearch);
    	display.addWidget(wNavigator);
    	
    }
    
    
    /**
     * Toolbar
     */
    private IWidget createToolbar () 
    {
		final Container c = new Container();
		c.getAppearance().add(new PlainBackground(Color.OPAQUE));
		
		c.setXY((display.getWidth() - 96 * 5)/2 , 0);
		c.setLayoutManager(new RowLayout(true));

		initButtons(c, display);
		buildMainMenu(c, display);
		return c;
    }
    
    /**
     * Init toolbar buttons
     * @param c
     * @param display
     */
	private void initButtons(final Container c, final Display display)
	{
		Binding.getInstance().setUseClassLoader(true);
		
		nav 	= new GameMenuButton("images/toolbar/96-nav0.png", "images/toolbar/96-nav1.png");
		open 	= new GameMenuButton("images/toolbar/96-open0.png", "images/toolbar/96-open1.png");
		search 	= new GameMenuButton("images/toolbar/96-Ysearch0.png", "images/toolbar/96-Ysearch1.png");
		www 	= new GameMenuButton("images/toolbar/96-www0.png", "images/toolbar/96-www1.png");
		quit 	= new GameMenuButton("images/toolbar/96-quit0.png", "images/toolbar/96-quit1.png");
		
		// Button events
		nav.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				BlueMarbeUtils.toggleWidget(display, wNavigator);
			}
		});

		open.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				handleFileOpen();
			}
		});
		
		search.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				BlueMarbeUtils.toggleWidget(display, wSearch);
			}
		});
		
		quit.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				BlueMarble3D.quit();
			}
		});		
		
	}

	/**
	 * Main menu
	 * @param c
	 * @param display
	 */
	private void buildMainMenu(final Container c, final Display display)
	{
		c.removeAllWidgets();
		
		c.addWidget(nav);
		c.addWidget(open);
		c.addWidget(search);
		c.addWidget(www);
		c.addWidget(quit);
		
		c.pack();
	}
	
	
	protected void doRender(DrawContext dc) 
	{
		dc.addOrderedRenderable(this.orderedImage);
	}

	@Override
	protected void doPick(DrawContext dc, Point point) {
		dc.addOrderedRenderable(this.orderedImage);
	}
	
	/**
	 * Draw layer
	 * @param dc
	 */
	protected void draw(DrawContext dc) 
	{
		if (display == null) {
			buildGUI();
		}

//		if ( dc.isPickingMode()) {
//			dc.addPickedObject(new PickedObject(dc.getUniquePickColor().getRGB(), this));
//		}
//		else {
			// pass the control over the OpenGL context to FengGUI so that
	        // it can render the GUI.
	        display.display();
//		}
	}

	public Display getDisplay () {
		return display;
	}
	
	/**
	 * Fires on file open pressed
	 */
	private void handleFileOpen () 
	{
		//Create a file chooser
		final JFileChooser fc	= new JFileChooser();
		
		//Add a custom file filter and disable the default
	    //(Accept All) file filter.
        fc.setAcceptAllFileFilterUsed(false);
        fc.addChoosableFileFilter(new BlueMarbleFileFilter("Google KML/KMZ", new String[]{"kml", "kmz"}));
        fc.addChoosableFileFilter(new BlueMarbleFileFilter("NetCDF (.nc .ncf .*)", new String[]{"*"}));

		final int returnVal 	= fc.showOpenDialog(canvas);

		if (returnVal == JFileChooser.APPROVE_OPTION) 
		{
			File file	= fc.getSelectedFile();
			String ext	= BlueMarbeUtils.getExtension(file);
			
			try {
				// 3 types of files can be opened: KM, KMZ, NetCDF
				if ( ext.equalsIgnoreCase(BlueMarbeUtils.EXT_KML)) {
					((NavigatorWindow)wNavigator).addKMLSource(new KMLSource(file, SimpleHTTPClient.CT_KML));
					((NavigatorWindow)wNavigator).showLayers();
				}
				else if ( ext.equalsIgnoreCase(BlueMarbeUtils.EXT_KMZ)) {
					((NavigatorWindow)wNavigator).addKMLSource(new KMLSource(file, SimpleHTTPClient.CT_KMZ));
					((NavigatorWindow)wNavigator).showLayers();
				}
				else {
					System.out.println("Opening necdf: " + file.getName());
				}
			} 
			catch (Exception e) {
				BlueMarbeUtils.MessageBox(display, "Unable to open " 
						+ file + " " + e.getMessage() );
			}
		}
	}
	
	/**
	 * File filter class for the {@link JFileChooser} 
	 * @author Owner
	 */
	private class BlueMarbleFileFilter extends FileFilter
	{
		private String[] extensions;
		private String description;
		
		public BlueMarbleFileFilter(String description, String[] extensions) {
			this.description = description;
			this.extensions = extensions;
		}
		
		@Override
		public boolean accept(File f) 
		{
			if (f.isDirectory()) return true;
			
			String extension = BlueMarbeUtils.getExtension(f);
			
			// accept all
			if ( extension == null || extension.equals("*")) return true;
			
			boolean found = false;
			
			for (int i = 0; i < extensions.length; i++) {
				if  ( extension.equals(extensions[i])) return true;
			}
			return found;
		}

		@Override
		public String getDescription() {
			return description;
		}
	}
}
