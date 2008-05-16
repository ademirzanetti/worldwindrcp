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
import gov.nasa.worldwind.layers.FogLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.Mars.MDIMLayer;
import gov.nasa.worldwind.layers.Mars.MOCColorizedLayer;
import gov.nasa.worldwind.layers.Mars.MOCLayer;
import gov.nasa.worldwind.layers.Mars.MolaColorASULayer;
import gov.nasa.worldwind.layers.Mars.MolaColoredJPLLayer;
import gov.nasa.worldwind.layers.Mars.THEMISLayer;

import java.awt.BorderLayout;
import java.awt.Color;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import worldwind.contrib.layers.PositionLayer;


/**
 * World Wind Mars View
 * @author Vladimir Silva
 *
 */
public class MarsView extends ViewPart
{
	//private static final Logger logger 		= Logger.getLogger(MarsView.class);
	
	public final String ID 							= MarsView.class.getName(); 
	private final WorldWindowGLCanvas world 	= new WorldWindowGLCanvas(); ;
	

    private static final String LAYERS = "gov.nasa.worldwind.layers.Earth.StarsLayer"
        + ",gov.nasa.worldwind.layers.Earth.SkyGradientLayer"
        + ",gov.nasa.worldwind.layers.Earth.FogLayer"
        + ",gov.nasa.worldwind.layers.Mars.MOCLayer"
        + ",gov.nasa.worldwind.layers.Mars.MOCColorizedLayer"
        + ",gov.nasa.worldwind.layers.Mars.MolaColorASULayer"
        + ",gov.nasa.worldwind.layers.Mars.MolaColoredJPLLayer"
        + ",gov.nasa.worldwind.layers.Mars.MDIMLayer"
        + ",gov.nasa.worldwind.layers.Mars.THEMISLayer"
        + ",gov.nasa.worldwind.layers.Mars.THEMISColorLayer"
        + ",gov.nasa.worldwind.layers.Earth.ScalebarLayer"
        + ",gov.nasa.worldwind.layers.CompassLayer";
	
	
	
	public MarsView() {
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
        Configuration.setValue(AVKey.GLOBE_CLASS_NAME, gov.nasa.worldwind.globes.Mars.class.getName());
        Configuration.setValue(AVKey.LAYERS_CLASS_NAMES, LAYERS);
		
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		// Add Position Layer
		m.getLayers().add(new PositionLayer(world));

        m.setShowWireframeExterior(false);
        m.setShowWireframeInterior(false);
        m.setShowTessellationBoundingVolumes(false);

        world.setModel(m);
        
        // Adjust sky and fog layers
        LayerList layers = world.getModel().getLayers();
        
        for (Layer layer : layers)
        {
            if(layer instanceof SkyGradientLayer)
            {
                SkyGradientLayer sky = (SkyGradientLayer) layer;
                sky.setHorizonColor(new Color(236, 223, 174));
                sky.setZenithColor(new Color(183, 117, 3));
                sky.setAtmosphereThickness(40000);
            }
            else if(layer instanceof FogLayer)
            {
                FogLayer fog = (FogLayer) layer;
                fog.setColor(new Color(236, 223, 174));
            }
            else if(layer instanceof MOCLayer
            		|| layer instanceof MOCColorizedLayer
                    || layer instanceof MolaColorASULayer
                    || layer instanceof MolaColoredJPLLayer
                    || layer instanceof THEMISLayer
//                    || layer instanceof THEMISColorLayer
                    || layer instanceof MDIMLayer)
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