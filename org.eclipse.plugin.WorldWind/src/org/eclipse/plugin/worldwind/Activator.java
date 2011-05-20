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

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.FileStore;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.plugin.WorldWind";

	// The shared instance
	private static Activator plugin;

	private static final String ICON_NASA_PATH = "icons/16x16-nasa.png";
	private static final String ICON_USGS_PATH = "icons/16x16-usgs.png";
	private static final String ICON_FLAG_US_PATH = "icons/16x16-flag-us.png";
	private static final String ICON_BROWSER_PATH = "icons/16x16-browser.png";

	public static Image ICON_NASA = getImage(ICON_NASA_PATH);
	public static Image ICON_USGS = getImage(ICON_USGS_PATH);
	public static Image ICON_FLAG_US = getImage(ICON_FLAG_US_PATH);
	
	public static ImageDescriptor ICON_KML = getImageDescriptor("icons/16x16-kml.png");
	public static ImageDescriptor ICON_GEORSS = getImageDescriptor("icons/16x16-georss.png");
	public static ImageDescriptor ICON_WMS = getImageDescriptor("icons/16x16-wms.png");
	public static ImageDescriptor ICON_NOAA = getImageDescriptor("icons/16x16-noaa.png");
	public static ImageDescriptor ICON_WEB_BROWSER = getImageDescriptor(ICON_BROWSER_PATH);	
	
    static
    {
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Basic Demo");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }
	
	/**
	 * The constructor
	 */
	public Activator() {
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
	public static Activator getDefault() {
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
	
	public static Image getImage (String path){
		return getImageDescriptor(path).createImage();
	}

	public static Image getSharedImage (String name){
		return PlatformUI.getWorkbench().getSharedImages().getImage(name);
	}

	public static ImageDescriptor getSharedImageDescriptor (String image) {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(image);
	}
	
	public static IViewPart getView (IWorkbenchWindow window,  String ViewID) 
	{
		IViewReference[] refs = window.getActivePage().getViewReferences();
		for (IViewReference viewReference : refs) {
			if ( viewReference.getId().equals(ViewID) )
				return viewReference.getView(true);
		}
		return null;
	}
	
}
