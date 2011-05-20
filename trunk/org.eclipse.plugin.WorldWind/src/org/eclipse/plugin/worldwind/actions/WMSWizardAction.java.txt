package org.eclipse.plugin.worldwind.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.ui.IWorkbenchWindow;

import org.eclipse.plugin.worldwind.actions.ICommandIds;
import org.eclipse.plugin.worldwind.wizard.WMSWizard;

public class WMSWizardAction extends Action {
    private final IWorkbenchWindow window;
	private final String ICON_WMS = "icons/16x16-wms.png";

	public WMSWizardAction(String text, IWorkbenchWindow window) {
        super(text);
        this.window = window;
        
        // The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_WMS_WIZARD);
        
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_WMS_WIZARD);
        setImageDescriptor(Activator.getImageDescriptor(ICON_WMS));
	
	}
	
    public void run() {
		
		try {
			// Will load WMS public list
			WMSWizard wiz = new WMSWizard(window);
			
			
			WizardDialog dialog = new WizardDialog(window.getShell(), wiz);
			dialog.open();
			
		} catch (Exception e) {
			Messages.showErrorMessage(window.getShell()
					, Messages.getString("wiz.wms.err.text", new Object[] {e.getMessage()}
					));
		}
		
    }
	
}
