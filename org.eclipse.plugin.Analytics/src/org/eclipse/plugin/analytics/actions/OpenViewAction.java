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
package org.eclipse.plugin.analytics.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

/**
 * Action to activate an existing view
 * @author Owner
 *
 */
public class OpenViewAction extends Action 
{
    private IWorkbenchWindow window;
	private String ViewID;
	
	public OpenViewAction(String text, IWorkbenchWindow window, String ViewID) 
	{
        super(text);
        this.window = window;
        this.ViewID = ViewID;
	}
	
	@Override
	public void run() {
        if ( window != null) {
        	try {
        		window.getActivePage().showView(ViewID);
			} 
        	catch (PartInitException e) {
        		MessageDialog.openError(window.getShell()
						, "Open View"
						, e.getMessage()
						);
			}
        }
	}
}
