package org.bluemarble.util;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.view.FlyToOrbitViewStateIterator;
import gov.nasa.worldwind.view.OrbitView;

import java.util.ArrayList;

import org.bluemarble.BlueMarble3D;
import org.fenggui.Display;
import org.fenggui.IWidget;
import org.fenggui.composites.MessageWindow;
import org.fenggui.layout.StaticLayout;

public class BM3DUtils {

	/**
	 * Helper {@link MessageWindow}
	 * @param display
	 * @param text
	 */
	public static void MessageBox(Display display, final String text)
	{
		MessageWindow mw = new MessageWindow(text);
		mw.setTitle(BlueMarble3D.APP_NAME);
		mw.pack();
		display.addWidget(mw);
		StaticLayout.center(mw, display);
	}
	
	/**
	 * Flip {@link IWidget} visibility
	 * @param display
	 * @param w
	 */
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
	
	/**
	 * Fly to a {@link LatLon} locaion
	 * @param canvas
	 * @param latlon
	 */
	static public void flyTo (WorldWindowGLCanvas canvas, LatLon latlon) 
	{
		View view 			= canvas.getView();
		Globe globe 		= canvas.getModel().getGlobe();
		
		view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator(
        		(OrbitView)view
        		, globe
        		, latlon		// bbox
        		, Angle.ZERO	// Heading
        		, Angle.ZERO	// Pitch
        		, 3e3 ) 		// Altitude/Zoom (m) Angle.ZERO.degrees)
        		);
	}
	
}
