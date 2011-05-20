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
package org.eclipse.plugin.worldwind.contrib.layers;

import java.util.Iterator;

import org.eclipse.plugin.worldwind.contrib.Messages;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * A list of NASA WW WMS 1.1.1 layers provided by:<br/>
 * http://worldwind21.arc.nasa.gov/geoserver/wms?service=WMS&request=GetCapabilities
 * <P>These layers are static (no time dimension support). 
 * All layers are enabled by default</P>
 * @author Owner
 *
 */
public class NASAWMSLayerList extends LayerList 
{
	private static final long serialVersionUID = 7188188062715741295L;
	
	// Layer Cache Names (Not display names)
	public static final String POLITICAL_BOUNDARIES = "Earth/PoliticalBoundaries"; 
	public static final String US_COUNTY_BOUNDARIES = "Earth/USCountyBoundaries"; 
	public static final String US_CITIES_OVER_100K = "Earth/US Cities over 100K"; 
	public static final String US_CITIES_OVER_10K = "Earth/US Cities over 10K"; 
	public static final String US_CITIES_OVER_500K = "Earth/US Cities over 500K"; 
	public static final String US_CITIES_OVER_50K = "Earth/US Cities over 50K"; 
	public static final String MOUNTAINS_VALLEYS = "Earth/Mountains Valleys"; 
	public static final String DESSET_PLAINS = "Earth/Dessert Plains"; 
	public static final String TRENCES_RIDGES = "Earth/Trenches Ridges"; 
	public static final String LAKES_RIVERS = "Earth/Lakes Rivers"; 
	public static final String US_STATE_BOUNDARIES = "Earth/US State Boundaries"; 
	
	/**
	 * Constructor
	 */
	public NASAWMSLayerList() 
	{
		String wmsUrl = Messages.getText("worldwind.wms.url");
		
		/*
		 * Political Boundaries: Already available as a WW default layer
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:cia"			// layer name
				, Messages.getText("layers.Earth.pb.Name")
				, "EPSG:4326"			// SRS
				, "countryboundaries"	// styles
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, POLITICAL_BOUNDARIES // WW cache name
				)));
		
		/*
		 * US county boundaries
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:us_counties"
				, Messages.getText("layers.Earth.uscountyb.Name")
				, "EPSG:4326"			// SRS
				, "borders_black"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, US_COUNTY_BOUNDARIES // WW cache name
				)));


		/*
		 * US Cities over 100k
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_uscitiesover100k"
				, Messages.getText("layers.Earth.uscitiesover100k.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, US_CITIES_OVER_100K // WW cache name
				)));

		/*
		 * US Cities over 10k
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_uscitiesover10k"
				, Messages.getText("layers.Earth.uscitiesover10k.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, US_CITIES_OVER_10K // WW cache name
				)));
		
		/*
		 * US Cities over 50k
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_uscitiesover50k"
				, Messages.getText("layers.Earth.uscitiesover50k.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, US_CITIES_OVER_50K // WW cache name
				)));
		
		/*
		 * US Cities over 500k
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_uscitiesover500k"
				, Messages.getText("layers.Earth.uscitiesover500k.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null
				, US_CITIES_OVER_500K // WW cache name
				)));
		

		/*
		 * Mountains Valleys
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_mountainsvalleys"
				, Messages.getText("layers.Earth.mountainsvalleys.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, MOUNTAINS_VALLEYS // WW cache name
				)));

		/*
		 * Dessert Plains
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_desertsplains"
				, Messages.getText("layers.Earth.DessertPlains.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, DESSET_PLAINS // WW cache name
				)));
		
		/*
		 * Trenches/Ridges
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_trenchesridges"
				, Messages.getText("layers.Earth.TrenchesRidges.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, TRENCES_RIDGES // WW cache name
				)));
		
		/*
		 * Lakes/Rivers
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:wpl_lakesrivers"
				, Messages.getText("layers.Earth.LakesRivers.Name")
				, "EPSG:4326"			// SRS
				, "capitals"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null
				, LAKES_RIVERS // WW cache name
				)));

		/*
		 * State Boundaries
		 */
		super.add(new TiledWMSLayer( new TiledWMSLayerDescriptor(
				wmsUrl
				, "topp:states"
				, Messages.getText("layers.Earth.StateBoundaries.Name")
				, "EPSG:4326"			// SRS
				, "countryboundaries"
				, Sector.FULL_SPHERE	// bbox
				, null					// time
				, TiledWMSLayer.FMT_PNG 
				, 13 		// levels
				, 512, 512	// w h
				, null		// version
				, US_STATE_BOUNDARIES // WW cache name
				)));
		
	}

	public void setAllEnabled (boolean enabled)
	{
		for (Iterator<Layer> iterator = iterator(); iterator.hasNext();) {
			Layer l = (Layer) iterator.next();
			l.setEnabled(enabled);
		}
	}
	
	public void setEnabled (String name, boolean enabled)
	{
		for (Iterator<Layer> iterator = iterator(); iterator.hasNext();) {
			TiledWMSLayer l = (TiledWMSLayer) iterator.next();
			if ( l.getDescriptor().getCacheName().equalsIgnoreCase(name) )
				l.setEnabled(enabled);
		}
	}

	
}
