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
package worldwind.contrib.layers.loop;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;


import worldwind.contrib.Messages;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.ScreenOverlayLayer;
import worldwind.contrib.layers.GroundOverlayLayer.OverlayListener;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.SimpleKMLParser;
import worldwind.contrib.parsers.SimpleKMLParser.KMLDocument;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.Logging;

/**
 * Animated ground overlay layer. It is a {@link RenderableLayer} with 
 * a layer list of {@link GroundOverlayLayer}. Used to build simple 
 * animations from KML or WMS. Animation works by switching child layer 
 * visibility using background thread based on a timeout affected 
 * by a speed value (0..100)
 * 
 * @author V Silva
 *
 */
public class TimeLoopGroundOverlay extends RenderableLayer 
	implements OverlayListener
{
	private static final Logger logger 				= Logger.getLogger(TimeLoopGroundOverlay.class);
	private CopyOnWriteArrayList<GroundOverlayLayer> overlays 	= new CopyOnWriteArrayList<GroundOverlayLayer>();    

    /** A thread used to flip layer visibility */
    private Worker animator 			= null;
    private WorldWindowGLCanvas canvas 	= null;
    
	// Animation loop status listeners
    private CopyOnWriteArrayList<OverlayLoopListener> listeners = new CopyOnWriteArrayList<OverlayLoopListener>(); 
    	
    // Legend
    private ScreenOverlayLayer legend;
    
    // Description (usually jtml)
    private String description;
    
    
    /**
     * An interface to receive animation step updates
     * @author vsilva
     *
     */
    static public interface OverlayLoopListener
    	extends OverlayListener
    {
    	void statusChanged(final int current, final int total, final GroundOverlayLayer layer);
    }
    
    /**
     * Thread to control animation by manipulating layer visibility
     * @author Owner
     *
     */
    static private class Worker implements Runnable 
    {
    	private static final Logger logger = Logger.getLogger(Worker.class);
    	
    	CopyOnWriteArrayList<GroundOverlayLayer> overlays;
    	CopyOnWriteArrayList<GroundOverlayLayer> hiddenOverlays = new CopyOnWriteArrayList<GroundOverlayLayer>();

    	// used to add layers & repaint canvas
        private WorldWindowGLCanvas canvas;

    	// Visible frame. Only 1 frame is visible at a given time
        private int visibleFrame = 0;
    	private long interval = 50000;

    	// animation speed: 0..100
    	private int speed = 50;
    	private boolean done = false;

    	// For loop status updates
    	//private Vector<GroundOverlayLoopListener> listeners;
    	private CopyOnWriteArrayList<OverlayLoopListener> listeners;
    	
    	public Worker(CopyOnWriteArrayList<GroundOverlayLayer> overlays, WorldWindowGLCanvas canvas) {
    		this.overlays = overlays;
    		this.canvas = canvas;
		}
    	
    	/* Remove a loop status listener */
    	void removeListener( OverlayLoopListener listener){
    		listeners.remove(listener);
    	}
    	
    	//void setLoopListeners (Vector<GroundOverlayLoopListener> listeners) {
    	void setLoopListeners (CopyOnWriteArrayList<OverlayLoopListener> listeners) {
    		this.listeners = listeners;
    	}
    	
    	/* notify listeners of a loop update */
    	void statusChanged (int current, int total, GroundOverlayLayer layer) 
    	{
    		Iterator<OverlayLoopListener> iter = listeners.iterator();
    		
    		while ( iter.hasNext()) {
    			iter.next().statusChanged(current, total , layer );
			}
    	}
    	
    	/*
    	 * Animation loop
    	 * @see java.lang.Runnable#run()
    	 */
    	public void run()  
    	{
    		done = false;
    		GroundOverlayLayer frame;
    		
    		do 
    		{
    			final long sleep = interval/getSpeed();
    			setAllEnabled(false);
    			
    			frame = (GroundOverlayLayer)overlays.get(visibleFrame);
    			
    			logger.debug("Visible frame=" + visibleFrame  
    					+ "/" + overlays.size() + " sleep=" + sleep + " done=" + done);
    			
    			if (!hiddenOverlays.contains(frame))
    			{
	    			// make current layer visible
	    			frame.setEnabled(true);
	    			
	    			statusChanged(visibleFrame + 1, overlays.size(), frame);
	    			
	    			// sleep
	    			try {
		    			Thread.sleep(sleep);
					} catch (InterruptedException e) {
						done = true;
					}
	
					frame.setEnabled(false);
					
					canvas.repaint();
    			}
    			
				// make next layer visible
				if ( ! done ) 
					visibleFrame++;
				
    			// check loop limits
    			if ( visibleFrame > overlays.size() - 1 )
    				visibleFrame = 0;
				
    		} 
    		while ( ! done );
    	}

    	private void setAllEnabled (boolean enabled)
    	{
    		for ( GroundOverlayLayer overlay : overlays) {
    			overlay.setEnabled(enabled);
    		}
    	}
    	
    	private synchronized void pause () {
    		done = true;
    	}

    	private synchronized void stop () {
    		// signal thread completion
    		done = true;
    		
			// thread done (reset)
    		visibleFrame = 0;
    	}
    	
    	private synchronized void setSpeed (int speed) {
    		this.speed = speed;
    	}

		private int getSpeed() {
			return speed;
		}
		
		private long getSleepInterval () {
			return interval/getSpeed();
		}
    }
    
    /**
     * TimeLoopGroundOverlay
     * @param name
     * @param world
     */
	public TimeLoopGroundOverlay(String name) { 
		super();
		super.setName(name);
	}

	/**
     * TimeLoopGroundOverlay
     * @param name
     * @param world
     */
	public TimeLoopGroundOverlay(String name, WorldWindowGLCanvas canvas ) {
		super();
	
		if ( canvas == null)
			throw new RuntimeException(Messages.getText("err.txt.gl"));
		
		this.canvas = canvas;
		
		super.setName(name);
		animator = new Worker((CopyOnWriteArrayList<GroundOverlayLayer>)overlays, canvas);
	}

	/**
	 * Add a ground overlay to this animated overlay
	 * @param overlay
	 */
	public void add(GroundOverlayLayer overlay) 
	{
        if (overlay == null)
        {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().log(java.util.logging.Level.FINE, msg);
            throw new IllegalArgumentException(msg);
        }
        
        // layer is invisible by default
        overlay.setEnabled(false);
        
        // listen for overlay events
        overlay.addOverlayListener(this);
        
        // local overlays (for animation)
        this.overlays.add(overlay);
        
        // add overlay to WW so it'll render itself
        if ( canvas != null &&  canvas.getModel() != null ) {
        	canvas.getModel().getLayers().add(overlay);
        }
    }

	/**
	 * Set child layer visibility
	 * @param layer Layer to hide from animation
	 */
	public synchronized void setVisible (Layer layer, boolean visible) 
	{
        if (layer == null)
        {
            String msg = Logging.getMessage("nullValue.Shape");
            Logging.logger().log(java.util.logging.Level.FINE, msg);
            throw new IllegalArgumentException(msg);
        }
        if ( animator == null ) 
        	return;
        
        if ( !visible ) 
        	animator.hiddenOverlays.add((GroundOverlayLayer)layer);
        else {
        	animator.hiddenOverlays.remove((GroundOverlayLayer)layer);
        }
    }
	
	public void setWindowCanvas( WorldWindowGLCanvas canvas)
	{
		this.canvas = canvas;
	}
	
    public void play()
    {	
    	if (canvas == null) return;
    	
    	animator = new Worker(overlays, canvas);    	
    	animator.hiddenOverlays.clear();
    	animator.setLoopListeners(listeners);
    	
    	new Thread(animator).start();
    }
    

    public void stop()
    {
    	if( animator != null ) 
    		animator.stop();
    	setAllEnabled(false);
    }
    
    public void pause()
    {
    	if( animator != null )
    		animator.pause();
    }
    public void resume() //WorldWindowGLCanvas world)
    {
    	play(); //world);
    }
    
    
    @Override
    public String toString() {
    	return super.getName(); // title;
    }


	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(int speed) {
		if ( speed > 100 ) speed = 100;
		if ( speed < 1 ) speed = 1;
		animator.setSpeed(speed);
	}

	/**
	 * @return the animation speed
	 */
	public int getSpeed() {
		return animator.getSpeed();
	}

	/**
	 * Set child ground overlays that compose this animated overlay
	 * @param overlays
	 */
	public void setOverlays(CopyOnWriteArrayList<GroundOverlayLayer> overlays) {
		this.overlays = overlays;
	}

	public CopyOnWriteArrayList<GroundOverlayLayer> getOverlays() {
		return overlays;
	}
	
	
	private void setAllEnabled (boolean enabled)
	{
        for (Layer layer : getOverlays())
        {	
        	layer.setEnabled(enabled);
        }
        // legend
        // if ( legend != null ) legend.setEnabled(enabled);
	}
	
	/** Enable/disable all layers */
	public void setEnabled (boolean enabled)
	{
		setAllEnabled(enabled);
	}

	/**
	 * Add child overlays to the WW layer model so they will render themselves
	 * @param canvas
	 */
	public synchronized void addToModel (WorldWindowGLCanvas canvas ) 
	{
		this.canvas = canvas;
    	for (GroundOverlayLayer ol : overlays) {
    		logger.debug("Adding " + ol + " to model");
    		canvas.getModel().getLayers().add(ol);
		} 
    	// legend
    	if ( legend != null ) {
    		logger.debug("Adding legend " + legend + " to the WW model");
    		canvas.getModel().getLayers().add(legend);
    	}
    	else
    		logger.debug("No legend for " + getName());
	}
	
	/**
	 * Remove child ground overlays from WW layer model
	 */
	public synchronized void removeFromModel () 
	{
		if ( canvas == null ) return;
		
    	for (GroundOverlayLayer ol : overlays) {
    		logger.debug("removing " + ol + " from model");
    		canvas.getModel().getLayers().remove(ol);
		} 
    	// legend
    	if ( legend != null )
    		canvas.getModel().getLayers().remove(legend);
	}

	/* Loop listeners */
	public void addLoopListener( OverlayLoopListener listener){
		listeners.add(listener);
	}

	public void removeLoopListener( OverlayLoopListener listener){
		listeners.remove(listener);
		if ( animator != null)
			animator.removeListener(listener);
	}
	
	/**
	 * Prefetch some layers to improve GUI response
	 */
//	public void synchFetchSome() {
//		int size = ( overlays.size() < 5) ? overlays.size() : 5;
//		for (int i = 0; i < size; i++) {
//			((GroundOverlayLayer)overlays.get(i)).synchFetch();
//		}
//	}

//	public void asynchFetchSome() {
//		int size = ( overlays.size() < 5) ? overlays.size() : 5;
//		for (int i = 0; i < size; i++) {
//			((GroundOverlayLayer)overlays.get(i)).asynchFetch();
//		}
//	}
	
	/**
	 * Overlay names
	 * @return array of overlay names
	 */
	public String[] getOverlayNames () {
		String[] names = new String[overlays.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = overlays.get(i).getName();
		}
		return names;
	}
	
	/**
	 * Set overlay legend
	 * @param legendURL URL of legend image
	 */
	public void setLegend ( URL legendURL) {
		legend = new ScreenOverlayLayer("Legend", legendURL, ScreenOverlayLayer.SOUTHWEST );
	}
	
	/**
	 * Set overlay legend
	 * @param legendURL Legend url
	 * @param position one of NORTHEASET, NORTHWEST, SOUTHEAST, SOUTHWEST
	 */
	public void setLegend ( URL legendURL, String position) {
		legend = new ScreenOverlayLayer("Legend", legendURL, position );
	}

	/**
	 * Set overlay legend
	 * @param name legend name
	 * @param legendURL legend URL
	 * @param position position see {@link ScreenOverlayLayer}
	 */
	public void setLegend (String name, URL legendURL, String position) {
		legend = new ScreenOverlayLayer(name, legendURL, position );
	}
	
	/**
	 * Set overlay legend
	 * @param legend legend {@link ScreenOverlayLayer}
	 */
	public void setLegend (ScreenOverlayLayer legend) {
		this.legend = legend;
	}
	
	@Override
	public void dispose() {
		logger.debug("Disposing loop " + getName());
		super.dispose();
		stop();
		removeFromModel();
		
		for (GroundOverlayLayer layer : overlays) {
			layer.deleteFromCache();
		}
	}

	/**
	 * Fires when an error occurs in the child ground overlay
	 */
	public void onError(Layer layer, Exception ex) 
	{
		// notify listeners
    	for (OverlayLoopListener listener : listeners) {
			listener.onError(layer, ex);
		}
	}
	
	/**
	 * Get overlay center {@link Point}
	 * @param globe {@link Globe}
	 * @return
	 */
	public Vec4 getCentroid(Globe globe) {
		// should be the same for all ground overlays
		return (overlays.size() > 0) ? overlays.get(0).getCentroid(globe) : null;
	}

	public Sector getSector() {
		// should be the same for all ground overlays
		return (overlays.size() > 0) ? overlays.get(0).getSector() : null;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return ( description != null ) ? description : getName();
	}
	
	/**
	 * Build a KML fragment representing this layer using absolute
	 * path names.
	 * @return KML fragment
	 */
	public String toKML() {
		return toKML(true, false);
	}
	
	/**
	 * Build a KML fragment representing this layer.
	 * @param 	useAbsolutePaths if true icon hrefs will use absolute paths.
	 * 			false is useful for building a KMZ with relative images
	 * @param	nameIsTimeSpan if true overlay (frame) name represents the
	 * 			KML time span (This will only work is the name is an ISO compliant
	 * 			time
	 * @return KML fragment representation of this layer.
	 */
	public String toKML(boolean useAbsolutePaths
			, boolean nameIsTimeSpan ) 
	{
		StringBuffer buf = new StringBuffer("<Folder><name>" + getName() + "</name>" + Messages.NL);
		
		// if no description use the name
		final String desc = ( description != null ) ? description : getName();
		
		//if ( description != null )
		buf.append("<description><![CDATA["	
				+ desc  
				+ "]]></description>" + Messages.NL);
		
		// append legend (screen overlay)
		if ( legend != null ) 
		{
			final String legendKml = legend.toKML(useAbsolutePaths) ;
			
			if ( legendKml != null)
				buf.append(legendKml + Messages.NL);
		}
			
		for (GroundOverlayLayer ov : overlays) 
		{
			final String dummy = ov.toKML(useAbsolutePaths, nameIsTimeSpan);
			
			if ( dummy != null )
				buf.append(dummy + Messages.NL);
		}
		
		buf.append("</Folder>");
		return buf.toString();
	}

	/**
	 * Show a frame from the loop overlay. Fetch it if not in WW cache
	 * @param index frame nu,ber
	 */
	public void showFrame (int index) 
	{
		if ( index < 0 || index > overlays.size()) return;
		
		GroundOverlayLayer layer = overlays.get(index);
		logger.debug("Showing frame "  +  index + " name=" + layer.getName());
		
		// hide all
		setAllEnabled(false);
		
		if ( layer.fetchOverlay() )
			layer.setEnabled(true);
		else
			logger.error("Showing frame "  +  index + " name=" + layer.getName() + " FAILED.");
	}
	
	public boolean isFrameinCache (int index) 
	{
		if ( index < 0 || index > overlays.size()) return false;
		return overlays.get(index).isTileInCache();
	}
	
	/**
	 * Gets the animation thread sleep interval
	 * @return 	Sleep interval or -1 if the animator thread has not been
	 * 			initialized.
	 */
	public long getSleepInterval () {
		if ( animator == null) return -1;
		return animator.getSleepInterval();
	}
	
	/**
	 * Create a {@link TimeLoopGroundOverlay} from a KML fragment 
	 * @param kmlFragment KML string with <GroundOverlay> tags
	 * @return {@link TimeLoopGroundOverlay}
	 * @throws Exception
	 */
	public static TimeLoopGroundOverlay newFromKML ( String kmlFragment)
		throws Exception
	{
		SimpleKMLParser parser = new SimpleKMLParser();
		KMLDocument doc = parser.parse("doc.kml", new ByteArrayInputStream(kmlFragment.getBytes()));
		return KMLSource.toTimeLoopGroundOverlay(doc);
	}
	
	/**
	 * Return the {@link ScreenOverlayLayer} legend
	 * @return
	 */
	public ScreenOverlayLayer getLegend() {
		return legend;
	}
	
	/**
	 * Set opacity for this time loop
	 * @param opacity a double range between 0..1
	 */
	public void setOpacity(double opacity) {
		super.setOpacity(opacity);
		for (GroundOverlayLayer layer : overlays) {
			layer.setOpacity(opacity);
		}
	}
}
