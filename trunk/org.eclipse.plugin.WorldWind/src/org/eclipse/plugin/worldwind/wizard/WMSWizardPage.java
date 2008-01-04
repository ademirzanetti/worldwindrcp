package org.eclipse.plugin.worldwind.wizard;

import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.operation.WMSParseOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.WMS_Capabilities;
import worldwind.contrib.parsers.WMS_Capabilities.Layer;

/**
 * WMS Wizard Page
 * @author vsilva
 *
 */
public class WMSWizardPage  extends WizardPage
{
	private static final Logger logger = Logger.getLogger(WMSWizardPage.class);
			
	// Widgets: Source serverCombo, Layers Table
	private TableViewer layerViewer;
	private Combo serverCombo;
	
	private Vector<ParserUtils.PublicWMSServer> servers;
	
	// WMS servers caps
	private WMS_Capabilities capabilities;
	
	// Indices of the selected layers from caps above
	private int[] selectedIndices;

	// Used to cache capabilities for increased performance
	private static Hashtable<String, WMS_Capabilities> capabilitiesCache
		= new Hashtable<String, WMS_Capabilities>();
		
		
	/**
	 * WMS Wizard server selection page
	 * @param pageName
	 */
	public WMSWizardPage(String pageName) {
		super(pageName);
		
		setTitle(Messages.getText("wiz.wms.page1.title"));
		setDescription(Messages.getText("wiz.wms.page1.desc"));
		setPageComplete(false);
	}
	
	/**
	 * Controls 
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(1, true));
		
		GridData data = new GridData(GridData.FILL_BOTH);
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText(Messages.getText("wiz.wms.page1.lbl1")); 
		lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		serverCombo = new Combo(container, SWT.NONE);
		serverCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		//serverCombo.addListener(SWT.Selection | SWT.DefaultSelection, this);

		serverCombo.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e) 
			{
				String text = ((Combo)e.getSource()).getText();
				
				if ( ! text.startsWith("http")) {
					setErrorMessage("Invalid text: " + text);
					return;
				}
					
				handleComboSelection((Combo)e.getSource(), true);
			}
			
			public void widgetSelected(SelectionEvent e) {
				handleComboSelection((Combo)e.getSource(), false);
			}
		});
	
		// init serverCombo data
		loadServers();
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText(Messages.getText("wiz.wms.page1.lbl2")); 
		lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));  
		
		layerViewer = new TableViewer(container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		layerViewer.getTable().setLayoutData(data);
		//layerViewer.getTable().addListener(SWT.Selection, this);
		
		layerViewer.addSelectionChangedListener(
			new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event) 
				{
					int[] indices = ((TableViewer)event.getSource()).getTable().getSelectionIndices();
					handleListSelection(indices);
				}
			}
		);
		
		setControl(container);
		setPageComplete(false);
	}

	/*
	 * Load WMS servers into serverCombo box
	 */
	private void loadServers()
	{
		for (ParserUtils.PublicWMSServer server : servers) {
			serverCombo.add(server.name);
		}
	}
	
	/* fires when the list is clicked */
	private void handleListSelection ( int[] indices )  
	{
		try {
			
			// Save indices to extract Layers from Caps obj later on
			selectedIndices = indices;
			
			for (int idx : indices) {
				logger.debug(capabilities.getLayers().get(idx));
			}
			
			loadNextPage();
			setPageComplete(true);
			
			getWizard().getContainer().updateButtons();
			
		} catch (Exception e) {
			setPageComplete(false);
		}
	}
	
	/* fires when a serverCombo item is selected */
	private void handleComboSelection ( Combo combo, boolean userInput ) 
	{
		String server 	= combo.getText();
		int idx 		= combo.getSelectionIndex();
		URL url 		= null;

		if ( idx == -1 && ! userInput ) return;
		
		// Clear any prev error/messages
		setErrorMessage(null);
		
		try {
			if ( userInput) { 
				// Load WMS layers from user input
				url = new URL(server);
					
			}
			else {
				// Load from public list
				url = servers.get(idx).capabilitiesURL;
			}

			logger.debug("Server=" + server + " idx=" + idx + " url:" + url);
			
			if ( capabilitiesCache.containsKey(url.toString())) 
			{
				// load from cache
				capabilities = capabilitiesCache.get(url.toString());
				
				// Populate list: remove any previous elements
				layerViewer.getTable().removeAll();
				
				// Ad layers to the list box
				for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
					layerViewer.add(layer.Title);
				}
				
			}
			else 
			{
				WMSParseOperation operation = new WMSParseOperation(server, url
					, layerViewer, getWizard().getContainer().getShell().getDisplay());
				
				// run a Jface operation (it will populate the layers list)
				getWizard().getContainer().run(true, true, operation);
			
				capabilities = operation.getCapabilities();

				// cache for repeated use
				capabilitiesCache.put(url.toString(), capabilities);
				
			}
			
			final WMS_Capabilities.Service service = capabilities.getService();
			
			String name = (service.Title != null )
				? service.Name + ": " + service.Title
				: service.Name;
			
			name += " has " 
				+ capabilities.getLayers().size() + "/"
				+ capabilities.getTotalLayers() 
				+ " renderable layers. ";
				
			
			setMessage(name + "Select a layer to continue.", IMessageProvider.INFORMATION);
		} 
		catch (Exception e) {
			//e.printStackTrace();
			setErrorMessage(e.getClass().getName() + ": " +  e.getMessage());
		}
	}

	public void setServers(Vector<ParserUtils.PublicWMSServer> servers) {
		this.servers = servers;
	}

	public Vector<ParserUtils.PublicWMSServer> getServers() {
		return servers;
	}
	
	public WMS_Capabilities getCapabilities () {
		return capabilities;
	}
	
	public int[] getSelectedIndices () {
		return selectedIndices;
	}
	
	private boolean isLayerSelected () {
		int[] indices = layerViewer.getTable().getSelectionIndices();
		return  indices != null && indices.length > 0;
	}
	
	@Override
	public boolean canFlipToNextPage() {
		if ( isLayerSelected()) return true;
		else return false;
	}

	/**
	 * Load page 2 data
	 */
	private void loadNextPage()
	{
		DimSelectionPage page2 = (DimSelectionPage)getWizard().getPage("page2");
		
		try {
			if ( capabilities != null ) 
			{
				// extract date range/bbox of the 1st layer only
				int idx = selectedIndices[0];
				
				Layer layer = capabilities.getLayers().get(idx);
				
				boolean showDates = false;
				String[] dates = null;
				
				if ( layer.ISOTimeSpan != null ) {
					showDates = true;
					
					// Comma sep string of ISO dates for this layer time span
					logger.debug("Building ISO time list for " + layer.ISOTimeSpan);
						
					final String csvDates = WMS_Capabilities.buildWMSTimeList(layer.ISOTimeSpan);
					
					// split csv string to generate dates
					dates =  csvDates.split(",");
				}
				
				// Layer formats
				String[] formats = new String[capabilities.getMapRequest().formats.size()];
				
				capabilities.getMapRequest().formats.toArray(formats);
				
				logger.debug("Using WMS layer " 
						+ layer.Name + " " + ((dates != null) ? dates.length: 0) + " time steps "
						+ " # combo items=" + page2.startDate.getItemCount()
						+ " formats size=" + formats.length);
				
				page2.loadData(showDates
						, layer.bbox.isValid() 	// show latlon
						, dates					// time steps
						, layer.bbox			// bbox
						, formats);				// layer formats
			}
		} 
		catch (Exception e) {
			//e.printStackTrace();
			setErrorMessage(e.getMessage());
		}
	}
}
