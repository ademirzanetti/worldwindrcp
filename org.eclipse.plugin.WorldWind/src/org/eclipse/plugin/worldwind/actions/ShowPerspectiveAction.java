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
package org.eclipse.plugin.worldwind.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;

/**
 * Action to switch perspectives or open views
 * @author Owner
 *
 */
public class ShowPerspectiveAction extends Action
{

    private IWorkbenchWindow window;
	private String ID;
	private boolean isView = false;

	public ShowPerspectiveAction(String text
			, IWorkbenchWindow window
			, String ID
			, ImageDescriptor image)
	{
        super(text, image);
        this.window = window;
        this.ID 	= ID;
	}
	
	public ShowPerspectiveAction(String text
			, IWorkbenchWindow window
			, String ID
			, ImageDescriptor image
			, boolean isView) 
	{
        super(text, image);
        this.window = window;
        this.ID 	= ID;
        this.isView	= isView;
	}
	
	@Override
	public void run() {
        if ( window != null) {
        	try {
        		if ( isView)
        			window.getActivePage().showView(ID);
        		else
        			window.getWorkbench().showPerspective(ID, window);
			} 
        	catch (WorkbenchException e) {
        		MessageDialog.openError(window.getShell()
						, "Open View"
						, e.getMessage()
						);
			}
        }
	}

}
