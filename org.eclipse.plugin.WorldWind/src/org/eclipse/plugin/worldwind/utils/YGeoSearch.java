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
package org.eclipse.plugin.worldwind.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;
import org.eclipse.plugin.worldwind.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.SimpleHTTPClient;



/**
 * Yahoo geoSearch client
 * 
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
public class YGeoSearch 
{
	private static final Logger logger = Logger.getLogger(YGeoSearch.class);
	
	private String location;
	
	/**
	 * Yahoo search result object
	 * @author Owner
	 *
	 */
	public static class YResult 
	{
		public String latitude, address, longitude, city, state, zip, country;
		public String warning;
		
		public String debug() {
			return "Y! lat= " + latitude + " lon=" + longitude + " city=" + city
				+ " st=" + state + " zip=" + zip + " c=" + country; 
		}
		@Override
		public String toString() {
			return  ( warning != null ? warning + ", " : "" )
				+ ( address != null ? address + ", " : "" )
				+ ( city != null ? city + ", " : "" )
				+ (state != null  ?  state + ", " : "")
				+ ( zip != null  ? zip  + ", " : "")
				+ ( country != null ? country : "");
		}
	}

	/**
	 * Search for a place using Yahoo
	 * @param location
	 */
	public YGeoSearch(String location) 
	{
		if ( location == null) 
			throw new  IllegalArgumentException("Invalid location");
		
		this.location = location.replaceAll(" ", "+");
	}
	
	/**
	 * Get search locations 
	 * @return array of {@link YResult} objects
	 * @throws Exception
	 */
	public YResult[] getLocations() throws Exception
	{
		
		final String url = Messages.getText("Y.geo.url.pref")
			+ location;
		
		logger.debug("Url=" + url);
		
		SimpleHTTPClient client = new SimpleHTTPClient(new URL(url));
		final String xml 		= client.doGet();
		
		if ( client.getStatus() == HttpURLConnection.HTTP_OK ) 
		{
			client.close();
			
			logger.debug("Got xml " + xml);
			return parseYahooXml(xml);
			
		}
		
		// handle error
		throw new IOException("HTTP request failed " 
				+ client.getStatus() 
				+ " " + url );
	}
	
	/**
	 * Parse xml
	 * @param xml
	 * @throws Exception
	 */
	private YResult[] parseYahooXml (String xml)
		throws Exception
	{
		Document doc = ParserUtils.parse(new ByteArrayInputStream(xml.getBytes()));
		
		// KML Doc 
		NodeList results = doc.getElementsByTagName("Result");
		
		YResult[] Yresults = new YResult[results.getLength()];
		
		for (int i = 0; i < results.getLength(); i++) 
		{
			final Element e = (Element)results.item(i);
		
			YResult Yres = new YResult();
			
			Yres.warning	= ParserUtils.getAttributeValue(e, "warning");
			Yres.address 	= ParserUtils.getNodeValue(e, "Address");
			Yres.latitude 	= ParserUtils.getNodeValue(e, "Latitude");
			Yres.longitude 	= ParserUtils.getNodeValue(e, "Longitude");
			Yres.city 		= ParserUtils.getNodeValue(e, "City");
			Yres.state 		= ParserUtils.getNodeValue(e, "State");
			Yres.country 	= ParserUtils.getNodeValue(e, "Country");
			Yres.zip 		= ParserUtils.getNodeValue(e, "Zip");
			
			// lat lon cannot be null
			if ( Yres.latitude == null || Yres.longitude == null) {
				//System.err.println("Invalid lat/lon for result " + Yres.debug());
				continue;
			}
			
			logger.debug(Yres.debug());
			Yresults[i] = Yres;
		}
		return Yresults;
	}
}
