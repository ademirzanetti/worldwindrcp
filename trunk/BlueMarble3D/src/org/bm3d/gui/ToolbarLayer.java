package org.bm3d.gui;

import javax.media.opengl.GLCanvas;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;

import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.FengGUI;
import org.fenggui.GameMenuButton;
import org.fenggui.Label;
import org.fenggui.background.PlainBackground;
import org.fenggui.composites.Window;
import org.fenggui.layout.Alignment;
import org.fenggui.layout.RowLayout;
import org.fenggui.render.Binding;
import org.fenggui.render.jogl.EventBinding;
import org.fenggui.render.jogl.JOGLBinding;
import org.fenggui.theme.DefaultTheme;
import org.fenggui.theme.ITheme;
import org.fenggui.theme.XMLTheme;
import org.fenggui.util.Color;

public class ToolbarLayer extends AbstractLayer 
{
	// the canvas on which the OpenGL will draw his stuff. We keep
	// it as a field because we need the canvas to instantiate the
	// JOGL binding.
    private GLCanvas canvas = null;
    
	// The root of the Widget tree.
    private Display display = null;	
    
    private final String THEME_PATH = "data/themes/QtCurveBlack/QtCurveBlack.xml";
    
    private GameMenuButton nav, open, search, www, quit;
    
    public ToolbarLayer(GLCanvas canvas) {
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
        
        new EventBinding(canvas, display);

    	loadTheme();
        
        Window w = new Window(true, false, false, true);
        Label l = new Label("Hello World!!");
        l.getAppearance().setAlignment(Alignment.MIDDLE);
        
        w.getContentContainer().addWidget(l);
        w.setXY(50, 50);
        w.setSize(200, 100);
        w.layout();
        display.addWidget(w);
        
        createToolbar();
    }

    /**
     * Toolbar
     */
    private void createToolbar () {
		final Container c = new Container();
		c.getAppearance().add(new PlainBackground(Color.OPAQUE));
		
		c.setXY((display.getWidth() - 96 * 5)/2 , 0);
		
		display.addWidget(c);
		display.setDepthTestEnabled(true);
		
		c.setLayoutManager(new RowLayout(true));

		initButtons(c, display);
		buildMainMenu(c, display);
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
		
		if (display == null) {
			buildGUI();
		}
		// pass the control over the OpenGL context to FengGUI so that
        // it can render the GUI.
        display.display();
	}

	public Display getDisplay () {
		return display;
	}
}
