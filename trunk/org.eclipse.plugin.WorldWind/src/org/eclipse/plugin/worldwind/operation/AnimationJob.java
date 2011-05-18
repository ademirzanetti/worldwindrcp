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

import gov.nasa.worldwind.layers.Layer;

import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.plugin.worldwind.utils.StatusLine;
import org.eclipse.plugin.worldwind.views.EarthView;
import org.eclipse.swt.widgets.Display;

import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.ScreenOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer.OverlayListener;
import org.eclipse.plugin.worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

/**
 * An eclipse job to make animations smoother. The class {@link TimeLoopGroundOverlay}
 * implements a thread based animator that hangs the GUI when a texture is fetched.
 * 
 * @author Vladimir Silva
 *
 */
public class AnimationJob extends Job
	implements OverlayListener
{
	private static final Logger logger	= Logger.getLogger(AnimationJob.class);
	
	private TimeLoopGroundOverlay layer;
	private boolean done 	= false;
	private long interval 	= 50000; 	// base sleep interval
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
		
		
		addOverLayListeners();
		logger.debug("layer=" + layer);
	}

	/* listen for layer events */
	private void addOverLayListeners() 
	{
		CopyOnWriteArrayList<GroundOverlayLayer> overlays = layer.getOverlays();
		
		for (GroundOverlayLayer gol : overlays) {
			gol.addOverlayListener(this);
		}
	}

	/* remove listeners */
	private void removeOverLayListeners() 
	{
		CopyOnWriteArrayList<GroundOverlayLayer> overlays = layer.getOverlays();
		
		for (GroundOverlayLayer gol : overlays) {
			gol.removeOverlayListener(this);
		}
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) 
	{
		CopyOnWriteArrayList<GroundOverlayLayer> overlays = layer.getOverlays();
		
		// # of frames
		int size = overlays.size();

		
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
					
					final String message =  overlays.get(i).getName() 
						+ " ("  + (i+1) + "/" + size + ")" ;
	
					monitor.beginTask(message, IProgressMonitor.UNKNOWN );
					
					// fetch & show frame
					layer.showFrame(i);
					
					// repaint globe
					EarthView.repaint();
					
					try {
						Thread.sleep(sleep);
					} catch (Exception e) {
						System.err.println(e);
						done = true;
					}

					monitor.done(); 
					
					// cancel job?
					if (monitor.isCanceled()) {
						// hide legend
						if ( legend != null) legend.setEnabled(false);
						
						// return w/ cancel status
						return Status.CANCEL_STATUS;
					}
					
					if ( done ) break;
				}
			}
			while ( ! done);
			
			// hide legend
			if ( legend != null) legend.setEnabled(false);
			
		}
		catch (Exception e) {
			//e.printStackTrace();
			return Status.CANCEL_STATUS;
		}
		finally {  
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	public void play () {
		schedule();
	}
	
	public void stop () {
		done = true;
		removeOverLayListeners();
	}
	
	
	@Override
	public String toString() {
		return "job=" + layer;
	}
	
	/******************************************************
	 * Ground Overlay listeners
	 ******************************************************/
	
	/**
	 * Fires on ground overlay error
	 */
	public void onError(Layer layer, Exception ex) 
	{
		ex.printStackTrace();
		
		final String message = layer.getName() + ": " 
			+ ex.getClass() + " " + ex.getMessage();
		
		logger.debug(message);
		
		display.syncExec(new Runnable() {
			public void run() {
				statusLine.setErrorMessage(message);
			}
		});

		// stop rendering layer
		layer.setEnabled(false);
	}
	
	public void setSpeed (int speed) {
		if ( speed > 100 ) speed = 100;
		if ( speed < 1 ) speed = 1;
		this.speed = speed;
	}
	
	public int getSpeed () {
		return speed;
	}
}
