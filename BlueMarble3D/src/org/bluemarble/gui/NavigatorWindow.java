package org.bluemarble.gui;

import org.fenggui.Container;
import org.fenggui.Display;
import org.fenggui.Label;
import org.fenggui.TabContainer;
import org.fenggui.composites.Window;
import org.fenggui.layout.GridLayout;

public class NavigatorWindow extends Window
{
	public NavigatorWindow(Display display) 
	{
		super(true, false, false, true);
    	setupTheme(NavigatorWindow.class);
        
        setSize(250, 400);
        setXY(10, display.getHeight() - 450);
        setTitle("Navigator");

        // Tabs
		TabContainer tabContainer = new TabContainer();
		getContentContainer().addWidget(tabContainer);
		
		tabContainer.addTab("Layers", null, buildLayersTab());
		tabContainer.addTab("Web Map Service", null, buildWMSTab());
		tabContainer.addTab("NetCDF", null, buildNetCDFTab());
		
        layout();
	}
	
	private Container buildLayersTab()
	{
		Container c = new Container(new GridLayout(2, 1));
		c.addWidget(new Label("tab 1"));
		return c;
	}
	
	private Container buildWMSTab()
	{
		Container c = new Container(new GridLayout(2, 1));
		c.addWidget(new Label("tab 2"));
		return c;
	}

	private Container buildNetCDFTab()
	{
		Container c = new Container(new GridLayout(2, 1));
		return c;
	}
	
}
