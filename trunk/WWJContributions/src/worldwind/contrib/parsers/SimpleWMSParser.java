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
package worldwind.contrib.parsers;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import worldwind.contrib.parsers.WMS_Capabilities.Layer;

/**
 * A simple WMS SAX Parser. SAX is way faster than the default XPath implementation
 * of World Wind, specially for large WMS catalogs like NASA's Scientific Visualization
 * Studio (SVS) 
 * <br/>Sample Usage:
 * <pre>
 * SimpleWMSParser parser = new SimpleWMSParser();
 * parser.parse(new FileInputStream("svs.gsfc.nasa.gov_cgi-bin_wms.xml"));
 * System.out.println(parser.getCapabilities());</pre>
 * @author Vladimir Silva
 *
 */
public class SimpleWMSParser extends DefaultHandler
{
	private static final Logger logger = Logger.getLogger(SimpleWMSParser.class);
	
	/*
	 * SAX parser stuff
	 */
	private boolean inService = false;
	private boolean inGetMap = false;
	private boolean inLayer = false;
	private boolean inStyle = false;
	private boolean inTimeDim = false;
	private boolean inDataUrl = false;
	private boolean inAttribution = false;
	
	private int totalLayers = 0;
	
	// # of parsed layers. Some may be invalid
	private int parsedCount = 0;
	
	static private WMS_Capabilities capabilities;
	
    // Buffer for collecting data from
    // the "characters" SAX event.
    private CharArrayWriter contents = new CharArrayWriter();
	
    // WMS_Capabilities work Layer (to be added to the caps obj)
    private Layer layer;

    // Required elems (CRS, BBOX) at a lower depth from the current layer
    // For layers that put this elems on a higher layer
    private int depth;
    private final int MAX_DEPTH = 4;
    private String[] requiredElemsAtDepth = new String[MAX_DEPTH];
    
	public SimpleWMSParser() {
		capabilities = new WMS_Capabilities();
	}
	
	@Override
	public void startDocument() throws SAXException 
	{
		super.startDocument();
		initDepthArray();
	}
	
	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		capabilities.setTotalLayers(totalLayers);
	}
	
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException 
	{
		contents.reset();
		
		if ( name.equals("Service")) inService = true;
		if ( name.equals("GetMap")) inGetMap = true;
		if ( name.equals("DataURL")) inDataUrl = true;
		if ( name.equals("Attribution")) inAttribution = true;
		
		/* extract WMS version */
		if ( name.equals("WMS_Capabilities") 
				|| name.equals("WMT_MS_Capabilities")) 
		{
			final String ver = attributes.getValue("version");
			if ( ver != null ) {
				final String[] tmp = ver.split("\\.");
				capabilities.setVersion(Integer.parseInt( tmp[0])
						, Integer.parseInt(tmp[1])
						, Integer.parseInt(tmp[2]));
			}
		}
		
		/* extract Request URLs: GetMap (don't need getCaps) */
		if ( name.equals("OnlineResource") && inGetMap ) {
			try {
				//capabilities.GetMapURL = new URL(attributes.getValue("xlink:href"));
				capabilities.getMapRequest().Url = new URL(attributes.getValue("xlink:href"));
			} catch (MalformedURLException e) {
			}
		}
		
		/* this will grab the last inner layer */
		if ( name.equals("Layer")) 
		{
			totalLayers ++;
			depth ++;
			
			layer = new WMS_Capabilities.Layer();
			
			inLayer 	= true;
			
			try {
				layer.fixedWidth = Integer.parseInt(attributes.getValue("fixedWidth"));
				layer.fixedHeight = Integer.parseInt(attributes.getValue("fixedHeight"));
				
			} catch (Exception e) {
				layer.fixedWidth 	= 1024;
				layer.fixedHeight 	= 512;
			}
		}
		
	    /* WMS Time Span Tags: <Dimension/> or <Extent/> */
	    if ( name.equals("Dimension") || name.equals("Extent") ) {
	    	if ( attributes.getValue("name") != null && attributes.getValue("name").equals("time") ) {
	    		inTimeDim = true;
	    	}
	    }
		
		/* Grab the last style */
		if ( name.equals("Style")) {
			inStyle = true;
		}

		/* style legend */
		if ( name.equals("OnlineResource") && inLayer && inStyle ) {
			try {
				layer.style.LegendURL = new URL(attributes.getValue("xlink:href"));
			} catch (MalformedURLException e) {
			}			
		}

		/* data url */
	    if ( name.equals("OnlineResource") && inLayer && inDataUrl ) {
	    	try {
	    		layer.DataURL = new URL(attributes.getValue("xlink:href"));
			} catch (MalformedURLException e) {}
	    }
		
	    /* bbox: BoundingBox or LatLonBoundingBox */
	    if (name.equals("BoundingBox") || name.equals("LatLonBoundingBox") ) 
	    {
	    	if ( layer.bbox.north == null  )
	    		layer.bbox.north =  attributes.getValue("maxy");
	    	
	    	if ( layer.bbox.south == null )
	    		layer.bbox.south = attributes.getValue("miny");
	    	
	    	if ( layer.bbox.east == null)
	    		layer.bbox.east 	= attributes.getValue("maxx");
	    	
	    	if ( layer.bbox.west == null)
	    		layer.bbox.west = attributes.getValue("minx");
	    }

	}
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException 
	{
		if ( name.equals("Name") && inService ) capabilities.getService().Name = getContents();
		if ( name.equals("Title") && inService ) capabilities.getService().Title = getContents();
		if ( name.equals("Abstract") && inService ) capabilities.getService().Abstract = getContents();

		/* grab the 1st valid GetMap image format */
		if ( name.equals("Format") && inGetMap ) {
			//if ( capabilities.GetMapFormat == null && getContents().indexOf("image/") != -1)
			//	capabilities.GetMapFormat = getContents();
			capabilities.getMapRequest().formats.add(getContents());
		}

		
		if ( inLayer ) {
			if (! inStyle && ! inAttribution) {
				if ( name.equals("Name")) layer.Name = getContents();
				if ( name.equals("Title")) layer.Title = getContents();
				if ( name.equals("Abstract")) layer.Abstract = getContents();
			}
			else {
				if ( name.equals("Name")) layer.style.Name = getContents();
			}
			
			/* Proj ID */
		    if ( name.equalsIgnoreCase("CRS" ) || name.equalsIgnoreCase("SRS" ) ) {
		    	putReqElemAtDepth("crs", getContents());
		    	layer.CRS = getContents();
		    }

		    /* ISO Time span */
			if ( (name.equals("Dimension") || name.equals("Extent")) && inTimeDim ) {
				layer.ISOTimeSpan = getContents();  
				inTimeDim = false;
			}

			/* bbox */
		    if ( name.equals("westBoundLongitude") ) {
		    	putReqElemAtDepth("west", getContents()); 
		    	layer.bbox.west = getContents();
		    }
		    if ( name.equals("eastBoundLongitude") ) {
		    	putReqElemAtDepth("east", getContents()); 
		    	layer.bbox.east = getContents();
		    }
		    if ( name.equals("southBoundLatitude") ) {
		    	putReqElemAtDepth("south", getContents()); 
		    	layer.bbox.south = getContents();
		    }
		    if ( name.equals("northBoundLatitude") ) {
		    	putReqElemAtDepth("north", getContents()); 
		    	layer.bbox.north = getContents();
		    }
		}

		
		if ( name.equals("Service")) inService = false;
		if ( name.equals("GetMap")) inGetMap = false;
		if ( name.equals("Style")) inStyle = false;
		if ( name.equals("DataURL")) inDataUrl = false;
		if ( name.equals("Attribution")) inAttribution = false;

		if ( name.equals("Layer")) {
			inLayer = false;
			
			depth--;
			//if ( depth == 1) initDepthArray();
			storeReqElemAtDepth();
			
			capabilities.addLayer(layer);
		}
	}

	/*
	 * Save required elements (CRS,BBOX) from a higher layer.
	 * This is a lame way to store upper layer CRS, BBOX not
	 * present at a given depth. The default WW WMS implementation
	 * is way more elegant (uses XPath) but horribly slow.
	 */
	private void initDepthArray() {
		for (int i = 0; i < requiredElemsAtDepth.length; i++) {
			requiredElemsAtDepth[i] = "";
		}
	}
	
	private void putReqElemAtDepth(String key , String value) {
		if ( value == null) return;
		if ( depth >= MAX_DEPTH) return;
		requiredElemsAtDepth[depth] += key + "=" + value + "&";
	}
	
	private void storeReqElemAtDepth() 
	{
		for (int i = 0; i < requiredElemsAtDepth.length; i++) {
			if ( requiredElemsAtDepth[i] != null && requiredElemsAtDepth[i].length() > 0) {
				try {
					Properties props = ParserUtils.httpQryStr2Properties(requiredElemsAtDepth[i]);
					
					if ( props.getProperty("crs") != null)
						layer.CRS 		= props.getProperty("crs");
					if ( props.getProperty("east") != null)
						layer.bbox.east = props.getProperty("east");
					if ( props.getProperty("west") != null)
						layer.bbox.west = props.getProperty("west");
					if ( props.getProperty("north") != null)
						layer.bbox.north = props.getProperty("north");
					if ( props.getProperty("south") != null)
						layer.bbox.south = props.getProperty("south");
				} catch (Exception e) {
					logger.error(e);
				}
			}
		}
	}
	
	
	/**
	 * Element characters
	 */
    public void characters( char[] ch, int start, int length )
		throws SAXException 
	{
	     contents.write( ch, start, length );
	}
	
    private String getContents() {
    	return contents.toString().trim();
    }
    
    public WMS_Capabilities getCapabilities()
    {
    	return capabilities;
    }
    
	/**
	 * 
	 * @param is
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void parse(String sourceName, InputStream is)
		throws SAXException, ParserConfigurationException, IOException
	{
		// Use an instance of ourselves as the SAX event handler
		DefaultHandler handler = new SimpleWMSParser();
		
		// Use the default (non-validating) parser
		SAXParserFactory factory = SAXParserFactory.newInstance();
		
		// Parse the input
	    SAXParser saxParser = factory.newSAXParser();
	    
	    InputSource src = new InputSource(is);
	    saxParser.parse( src , handler );
	    
	    // Cleanup invalid layers
	    validate(sourceName);
	}

	/**
	 * 
	 * @param sourceName
	 * @param url
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void parse(String sourceName, URL url)
		throws SAXException, ParserConfigurationException, IOException
	{
		HttpURLConnection uc = (HttpURLConnection)url.openConnection();
		parse(sourceName, uc.getInputStream());
	}
	
	/*
	 * WMS Caps validation & layer cleanup
	 */
	private synchronized void validate(String sourceName) throws SAXException
	{
		logger.debug("Validating:" + sourceName);
		
		if (! capabilities.getVersion().isValid())
			throw new SAXException("Invalid or unsupported WMS version " + capabilities.getVersion()+ " for " + sourceName);
		
		//if (capabilities.GetMapURL == null )
		if ( capabilities.getMapRequest().Url == null )
			throw new SAXException("Invalid or NULL GetMap URL for " + sourceName);
		
		// Service validation
		WMS_Capabilities.Service service = capabilities.getService();
		
		if ( service.Name == null )
			throw new SAXException("Invalid Service name for " + sourceName);
		
		// Layer cleanup: required fields: Name, BBOX, CRS
		ArrayList<Layer> layers = capabilities.getLayers();
		
		parsedCount = layers.size();
		
		// Is layer valid?
		for (Iterator<Layer> iterator = layers.iterator(); iterator.hasNext();) 
		{
			Layer l = iterator.next();
			if ( l.Name == null || !l.bbox.isValid() || l.CRS == null ) {
				logger.debug("Removing layer " + l.Name + ". Missing name, bbox or CRS.");
				iterator.remove();
			}
		}
		logger.debug("Completed " + sourceName + " - Capabilities:\n" + getCapabilities());
	}
	
	/**
	 * Total # of parsed layers. Invalid layers will be removed
	 * @return Total # of parsed layers (including invalid ones)
	 */
	public int getParsedCount () {
		return parsedCount;
	}
	
	/* test only */
	/* 
	public static void main(String[] args) {
		try {
			SimpleWMSParser parser = new SimpleWMSParser();
			
			//String file = "src/demo/xml/svs.gsfc.nasa.gov_cgi-bin_wms.xml";
			//String file = "src/demo/xml/worldwind21.arc.nasa.gov_geoserver_wms.xml";
			URL url = new URL("http://gisdata.usgs.net/servlet/com.esri.wms.Esrimap?REQUEST=GetCapabilities&SERVICE=wms");
			//URL url = new URL("http://gds.rtpnc.epa.gov/wms/rsig.xml");
			//URL url = new URL("http://neowms.sci.gsfc.nasa.gov/wms/wms?service=WMS&request=GetCapabilities");
			
			//parser.parse("NASA Viz Studio", new java.io.FileInputStream(file));
			parser.parse("NASA Viz Studio", url);

			System.out.println(parser.getCapabilities());
			
			ArrayList<Layer> layersList = parser.getCapabilities().getLayers();
			
			for (Layer layer : layersList) {
				System.out.println(layer);
			}
//			
//			WMS_Capabilities.Layer[] layers = new WMS_Capabilities.Layer[layersList.size()];
//			
//			layersList.toArray(layers);
//			
//			// dump layers
//			for (WMS_Capabilities.Layer layer : layers) {
//				System.out.println(layer);
//			}
			
			// convert to ground overlays
//			GroundOverlayLayer[] ovs = 
//				ParserUtils.WMS_Capabilities2GroundOverlay( layers );
//			
//			for (GroundOverlayLayer groundOverlayLayer : ovs) {
//				System.out.println(groundOverlayLayer);
//			}

			// convert to TiledWMSLayer
//			worldwind.contrib.layers.TiledWMSLayer[] wms = ParserUtils.WMS110_Capabilities2WMSTiledLayer(layers, null);
//			
//			for (worldwind.contrib.layers.TiledWMSLayer twl : wms) {
//				System.out.println(twl);
//			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
}
