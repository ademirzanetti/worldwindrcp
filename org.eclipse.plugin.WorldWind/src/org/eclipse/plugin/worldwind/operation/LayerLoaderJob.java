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

import java.io.FileNotFoundException;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.views.LayersTree;
import org.eclipse.plugin.worldwind.views.StatusLine;
import org.eclipse.plugin.worldwind.views.LayersTree.TreeObject;
import org.eclipse.plugin.worldwind.views.LayersTree.TreeParent;
import org.eclipse.swt.widgets.Display;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.loop.HTTPSatLoopLayerList;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;


/**
 * An eclipse job to load remote real time satellite data sets from
 * the NAVY Labs & NOAA GOES. The datasets are images siting on a HTTP
 * server with a date encoded in the name, and change constantly.
 * @author vsilva
 *
 */
public class LayerLoaderJob extends Job 
{

	LayersTree treeViewer;
	Display display;
	StatusLine statusLine;
	
	public LayerLoaderJob(Display display
			, LayersTree treeViewer
			, StatusLine statusLine) 
	{ 
		super(Messages.getText("remote.layers.load.lbl"));
		this.treeViewer = treeViewer;
		this.display = display;
		this.statusLine = statusLine;
	}
	
	public IStatus run(IProgressMonitor monitor) 
	{
		try {
			monitor.beginTask(Messages.getText("remote.layers.load.lbl"), IProgressMonitor.UNKNOWN );
			
			display.syncExec(new Runnable() {
				public void run() {
					statusLine.beginTask(Messages.getText("remote.layers.load.lbl")
							, IProgressMonitor.UNKNOWN);
					statusLine.lockProgress();
				}
			});

			TreeParent tp = buildSatLoopLayerList(); 

			display.syncExec(new Runnable() {
				public void run() {
					statusLine.taskDone();
					statusLine.unlockProgress();
				}
			});
			monitor.done();
			
			if ( ! tp.hasChildren() ) return Status.OK_STATUS;

			// must clone the tree parent so the children won't be duplicated
			// when added to the tree
			final TreeParent parent 	= new TreeParent(tp.getLayer());
			final TreeObject[] children = tp.getChildren().clone();
			
	        // self + children
			parent.setRemovable(false);
			
			tp = null;
			
			display.syncExec(new Runnable() {
				public void run() {
					treeViewer.addTreeObject(parent
							, children
							, true
							, false);
				}
			});
			return Status.OK_STATUS;
		} 
		catch (final Exception e) 
		{
			display.syncExec(new Runnable() {
				public void run() {
					statusLine.setErrorMessage(Messages.getText("remote.layers.err.lbl") 
							+ e.getMessage());
				}
			});
			return new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e);
			//return Status.CANCEL_STATUS;
		}
	}
	
	/**
	 * Build a list of the best loop layers from the NAvy labs & NOAA
	 * There is a lot of stuff so only a subset of the total layers
	 * and only the latest MAX_LAYERS from each dataset are fetched.
	 * All data is in real time. So it should not be cached
	 * @return
	 */
    private TreeParent buildSatLoopLayerList() 
    	throws FileNotFoundException
    {
		/*
		 * Satellite layers: NRL & NOAA
		 */
    	RenderableLayer topLayer = new RenderableLayer();
		topLayer.setName( Messages.getText("remote.layers.tree.lbl") );
		
		TreeParent top = new TreeParent(topLayer, null);
		
		// Build the layer list from the XML file config/sat.loops.xml
		HTTPSatLoopLayerList list = new HTTPSatLoopLayerList(
				Messages.getInputStream(LayerLoaderJob.class, Messages.getText("layers.loops.xml.file"))
				);
		
		
		list.setAllEnabled(false);

		// Add children layers (Animated overlays) to the tree
        for (Layer layer : list)
        {
        	final TreeParent tp = new TreeParent(layer, null);
        	
        	top.addChild(tp);
        	
        	// Add frames from each overlay
        	for ( GroundOverlayLayer go : ((TimeLoopGroundOverlay)layer).getOverlays() )
        	{
        		final TreeObject to = new TreeObject(go, null);
        		tp.addChild(to);
        	}
        }
        top.setRemovable(false);
		return top;
    }
	
}
