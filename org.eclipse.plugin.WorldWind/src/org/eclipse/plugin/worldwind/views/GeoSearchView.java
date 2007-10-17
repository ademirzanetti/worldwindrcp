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

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.utils.YGeoSearch;
import org.eclipse.plugin.worldwind.utils.YGeoSearch.YResult;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.part.ViewPart;

/**
 * GeoSearch using Yahoo GeoCoding Services:
 * For example :
 * 	http://local.yahooapis.com/MapsService/V1/geocode?appid=YahooDemo&location=durham
 * 
 * Returns
 * <pre>
&lt;ResultSet xsi:schemaLocation="urn:yahoo:maps http://api.local.yahoo.com/MapsService/V1/GeocodeResponse.xsd"&gt;
&lt;Result precision="city"&gt;
	&lt;Latitude&gt;35.949417&lt;/Latitude&gt;
	&lt;Longitude&gt;-93.980476&lt;/Longitude&gt;
	&lt;Address/&gt;
	&lt;City&gt;DURHAM&lt;/City&gt;
	&lt;State&gt;AR&lt;/State&gt;
	&lt;Zip/&gt;
	&lt;Country&gt;US&lt;/Country&gt;
&lt;/Result&gt;
&lt;Result precision="city"&gt;
	&lt;Latitude&gt;39.64608&lt;/Latitude&gt;
	&lt;Longitude&gt;-121.799248&lt;/Longitude&gt;
	&lt;Address/&gt;
	&lt;City&gt;DURHAM&lt;/City&gt;
	&lt;State&gt;CA&lt;/State&gt;
	&lt;Zip/&gt;
	&lt;Country&gt;US&lt;/Country&gt;
&lt;/Result&gt;
&lt;/ResultSet&gt; * </pre> 
 * @author Owner
 *
 */
public class GeoSearchView extends ViewPart implements Listener
{
	final static public String ID = GeoSearchView.class.getName();
	
	private TableViewer viewer;
	private YResult[] results;
	
	// Status line
	private StatusLine statusLine;
	
	@Override
	public void createPartControl(Composite parent) 
	{
		GridData fillBoth = new GridData(GridData.FILL_BOTH);

		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(1, false));

		Label lbl = new Label(container, SWT.NONE);
		lbl.setText(Messages.getText("Y.geo.url.lbl1"));
		
		Combo searchCombo = new Combo(container, SWT.NONE);
		searchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		searchCombo.addListener(SWT.Selection, this);
		searchCombo.addListener(SWT.DefaultSelection, this);
		
		viewer = new TableViewer(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getTable().setLayoutData(fillBoth);
		viewer.getTable().addListener(SWT.Selection, this);
		viewer.getTable().addListener(SWT.DefaultSelection, this);
		
		// initialize status line
		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine();
	}

	@Override
	public void setFocus() {
	}

	/**
	 * Handle events
	 */
	public void handleEvent(Event event) 
	{
		Widget w = event.widget;

		try {
			if ( w instanceof Combo ) {
				String location = ((Combo)w).getText();
				
				
				YGeoSearch yahoo 	= new YGeoSearch(location);
				results 			=  yahoo.getLocations();
				
				viewer.getTable().removeAll();
				
				for (YResult result : results) {
					viewer.add(result);
				}
				
				if ( event.type == SWT.DefaultSelection)
					((Combo)w).add(location);
			}
			else {
				int idx = ((Table)w).getSelectionIndex();

				// grab earth view
				EarthView view = (EarthView)Activator.getView(
						getViewSite().getWorkbenchWindow()
						, EarthView.ID);
				
				// lat/lon cannot be null
				double lat = Double.parseDouble(results[idx].latitude);
				double lon = Double.parseDouble(results[idx].longitude);
				
				LatLon latlon = new LatLon(Angle.fromDegrees(lat)
						, Angle.fromDegrees(lon));
				
				view.flyTo(latlon);
			}
			
		} catch (Exception e) {
			// show error in status line
			// e.printStackTrace();
			statusLine.setErrorMessage(e.getMessage());
		}
	}
}
