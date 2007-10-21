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
package org.eclipse.plugin.worldwind;


import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import org.eclipse.plugin.worldwind.views.EarthView;
import org.eclipse.plugin.worldwind.views.GeoSearchView;
import org.eclipse.plugin.worldwind.views.LayersView;
import org.eclipse.plugin.worldwind.views.PlacesView;
import org.eclipse.plugin.worldwind.views.WebBrowserView;

public class Perspective implements IPerspectiveFactory 
{

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);

		// TopLeft: Places & GeoSearch
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 0.18f, editorArea);
		
		//topLeft.addPlaceholder(LayersView.ID + ":*");
		topLeft.addView(PlacesView.ID);
		topLeft.addView(GeoSearchView.ID);
		
		// Bottom left: Layers view
		IFolderLayout bottomLeft = layout.createFolder("bottomLeft", IPageLayout.BOTTOM, 0.50f,
		 	   "topLeft");
		bottomLeft.addView(LayersView.ID);

		// TopRight: Earth & web browser
		IFolderLayout topRight = layout.createFolder("Earth", IPageLayout.RIGHT, 1.0f, editorArea);
		topRight.addView(EarthView.ID);
		topRight.addView(WebBrowserView.ID);
		
		// These layers cannt be closed
		layout.getViewLayout(LayersView.ID).setCloseable(false);
		layout.getViewLayout(PlacesView.ID).setCloseable(false);
		layout.getViewLayout(EarthView.ID).setCloseable(false);
		layout.getViewLayout(GeoSearchView.ID).setCloseable(false);
	}
}
