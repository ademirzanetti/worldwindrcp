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
import org.eclipse.plugin.worldwind.views.WebBrowserView;

public class Perspective implements IPerspectiveFactory 
{

	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);

		IFolderLayout folder = layout.createFolder("Layers", IPageLayout.LEFT, 0.2f, editorArea);
		folder.addPlaceholder(LayersView.ID + ":*");
		folder.addView(LayersView.ID);
		folder.addView(GeoSearchView.ID);
		
		IFolderLayout folder1 = layout.createFolder("Earth", IPageLayout.RIGHT, 1.0f, editorArea);
		folder1.addView(EarthView.ID);
		folder1.addView(WebBrowserView.ID);
		
		layout.getViewLayout(LayersView.ID).setCloseable(false);
		layout.getViewLayout(EarthView.ID).setCloseable(false);
		layout.getViewLayout(GeoSearchView.ID).setCloseable(false);
	}
}
