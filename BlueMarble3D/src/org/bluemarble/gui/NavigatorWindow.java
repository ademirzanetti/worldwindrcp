package org.bluemarble.gui;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.Earth.FogLayer;
import gov.nasa.worldwind.layers.Earth.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.SkyGradientLayer;
import gov.nasa.worldwind.layers.Earth.StarsLayer;

import org.apache.log4j.Logger;
import org.bluemarble.util.LayerSet;
import org.fenggui.CheckBox;
import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.IContainer;
import org.fenggui.IWidget;
import org.fenggui.TabContainer;
import org.fenggui.composites.Window;
import org.fenggui.event.mouse.IMouseReleasedListener;
import org.fenggui.event.mouse.MouseButton;
import org.fenggui.event.mouse.MouseReleasedEvent;
import org.fenggui.layout.RowLayout;
import org.fenggui.menu.Menu;
import org.fenggui.menu.MenuItem;
import worldwind.contrib.layers.PositionLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.KMLSource;

public class NavigatorWindow extends Window
{
	private static final Logger logger = Logger.getLogger(NavigatorWindow.class);
	
	private WorldWindowGLCanvas canvas;
	
	// Container for layers. Used for adding removing
	private IWidget CLayers;
	
	// Layers popup
	private Menu popupMenu = new Menu();
	private Display display;

	// Tab Container
	private final TabContainer tabContainer = new TabContainer();

	/**
	 * Constructor: Window with 2 tabs
	 */
	public NavigatorWindow(WorldWindowGLCanvas canvas, Display display) 
	{
		super(true, false, false, true);
    	setupTheme(NavigatorWindow.class);
        
    	this.display = display;
    	this.canvas	= canvas;
    	
        setSize(350, 530);
        setXY(10, display.getHeight() - 550);
        setTitle("Navigator");

		getContentContainer().addWidget(tabContainer);
		
		CLayers = buildLayersTab(canvas.getModel().getLayers());
		
		// Tabs: WW Layers and WMS UI
		tabContainer.addTab("Layers", null, CLayers );
		tabContainer.addTab("Web Map Service", null, new WMSTab(display)); 
		
		// Layers tab Popup menu
		popupMenu.addItem(new MenuItem("test"));
		popupMenu.addItem(new MenuItem("test1"));
		
        layout();
	}
	
	/**
	 * Layers Popup menu
	 * @param x
	 * @param y
	 */
	private void showPopUp (int x, int y) 
	{
		popupMenu.setXY(x, y);
		popupMenu.setSizeToMinSize();
		
		display.displayPopUp(popupMenu);
		display.setFocusedWidget(popupMenu);		
	}
	
	/**
	 * Ignore some stuff for simplicity
	 * @param layer
	 * @return
	 */
	private boolean ignoreLayer (Layer layer) {
		return layer instanceof MasterUILayer 
			|| layer instanceof StarsLayer
			|| layer instanceof SkyGradientLayer
			|| layer instanceof ScalebarLayer
			|| layer instanceof PositionLayer
			|| layer instanceof FogLayer;
	}
	
	/**
	 * Add a check box layer to the list
	 * @param c
	 * @param layer
	 */
	private void addCheckBox (IContainer c, Layer layer)
	{
		final CheckBox<Layer> cb = new CheckBox<Layer>(layer.getName());
		
		cb.addMouseReleasedListener(new IMouseReleasedListener()
		{
			@SuppressWarnings("unchecked")
			public void mouseReleased(MouseReleasedEvent e) 
			{
				// On Left click toggle, else cancel
				if ( e.getButton() == MouseButton.LEFT) 
				{
					handleCheckState(cb.isSelected(), cb.getValue());
				}
				else if ( e.getButton() == MouseButton.RIGHT) 
				{
					showPopUp(e.getDisplayX(), e.getDisplayY());
					cb.setSelected(!cb.isSelected()); // cancel event
				}
				else
					cb.setSelected(!cb.isSelected()); // cancel event
			}
		});
		
		logger.debug("Adding checkbox for " + layer + " selected=" + layer.isEnabled());
		
		cb.setValue(layer);
		cb.setSelected(layer.isEnabled());
		
		c.addWidget(cb);			
	}

	
	/**
	 * Layers Tab
	 * @param layers
	 * @return
	 */
	private IWidget buildLayersTab(LayerList layers)
	{
		Container c = new Container(new RowLayout(false));
//        ScrollContainer c = FengGUI.createScrollContainer(getContentContainer());
		
		// Add layers
		for (Layer layer : layers) 
		{
			if ( ignoreLayer(layer)) 
				continue;
			
			addCheckBox(c, layer);
		}
		return c;
	}

	/**
	 * Add aleyer to the view
	 * @param layer
	 */
	public void addLayer(Layer layer)
	{
		addCheckBox((IContainer)CLayers, layer);
	}
	
	/**
	 * Add an array of {@link Layer} to the view as 1 entity
	 * to the view
	 * @param parentName Name of the tree parent node
	 * @param layers WW layers
	 */
	public void addLayers( String parentName, Layer[] layers) //, boolean enabled) 
	{
		try {
			addCheckBox((IContainer)CLayers, new LayerSet(parentName, layers));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * Add a kml/kmz source. All layers regardless of protocol are cached as KML in
	 * WW cache.
	 * @param kml The {@link KMLSource} object to store
	 */
	public void addKMLSource (KMLSource kml) //,  boolean enabled) 
	{
		try {
			addCheckBox((IContainer)CLayers, new LayerSet(kml.getDocument().getName(), kml.toLayerList()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Handle layer visibility
	 * @param checked
	 * @param layer
	 */
	private void handleCheckState (boolean checked, Layer layer)
	{
		logger.debug("Layer " + layer + " Type: " + layer.getClass() + " Checked=" + checked);
		try {
			if  ( layer instanceof TimeLoopGroundOverlay) 
			{
				handleTimeLoop(checked, (TimeLoopGroundOverlay)layer);
			}
			else {
				layer.setEnabled( checked);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void handleTimeLoop (boolean checked, TimeLoopGroundOverlay layer)
	{
		if ( checked) {
			layer.addToModel(canvas);
			layer.play();
		}
		else {
			layer.stop();
			layer.removeFromModel();
		}
	}
	
	public void showLayers () {
		tabContainer.selectTab(0);
	}
}
