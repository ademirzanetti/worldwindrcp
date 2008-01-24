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
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
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
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;

import org.eclipse.plugin.worldwind.actions.ICommandIds;
import org.eclipse.plugin.worldwind.actions.OpenFileAction;
import org.eclipse.plugin.worldwind.actions.OpenViewAction;
import org.eclipse.plugin.worldwind.actions.ShowPerspectiveAction;
import org.eclipse.plugin.worldwind.actions.WMSWizardAction;
import org.eclipse.plugin.worldwind.actions.WeatherWizardAction;
import org.eclipse.plugin.worldwind.actions.OpenViewAction.VIEW_TYPE;
import org.eclipse.plugin.worldwind.utils.CacheManagerDialog;
import org.eclipse.plugin.worldwind.views.NetCDFView;
import org.eclipse.plugin.worldwind.views.StatusLine;
import org.eclipse.plugin.worldwind.views.WebBrowserView;


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

	// File Menu Actions
	private OpenFileAction openFileAction;
	private WMSWizardAction wmsWizardAction;
	private WeatherWizardAction weatherWizardAction;
//	private OpenWebBrowserAction openWebBrowser;
	private OpenViewAction openWebBrowser;
	private OpenViewAction openDataSet;
	
	private IWorkbenchAction exitAction;

	private Action cacheManagerAction;
	
	// Help Menu actions
	private IWorkbenchAction aboutAction;
	private IWorkbenchAction showHelpAction;
	private IWorkbenchAction searchHelpAction;
	
	// Perspective Menu
	private IContributionItem showViewsItem;
	
	/** List of Open perspective Actions (Perspective menu) */
	private ArrayList<Action> perspectiveActions = new ArrayList<Action>();

	/** List of Open Views Actions (Perspective menu) */
	private ArrayList<Action> openViewActions = new ArrayList<Action>();
	
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

		// File Menu
		openFileAction = new OpenFileAction(Messages.getString("file.open.tooltip"), window);
		register(openFileAction);
		
		wmsWizardAction = new WMSWizardAction(Messages.getString("wms.wiz.tooltip"), window);
		register(wmsWizardAction);
		
		weatherWizardAction = new WeatherWizardAction(Messages.getString("weather.wiz.tooltip"), window);
		register(weatherWizardAction);
		
		//openWebBrowser = new OpenWebBrowserAction(Messages.getText("open.web.browset.tooltip"), window);
		openWebBrowser = new OpenViewAction(Messages.getString("open.web.browset.tooltip")
				, window, WebBrowserView.ID, true, VIEW_TYPE.WEB_BROWSER);
		
		// menu information
		openWebBrowser.initMenu(ICommandIds.CMD_OPEN_WEB_BROWSER, Activator.ICON_WEB_BROWSER);
		
		register(openWebBrowser);

		openDataSet = new OpenViewAction("Open NetCDF Dataset"
				, window, NetCDFView.ID, true, VIEW_TYPE.NETCDF);
		
		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
		
		/*
		 * Perspective Menu: Get All perspectives , and add actions to open them
		 */
		IWorkbench workbench 			= window.getWorkbench();
		IPerspectiveRegistry registry 	= workbench.getPerspectiveRegistry();
		
		IPerspectiveDescriptor[] descriptors = registry.getPerspectives();
		
		for (IPerspectiveDescriptor perspective : descriptors) {
			perspectiveActions.add(new ShowPerspectiveAction(
					perspective.getLabel()
					, window
					, perspective.getId()
					, perspective.getImageDescriptor()
					)
			);
		}
		
		// Get the 1st 4 views and add actions to open them
		IViewRegistry viewRegistry 	= workbench.getViewRegistry();
		IViewDescriptor[] views  	= viewRegistry.getViews();
		
		for (int i = 0; i < 4; i++) {
			final IViewDescriptor view = views[i];
			openViewActions.add(new ShowPerspectiveAction(
					view.getLabel()
					, window
					, view.getId()
					, view.getImageDescriptor()
					, true
					)
			);
		}
		
		// Show Views/Other contribution item
		showViewsItem = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		// Open the cache manager action
		cacheManagerAction = new Action(){ 
			public void run() {
				new CacheManagerDialog(window.getShell()).open();
			}
		};
		cacheManagerAction.setText(Messages.getString("menu.cache.name"));
		
		// Help menu
		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);
		
		showHelpAction = ActionFactory.HELP_CONTENTS.create(window); 
	    register(showHelpAction); 
		
		searchHelpAction = ActionFactory.HELP_SEARCH.create(window);
		register(searchHelpAction);
		
	}

	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager(Messages.getString("menu.file.name"),
				IWorkbenchActionConstants.M_FILE);

		MenuManager helpMenu = new MenuManager(Messages.getString("menu.help.name"), IWorkbenchActionConstants.M_HELP);
		
		menuBar.add(fileMenu);

		// File
		fileMenu.add(openFileAction);
		fileMenu.add(openDataSet);
		fileMenu.add(weatherWizardAction);
		fileMenu.add(wmsWizardAction);
		fileMenu.add(new Separator());
		fileMenu.add(openWebBrowser);
	    fileMenu.add(new Separator());
		fileMenu.add(exitAction);

		// Window
		MenuManager perspectiveMenu = new MenuManager(
				Messages.getString("menu.perspective.name")
				, "org.eclipse.plugin.worldwind.PERSPECTIVES");
		
		// Create a Perspective switch menu only if 1+ perspectives available
		if ( perspectiveActions.size() > 1 ) {
			// Add perspective actions
			for (Action action : perspectiveActions) {
				perspectiveMenu.add(action);
			}
		}
		// Show View Menu
		MenuManager showViewMenuMgr = new MenuManager(
				Messages.getString("menu.showview.name")
				, "showView");

		// Add views
		for (Action action : openViewActions) {
			showViewMenuMgr.add(action);
		}
		
		showViewMenuMgr.add(showViewsItem);
		
		// Add show views menu
		perspectiveMenu.add(showViewMenuMgr);
		
		// Cache mgr dlg
		perspectiveMenu.add(new Separator());
		perspectiveMenu.add(cacheManagerAction);
		
		menuBar.add(perspectiveMenu);

		// Add a group marker indicating where action set menus will appear.
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		menuBar.add(helpMenu);

		// Help
		helpMenu.add(showHelpAction);	
		helpMenu.add(searchHelpAction);
		helpMenu.add(aboutAction);
		helpMenu.add(new Separator());
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
        helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		
	}

	@Override
	protected void fillCoolBar(ICoolBarManager coolBar) 
	{
        IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        coolBar.add(new ToolBarContributionItem(toolbar, "main"));   
        
        toolbar.add(weatherWizardAction);
        toolbar.add(wmsWizardAction);
        toolbar.add(openWebBrowser);
	}
	
	protected void fillStatusLine(IStatusLineManager statusLine) {
		ApplicationActionBarAdvisor.statusLine = new StatusLine(statusLine);
	}
	
	public static StatusLine getDefaultStatusLine() {
		return statusLine;
	}
}
