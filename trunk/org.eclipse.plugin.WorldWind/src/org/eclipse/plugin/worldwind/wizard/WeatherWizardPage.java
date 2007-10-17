package org.eclipse.plugin.worldwind.wizard;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import worldwind.contrib.layers.loop.HTTPLoopFileLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
//import worldwind.contrib.layers.loop.HTTPGOESLoopFileLayer;
//import worldwind.contrib.layers.loop.HTTPNavyWeatherLayer;
import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.ParserUtils.HTTPDataSet;

/**
 * Weather Wizard Page
 * @author vsilva
 *
 */
public class WeatherWizardPage extends WizardPage 
{
	private static final Logger logger = Logger.getLogger(WeatherWizardPage.class);
	
	// GOES Sat layers (from config/goes.noaa.gov.xml)
	static Vector<ParserUtils.HTTPDataSet> goes= null; 
	
	// Navy Res Labs
	static Vector<ParserUtils.HTTPDataSet> nrl= null; 
	
	// Widgets: Source combo, Layers Table
	private TableViewer viewer;
	private Combo combo;
	
	// Layers selected
	private ParserUtils.HTTPDataSet[] dataSets;

	// WW Layer selected from list box 
	TimeLoopGroundOverlay layer;
	
	/**
	 * 
	 * @param pageName
	 */
	protected WeatherWizardPage(String pageName) 
	{
		super(pageName); 
		setTitle(Messages.getText("wiz.weather.page.title"));
		setPageComplete(false);
		setDescription(Messages.getText("wiz.weather.page.desc"));
		
		// Parse Source XML Files (stored in config/)
		try {
			if ( goes == null )
				goes = ParserUtils.parseGOESSatFile(
						Messages.getInputStream(WeatherWizardPage.class
								, Messages.getText("layers.goes.xml.file")));
			
			if ( nrl == null)
				nrl = ParserUtils.parseNavyLabsFile(
						Messages.getInputStream(WeatherWizardPage.class
								, Messages.getText("layers.nrl.xml.file")));
		} 
		catch (Exception e) {
			setErrorMessage("Error parsing satellite data from config: " 
					+ e.getMessage());
		}
	}

	/**
	 * Create widgets
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(1, true));
		
		GridData data = new GridData(GridData.FILL_BOTH);
		
		Label lbl = new Label(container, SWT.NONE);
		lbl.setText(Messages.getText("wiz.weather.page.lbl1")); 
		lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		combo = new Combo(container, SWT.READ_ONLY);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.add(Messages.getText("layer.nrl.name"));
		combo.add(Messages.getText("layer.goes.name"));
		combo.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e){
			}
			public void widgetSelected(SelectionEvent e) {
				handleComboSelection((Combo)e.getSource());
			}
		});
		
		lbl = new Label(container, SWT.NONE);
		lbl.setText(Messages.getText("wiz.weather.page.lbl2")); 
		lbl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));  
		
		viewer = new TableViewer(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTable().setLayoutData(data);
		viewer.addSelectionChangedListener(
			new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event){
					handleListSelection((StructuredSelection)event.getSelection());
				}
			}
		);
		
		setControl(container);
	}

	/*
	 * On list selected add data sets to dataSets instance variable.
	 * dataSets will be used to retrieve information on finish
	 */
	private void handleListSelection ( StructuredSelection selection) 
	{
		try {
			// clear any error messages
			setErrorMessage(null); 
			
			int selectedIdx = combo.getSelectionIndex();
			dataSets = new ParserUtils.HTTPDataSet[selection.size()];
			
			logger.debug("selectionChanged " + selection + " combo idx=" + selectedIdx);
			
			int i = 0, j = 0;
			
			if ( selectedIdx == 0) 
			{
				// navy: add selected datasets to instance var dataSets
				for (Object element : selection.toArray()) {
					j = 0;
					for ( ParserUtils.HTTPDataSet ds : nrl) 
					{
						if ( ds.name.equals((String)element) ) 
						{
							logger.debug("Adding NRL dataset " + nrl.get(j).name);
							
							dataSets[i] = nrl.get(j);
							break;
						}
						j++;
					}
					i++;
				}
			}
			else {
				// GOES: add goes data to instance
				for (Object element : selection.toArray()) {
					j = 0;
					for ( ParserUtils.HTTPDataSet ds : goes ) 
					{
						if ( ds.name.equals((String)element) ) 
						{
							logger.debug("Adding GOES dataset " + goes.get(j).name);
							
							dataSets[i] = goes.get(j);
							break;
						}
						j++;
					}
					i++;
				}
			}
			
			loadNextPage();
			setPageComplete(true);
			
		} 
		catch (Exception e) {
			e.printStackTrace();
			setErrorMessage(e.getClass().getName() 
					+ ": " + e.getMessage());
			
			setPageComplete(false);
		}
	}
	
	/*
	 * When the combo changes add dataset names to the list
	 */
	private void handleComboSelection ( Combo combo ) 
	{
		if ( viewer == null ) return;
		
		int idx = combo.getSelectionIndex();
		viewer.getTable().removeAll();
		
		// Navy
		if (idx == 0) {
			for ( ParserUtils.HTTPDataSet ds : nrl) {
				viewer.add(ds.name);
			}
			setMessage(Messages.getText("wiz.weather.title.msg1",  new Object[] {nrl.size()}));
		}
		// GOES Sat
		else if (idx  == 1) {
			for ( ParserUtils.HTTPDataSet ds : goes) {
				viewer.add(ds.name);
			}
			setMessage(Messages.getText("wiz.weather.title.msg2",  new Object[] {goes.size()}));
		}
	}


	/**
	 * Get selected datasets
	 * @return
	 */
	ParserUtils.HTTPDataSet[] getDataSets() {
		return dataSets;
	}

	/**
	 * Index of the data source
	 * @return
	 */
	int getSourceIndex() {
		return combo.getSelectionIndex();
	}


	//public IWizardPage getNextPage()
	private void loadNextPage() throws IOException, MalformedURLException
	{
		DimSelectionPage page2 = (DimSelectionPage )getWizard().getPage("page2");//super.getNextPage();
		

//		int srcIndex 				= getSourceIndex();
		HTTPDataSet dataSet 		= getDataSets()[0];
		String[] dates				= null;

		layer = new HTTPLoopFileLayer(dataSet);
		dates = ((HTTPLoopFileLayer)layer).getFrameNames();
		
/*		
		// Get time steps from the respective source
		if ( srcIndex == 0 ) {
			// NRL (Navy Labs)
			// Fetch layer time steps
			layer = new HTTPNavyWeatherLayer(dataSet);
			dates = ((HTTPNavyWeatherLayer)layer).getFrameNames();
				
		}
		else {
			// GOES
			// fetch time steps
			layer = new HTTPGOESLoopFileLayer(dataSet);
			dates = ((HTTPGOESLoopFileLayer)layer).getFrameNames();
				
		}
*/		
		// load page 2 data: show times, no lat/lon
		// All images are JPGs
		page2.loadData(true, false,  dates, null , new String[] {"image/jpg"}); 
	}
	
	TimeLoopGroundOverlay getlayer () {
		return layer;
	}
	
}
