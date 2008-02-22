package org.bluemarble.gui;


import java.awt.Point;


import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;

import org.bluemarble.BlueMarble3D;
import org.bluemarble.util.BM3DUtils;
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
    private Window wNav;
    
    // Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
    private OrderedIcon orderedImage = new OrderedIcon();

    private class OrderedIcon implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
        	MasterUILayer.this.draw(dc);
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
    	wNav	= new NavigatorWindow(canvas, display);
    	
    	display.addWidget(wSearch);
    	display.addWidget(wNav);
    	
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
				BM3DUtils.toggleWidget(display, wNav);
			}
		});

		search.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				BM3DUtils.toggleWidget(display, wSearch);
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
	
//    private PickSupport pickSupport = new PickSupport();
	
	protected void draw(DrawContext dc) 
	{
		if (display == null) {
			buildGUI();
		}

//		if ( dc.isPickingMode()) {
//	        this.pickSupport.clearPickList();
//	        this.pickSupport.beginPicking(dc);
//	        // Where in the world are we picking ?
//	        Position pickPosition = Position.ZERO;
//	        
//	        // Draw unique color across the map
//	        java.awt.Color color = dc.getUniquePickColor();
//	        int colorCode = color.getRGB();
//
//	        this.pickSupport.addPickableObject(colorCode, this, pickPosition, false);
//	        // Done picking
//	        this.pickSupport.endPicking(dc);
//	        this.pickSupport.resolvePick(dc, dc.getPickPoint(), this);
//	        
//	        return;
//		}
		
		// pass the control over the OpenGL context to FengGUI so that
        // it can render the GUI.
        display.display();
	}

	public Display getDisplay () {
		return display;
	}
}
