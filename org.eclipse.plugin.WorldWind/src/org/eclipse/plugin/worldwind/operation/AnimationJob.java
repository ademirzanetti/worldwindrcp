/*******************************************************************************
 * Copyright (c) 2006 Vladimir Silva and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Silva - initial API and implementation
 *******************************************************************************/
package org.eclipse.plugin.worldwind.operation;


import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.plugin.worldwind.views.EarthView;
import org.eclipse.plugin.worldwind.views.StatusLine;
import org.eclipse.swt.widgets.Display;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.ScreenOverlayLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

/**
 * An eclipse job to make animations smoother. The class {@link TimeLoopGroundOverlay}
 * implements a thread based animator that hangs the GUI when a texture is fetched.
 * 
 * @author Vladimir Silva
 *
 */
public class AnimationJob extends Job 
{
	private TimeLoopGroundOverlay layer;
	private boolean done 	= false;
	private long interval 	= 100000; 	// base sleep interval
	private int speed 		= 50;		// animation speed: 0..100
	private Display display;
	private StatusLine statusLine;

	private ScreenOverlayLayer legend;
	
	
	public AnimationJob(Display display
			, TimeLoopGroundOverlay layer
			, StatusLine statusLine) 
	{
		super(layer.getName());
		this.layer 		= layer;
		this.statusLine	= statusLine;
		this.display 	= display;
		legend 			= layer.getLegend();
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) 
	{
		ArrayList<GroundOverlayLayer> overlays = layer.getOverlays();
		
		// # of frames
		int size = overlays.size();

		monitor.beginTask(layer.getName(), size );
		
		try 
		{
			// display the legend
			if ( legend != null)
				legend.setEnabled(true);
			
			do {
				// loop thru frames
				for (int i = 0; i < size; i++) 
				{
					final long sleep = interval/speed;
					
					final String message = layer.getName() + ":" 
						+ overlays.get(i).getName() 
						+ " ("  + (i+1) + "/" + size + ")" ;
	
					
					if ( display != null && !display.isDisposed() ) {
						display.syncExec(new Runnable() {
							public void run() {
								statusLine.setLoopStatusMessage(message);
							}
						});
					}
					
					// fetch & show frame
					layer.showFrame(i);
					
					// repaint GUI
					EarthView.repaint();
					
					monitor.worked(1);
					
					try {
						Thread.sleep(sleep);
					} catch (Exception e) {
						e.printStackTrace();
						done = true;
					}
					if ( done ) break;
				}
			}
			while ( ! done);
			
			// hide legend
			if ( legend != null) legend.setEnabled(false);
			
		}
		catch (Exception e) {
			return Status.CANCEL_STATUS;
		}

		monitor.done();

		return Status.OK_STATUS;
	}

	public void play () {
		schedule();
	}
	
	public void stop () {
		done = true;
	}
	
	
	@Override
	public String toString() {
		return "job=" + layer;
	}
}
