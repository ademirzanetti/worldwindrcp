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
package org.eclipse.plugin.worldwind.views;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;


import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.Moon.Clementine30Layer;
import gov.nasa.worldwind.layers.Moon.ShadedElevationLayer;

import java.awt.BorderLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import worldwind.contrib.layers.PositionLayer;


/**
 * World Wind Moon View
 * @author Vladimir Silva
 *
 */
public class MoonView extends ViewPart
{
	//private static final Logger logger 		= Logger.getLogger(MoonView.class);
	
	public static final String ID 			= MoonView.class.getName(); 
	public static final WorldWindowGLCanvas world = new WorldWindowGLCanvas(); ;
	
//	private HeartBeatProbe probe;
//	private StatusLine statusLine;

    private static final String LAYERS = "gov.nasa.worldwind.layers.Earth.StarsLayer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine40Layer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine30Layer"
        + ",gov.nasa.worldwind.layers.Moon.ShadedElevationLayer"
        + ",gov.nasa.worldwind.layers.Earth.ScalebarLayer"
        + ",gov.nasa.worldwind.layers.CompassLayer";
	
	/**
	 * Initialize the default WW layers
	 */
	static {
		initWorldWindLayerModel();
	}
	
	/*
	 * Heartbeat Probe: It uses a thread to probe WW for active tasks
	 * If so it displays the progress monitor in the status line w/ the
	 * label 'Downloading'
	 */
//	private static class HeartBeatProbe 
//	{
//		Display display;
//		StatusLine statusLine;
//    	final int interval = 1000;
//		int count = 1;
//		private boolean done = false;
//		
//	    public HeartBeatProbe(final Display display, final StatusLine statusLine) 
//	    {
//	    	this.display = display;
//	    	this.statusLine = statusLine;
//	    }
//	    
//	    /* Probe main task thread */
//	    void run () 
//	    {
//	    	if ( display == null ) return;
//	    	
//	    	display.timerExec(interval , new Runnable() {
//	    		public void run() 
//	    		{
//	    			if ( done ) return;
//	    			
//	                RetrievalService service = WorldWind.getRetrievalService();
//
//	    			// probe heartbeat here
//	                if (service.hasActiveTasks()) 
//	                {
//	        			statusLine.beginTask(
//	        					Messages.getString("layer.worldview.probe.task.name")
//	        						, IProgressMonitor.UNKNOWN);
//	                }
//	                else {
//	                	statusLine.taskDone();
//	                }
//	                
//	    			// loop
//	                if ( ! done ) {
//	                	if (display != null )
//	                		display.timerExec(interval, this);
//	                }
//	    	    }
//	    	});
//		}
//
//		public void setDone(boolean done) {
//			this.done = done;
//		}
//
//		public boolean isDone() {
//			return done;
//		}
//	}
	
	
	public MoonView() {
		
	}
	
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) 
	{
        // Build GUI: top(SWT)->Frame(AWT)->Panel(AWT)->WorldWindowGLCanvas(AWT)
		Composite top = new Composite(parent, SWT.EMBEDDED);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
        
		java.awt.Frame worldFrame = SWT_AWT.new_Frame(top);
		java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout());
		
		worldFrame.add(panel);
        panel.add(world, BorderLayout.CENTER);

        // max parent widget
        parent.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // globe position listener
        //Display display = getViewSite().getShell().getDisplay();

		// initialize status line
//		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine(); 
//        
//        // probe heartbeat
//        probe = new HeartBeatProbe(display, statusLine);
//        probe.run();
	}

	/*
	 * Initialize WW model with default layers
	 */
	static void initWorldWindLayerModel () 
	{
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, gov.nasa.worldwind.globes.Moon.class.getName());
        Configuration.setValue(AVKey.LAYERS_CLASS_NAMES, LAYERS);
		
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		// Add Position Layer
		m.getLayers().add(new PositionLayer(world));

        m.setShowWireframeExterior(false);
        m.setShowWireframeInterior(false);
        m.setShowTessellationBoundingVolumes(false);

        world.setModel(m);
        
        // Adjust layers states
        LayerList layers = world.getModel().getLayers();
        for (Layer layer : layers)
        {
            if(layer instanceof Clementine30Layer || layer instanceof ShadedElevationLayer)
                layer.setEnabled(false);
        }
        
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}
	
	public static void repaint() {
		world.repaint();
	}

	@Override
	public void dispose() {
		super.dispose();
//		probe.setDone(true);
	}
	
}