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


/**
 * World Wind Moon View
 * @author Vladimir Silva
 *
 */
public class MoonView extends ViewPart
{
	//private static final Logger logger 		= Logger.getLogger(MoonView.class);
	
	public static final String ID 			= MoonView.class.getName(); 
	private final WorldWindowGLCanvas world = new WorldWindowGLCanvas(); ;
	
	/* ww 0.5
    private static final String LAYERS = "gov.nasa.worldwind.layers.Earth.StarsLayer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine40Layer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine30Layer"
        + ",gov.nasa.worldwind.layers.Moon.ShadedElevationLayer"
        + ",gov.nasa.worldwind.layers.Earth.ScalebarLayer"
        + ",gov.nasa.worldwind.layers.CompassLayer"; */
	
    private static final String LAYERS = "gov.nasa.worldwind.layers.StarsLayer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine40BaseLayer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine40Layer"
        + ",gov.nasa.worldwind.layers.Moon.Clementine30Layer"
        + ",gov.nasa.worldwind.layers.Moon.ShadedElevationLayer"
        + ",gov.nasa.worldwind.layers.ScalebarLayer"
        + ",gov.nasa.worldwind.layers.CompassLayer";
	
	
	public MoonView() {
		/**
		 * Initialize the default WW layers
		 */
		initWorldWindLayerModel();
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
        
	}

	/*
	 * Initialize WW model with default layers
	 */
	private void initWorldWindLayerModel () 
	{
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, gov.nasa.worldwind.globes.Moon.class.getName());
        Configuration.setValue(AVKey.LAYERS_CLASS_NAMES, LAYERS);

        // ww 0.6
        Configuration.setValue(AVKey.MOON_ELEVATION_MODEL_CONFIG_FILE, "config/Moon/MoonElevationModel.xml");
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 6000e3);  // 6000km
        
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		// Add Position Layer
		//m.getLayers().add(new PositionLayer(world));

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
	
	public void repaint() {
		world.repaint();
	}

	@Override
	public void dispose() {
		super.dispose();
	}
	
}