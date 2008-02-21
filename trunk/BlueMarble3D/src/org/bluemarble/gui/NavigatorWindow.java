package org.bluemarble.gui;

import org.fenggui.Display;
import org.fenggui.composites.Window;

public class NavigatorWindow extends Window
{
	public NavigatorWindow(Display display) 
	{
		super(true, false, false, true);
    	setupTheme(NavigatorWindow.class);

        
        setSize(250, 400);
        setXY(10, display.getHeight() - 500);
        setTitle("Navigator");
    	
        layout();
	}
}
