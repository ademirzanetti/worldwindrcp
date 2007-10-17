package org.eclipse.plugin.worldwind.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.wizard.WeatherWizard;
import org.eclipse.ui.IWorkbenchWindow;

public class WeatherWizardAction extends Action 
{
	IWorkbenchWindow window;
	
	public WeatherWizardAction(String text, IWorkbenchWindow window) 
	{
		super(text);
		this.window = window;

		// The id is used to refer to the action in a menu or toolbar
        setId(ICommandIds.CMD_WEATHER_WIZARD);
        
        // Associate the action with a pre-defined command, to allow key bindings.
        setActionDefinitionId(ICommandIds.CMD_WEATHER_WIZARD);
        setImageDescriptor(Activator.ICON_NOAA);
	}

	@Override
	public void run() 
	{
		// The WW GL canvas is required
//		WorldWindowGLCanvas world = EarthView.world;

		// this cannot happen!
//		if ( world == null ) return;
		
		WeatherWizard wiz = new WeatherWizard(window); //, world);
		WizardDialog dialog = new WizardDialog(window.getShell(), wiz);
		dialog.open();
	}
}
