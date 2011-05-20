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
package org.eclipse.plugin.worldwind.contrib.parsers;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.eclipse.plugin.worldwind.contrib.Messages;
import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.ScreenOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.layers.TiledWMSLayer;
import org.eclipse.plugin.worldwind.contrib.layers.TiledWMSLayerDescriptor;
import org.eclipse.plugin.worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import org.eclipse.plugin.worldwind.contrib.parsers.SimpleKMLParser.GroundOverlay;
import org.eclipse.plugin.worldwind.contrib.parsers.SimpleKMLParser.ScreenOverlay;

public class ParserUtils 
{
	private static final Logger logger = Logger.getLogger(ParserUtils.class);

	/**
	 * Encapsulates an HTTP dataset information. It is used by 
	 * {@link HTTPNavyWeatherLayer} and {@link HTTPGOESLoopFileLayer}
	 * to build satellite {@link TimeLoopGroundOverlay} WorldWind layers
	 * @author Vladimir Silva
	 *
	 */
	public static class HTTPDataSet
	{
		public HTTPDataSet(String name, URL url, Sector bbox) {
			this.name = name;
			this.directoryURL = url;
			this.bbox = bbox;
			this.accessType = null; //"loopfile"; // default
		}
		public String name;
		public URL directoryURL;
		public String accessType;
		public URL loopFile;
		public Sector bbox;
		
		// optional
		public String filter;
		public String dateFormat;
		
		@Override
		public String toString() {
			return name + "," + bbox + ", dir url=" + directoryURL 
				+ ", access=" + accessType + ", loop file=" + loopFile
				+ ", filter=" + filter + ", dateformat=" + dateFormat;
		}
	}

/* Sample World Wind Navy Labs XML format	
	<NRL_DataSet Name="GOES real-time color: East">
	<Directory_Url>http://goes.gsfc.nasa.gov/goescolor/goeseast/hurricane2/geo/</Directory_Url>
	<Description>Real-Time, Full-Resolution, Calibrated and Navigated GOES Images</Description>
	<Documentation>http://goes.gsfc.nasa.gov/</Documentation>
	
	<!-- This thag was added to handle GOES loop files -->
	<AccessMethod type="http_index" BaseUrl="http://goes.gsfc.nasa.gov/goescolor/goeseast/hurricane2/geo/" 
		Url="http://goes.gsfc.nasa.gov/goescolor/goeseast/hurricane2/geo/" filter=".*if" 
		dateformat="SPLIT=.&amp;Y=0 0 2&amp;M=0 2 4&amp;D=0 4 6&amp;h=0 6 8&amp;m=0 8 10&amp;s=NULL" />

	<GeographicBoundingBox>
			<North>
				<Value>48.557747316891</Value>
			</North>
			<South>
				<Value>15.442252683109</Value>
			</South>
			<West>
				<Value>-99.477622167829</Value>
			</West>
			<East>
				<Value>-64.522377832171</Value>
			</East>
	</GeographicBoundingBox>
	</NRL_DataSet>
*/

	/**
	 * Parse a local World Wind XML file. The format is the same as the Win32
	 * Navy Res Lab Real time weather plugin
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static Vector<HTTPDataSet> parseHTTPDatasetXMLFile(InputStream is)
		throws Exception
	{
		Document document 		= ParserUtils.parse(is);
		
		NodeList nl 			= document.getElementsByTagName("NRL_DataSet");
		Vector<HTTPDataSet> v 	= new Vector<HTTPDataSet>(); 
		
		for (int i = 0; i < nl.getLength(); i++) 
		{
			final Element e 	= (Element)nl.item(i);
			final String name 	= ParserUtils.getAttributeValue(e, "Name");
			final String url 	= ParserUtils.getNodeValue(e, "Directory_Url");

			// ignore non-leaf nodes
			if ( e.getElementsByTagName("NRL_DataSet").getLength() > 0 ) 
				continue;

			//double north, south, east, west;
			Element e1 = (Element)e.getElementsByTagName("North").item(0);
			final double north = Double.parseDouble(ParserUtils.getNodeValue(e1, "Value"));
			
			e1 = (Element)e.getElementsByTagName("South").item(0);
			final double south = Double.parseDouble(ParserUtils.getNodeValue(e1, "Value"));
			
			e1 = (Element)e.getElementsByTagName("East").item(0);
			final double east = Double.parseDouble(ParserUtils.getNodeValue(e1, "Value"));
			
			e1 = (Element)e.getElementsByTagName("West").item(0);
			final double west = Double.parseDouble(ParserUtils.getNodeValue(e1, "Value"));

			e1 = (Element)e.getElementsByTagName("AccessMethod").item(0);

			// data set
			final HTTPDataSet ds = new HTTPDataSet(name, new URL(url),
					new Sector(
			    			Angle.fromDegrees(south), Angle.fromDegrees(north),
			                Angle.fromDegrees(west), Angle.fromDegrees(east)
			                ));

			// All this is optional
			if ( e1 != null ) 
			{
				ds.accessType = "loopfile";	// default
				
				// type = {loopfile, http_index}
				final String accessType =  ParserUtils.getAttributeValue(e1, "type");
				
				final String loopfileUrl = ( accessType.equalsIgnoreCase("loopfile") )
					? ParserUtils.getAttributeValue(e1, "Url")
					: null ;

				// for access type http_index
				String filter 		= null;
				String dateFormat 	= null;
				
				if ( accessType.equalsIgnoreCase("http_index")) {
					filter 		= ParserUtils.getAttributeValue(e1, "filter");
					dateFormat	= ParserUtils.getAttributeValue(e1, "dateformat");
				}
				else if ( accessType.equalsIgnoreCase("loopfile") ) 
				{
					final String baseURL = ParserUtils.getAttributeValue(e1, "BaseUrl");

					if ( baseURL != null ) 
						ds.directoryURL = new URL(baseURL);
				}
				
				// Add loop file
				if ( loopfileUrl != null )  
					ds.loopFile = new URL(loopfileUrl);
				
				ds.accessType	= accessType;
				ds.filter 		= filter;
				ds.dateFormat 	= dateFormat;	// string used to extract the date from the file name
			}
			
			//System.out.println("ParserUtils: " + ds + " (n,s,e,w)=" + north + "," + south + "," + east + "," + west);
			
			v.add(ds);
		}
		return v;
	}
	
	/*
	 * XML Document 
	 */
	public static Document parse(InputStream is) 
		throws SAXException, IOException, ParserConfigurationException
	{
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        return docBuilder.parse(is);
	}
	
	/**
	 * Extract the value of a XML element  
	 * @param e Document element
	 * @param name
	 * @return
	 */
	static public String getNodeValue (Element e, String name)
    {
        NodeList nl = e.getElementsByTagName(name);

        try {
            return ( nl != null && nl.getLength() > 0 ) ?
                    nl.item(0).getFirstChild().getNodeValue().trim() : null;
			
		} catch (NullPointerException ex) {
			return null;
		}
    }

	static public String getTextContent (Element e, String name)
    {
        NodeList nl = e.getElementsByTagName(name);

        try {
            return ( nl != null && nl.getLength() > 0 ) ?
                    nl.item(0).getTextContent().trim() : null;
			
		} catch (NullPointerException ex) {
			return null;
		}
    }
	
	/**
	 * Get an attribute value
	 * @param e
	 * @param name
	 * @return
	 */
    static public String getAttributeValue (Element e, String name)
    {
        Node n = e.getAttributes().getNamedItem(name);
        return ( n!= null ) ? n.getNodeValue() : null;
            
    }

    /**
     * Build an AnimatedGround overlay layer from an array of ground overlays
     * extracted from a KML doc
     */
    public static TimeLoopGroundOverlay buildGroundOverlays(String title, WorldWindowGLCanvas world, GroundOverlay[] overlays)
    {
    	TimeLoopGroundOverlay overlay = new TimeLoopGroundOverlay(title); 
    	
    	for (int i = 0; i < overlays.length; i++) 
    	{
    		final Sector bbox = new Sector(Angle.fromDegrees(overlays[i].bbox.south)
					, Angle.fromDegrees(overlays[i].bbox.north)
					, Angle.fromDegrees(overlays[i].bbox.west)
					, Angle.fromDegrees(overlays[i].bbox.east));
    		
			overlay.add(new GroundOverlayLayer(
					overlays[i].name
					, bbox
					, overlays[i].icon
					, getIconSuffix(overlays[i].icon.toString())));
		}
    	return overlay;
    }

    /**
     * Convert an array of {@link WMS_Capabilities.Layer} to an array of {@link GroundOverlayLayer}
     * @param layers Array of {@link WMS_Capabilities.Layer}
     * @return array of {@link GroundOverlayLayer}
     */
    public static GroundOverlayLayer[] newGroundOverlay(WMS_Capabilities.Layer[] layers
    		, final String format) 
    {
    	if ( layers == null )
    		throw new IllegalArgumentException("Invalid WMS layers");
    	
    	GroundOverlayLayer[] overlays = new GroundOverlayLayer[layers.length];
    	
    	for (int i = 0; i < layers.length; i++) 
    	{
    		final WMS_Capabilities.Layer layer = layers[i];
    		final Sector sector = new Sector(Angle.fromDegrees(Double.parseDouble(layer.bbox.south))
    				, Angle.fromDegrees(Double.parseDouble(layer.bbox.north)) 
    				, Angle.fromDegrees(Double.parseDouble(layer.bbox.west))
    				, Angle.fromDegrees(Double.parseDouble(layer.bbox.east))
    				);
    		
    		String fileSuffix =  ( format != null) ? "." + format.split("/")[1] : null;
    		
    		logger.debug("Layer " + layer.Name + " Sector=" + sector 
    				+ " Get Map url=" + layer.getMapUrl(format) + " Img fmt=" + format);
    		
    		overlays[i] = new GroundOverlayLayer(layer.Title + " ("+ layer.Name + ")"
    				, sector 
    				, layer.getMapUrl(format)
    				, fileSuffix );
    		
		}
    	return overlays;
    }

    /**
     * Convert a {@link WMS_Capabilities.Layer} instance to a {@link TimeLoopGroundOverlay} whose overlays 
     * are defined by the array frames
     * @param layer {@link WMS_Capabilities} layer instance
     * @param frames overlay frame names
     * @return {@link TimeLoopGroundOverlay}
     * @throws MalformedURLException
     */
    public static TimeLoopGroundOverlay newTimeLoopGroundOverlay(WMS_Capabilities.Layer layer
    		, String[] frames
    		, final String format )
    
    	throws MalformedURLException
    {
    	if ( layer == null  || frames == null )
    		throw new IllegalArgumentException("Invalid WMS layers or overlay frame names");
    	
    	//String name = ( layer.Title != null ) ? layer.Title : layer.Name;

		final Sector sector = new Sector(Angle.fromDegrees(Double.parseDouble(layer.bbox.south))
				, Angle.fromDegrees(Double.parseDouble(layer.bbox.north)) 
				, Angle.fromDegrees(Double.parseDouble(layer.bbox.west))
				, Angle.fromDegrees(Double.parseDouble(layer.bbox.east))
				);

		// Suffix of the frame format (.jpg, .png,...)
		String fileSuffix =  ( format != null) ? "." + format.split("/")[1] : null;

		// Location where frames will be cached
		final String cachePath = "Earth/" + Messages.forCachePath(layer.Name) + "/";
    	
    	TimeLoopGroundOverlay loopOverlay = new TimeLoopGroundOverlay(layer.Name); //name);
    
    	loopOverlay.setDescription(layer.getDescriptionAsHTML());
    	
    	if ( layer.style.isValid() && layer.style.LegendURL != null )
    		loopOverlay.setLegend(layer.style.LegendURL);
    	
    	for (int i = 0; i < frames.length; i++) 
    	{
    		final URL icon = 
    			new URL(layer.getMapUrl(format)  
						+ "&time=" + frames[i] 
						);
    		
    		logger.debug("WMS Animated Ov=" + layer.Name 
    				+ " Frame=" + frames[i] + " Sector=" + sector 
    				+ " Icon=" + icon + " Img fmt=" + format
    				+ " Cache path=" + cachePath );
    		
    		final GroundOverlayLayer overlay = new GroundOverlayLayer(frames[i]
    				, sector 
    				, icon 
    				, fileSuffix );
    		
        	overlay.setBaseCachePath(cachePath);
        	
        	loopOverlay.add(overlay);
		}
    	return loopOverlay;
    }
    
    /**
     * Convert an array of {@link WMS_Capabilities.Layer} to an array of {@link TiledWMSLayer}
     * @param layers Array to convert
     * @return array of {@link TiledWMSLayer}
     */
    public static TiledWMSLayer[] newWMSTiledLayer(WMS_Capabilities.Layer[] layers
    		, final String format) 
    {
    	if ( layers == null )
    		throw new IllegalArgumentException("WMS110_Capabilities2WMSTiledLayer: Invalid WMS layers");
    	
    	TiledWMSLayer[] overlays = new TiledWMSLayer[layers.length];
    	
    	logger.debug("# of layers=" + layers.length);
    	
    	for (int i = 0; i < layers.length; i++) 
    	{
        	if ( layers[i] == null )
        		throw new IllegalArgumentException("WMS110_Capabilities2WMSTiledLayer: Invalid or NULL layer.");
    		
    		final WMS_Capabilities.Layer layer = layers[i];
    		final WMS_Capabilities capabilities = layer.getCapabilities();

    		final Sector bbox = new Sector(Angle.fromDegrees(Double.parseDouble(layer.bbox.south))
    				, Angle.fromDegrees(Double.parseDouble(layer.bbox.north)) 
    				, Angle.fromDegrees(Double.parseDouble(layer.bbox.west))
    				, Angle.fromDegrees(Double.parseDouble(layer.bbox.east))
    				);
    		
    		// WW cache location
    		final String cacheLoc = (layer.Title != null)
    				? Messages.forCachePath(layer.Title)
    				: Messages.forCachePath(layer.Name);

    		final String fmt = ( format != null) 
    			? format 
    			: capabilities.getMapRequest().formats.firstElement();
    		
    		// CRS cannot be null
    		final String CRS = layer.CRS != null  ? layer.CRS : layer.SRS;
    		
    		if ( CRS == null )
    			throw new IllegalArgumentException("No Coordinate Reference System for " + layer.Name);
    		
    		overlays[i] =  new TiledWMSLayer(new TiledWMSLayerDescriptor(
    				capabilities.getMapRequest().Url.toString() //  GetMapURL.toString()
    				, layer.Name		// wms layer name
    				, layer.Title		// title
    				, CRS				// CRS
    				, layer.style.Name	// style name
    				, bbox				// lat/lon box
    				, null				// time: null for wms 1.1.0
    				, fmt 				// img fmt: image/png,...
    				, 13				// # of levels
    				, layer.fixedWidth != 0 ? layer.fixedWidth : 512 	// width
    				, layer.fixedHeight!= 0 ? layer.fixedHeight : 512	// tile w,h
    				, capabilities.getVersion().toString()
    				, "Earth/" + cacheLoc	// WW cache location
    				));
		}
    	return overlays;
    }
    
    /**
     * Build a World Wind {@link ScreenOverlayLayer} array from an array of KML {@link ScreenOverlay}s
     * @param overlays Array of {@link ScreenOverlay}
     * @return
     */
    public static ScreenOverlayLayer[] buildScreenOverlays(ScreenOverlay[] overlays) // String title,  
    {
    	ScreenOverlayLayer[] layers = new ScreenOverlayLayer[overlays.length];
    	String position = ScreenOverlayLayer.NORTHWEST;
    	
    	for (int i = 0; i < overlays.length; i++) 
    	{
    		if ( i == 1 ) position = ScreenOverlayLayer.NORTHEAST;
    		if ( i == 2 ) position = ScreenOverlayLayer.SOUTHWEST;
    		if ( i == 3 ) position = ScreenOverlayLayer.SOUTHEAST;
    		
    		// >4 overlays will overwrite
    		layers[i] = new ScreenOverlayLayer(overlays[i].name
    				, overlays[i].icon, position);
		}
    	return layers;
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    static public Vector<HTTPDataSet> parseGOESSatFile ()
    	throws Exception
    {
		// Parse GOES Dataset XML file
		return parseGOESSatFile(Messages.getText("layers.goes.xml.file"));
    }

    static public Vector<HTTPDataSet> parseGOESSatFile (String path)
    	throws Exception
	{
		// Parse GOES Dataset XML file
		return parseHTTPDatasetXMLFile(Messages.getInputStream(ParserUtils.class, path));
	}

    static public Vector<HTTPDataSet> parseGOESSatFile (InputStream is)
		throws Exception
	{
		// Parse GOES Dataset XML file
		return parseHTTPDatasetXMLFile(is);
	}
    
    /**
     * Utility methods to load & parse the local NRL file 
     * @return
     * @throws Exception
     */
    static public Vector<HTTPDataSet> parseNavyLabsFile ()
		throws Exception
	{
		return parseNavyLabsFile(Messages.getText("layers.nrl.xml.file"));
	}
    
    static public Vector<HTTPDataSet> parseNavyLabsFile (String path)
		throws Exception
	{
		return parseHTTPDatasetXMLFile(Messages.getInputStream(ParserUtils.class, path)); 
	}
    
    static public Vector<HTTPDataSet> parseNavyLabsFile (InputStream is)
		throws Exception
	{
		return parseHTTPDatasetXMLFile(is); 
	}
 
    /**
     * Convert a HTTP query strinf key1=val1&key2=val2&... into properties for
     * easy access
     * @param data HTTP query string of the form key1=val1&key2=val2&...
     * @return
     * @throws IOException
     */
    static public Properties httpQryStr2Properties (String data) throws IOException {
    	Properties props = new Properties();
    	
    	// XML attributes are escaped
    	if ( data.indexOf("&amp;") != -1)
    		props.load(new ByteArrayInputStream(data.replaceAll("\\&amp;", "\n").getBytes()));
    	else
    		props.load(new ByteArrayInputStream(data.replaceAll("\\&", "\n").getBytes()));
    	
    	return props;
    }
    
    /**
     * HTTP Image frame. Represents a time step from a {@link HTTPNavyWeatherLayer}
     * or {@link HTTPGOESLoopFileLayer} object
     * @author vsilva
     *
     */
	public static class HTTPImageFrame 
	{
		public HTTPImageFrame(String date, String frameUrl) {
			this.date = date;
			this.frameUrl = frameUrl;
			this.frameExt = getIconSuffix(this.frameUrl);
		}
		public String date;
		public String frameUrl;
		public String frameExt;
		@Override
		public String toString() {
			return date  + "," + frameUrl + "," + frameExt;
		}
	}
    
    /**
     * A loop file URL http://server/path/loopfile.txt of the form:
     * 	img/2007137_0715avn.jpg overlay= over/LALO.GIF, over/SATWIN_H.GIF, over/SATWIN_M.GIF, over/SATWIN_L.GIF, over/POINTS.GIF, over/FRONTS.GIF, over/WARN.GIF, over/IRTEMP.GIF
     * 	img/2007137_0715avn.jpg overlay= over/LALO.GIF, over/SATWIN_H.GIF, over/SATWIN_M.GIF, over/SATWIN_L.GIF, over/POINTS.GIF, over/FRONTS.GIF, over/WARN.GIF, over/IRTEMP.GIF
     * 	...
     * @param baseUrl
     * @param accessUrl
     * @param imgField
     * @return
     * @throws Exception
     */
    static public Vector<HTTPImageFrame> handleLoopFile(String baseUrl, URL accessUrl, int imgField) 
    	throws IOException, MalformedURLException
    {
    	Vector<HTTPImageFrame> dirEntries = new Vector<HTTPImageFrame>();
    	
        // Handle Access type loopfile  - (GOES). Loop url file format:
        // img/2007137_0715avn.jpg overlay= over/LALO.GIF, over/SATWIN_H.GIF, over/SATWIN_M.GIF, over/SATWIN_L.GIF, over/POINTS.GIF, over/FRONTS.GIF, over/WARN.GIF, over/IRTEMP.GIF
        ByteArrayOutputStream txt  = new ByteArrayOutputStream();

        SimpleHTTPClient client = new SimpleHTTPClient(accessUrl);
        
        client.doGet(txt);
       	txt.close();


        String[] tmp = txt.toString().split("\\n");
        
		// format ints w/ 2 digits
        NumberFormat.getInstance().setMinimumIntegerDigits(2);	        	

        // loop thru lines in file of the form above
        Calendar date = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        
        logger.debug("Base url=" + baseUrl + " access url=" + accessUrl + " img field=" + imgField);
        
        for ( int i = 0 ; i < tmp.length ; i++ ) 
        {
        	final String[] fields = tmp[i].split(" ");	// split fields
        	
        	// URL
        	final String url 	= ( baseUrl == null ) ? fields[imgField] : baseUrl + fields[imgField];
        	
        	// build iso date
			final int year 		= Integer.parseInt(fields[imgField].substring(4,8));
			final int dayOfYear = Integer.parseInt(fields[imgField].substring(8,11));
			
        	date.set(Calendar.DAY_OF_YEAR, dayOfYear);
        	date.set(Calendar.YEAR, year);

        	// Build date from file format: img/2007137_0715avn.jpg - Format: YYYY[DAY_OF_YEAR]_TIME{SAT TYPE}.ext
        	final String isoDate =  formatter.format(date.getTime())
        		+ "T" + fields[imgField].substring(12,14) + ":"
        		+ fields[imgField].substring(14,16) + ":00Z";

//System.out.println("url=" + url +  " year=" + year + " day of year =" + dayOfYear + " date=" + isoDate);	        		

        	dirEntries.add(new HTTPImageFrame(isoDate, url));
        }
        
        logger.debug("Returning " + dirEntries.size() + " http directory entires");
        return dirEntries;
    }
    
	/*
	 * Parse the Navy Labs HTML Directory. The format is:
    <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
    <html>
     <head>
      <title>Index of /archdat/global/stitched/cloudtop</title>
     </head>
     <body>
    <h1>Index of /archdat/global/stitched/cloudtop</h1>
    <pre><img src="/icons/blank.gif" alt="Icon "> <a href="?C=N;O=D">Name</a><a href="?C=M;O=A">Last modified</a>      <a href="?C=S;O=A">Size</a>  <a href="?C=D;O=A">Description</a><hr><img src="/icons/back.gif" alt="[DIR]"> <a href="/archdat/global/stitched/">Parent Directory</a>                                                          -   
    <img src="/icons/image2.gif" alt="[IMG]"> <a href="20061017.0600.multisat.ir.cloudtop.global.nogaps.jpg">20061017.0600.multisat.ir.cloudtop.global.nogaps.jpg</a> 17-Oct-2006 02:19  390K  
    <img src="/icons/image2.gif" alt="[IMG]"> <a href="20061017.0900.multisat.ir.cloudtop.global.nogaps.jpg">20061017.0900.multisat.ir.cloudtop.global.nogaps.jpg</a> 17-Oct-2006 05:09  391K  
	 * @param html HTML to parse
	 * @param filter Regular expression to filter file (e.g .*\/.tif will match tifs only)
	 * @params dateExtraction Properties used to extract a date from each listing frame/image
	 *  For example tghe string: SPLIT=.&Y=0 0 4&M=0 4 6&D=0 6 8&h=1 0 2&m=1 2 4&s=NULL
	 *  Will: Split by "."  and build a date as Year = field:0, substr(0,4), Month = field:0 substr(4,6),....
	 */
	static public Vector<HTTPImageFrame> parseHTTPDirectory ( URL directoryUrl
			, String baseUrl
			, String filter
			, Properties dateExtraction)
			
    throws IOException
    {
        // Will parse Directory entries <img....

		ByteArrayOutputStream html = new ByteArrayOutputStream();
		
		SimpleHTTPClient client = new SimpleHTTPClient(directoryUrl);
        client.doGet(html); // ParserUtils.httpGet(directoryUrl, html);
        html.close();
		
        Vector<HTTPImageFrame> dirEntries = new Vector<HTTPImageFrame>();
        String[] tmp      = null;
        String[] lines    = html.toString().split("\n");

        
        logger.debug( "html len=" + html.toString().length() + " date extraction props=" + dateExtraction + " filter=" + filter);

        if (dateExtraction == null || dateExtraction.getProperty("Y") == null || dateExtraction.getProperty("M") == null || dateExtraction.getProperty("D") == null
        	|| dateExtraction.getProperty("h") == null || dateExtraction.getProperty("m") == null || dateExtraction.getProperty("s") == null
        )
        	throw new IOException("Invalid date extraction string: " + dateExtraction);

        final String splitRegEx 	= "\\" + dateExtraction.getProperty("SPLIT");
        
        logger.debug("Date Split regex is:" + splitRegEx + " Directory has " + lines.length + " lines.");
        
        // Date extraction data f the form: x,y,z where x = split field (y,z) substr value within field
        int[] Year 	= parseDateField(dateExtraction.getProperty("Y"));
        int[] Month = parseDateField(dateExtraction.getProperty("M"));
        int[] Day 	= parseDateField(dateExtraction.getProperty("D"));
        int[] Hour	= parseDateField(dateExtraction.getProperty("h"));
        int[] Min 	= parseDateField(dateExtraction.getProperty("m"));
        int[] Sec 	= parseDateField(dateExtraction.getProperty("s"));
        
        for (int i = 0 ; i < lines.length ; i++ ) 
        {
            if ( lines[i].startsWith("<img") || lines[i].startsWith("<IMG")) 
            {
            	// Careful: IMG Frames are case sensitive!
                final String line   = lines[i];
                final String lcLine = lines[i].toLowerCase();
                
                // case of the img frame must not be changed
                String iconUrl	= line.substring(lcLine.indexOf("href=\"") + 6,
                                        line.indexOf("\">", lcLine.indexOf("href=\"")));
                                        
                final String timeSize   = line.substring(lcLine.indexOf("</a>") + 5);
                tmp                     = timeSize.split("  ");

                // get date from frame name (in ISO format)
                String isoDate = null;
                
                if ( filter != null ) {
                    filter = filter.replaceAll("/", "\\\\");	// fix regex
                    
                	if ( !iconUrl.matches(filter) ) {
                		// Ignore  iconUrl
                		continue;
                	}
                }
                	
                // skip LATEST.jpg frame/image
				if (lcLine.indexOf("latest") != -1 )  
					continue;
				
				// extract date from img href
                if ( iconUrl.indexOf(".") != -1) 
                {
                	// Image format: 20061017.0600.multisat.ir.cloudtop.global.nogaps.jpg
                    tmp = iconUrl.split(splitRegEx); // "\\.");

                    if ( tmp.length == 0) 
                    	throw new IOException("Unable to split frame " + iconUrl + " using regex:" + splitRegEx);
                    
                    // KML Iso dates must be well formed: YYYY-MM-DDThh:mm:ssZ (even if data is missing)
                    isoDate = tmp[ Year[0] ].substring(Year[1], Year[2]);
                    
                    // Ugly Hack: 2 digit year?, years must be YYYY
                    if ( isoDate.length() == 2 ) isoDate = "20" + isoDate;
                    
                    isoDate += ( !isDateFieldNull(Month)) 
                    	? "-" + tmp[Month[0]].substring(Month[1], Month[2])
                    	: "-01";
                    isoDate += ( !isDateFieldNull(Day))  
                    	? "-" + tmp[ Day[0] ].substring( Day[1], Day[2])
                    	: "-01";
                    	
                    // Time: hh:mm:ss
                    isoDate +=  ( !isDateFieldNull(Hour))  
                    	? "T" + tmp[Hour[0]].substring(Hour[1], Hour[2])
                    	: "T00";

                    isoDate += ( !isDateFieldNull(Min))
                    	? ":" + tmp[Min[0]].substring(Min[1],Min[2])
                    	: ":00";
                    isoDate += ( !isDateFieldNull(Sec)) 
                    	? ":" + tmp[Sec[0]].substring(Sec[1],Sec[2])
                        : ":00";
                    
                    isoDate += "Z";
                }

            	if ( baseUrl != null ) iconUrl = baseUrl + iconUrl;

                //logger.debug("frame=" + iconUrl + " date=" + isoDate);
                
				// date cannot be NULL
				if ( isoDate != null )
					dirEntries.add(new HTTPImageFrame(isoDate, iconUrl));
            }
        }
        return dirEntries;
    }
    
    /**
     * Extract ints from a field of the form x,y,z where x,y,z are used as such
     * val = FrameUrlWithDate.split("split-char")[x].substring(y,z)
     * @param fieldString x,y,z values
     * @return
     * @throws IOException
     */
    static private int[] parseDateField(String fieldString) throws IOException 
    {
    	if ( fieldString.equals("NULL")) 
    		return new int[3]; // all zeros - no value
    	
        String[] tmp = fieldString.split(" ");
        
        if ( tmp.length != 3) 
        	throw new IOException("Invalid date field string:" + fieldString);
        
        int[] data = new int[3];
        data[0]	= (tmp.length > 0) ? Integer.parseInt(tmp[0]) : -1;
        data[1]	= (tmp.length > 0) ? Integer.parseInt(tmp[1]) : -1;
        data[2]	= (tmp.length > 0) ? Integer.parseInt(tmp[2]) : -1;
        
        //log.debug("(" + data[0] + "," + data[1] + "," + data[2] + ")");
        return data;
    }
    
    static private boolean isDateFieldNull (int[] field) throws IOException {
    	if ( field == null || field.length != 3)
    		throw new IOException("Invalid date extraction array: " + field);
    	
    	return ( field[0] == 0 && field[1] == 0 && field[2] == 0) ? true : false;
    }
    
    /**
     * Get the suffix of the URL of an icon.
     * <br>http://host.com/path/icon.jpg ==> .jpg
     * <br> http://host.com/path/cgi?service=WMS..format=image/png&.. ==> .png
     * @param icon Icon url string
     * @return Extension including "."
     */
    public static String getIconSuffix (String icon) 
    {
    	if ( icon.indexOf("?") == -1)
    	{
    		return icon.substring(icon.lastIndexOf("."), icon.length());
    	}
    	else {//if ( icon.indexOf("format=") != -1) {
    		final String key 	= "image/";
    		final int i 		= icon.indexOf(key);
    		final int delta		= key.length();
    		
    		return "." + icon.substring(i + delta
    				, (icon.lastIndexOf("&") > (i + delta))
    					? icon.indexOf("&", i + delta)
    					: icon.length() );
    	}
    }
    
    /**
     * Public WMS server descriptor
     * @author vsilva
     *
     */
    static public class PublicWMSServer {
    	public String name;
    	public URL capabilitiesURL;
    	
    	public PublicWMSServer(String name, URL capsUrl) {
			this.name = name;
			this.capabilitiesURL = capsUrl;
		}
    	@Override
    	public String toString() {
    		return name + " " + capabilitiesURL;
    	}
    }
    
    /**
     * Utility to extract WMS server Capabilities URL & names from a public
     * web list
     * @param pageURL URL of the Web list. For example:
     * http://www.skylab-mobilesystems.com/en/wms_serverlist.html
     * @return
     * @throws IOException
     * @throws MalformedURLException
     */
    static public Vector<PublicWMSServer> parsePublicWmsHTTPPage (URL pageURL)
    	throws IOException, MalformedURLException
    {
        /*
         * Will parse HRTTP entries <a href="wms caps url"...>server name</a>
         */ 
		ByteArrayOutputStream html = new ByteArrayOutputStream();
		
		SimpleHTTPClient client = new SimpleHTTPClient(pageURL);
		
        client.doGet(html); // ParserUtils.httpGet(pageURL, html);
        html.close();
        return parsePublicWmsHTTPPage( html.toString());
    }	

    /**
     * Utility to extract WMS server Capabilities URL & names from a public list
     * @param html HTML page
     * @return
     * @throws IOException
     * @throws MalformedURLException
     */
    static public Vector<PublicWMSServer> parsePublicWmsHTTPPage (String html)
		throws IOException, MalformedURLException
	{
        Vector<PublicWMSServer> v = new Vector<PublicWMSServer>();
        String[] lines    = html.split(System.getProperty("line.separator")); // "\n");

        // loop thru lines
        for (int i = 0 ; i < lines.length ; i++ ) 
        {
            if ( lines[i].startsWith("<a href=\"") || lines[i].startsWith("<A HREF=\"") 
            	&& (lines[i].indexOf("SERVICE=WMS") != -1) 
            	|| lines[i].indexOf("service=wms") != -1 ) 
            {
            	//System.out.println("line=" + lines[hi]);
                final String line   = lines[i];
                final String lcLine = lines[i].toLowerCase();
                
                /*
                 * Simple string extraction of CAPS url & name
                 * <a href="WMS CAPS URL">Server Name</a>
                 */
                String capsUrl	= line.substring(lcLine.indexOf("href=\"") + 6,
                                        line.indexOf("\">", lcLine.indexOf("href=\"")));
                
                // Server name
                String name = line.substring(capsUrl.length() 
                		+ "<a href=\"".length() + 2 , line.length());
                
                // cleanup HTML junk from name: up to 2 inner html tags
                name = name.replaceAll("<.*?>", "").replaceAll("<.*?>", "");
                
                if ( capsUrl != null && name != null )
                	v.add(new PublicWMSServer(name, new URL(capsUrl)));
            }
        }
    	return v;
    }

    /**
     * Utility to read from an input stream
     * @param buffer
     * @param os
     * @throws IOException
     */
    static public void readStream (InputStream buffer, OutputStream os)
    	throws IOException
    {
	    int c;
	    
	    while ((c = buffer.read()) != -1) 
	    {
	      os.write(c);
	    } 
	    os.close();
    }
    
    /**
     * Parse errors returned by WMS 
     * <pre>&lt;ServiceExceptionReport version="1.3.0"
  xmlns="http://www.opengis.net/ogc"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.opengis.net/ogc http://svs.gsfc.nasa.gov/exceptions_1_3_0.xsd">
  &lt;ServiceException code="InvalidDimensionValue">
    2005-08-23T14:45:00Z is not valid for Layer 3240_24673.
  &lt;/ServiceException>
  &lt;ServiceException code="LayerNotDefined">
    Unable to find layer corresponding to parameters: LAYERS=3240_24673,STYLES=opaque,CRS=CRS:84,BBOX=-100,10,-60,50,TIME=2005-08-23T14:45:00Z,WIDTH=1024,HEIGHT=1024
  &lt;/ServiceException>
&lt;/ServiceExceptionReport> </pre>
     * @param is
     * @return
     */
    static public String parseServiceExceptionReportXML (InputStream is)
    {
    	Document doc 	= null;
    	String errors 	= null;
    	try {
    		doc = parse(is);
    		
    		NodeList nl =  doc.getElementsByTagName("ServiceException");
    		
    		if ( nl.getLength() == 0) return null;
    		
    		errors = ((Element)nl.item(0)).getTextContent().trim();
    		
    		for (int i = 1; i < nl.getLength(); i++) {
    			final Element e = (Element)nl.item(i);
    			errors +=  e.getTextContent().trim();
			}
		} catch (Exception e) {	}
		
    	return errors;
    }
    
}
