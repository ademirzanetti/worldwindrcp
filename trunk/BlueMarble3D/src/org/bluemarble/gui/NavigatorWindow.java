package org.bluemarble.gui;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.Earth.FogLayer;
import gov.nasa.worldwind.layers.Earth.ScalebarLayer;
import gov.nasa.worldwind.layers.Earth.SkyGradientLayer;
import gov.nasa.worldwind.layers.Earth.StarsLayer;

import org.fenggui.CheckBox;
import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.IContainer;
import org.fenggui.IWidget;
import org.fenggui.Label;
import org.fenggui.TabContainer;
import org.fenggui.composites.Window;
import org.fenggui.event.mouse.IMouseReleasedListener;
import org.fenggui.event.mouse.MouseButton;
import org.fenggui.event.mouse.MouseReleasedEvent;
import org.fenggui.layout.GridLayout;
import org.fenggui.layout.RowLayout;
import org.fenggui.menu.Menu;
import org.fenggui.menu.MenuItem;

import worldwind.contrib.layers.PositionLayer;

public class NavigatorWindow extends Window
{
	//private WorldWindowGLCanvas canvas;
	// Container for layers. Used for adding removing
	private IWidget CLayers;
	
	// Layers popup
	private Menu popupMenu = new Menu();
	private Display display;
	
	/**
	 * Contructor
	 */
	public NavigatorWindow(WorldWindowGLCanvas canvas, Display display) 
	{
		super(true, false, false, true);
    	setupTheme(NavigatorWindow.class);
        
    	this.display = display;
    	
        setSize(250, 400);
        setXY(10, display.getHeight() - 450);
        setTitle("Navigator");

        // Tabs
		TabContainer tabContainer = new TabContainer();
		getContentContainer().addWidget(tabContainer);
		
		CLayers = buildLayersTab(canvas.getModel().getLayers());
		
		tabContainer.addTab("Layers", null, CLayers );
		tabContainer.addTab("Web Map Service", null, buildWMSTab());
		tabContainer.addTab("NetCDF", null, buildNetCDFTab());
		
		// Popup menu
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
	 * 
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
				if ( e.getButton() == MouseButton.LEFT) {
					final Layer layer = cb.getValue();
					layer.setEnabled( cb.isSelected());
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
		
		cb.setValue(layer);
		cb.setSelected(layer.isEnabled());
		c.addWidget(cb);			
	}

	public void addLayer(Layer layer)
	{
		addCheckBox((IContainer)CLayers, layer);
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
	
	private IWidget buildWMSTab()
	{
		Container c = new Container(new GridLayout(2, 1));
		c.addWidget(new Label("tab 2"));
		return c;
	}

	private IWidget buildNetCDFTab()
	{
		Container c = new Container(new GridLayout(2, 1));
		return c;
	}
	
}
