package org.eclipse.plugin.worldwind.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import org.eclipse.plugin.worldwind.actions.ICommandIds;
import org.eclipse.plugin.worldwind.views.WebBrowserView;

/**
 * Open Web Browser Action
 * @author vsilva
 *
 */
public class OpenWebBrowserAction extends Action 
{
    private final IWorkbenchWindow window;
	private final String ICON_WEBBROWSER = "icons/16x16-browser.png";
	
	public OpenWebBrowserAction(String text, IWorkbenchWindow window) {
        super(text);
        this.window = window;
        
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_OPEN_WEB_BROWSER);
        
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_OPEN_WEB_BROWSER);
        setImageDescriptor(Activator.getImageDescriptor(ICON_WEBBROWSER));
	}
	
    public void run() {
        if ( window != null) {
        	try {
        		window.getActivePage().showView(WebBrowserView.ID);
			} 
        	catch (PartInitException e) {
				Messages.showErrorMessage(window.getShell()
						, Messages.getText("err.dialog.title")
						, Messages.getText(""
								, new Object[] { Messages.getText("view.web.browser.name")
										, e.getMessage()
									}
								)
						);
			}
        }
    }
	
}
