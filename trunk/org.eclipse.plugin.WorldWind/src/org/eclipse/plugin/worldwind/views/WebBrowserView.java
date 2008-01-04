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


import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.WorldWind;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.*;

import org.eclipse.plugin.worldwind.actions.OpenViewAction;
import org.eclipse.plugin.worldwind.actions.OpenViewAction.VIEW_TYPE;
import org.eclipse.plugin.worldwind.views.WebBrowserView;

import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.SimpleHTTPClient;

/**
 * Web Browser View: The rationale i=of this view is a built in web browser
 * to trap URL click events, download the HTML using WW and if the mime type
 * is supported, display the output on the Earth View 
 * 
 * @author vsilva
 */
public class WebBrowserView extends ViewPart 
{
	private static final Logger logger = Logger.getLogger(WebBrowserView.class);
	
	static public String ID = WebBrowserView.class.getName();

	private Combo urlCombo;
	private Browser browser;

	// View Actions
	private Action actionBack;
	private Action actionForward;
	private Action actionHome;
	private Action actionAddBookmark;
	private Action removeBookmarks;
	
	public static ImageDescriptor ICON_HOME = Activator.getImageDescriptor("icons/16x16-home.gif");
	
	// Status line
	private StatusLine statusLine;
	
	/**
	 * The constructor.
	 */
	public WebBrowserView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) 
	{
		// initialize status line
		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine();
		
		parent.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent arg0) {
				saveBookmarks();
			}
		});
		
		parent.setLayout(new GridLayout(1, true));
		
		CoolBar coolbar = new CoolBar(parent, SWT.NONE);
		coolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
	    // Create a coolitem with a URL combo
		CoolItem item = new CoolItem(coolbar, SWT.NONE);
	    item.setControl(createComboView(coolbar, new GridData(GridData.FILL_HORIZONTAL))); //gridData));
	    calcSize(item);
	    
		SashForm sashForm = new SashForm(parent, SWT.NONE);
		sashForm.setOrientation(SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		
	    browser = new Browser(sashForm, SWT.NONE);
	    browser.setLayoutData(new GridData(GridData.FILL_VERTICAL));
	    browser.setUrl(Messages.getText("wb.start.url")); 

	    browser.addLocationListener(new LocationListener(){

			public void changed(LocationEvent event) {
				locChanged(event);
			}

			public void changing(LocationEvent event) 
			{
				locChanging(event);
			}
	    	
	    });
	    
	    // Progress listener
	    browser.addProgressListener(new ProgressListener() {
				public void changed(ProgressEvent event) {
					onProgress(event);
				}
				public void completed(ProgressEvent event) {
				}
			});

	    
		
		makeActions();
		contributeToActionBars();
	}


	/**
	 * Fires when the location in the web browser is changing
	 * @param evt
	 */
	void locChanging (LocationEvent evt) 
	{
				
		String location = evt.location;
		
		statusLine.beginTask(location, 100);
		statusLine.lockProgress();
		
		if ( ! location.startsWith("http")) {
			logger.debug("WebBrowser::abort loc=" + location);
			return;
		}
	
		try {
			SimpleHTTPClient client = new SimpleHTTPClient(location);
			client.doGet();
			
			if ( client.isContentTypeKML() || client.isContentTypeKMZ()) {
				evt.doit = false;
				
				// handle kml/kmz
				handleKmlKmz( location);
			}
			else {
				// is this a DODS Request?
				// GDS HTTP Headers: XDODS-Server=[3.1], Content-Description=[dods_info]
				// THREDDS Headers: XDODS-Server=[opendap/3.7], Content-Description=[dods-error]
				boolean isDODS = client.getHeaders().get("XDODS-Server") != null;
				
				if ( isDODS ) {
					evt.doit = false;
					
					logger.debug("DODS URL detected: " + location);
					handleDODSLocation(location);
				}
			}
		} 
		catch (IOException e) {
			//e.printStackTrace();
			statusLine.setErrorMessage(e.getMessage());
		}
		finally {
			statusLine.unlockProgress();
		}
	}

	/**
	 * Handle a DODS request within the web browser. It will open the {@link VerdiModels3View}
	 * from the Analytics plugin and load the remote dataset. Here are some DODS catalogs:
	 * 	<br/>
	 * 	<li>LAS: http://ferret.pmel.noaa.gov/thredds/dodsC/las/catalog.html
	 * 	<li>Unidata: THREDSS http://motherlode.ucar.edu:8080/thredds/catalog.html
	 * @param location URL. It must contain the suffix .dods
	 */
	private void handleDODSLocation(String location) {
		try {
			//"org.eclipse.plugin.analytics.views.VerdiModels3View";
			//final String VERDI_VIEW_ID = "org.eclipse.plugin.analytics.views.VerdiModels3View.ID";

			// Clean up URL: remove any .info|.das|.dds|.html
			location = location.replaceAll("\\.info|\\.das|\\.dds|\\.html", "");
//			
//			// The suffix .dods is required in the URL for the dataset to be loaded
//			if ( 	location.indexOf(".jnl") == -1 
//					&& location.indexOf(".grib") == -1 
//					&& location.indexOf(".dods") == -1) 
//			{
//				logger.debug("Appending suffix .dods to " + location);
//				location += ".dods";
//			}

			// Show the Verdi view 
			//getViewSite().getWorkbenchWindow().getActivePage().showView(VERDI_VIEW_ID);
			
			// Get it
//			IViewPart view = Activator.getView(getViewSite().getWorkbenchWindow()
//					, VERDI_VIEW_ID);
			
			logger.debug("Opening remote DODS url " + location);

			OpenViewAction action = new OpenViewAction(""
					, getViewSite().getWorkbenchWindow()
					, NetCDFView.ID
					, true
					, VIEW_TYPE.NETCDF);
			
			action.setNetCDFUri(location);
			action.run();
			
			// load remote data set
//			if ( view == null ) {
//				MessageDialog.openError(getViewSite().getShell()
//						, Activator.PLUGIN_ID
//						, "Unable to load Verdi view"); //$NON-NLS-1$
//				return;
//			}
			
			// load remote dataset
			//((org.eclipse.plugin.analytics.views.VerdiModels3View)view).addDatasetFromUrl(location);
		} 
		catch (Exception e) {
			e.printStackTrace();
			MessageDialog.openError(getViewSite().getShell()
					, Activator.PLUGIN_ID
					, e.getMessage());

		}
	}
	
	/**
	 * Fires after the web browser location has changed
	 * @param event
	 */
	void locChanged (LocationEvent event) 
	{
		urlCombo.setText(event.location);
		
		statusLine.unlockProgress();
		statusLine.taskDone();
	}
	
	/**
	 * 
	 * @param event
	 */
	private void onProgress (ProgressEvent event) {
		if (event.total == 0) return;                            
		int ratio = event.current * 100 / event.total;

		statusLine.worked(ratio);
	}
	
	/**
	 * Deal with KML or KMZ files
	 * @param location
	 */
	private void handleKmlKmz ( String location ) 
	{
		try {
			// Grab layers view
//			PlacesView view = (PlacesView)Activator.getView(getViewSite().getWorkbenchWindow()
//					, PlacesView.ID);
			NavigatorView view = (NavigatorView)Activator.getView(getViewSite().getWorkbenchWindow()
					, NavigatorView.ID);
			
			handleKmlKmz(new KMLSource(new URL(location)), view);
		} catch (Exception e) {
			statusLine.setErrorMessage(e.getMessage());
		}
	}
	
	/**
	 * Add s KML or KMZ file source to the {@link PlacesView}
	 * @param kml {@link KMLSource} object
	 * @param view {@link PlacesView} where the source will be added
	 */
	public static void handleKmlKmz ( KMLSource kml , NavigatorView view )
	{
		try {
			LayerList list = kml.toLayerList();

			if ( list.size() == 0 )
				throw new Exception("No Ground, Screen or Placemark overlays in document.");

			view.addKMLSource(kml, false);
			
			// show the earth view
			view.getViewSite().getWorkbenchWindow().getActivePage().showView(EarthView.ID);
		} 
		catch (Exception e) 
		{
			view.setStatusErrorMessage(e.getMessage());
		}
	}
	
	/**
	 * Creates the urlCombo box view.
	 * 
	 * @param parent the parent control
	 */
	private Control createComboView(Composite parent, Object layoutData) 
	{
		urlCombo = new Combo(parent, SWT.NONE);
		urlCombo.setLayoutData(layoutData);
		urlCombo.addSelectionListener(new SelectionListener(){

			public void widgetDefaultSelected(SelectionEvent e) {

				final String url = ((Combo)e.getSource()).getText();
				browser.setUrl(url);
				urlCombo.add(url);
			}

			public void widgetSelected(SelectionEvent e) {
				browser.setUrl(((Combo)e.getSource()).getText());
			}
			
		});
		
		return urlCombo;
	}
	
	 /**
	   * Helper method to calculate the size of the cool item
	   * 
	   * @param item the cool item
	   */
	private void calcSize(CoolItem item) {
	    Control control = item.getControl();
	    org.eclipse.swt.graphics.Point pt = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	    pt = item.computeSize(pt.x, pt.y);
	    item.setSize(pt);
	}
	
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}
	
	private void makeActions() {
		actionBack = new Action() {
			public void run() {
				browser.back();
			}
		};
		actionBack.setText(Messages.getText("wb.action.back.text"));
		actionBack.setToolTipText(Messages.getText("wb.action.back.tooltip"));
		actionBack.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_TOOL_BACK));
		
		actionForward = new Action() {
			public void run() {
				browser.forward();
			}
		};
		actionForward.setText(Messages.getText("wb.action.forward.text"));
		actionForward.setToolTipText(Messages.getText("wb.action.forward.tooltip"));
		actionForward.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
				getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		

		actionHome = new Action() {
			public void run() {
				browser.setUrl(Messages.getText("wb.start.url"));
			}
		};
		actionHome.setText(Messages.getText("wb.action.home.text"));
		actionHome.setToolTipText(Messages.getText("wb.action.home.tooltip"));
		actionHome.setImageDescriptor(ICON_HOME);

		actionAddBookmark = new Action() {
			public void run() {
				addBookmark(urlCombo.getText());
			}
		};
		actionAddBookmark.setText(Messages.getText("wb.action.bookmark.text"));
		actionAddBookmark.setToolTipText(Messages.getText("wb.action.bookmark.tooltip"));
		actionAddBookmark.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJ_FILE));
		
		
		removeBookmarks = new Action() {
			public void run() {
				getViewSite().getActionBars().getMenuManager().removeAll();
			}
		};
		removeBookmarks.setText(Messages.getText("wb.action.remove.bm.text"));
		removeBookmarks.setToolTipText(Messages.getText("wb.action.remove.bm.tooltip"));
		removeBookmarks.setImageDescriptor( PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		// Fill local toobar
		manager.add(actionHome);
		manager.add(actionBack);
		manager.add(actionForward);
	}
	
	private void fillLocalPullDown(IMenuManager manager) 
	{
		// 2 local actions: add/remove bookmarks
		manager.add(actionAddBookmark);
		manager.add(new Separator());
		manager.add(removeBookmarks);
		manager.add(new Separator());
		
		String[] bookmarks = loadBookmarks();
		if ( bookmarks != null) {
			for (String bm : bookmarks) {
				addBookmark(bm);
			}
		}
	}
	
	/**
	 * Add book-mark
	 * @param url
	 */
	private void addBookmark(final String url) 
	{
		if ( url == null || url.length() == 0 ) return;
		
		Action bm = new Action() {
			public void run() {
				browser.setUrl(url);
			}
		};
		bm.setText(url);
		getViewSite().getActionBars().getMenuManager().add(bm);
	}
	
	/**
	 * Save bookmarks on WW cache
	 */
	private void saveBookmarks () {
		try {
			File file = WorldWind.getDataFileCache().newFile("bookmarks.txt");
			FileOutputStream fos = new FileOutputStream(file);
			
			IContributionItem[] items = getViewSite().getActionBars().getMenuManager().getItems();
			
			for (IContributionItem item : items) 
			{
				if ( item instanceof ActionContributionItem ) {
					final String text = 
						((ActionContributionItem)item).getAction().getText() + Messages.NL;
					
					if ( text.startsWith("http")) {
						fos.write(text.getBytes());
					}
				}
			}
			fos.close();
		} 
		catch (Exception e) {
		}
	}
	
	/**
	 * load bookmarks
	 * @return
	 */
	private String[] loadBookmarks() {
		try {
			File file = WorldWind.getDataFileCache().newFile("bookmarks.txt");
			RandomAccessFile raf = new RandomAccessFile(file,"r");
			
			byte[] bytes = new byte[(int)raf.length()];
			raf.readFully(bytes);
			return new String(bytes).split(Messages.NL);
		} 
		catch (Exception e) {
			return null;
		}
	}
	
	public void setUrl (String url) {
		browser.setUrl(url);
	}
}