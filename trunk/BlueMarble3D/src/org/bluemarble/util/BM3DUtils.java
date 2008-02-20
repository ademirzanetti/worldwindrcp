package org.bluemarble.util;

import java.util.ArrayList;

import org.bluemarble.BlueMarble3D;
import org.fenggui.Display;
import org.fenggui.IWidget;
import org.fenggui.composites.MessageWindow;
import org.fenggui.layout.StaticLayout;

public class BM3DUtils {

	public static void MessageBox(Display display, final String text)
	{
		MessageWindow mw = new MessageWindow(text);
		mw.setTitle(BlueMarble3D.APP_NAME);
		mw.pack();
		display.addWidget(mw);
		StaticLayout.center(mw, display);
	}
	
	public static void toggleWidget (Display display, IWidget w)
	{
		
		boolean found = false;
		ArrayList<IWidget> widgets = (ArrayList<IWidget>)display.getWidgets();
		for (IWidget widget : widgets) {
			if ( widget.equals(w) ) {
				found = true;
				break;
			}
		}
		if (  found ) display.removeWidget(w);
		else display.addWidget(w);
	}
}
