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
package org.eclipse.plugin.analytics;

import org.eclipse.plugin.analytics.views.VisADSpreadSheetView;
import org.eclipse.plugin.analytics.views.VerdiModels3View;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class Perspective implements IPerspectiveFactory {
	
	public void createInitialLayout(IPageLayout layout) 
	{
		defineLayout(layout);
		
	}
	
	public void defineLayout(IPageLayout layout) 
	{
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
		
		// Views are placed left of the editor area
		IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, 1.0f, editorArea);

		topLeft.addView(VerdiModels3View.ID);
		topLeft.addView(VisADSpreadSheetView.ID);
		
		layout.getViewLayout(VisADSpreadSheetView.ID).setCloseable(false);
		layout.getViewLayout(VerdiModels3View.ID).setCloseable(false);
	}

}
