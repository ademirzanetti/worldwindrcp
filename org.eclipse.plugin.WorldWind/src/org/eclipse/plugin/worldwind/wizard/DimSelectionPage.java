package org.eclipse.plugin.worldwind.wizard;


import gov.nasa.worldwind.geom.Sector;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import worldwind.contrib.parsers.WMS_Capabilities;


public class DimSelectionPage extends WizardPage implements Listener 
{
	// Date combos
	Combo startDate; 
	Combo endDate;
	Combo formats;
	
	// Lat/lon boxes
	Text north, south, east, west;

	// Labels 
	Label l1, l2, l3,l4, l5, l6, l7;
	
	boolean showDates, showLatLon, showFormats;
	
	protected DimSelectionPage(String pageName) {
		super(pageName);
		setTitle(Messages.getString("wiz.weather.page.title"));
		setPageComplete(false);
		setDescription(Messages.getString("wiz.weather.page1.desc"));
	}

	public void createControl(Composite parent) 
	{
		Composite container = new Composite(parent, SWT.NULL);		
		container.setLayout(new GridLayout(2, false));
		
		/*
		 * Start/End date combos
		 */
		l1 = new Label(container, SWT.NONE);
		l1.setText(Messages.getString("wiz.weather.page1.lbl1"));
		
		final GridData gridData = new GridData(GridData.BEGINNING, GridData.CENTER, true, true);
		gridData.minimumWidth = 300;

		// start date combo
		startDate = new Combo(container, SWT.NONE); // SWT.READ_ONLY);
		startDate.setLayoutData(gridData); // new GridData(GridData.FILL_HORIZONTAL));
		startDate.addListener(SWT.Selection, this);
		
		l2 = new Label(container, SWT.NONE);
		l2.setText(Messages.getString("wiz.weather.page1.lbl2"));

		// end date combo
		endDate = new Combo(container, SWT.NONE); // SWT.READ_ONLY); 
		endDate.setLayoutData(gridData); // new GridData(GridData.FILL_HORIZONTAL));
		endDate.addListener(SWT.Selection, this);

		/*
		 * Formats combo
		 */
		l7 = new Label (container, SWT.NONE);
		l7.setText(Messages.getString("wiz.dim.page.formats"));
		
		formats = new Combo(container, SWT.READ_ONLY);
		formats.setLayoutData(gridData);
		formats.addListener(SWT.Selection, this);
		
		/*
		 * BBOX: East, West, North, South
		 */
		l3 = new Label(container, SWT.NONE);
		l3.setText(Messages.getString("wiz.dim.page.east"));
		
		east = new Text(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
		east.addListener(SWT.Selection, this);
		
		l4 = new Label(container, SWT.NONE);
		l4.setText(Messages.getString("wiz.dim.page.west"));
		
		west = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		west.addListener(SWT.Selection, this);
		
		l5 = new Label(container, SWT.NONE);
		l5.setText(Messages.getString("wiz.dim.page.north"));
		
		north = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		north.addListener(SWT.Selection, this);
		
		l6 = new Label(container, SWT.NONE);
		l6.setText(Messages.getString("wiz.dim.page.south"));
		
		south = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
		south.addListener(SWT.Selection, this);
		
		
		setControl(container);
	}

	/**
	 * Load data into the weather wizard dimensions page. GUI elements may
	 * or may not be displayed.
	 * @param showDates Display date combos
	 * @param showLatLon Display lat/lon text boxes
	 * @param dates Array of string dates
	 * @param bbox Lat/Lon box. Instance of {@link WMS_Capabilities.BBox}
	 * @param formats Frame formats (ussually image/png, etc.)
	 * or {@link Sector}
	 */
	void loadData(boolean showDates, boolean showLatLon
			, String[] dates 
			, Object bbox
			, String[] formats)  
	{
		// Used to control pageComplete
		this.showDates = showDates;
		this.showLatLon = showLatLon;
		this.showFormats = formats != null;
		
		updateUI(showDates, showLatLon, showFormats);
		
		if ( showDates ) 
		{
			// clear combos
			startDate.removeAll();
			endDate.removeAll();
	
			// Load date combos if less than 20000 steps
			// otherwise response is too slow
			if ( dates.length < 20000) {
				for (int i = 0; i < dates.length; i++) {
					startDate.add(dates[i]);
					endDate.add(dates[i]);
				}
			}
			else {
				// Let user enter dates  (add the 1st only)
				startDate.add(dates[0]);
				endDate.add(dates[0]);
			}
		}
		
		if ( showLatLon) 
		{
			// Insert lat/lon vals
			if ( bbox != null ) {
				if ( bbox instanceof WMS_Capabilities.BBox ) {
					WMS_Capabilities.BBox box = (WMS_Capabilities.BBox)bbox;
					north.setText(box.north);
					south.setText(box.south);
					east.setText(box.east);
					west.setText(box.west);
				}
			}
		}
		
		// formats are optional 
		if ( formats != null ) {
			this.formats.removeAll();
			for (String format : formats) {
				this.formats.add(format);
			}
		}
			
		
		dialogChanged();
	}

	/*
	 * Update GUI visibility
	 */
	private void updateUI (boolean showDates, boolean showLatLon
			, boolean showFormats) 
	{
		// date combos
		startDate.setVisible(showDates);
		endDate.setVisible(showDates);
		l1.setVisible(showDates);
		l2.setVisible(showDates);
		
		// bbox
		north.setVisible(showLatLon); south.setVisible(showLatLon);
		east.setVisible(showLatLon); west.setVisible(showLatLon);
		l3.setVisible(showLatLon); l4.setVisible(showLatLon);
		l5.setVisible(showLatLon); l6.setVisible(showLatLon);
		
		// formats
		l7.setVisible(showFormats); formats.setVisible(showFormats);
	}
	
	int getStartIndex (){
		return startDate.getSelectionIndex();
	}

	int getEndIndex (){
		return endDate.getSelectionIndex();
	}

	int getFormatIndex (){
		return formats.getSelectionIndex();
	}
	
	/* handle events */
	public void handleEvent(Event evt) {
		dialogChanged();
	}
	
	/**
	 * Ensures that both gui fields are set.
	 */
	private void dialogChanged() 
	{
		boolean complete = false;
		int i = getStartIndex();
		int j = getEndIndex();
		int k = getFormatIndex();
		
		if ( showDates )
			complete = ( i != -1) && (j != -1) && (i < j);
		
		if ( showLatLon)
			complete = true;
		
		if ( showFormats)
			complete = complete && ( k != -1 );
		
		setPageComplete(complete);
		getWizard().getContainer().updateButtons();
	}
	
	boolean isTimeSpanVisible() {
		return startDate.isVisible();
	}
	
	/**
	 * Get selected time span valies
	 * @return Array of string, each value represents a date/time value
	 */
	String[] getSelectedTimes () {
		int i = getStartIndex();
		int j = getEndIndex();

		if ( i < 0 || j < 0 ) return null;
		
		String[] times = new String[j - i];
		
		for (int k = 0; k < times.length; k++) {
			times[k] = startDate.getItem(i + k);
		}
		return times;
	}
	
	String getFormat() {
		return formats.getText();
	}
}
