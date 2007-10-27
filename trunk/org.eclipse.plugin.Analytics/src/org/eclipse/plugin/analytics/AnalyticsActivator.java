package org.eclipse.plugin.analytics;

import javax.swing.UIManager;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class AnalyticsActivator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.plugin.Analytics"; //$NON-NLS-1$
	public static final String PLUGIN_NAME = Messages.getString("AnalyticsActivator.1"); //$NON-NLS-1$
	
	/** ID of the WB view from the WW Plugin */
	public static final String WB_VIEW_ID = "org.eclipse.plugin.worldwind.views.WebBrowserView"; //$NON-NLS-1$

	// The shared instance
	private static AnalyticsActivator plugin;
	
	
	/**
	 * The constructor
	 */
	public AnalyticsActivator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static AnalyticsActivator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
	
	/* init java Swing look and feel */
	static public void initSwingLookAndFeel () {
		try {
			String lf = UIManager.getSystemLookAndFeelClassName();
			if (lf.toLowerCase().contains("gtk")) //$NON-NLS-1$
				lf = UIManager.getCrossPlatformLookAndFeelClassName();
			UIManager.setLookAndFeel(lf);
		} catch (Exception ex) {
		}
	}

	/** utility subs */
	public static Image getSharedImage (String name){
		return PlatformUI.getWorkbench().getSharedImages().getImage(name);
	}

	public static ImageDescriptor getSharedImageDescriptor (String name) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(name);
	}
	
	public static org.eclipse.ui.IViewPart getView (IWorkbenchWindow window,  String ViewID) 
	{
		IViewReference[] refs = window.getActivePage().getViewReferences();
		for (IViewReference viewReference : refs) {
			if ( viewReference.getId().equals(ViewID) )
				return viewReference.getView(true);
		}
		return null;
	}
	
	
}
