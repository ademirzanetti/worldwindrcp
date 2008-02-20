package org.bluemarble.gui;

import javax.media.opengl.GLCanvas;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;

import org.bluemarble.BlueMarble3D;
import org.bluemarble.util.BM3DUtils;
import org.fenggui.Button;
import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.FengGUI;
import org.fenggui.GameMenuButton;
import org.fenggui.Label;
import org.fenggui.TextEditor;
import org.fenggui.background.PlainBackground;
import org.fenggui.border.TitledBorder;
import org.fenggui.composites.Window;
import org.fenggui.event.ButtonPressedEvent;
import org.fenggui.event.IButtonPressedListener;
import org.fenggui.layout.Alignment;
import org.fenggui.layout.FormAttachment;
import org.fenggui.layout.FormData;
import org.fenggui.layout.FormLayout;
import org.fenggui.layout.GridLayout;
import org.fenggui.layout.RowLayout;
import org.fenggui.render.Binding;
import org.fenggui.render.jogl.EventBinding;
import org.fenggui.render.jogl.JOGLBinding;
import org.fenggui.theme.DefaultTheme;
import org.fenggui.theme.ITheme;
import org.fenggui.theme.XMLTheme;
import org.fenggui.util.Color;
import org.fenggui.util.Spacing;

public class MasterUILayer extends AbstractLayer 
{
	// the canvas on which the OpenGL will draw his stuff. We keep
	// it as a field because we need the canvas to instantiate the
	// JOGL binding.
    private GLCanvas canvas = null;
    
	// The root of the Widget tree.
    private Display display = null;	
    
    private final String THEME_PATH = "data/themes/QtCurveBlack/QtCurveBlack.xml";
    
    // Toolbar buttons
    private GameMenuButton nav, open, search, www, quit;
    
    // Search Window
    private Window wSearch; 
    
    // Navigator
    private Window wNav;
    
    public MasterUILayer(GLCanvas canvas) {
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
        
     	createWindows();
    	createToolbar();
    }

    /**
     * Create all windows using the selected theme
     */
    private void createWindows() {
    	createSearchWindow();
    }
    
    /**
     * Creatre search window
     */
    private void createSearchWindow() 
    {
    	wSearch = new Window(true, false, false, true);
        wSearch.setXY(50, 50);
        wSearch.setSize(250, 280);
        wSearch.setTitle("Places Search");
        
		
		wSearch.getContentContainer().setLayoutManager(new FormLayout());
		
		Container c = FengGUI.createContainer(wSearch.getContentContainer());
		
		c.getAppearance().setPadding(new Spacing(5,5));

		FormData fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100,0);
		fd.top = new FormAttachment(100,0);
		
		c.setLayoutData(fd);
		c.setLayoutManager(new GridLayout(2,2));
		
		
		TextEditor searchText = FengGUI.createTextField(c);
		searchText.getAppearance().setMargin(new Spacing(0, 0, 0, 2));
		searchText.setSize(80, searchText.getMinHeight());
		searchText.setShrinkable(false);

		Button goButton = FengGUI.createButton(c, "Go");
		goButton.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				BM3DUtils.MessageBox(display, "Search!");
			}
		});		

//		Label l1 = FengGUI.createLabel(c, "Button");
//		l1.getAppearance().setMargin(new Spacing(0, 0, 0, 5));
		
//        Label l = new Label("Hello World!!");
//        l.getAppearance().setAlignment(Alignment.MIDDLE);
//        
//        wSearch.getContentContainer().addWidget(l);
		
        wSearch.layout();
        display.addWidget(wSearch);
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
		
		// Button events
		nav.addButtonPressedListener(new IButtonPressedListener()
		{
			public void buttonPressed(ButtonPressedEvent e)
			{
				BM3DUtils.MessageBox(display, "Nothing yet!");
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
