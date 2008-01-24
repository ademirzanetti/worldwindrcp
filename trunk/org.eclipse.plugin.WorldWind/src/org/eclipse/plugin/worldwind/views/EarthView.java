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

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;

import org.apache.log4j.Logger;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.examples.BasicDragger;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Earth.TerrainProfileLayer;
import gov.nasa.worldwind.layers.Earth.WorldMapLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.view.FlyToOrbitViewStateIterator;
import gov.nasa.worldwind.view.OrbitView;
import gov.nasa.worldwind.View;

import java.awt.BorderLayout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;


import org.eclipse.plugin.worldwind.views.EarthView;

import worldwind.contrib.layers.PlacemarkLayer;
import worldwind.contrib.layers.PositionLayer;


/**
 * World Wind Eclipse RCP Earth View
 * @author Vladimir Silva
 *
 */
public class EarthView extends ViewPart
{
	private static final Logger logger 		= Logger.getLogger(EarthView.class);
	
	public static final String ID 			= EarthView.class.getName(); 
	public static final WorldWindowGLCanvas world = new WorldWindowGLCanvas(); ;
	
//	private HeartBeatProbe probe;
//	private StatusLine statusLine;

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
	
	/**
	 * Globe Selection listener
	 * @author Owner
	 *
	 */
	public class GlobeSelectionListener implements SelectListener 
	{
	    private WWIcon lastToolTipIcon = null;
	    private BasicDragger dragger = null;
	    private WorldWindowGLCanvas world;
	    private WWIcon lastPickedIcon;
		
		public GlobeSelectionListener(WorldWindowGLCanvas canvas) {
			dragger = new BasicDragger(canvas);
			world = canvas;
		}
		

	    private void highlight(Object o)
	    {
	        if (this.lastPickedIcon == o)
	            return; // same thing selected

	        if (this.lastPickedIcon != null)
	        {
	            this.lastPickedIcon.setHighlighted(false);
	            this.lastPickedIcon = null;
	        }

	        if (o != null && o instanceof WWIcon)
	        {
	            this.lastPickedIcon = (WWIcon) o;
	            this.lastPickedIcon.setHighlighted(true);
	        }
	    }

	    /**
	     * Select
	     */
	    public void selected(SelectEvent event)
	    {
	        if (event.getEventAction().equals(SelectEvent.LEFT_CLICK))
	        {
	            if (event.hasObjects())
	            {
	                if (event.getTopObject() instanceof WorldMapLayer)
	                {
	                    // Left click on World Map : iterate view to target position
	                    Position targetPos 	= event.getTopPickedObject().getPosition();
	                    OrbitView view 		= (OrbitView)world.getView();
	                    Globe globe 		= world.getModel().getGlobe();
	                    
	                    // Use a PanToIterator
	                    view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator(
	                            view, globe, new LatLon(targetPos.getLatitude(), targetPos.getLongitude()),
	                            Angle.ZERO, Angle.ZERO, targetPos.getElevation()));
	                }
	                else if  (event.getTopObject() instanceof WWIcon ) 
	                {
	                	// toggle icon annotation
	                	PlacemarkLayer.PlacemarkIcon icon = (PlacemarkLayer.PlacemarkIcon)event.getTopObject();
	                	
	                	if  ( icon.getAnnotation() != null )
	                		icon.getAnnotation().getAttributes().setVisible(
	                				! icon.getAnnotation().getAttributes().isVisible());
	                	
	                }
	            }
	            else
	                logger.debug("Single clicked " + "no object");
	        }
	        else if (event.getEventAction().equals(SelectEvent.HOVER))
	        {
	            if (lastToolTipIcon != null)
	            {
	                lastToolTipIcon.setShowToolTip(false);
	                this.lastToolTipIcon = null;
	                world.repaint();
	            }

	            if (event.hasObjects() && !this.dragger.isDragging())
	            {
	                if (event.getTopObject() instanceof WWIcon)
	                {
	                    this.lastToolTipIcon = (WWIcon) event.getTopObject();
	                    lastToolTipIcon.setShowToolTip(true);
	                    world.repaint();
	                }
	            }
	        }
	        else if (event.getEventAction().equals(SelectEvent.ROLLOVER) && !this.dragger.isDragging())
	        {
	            highlight(event.getTopObject());
	        }
	        else if (event.getEventAction().equals(SelectEvent.DRAG_END)
	            || event.getEventAction().equals(SelectEvent.DRAG))
	        {
	            // Delegate dragging computations to a dragger.
	            this.dragger.selected(event);
	            if (event.getEventAction().equals(SelectEvent.DRAG_END))
	            {
	                PickedObjectList pol = world.getObjectsAtCurrentPosition();
	                if (pol != null)
	                    highlight(pol.getTopObject());
	            }
	        }
	    }
	}
	
	
	public EarthView() {
		
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
//        Display display = getViewSite().getShell().getDisplay();

		// initialize status line
//		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine(); 
        
        GlobeSelectionListener listener1 = new GlobeSelectionListener(world);
        world.addSelectListener(listener1);

        // probe heartbeat
//        probe = new HeartBeatProbe(display, statusLine);
//        probe.run();
	}

	/*
	 * Initialize WW model with default layers
	 */
	static void initWorldWindLayerModel () 
	{
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		// Add Position Layer
		m.getLayers().add(new PositionLayer(world));

        m.setShowWireframeExterior(false);
        m.setShowWireframeInterior(false);
        m.setShowTessellationBoundingVolumes(false);

        world.setModel(m);
		
		// Add Terrain Profiler
		TerrainProfileLayer tp = new TerrainProfileLayer();
		
		tp.setEventSource(world);
		tp.setStartLatLon(LatLon.fromDegrees(0, -10));
        tp.setEndLatLon(LatLon.fromDegrees(0, 65));
        tp.setFollow(TerrainProfileLayer.FOLLOW_CURSOR);
        
        world.getModel().getLayers().add(tp);
        
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
	
	public void flyTo (LatLon latlon) 
	{
		View view 			= world.getView();
		Globe globe 		= world.getModel().getGlobe();
		
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