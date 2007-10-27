package org.eclipse.plugin.analytics;


import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.plugin.analytics.actions.OpenViewAction;
import org.eclipse.plugin.analytics.views.VerdiModels3View;
import org.eclipse.plugin.analytics.views.VisADSpreadSheetView;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * An action bar advisor is responsible for creating, adding, and disposing of
 * the actions added to a workbench window. Each window will be populated with
 * new actions.
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
	
	static public String A_MENU_ID = "ANALYTICS"; //$NON-NLS-1$

	// Actions - important to allocate these only in makeActions, and then use
	// them
	// in the fill methods. This ensures that the actions aren't recreated
	// when fillActionBars is called with FILL_PROXY.
	private IWorkbenchAction exitAction;

	private OpenViewAction openVisADViewAction;
	private OpenViewAction openModels3ViewAction;

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

		openVisADViewAction = new OpenViewAction(Messages.getString("ApplicationActionBarAdvisor.1") //$NON-NLS-1$
				, window, VisADSpreadSheetView.ID );

		openModels3ViewAction = new OpenViewAction(Messages.getString("ApplicationActionBarAdvisor.2") //$NON-NLS-1$
				, window, VerdiModels3View.ID );

		
		// default exit action
		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);
	}

	
	
	protected void fillMenuBar(IMenuManager menuBar) {
		MenuManager fileMenu = new MenuManager(Messages.getString("ApplicationActionBarAdvisor.3"), //$NON-NLS-1$
				IWorkbenchActionConstants.M_FILE);
		menuBar.add(fileMenu);
		fileMenu.add(exitAction);
		
		MenuManager analysisMenu = new MenuManager(Messages.getString("ApplicationActionBarAdvisor.4"), //$NON-NLS-1$
				A_MENU_ID);

		analysisMenu.add( openVisADViewAction);
		analysisMenu.add( openModels3ViewAction);
		
		menuBar.add(analysisMenu);

	}

}
