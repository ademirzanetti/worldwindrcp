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

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

import org.eclipse.plugin.worldwind.actions.OpenFileAction;
import org.eclipse.plugin.worldwind.actions.OpenWebBrowserAction;
import org.eclipse.plugin.worldwind.actions.ShowPerspectiveAction;
import org.eclipse.plugin.worldwind.actions.WMSWizardAction;
import org.eclipse.plugin.worldwind.actions.WeatherWizardAction;
import org.eclipse.plugin.worldwind.views.StatusLine;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	// Actions - important to allocate these only in makeActions, and then use
	// them
	// in the fill methods. This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	private IWorkbenchAction exitAction;
	private OpenFileAction openFileAction;
	private WMSWizardAction wmsWizardAction;
	private WeatherWizardAction weatherWizardAction;
	private OpenWebBrowserAction openWebBrowser;
	
	/** List of Actions used to open perspectives */
	private ArrayList<Action> perspectiveActions = new ArrayList<Action>();
	
	// Status line
	private static StatusLine statusLine;
	
	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	protected void makeActions(final IWorkbenchWindow window) {
		// Creates the actions and registers them.
		// Registering is needed to ensure that key bindings work.
		// The corresponding commands keybindings are defined in the plugin.xml
		// file.
		// Registering also provides automatic disposal of the actions when
		// the window is closed.

		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		
		openFileAction = new OpenFileAction(Messages.getText("file.open.tooltip"), window);
		register(openFileAction);
		
		
		wmsWizardAction = new WMSWizardAction(Messages.getText("wms.wiz.tooltip"), window);
		register(wmsWizardAction);
		
		weatherWizardAction = new WeatherWizardAction(Messages.getText("weather.wiz.tooltip"), window);
		
		register(weatherWizardAction);
		
		openWebBrowser = new OpenWebBrowserAction(Messages.getText("open.web.browset.tooltip"), window);
		register(openWebBrowser);
		
		/*
		 * Get All perspectives , and add actions to open them
		 */
		IWorkbench workbench 			=  window.getWorkbench();
		IPerspectiveRegistry registry 	= workbench.getPerspectiveRegistry();
		
		IPerspectiveDescriptor[] descriptors = registry.getPerspectives();
		
		for (IPerspectiveDescriptor perspective : descriptors) {
			perspectiveActions.add(new ShowPerspectiveAction(
					perspective.getLabel()
					, window
					, perspective.getId()
					)
			);
		}
		
	}

	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager(Messages.getText("menu.file.name"),
				IWorkbenchActionConstants.M_FILE);
		menuBar.add(fileMenu);

		fileMenu.add(openFileAction);
		
		fileMenu.add(weatherWizardAction);
//		fileMenu.add(new Separator());
		
		fileMenu.add(wmsWizardAction);
		fileMenu.add(new Separator());
		
		fileMenu.add(openWebBrowser);
		
	    fileMenu.add(new Separator());
		fileMenu.add(exitAction);

		// Create a Perspective switch menu only if 1+ perspectives available
		if ( perspectiveActions.size() > 1 ) {
		
			MenuManager perspectiveMenu = new MenuManager("&Perpective"
					, "org.eclipse.plugin.worldwind.PERSPECTIVES");

			// Add perspective actions
			for (Action action : perspectiveActions) {
				perspectiveMenu.add(action);
			}
		
			menuBar.add(perspectiveMenu);
		}
		
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) 
	{
        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        coolBar.add(new ToolBarContributionItem(toolbar, "main"));   
        
        toolbar.add(weatherWizardAction);
//        toolbar.add(new Separator());
        
        toolbar.add(wmsWizardAction);
        
//        toolbar.add(new Separator());
        toolbar.add(openWebBrowser);
	}
	
	protected void fillStatusLine(IStatusLineManager statusLine) {
		ApplicationActionBarAdvisor.statusLine = new StatusLine(statusLine);
	}
	
	public static StatusLine getDefaultStatusLine() {
		return statusLine;
	}
}
