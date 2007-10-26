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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.WorkbenchException;

/**
 * Action to switch perspectives
 * @author Owner
 *
 */
public class ShowPerspectiveAction extends Action
{

    private IWorkbenchWindow window;
	private String PerspectiveID;
	
	public ShowPerspectiveAction(String text, IWorkbenchWindow window, String PerspectiveID) 
	{
        super(text);
        this.window = window;
        this.PerspectiveID = PerspectiveID;
	}
	
	@Override
	public void run() {
        if ( window != null) {
        	try {
        		window.getWorkbench().showPerspective(PerspectiveID, window);
			} 
        	catch (WorkbenchException e) {
        		//e.printStackTrace();
        		MessageDialog.openError(window.getShell()
						, "Open View"
						, e.getMessage()
						);
			}
        }
	}

}
