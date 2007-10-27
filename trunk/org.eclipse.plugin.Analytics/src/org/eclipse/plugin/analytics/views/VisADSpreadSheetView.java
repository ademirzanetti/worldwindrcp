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
package org.eclipse.plugin.analytics.views;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.plugin.analytics.AnalyticsActivator;
import org.eclipse.plugin.analytics.Messages;
import org.eclipse.plugin.worldwind.actions.OpenWebBrowserAction;
import org.eclipse.plugin.worldwind.views.WebBrowserView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.ViewPart;

import visad.ss.SpreadSheet;


public class VisADSpreadSheetView extends ViewPart {
	public static final String ID = VisADSpreadSheetView.class.getName();

	/** VisAD SpredSheet */
	private SpreadSheet sheet;
	
	/** spreadsheet actions */
	private Action addRowAction;
	private Action addColAction;

	private Action delRowAction;
	private Action delColAction;

	static {
		AnalyticsActivator.initSwingLookAndFeel();
	}
	
	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent) 
	{
		Composite top = new Composite(parent, SWT.EMBEDDED);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		java.awt.Frame mainFrame = SWT_AWT.new_Frame(top);

		sheet = new SpreadSheet(70, 70, 2, 2, null, null, Messages.getString("VisADSpreadSheetView.0")); //$NON-NLS-1$

		mainFrame.add( sheet.getContentPane());
		
		makeActions();
		contributeToActionBars();
	}

	// Local actions
	private void makeActions() {
		addRowAction = new Action() {
			public void run() {
				sheet.addRow();
			}
		};
		addRowAction.setText(Messages.getString("VisADSpreadSheetView.1")); //$NON-NLS-1$

		addColAction = new Action() {
			public void run() {
				sheet.addColumn();
			}
		};
		addColAction.setText(Messages.getString("VisADSpreadSheetView.2")); //$NON-NLS-1$

		delRowAction = new Action() {
			public void run() {
				sheet.deleteRow();
			}
		};
		delRowAction.setText(Messages.getString("VisADSpreadSheetView.3")); //$NON-NLS-1$

		delColAction = new Action() {
			public void run() {
				sheet.deleteColumn();
			}
		};
		delColAction.setText(Messages.getString("VisADSpreadSheetView.4")); //$NON-NLS-1$
		
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
//		fillLocalToolBar(bars.getToolBarManager());
		
	}
	
	/**
	 * Local toolbars
	 * @param manager
	 */
//	private void fillLocalToolBar(IToolBarManager manager) {
//		final IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
//		
//		// A help action that opens the WebBrowser view to the VisAD spreadsheet
//		// help at http://www.ssec.wisc.edu/~curtis/ss.html
//		Action helpAction = new Action () {
//			public void run() {
//				openWebBrowserLocation(window
//						, Messages.getString("VisADSpreadSheetView.5")); //$NON-NLS-1$
//			}
//		};
//		helpAction.setToolTipText(Messages.getString("VisADSpreadSheetView.6")); //$NON-NLS-1$
//		helpAction.setImageDescriptor(AnalyticsActivator
//				.getSharedImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
//		manager.add(helpAction);
//	}
	
	/** Local pulldown */
	private void fillLocalPullDown(IMenuManager manager) 
	{
		manager.add(addRowAction);
		manager.add(addColAction);
		manager.add(new Separator());
		manager.add(delRowAction);
		manager.add(delColAction);
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		
	}
	
	/**
	 * Open the World Wind plugin web browser & navigate to the given location
	 * @param window
	 * @param location
	 */
	public void openWebBrowserLocation (IWorkbenchWindow window, String location) 
	{
		// invalid wb window?
		if ( window == null ) {
			return;
		}
		
		try {
			// Display location on WB view
			System.out.println("***Show WB. URL=" + location); // + " inst=" + wbInstanceNum);			

			// Open web browser
			(new OpenWebBrowserAction("", window)).run();
			
			WebBrowserView wb = (WebBrowserView)AnalyticsActivator.getView(window, WebBrowserView.ID );
			
			if ( wb == null ) {
				MessageDialog.openError(window.getShell()
						, AnalyticsActivator.PLUGIN_NAME
						, Messages.getString("AnalyticsActivator.4")); //$NON-NLS-1$
				return;
			}
			
			// Set URL
			wb.setUrl(location);
		} 
		catch (Exception e) {
			MessageDialog.openError(window.getShell()
					, AnalyticsActivator.PLUGIN_NAME
					, Messages.getString("AnalyticsActivator.5") + e.getClass()  //$NON-NLS-1$
						+ ": " + e.getMessage()); //$NON-NLS-1$
		}
	}
	
}