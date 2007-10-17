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

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.util.StatusLineContributionItem;

/**
 * Status Line Manager. 
 * @author vsilva
 *
 */
public class StatusLine 
{
	static final String HEARTBEAT 	= "HEARTBEAT";
	static final String LOOP_STATUS = "LOOP_STATUS";
	
	IStatusLineManager statusLine;
	
	private boolean locked = false;
	
	public StatusLine(IStatusLineManager statusLine) {
		this.statusLine = statusLine;

		StatusLineContributionItem item1 = new StatusLineContributionItem(LOOP_STATUS, 70);
		item1.setVisible(true);
		this.statusLine.add(item1);
		
		StatusLineContributionItem item = new StatusLineContributionItem(HEARTBEAT, 60);
		item.setVisible(true);
		this.statusLine.add(item);
		
	}
	
	public synchronized void setMessage (String message) {
		statusLine.setMessage(message);
	}
	
	public synchronized void setErrorMessage (String message) 
	{
		ImageDescriptor ICON_ERROR = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
		statusLine.setErrorMessage(ICON_ERROR.createImage(), message); 
	}

	public synchronized void setLoopStatusMessage (String message) {
		StatusLineContributionItem item =  (StatusLineContributionItem)statusLine.find(LOOP_STATUS);
		
		if ( item != null ) 
			item.setText(message);
		else 
			statusLine.setMessage(message);        		
	}
	
	public synchronized void beginTask (String message, int totalWork) {
		if ( locked ) return;
		statusLine.getProgressMonitor().beginTask(message, totalWork);
	}
	
	public synchronized void worked (int work) {
		statusLine.getProgressMonitor().worked(work);
	}
	
	public synchronized void taskDone () {
		if ( locked ) return;
		statusLine.getProgressMonitor().done();
	}
	
	public synchronized void setHeartbeatMessage(String message) {
		StatusLineContributionItem item =  (StatusLineContributionItem)statusLine.find(HEARTBEAT);
		if ( item != null ) item.setText(message);
		else statusLine.setMessage(message);
	}
	
	/**
	 * Used to lock the progress bar. Threads in several views can control the PM.
	 */
	public synchronized void lockProgress () {
		locked = true;
	}
	
	public synchronized void unlockProgress () {
		locked = false;
	}
	
	public boolean isLocked() {
		return locked;
	}
}
