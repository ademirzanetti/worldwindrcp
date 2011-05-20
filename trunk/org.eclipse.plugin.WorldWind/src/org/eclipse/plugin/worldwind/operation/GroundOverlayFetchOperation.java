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

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer;

/**
 * JFace operation to pre-fetch ground overlay layers. Used by the WMS Wizard
 * and others
 * @author Owner
 *
 */
public class GroundOverlayFetchOperation implements IRunnableWithProgress
{
	private static final Logger logger = Logger.getLogger(GroundOverlayFetchOperation.class);
	
	GroundOverlayLayer[] overlays;
	
	public GroundOverlayFetchOperation(GroundOverlayLayer[] overlays) {
		this.overlays = overlays;
	}

	public void run(IProgressMonitor monitor) 
		throws InvocationTargetException, InterruptedException 
	{
		for (int i = 0; i < overlays.length; i++) 
		{
			final GroundOverlayLayer layer = overlays[i];
			
			monitor.beginTask(layer.getName() +  " (" +i + "/" + overlays.length + ")"
					, IProgressMonitor.UNKNOWN);
			
			logger.debug("Pre-fetching " + layer + " Texture url=" + layer.getTextureURL());
			
			// fetch synchronously...
			layer.fetchOverlay(true); 
			
			monitor.done();
		}

		if (monitor.isCanceled())
	        throw new InterruptedException("Ground overlay fetch operation cancelled.");
		
	}

}
