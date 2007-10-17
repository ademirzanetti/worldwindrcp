package worldwind.contrib.parsers;

import gov.nasa.worldwind.WorldWind;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Very simple KML parser.
 * Supports <GroundOverlay> , <ScreenOverlay> , and <Placemark> tags only
 * It is capable of extracting the doc name and description also.
 * @author vsilva
 *
 */
public class SimpleKMLParser 
{
	private static final Logger logger = Logger.getLogger(SimpleKMLParser.class);
	
	public static class KMLDocument 
	{
		String name;
		String description;
		GroundOverlay[] groundOverlays;
		ScreenOverlay[] screenOverlays;
		PlaceMark[] placemarks;
		
		public KMLDocument(String name) {
			this.name = name;
		}

		public String getName() { return name;}
		public String getDescription() { return description;}
		
		public GroundOverlay[] getGroundOverlays() {
			return groundOverlays;
		}

		public ScreenOverlay[] getScreenOverlays() {
			return screenOverlays;
		}

		public PlaceMark[] getPlaceMarks() {
			return placemarks;
		}
		
		@Override
		public String toString() 
		{
			StringBuffer buff = new StringBuffer("Document name: " + name + "\n"
					+ "Description:" + description + "\n");
			
			if ( groundOverlays != null ) {
				for (int i = 0; i < groundOverlays.length; i++) {
					buff.append("GOverlay: " + groundOverlays[i] + "\n");
				}
			}
			if ( screenOverlays != null) {
				for (int i = 0; i < screenOverlays.length; i++) {
					buff.append("SOverlay: " + screenOverlays[i] + "\n");
				}
			}
			if ( placemarks != null ) {
				for (int i = 0; i < placemarks.length; i++) {
					buff.append("PM: " + placemarks[i] + "\n");
				}
			}
			return buff.toString();
		}
	}
	
	public static class GroundOverlay 
	{
		LatLonBox bbox;
		TimeSpan time;
		String name;
		String color;
		URL icon;
//		String iconPath;
		
		public GroundOverlay(String name , LatLonBox bbox , URL icon
				, TimeSpan time,  String color) {
			this.name = name;
			this.color = color;
			this.icon = icon;
			this.bbox = bbox;
			this.time = time;
		}

//		public GroundOverlay(String name , LatLonBox bbox , String iconPath
//				, TimeSpan time,  String color) {
//			this.name = name;
//			this.color = color;
//			this.iconPath = iconPath;
//			this.bbox = bbox;
//			this.time = time;
//		}
		
		public String toString() {
			return name + "," + bbox + ", icon=" 
				+ icon //( icon != null  ? icon : iconPath ) 
				+ "," + time + ", color=" + color;
		}
/*
		public String toKmlFragment() {
			return "<GroundOverlay>" + Messages.NL 
				+ "<name>" + name + "</name>" + Messages.NL
				//+ "<description>" + name + "</description>" + Messages.NL
				+ "<Icon><href><![CDATA[" + icon + "]]></href></Icon>" + Messages.NL
				+ "<LatLonBox><north>" + bbox.north + "</north>"
				+ "<south>" + bbox.south + "</south>"
				+ "<east>" + bbox.east + "</east>"
				+ "<west>" + bbox.west + "</west><LatLonBox>" + Messages.NL
				+ "</GroundOverlay>" + Messages.NL;
		}
		*/
	}

	static class LatLonBox 
	{
		double north, south, east, west;
		public LatLonBox(double north, double south, double east, double west) {
			this.north = north;
			this.south = south;
			this.east = east;
			this.west = west;
		}
		@Override
		public String toString() {
			return "bbox:(n,s,e,w)=" + north + "," + south + "," + east + "," + west;
		}
	}

	static class TimeSpan 
	{
		String begin, end;
		public TimeSpan ( String begin, String end) {
			this.begin = begin;
			this.end = end;
		}
		@Override
		public String toString() {
			return "t=[" + begin + "," + end +"]";
		}
	}
	
	public static class ScreenOverlay 
	{
	    public final static String NORTHWEST = "gov.nasa.worldwind.CompassLayer.NorthWest";
	    public final static String SOUTHWEST = "gov.nasa.worldwind.CompassLayer.SouthWest";
	    public final static String NORTHEAST = "gov.nasa.worldwind.CompassLayer.NorthEast";
	    public final static String SOUTHEAST = "gov.nasa.worldwind.CompassLayer.SouthEast";
		
		String name;
		String color;
		URL icon;
		String iconPath;
		String position;
		
		// scaling
		String sizeX, sizeY;
		
		public ScreenOverlay(String name , URL icon, String color, String position) {
			this.name = name;
			this.color = color;
			this.icon = icon;
			this.position = position;
		}

		public ScreenOverlay(String name , String iconPath, String color, String position) {
			this.name = name;
			this.color = color;
			this.iconPath = iconPath;
			this.position = position;
		}
		
		public String toString() {
			return name + ", icon=" 
			+ ( icon != null  ? icon : iconPath ) 
			+ "," + color + "," + position + " SizeXY=" + sizeX + "," + sizeY;
		}
	}
	
	public static class PlaceMark 
	{
		String name, description;
		double lon, lat, range, tilt, heading;
		
		@Override
		public String toString() {
			return name + "," + description + ", lookat(lon,lat,range,tilt,head):" 
				+ lon + "," + lat + "," + range + "," + tilt + "," + heading;
		}
	}
	
	public SimpleKMLParser()
	{
	}
	
	/**
	 * 
	 * @param nl
	 * @return
	 * @throws MalformedURLException
	 */
	private GroundOverlay[] parseGroundOverlays(NodeList nl)
		throws IOException
	{
		if ( nl.getLength() == 0 ) return null;
		
		GroundOverlay[] overlays = new GroundOverlay[nl.getLength()];

		for (int i = 0; i < nl.getLength(); i++) {
			final Element e = (Element)nl.item(i);
			
			final String name = ParserUtils.getNodeValue(e, "name");
			final String color = ParserUtils.getNodeValue(e, "color");
			final String icon = ParserUtils.getNodeValue(e, "href");

			if ( icon == null)
				throw new IOException("Invalid icon for Ground Overlay " + name);
			
			LatLonBox bbox;
			
			try {
				bbox = new LatLonBox(
						Double.parseDouble(ParserUtils.getNodeValue(e, "north"))
						, Double.parseDouble(ParserUtils.getNodeValue(e, "south"))
						, Double.parseDouble(ParserUtils.getNodeValue(e, "east"))
						, Double.parseDouble(ParserUtils.getNodeValue(e, "west")) );
			} 
			catch (Exception ex1) {
				throw new IOException("Invalid lat/lon box: " + ex1.getMessage());
			}
			
			final TimeSpan time = new TimeSpan(ParserUtils.getNodeValue(e, "begin")
					, ParserUtils.getNodeValue(e, "end"));
	
			logger.debug("GroundOverlay: " + name + ","  + bbox + "," + icon + "," + time);
			
			try {
				overlays[i] = new GroundOverlay(name, bbox, new URL(icon), time, color);
			} 
			catch (MalformedURLException ex) 
			{
				// Is icon local? ( file:/... or just a file name)?
				// Look 4 icon in file system
				final File file = new File (icon);
				
				logger.debug("Invalid icon url " + icon + ":" 
						+ ex.getMessage() + ". Looking in file system as " + file);
				
				if ( file.exists())
					overlays[i] = new GroundOverlay(name, bbox, file.toURL() , time, color); //icon
				else {
					final URL url = WorldWind.getDataFileCache().findFile(icon, false);					
					logger.debug("File " + file + " not found. Looking in WW cache for " + url);
					
					if ( url == null) {
						logger.debug("Icon " + icon + " not found in WW cache. Giving up.");
						throw new IOException("Invalid ground overlay icon " 
								+ icon 
								+ ": Not a URL, file, or WW cache file.");
					}
					else
						overlays[i] = new GroundOverlay(name, bbox, url , time, color); 
				}
			}
		}
		return overlays;
	}

	/**
	 * 
	 * @param nl
	 * @return
	 * @throws MalformedURLException
	 */
	private ScreenOverlay[] parseScreenOverlays(NodeList nl)
		throws IOException
	{
		if ( nl.getLength() == 0 ) return null;
		
		ScreenOverlay[] overlays = new ScreenOverlay[nl.getLength()];

		for (int i = 0; i < nl.getLength(); i++) {
			final Element e = (Element)nl.item(i);
			
			final String name = ParserUtils.getNodeValue(e, "name");
			final String color = ParserUtils.getNodeValue(e, "color");
			final String icon = ParserUtils.getNodeValue(e, "href");
			
			if ( icon == null)
				throw new IOException("Invalid icon for Screen Overlay " + name);
			
			String position = ScreenOverlay.SOUTHWEST;
			try {
				final String x = ParserUtils.getAttributeValue((Element)e.getElementsByTagName("screenXY").item(0), "x");
				final String y = ParserUtils.getAttributeValue((Element)e.getElementsByTagName("screenXY").item(0), "y");
				
				
				if ( x.equals("0") && y.equals("1")) position = ScreenOverlay.NORTHWEST;
				if ( x.equals("1") && y.equals("0")) position = ScreenOverlay.SOUTHEAST;
				if ( x.equals("1") && y.equals("1")) position = ScreenOverlay.NORTHEAST;
				

				
			} catch (Exception ex1) {
				position = ScreenOverlay.NORTHWEST;
			}
			
			logger.debug("ScreenOverlay " + name + " icon=" + icon  + " pos=" + position);
			
			try {
				// icon could be a file name in which case a MalforedURL Ex will be thrown
				overlays[i] = new ScreenOverlay(name, new URL(icon), color, position);
			} 
			catch (MalformedURLException ex) 
			{
				// Use path instead
				overlays[i] = new ScreenOverlay(name, icon, color, position);
			} 
			finally 
			{
				// scale
				try {
					overlays[i].sizeX = ParserUtils.getAttributeValue((Element)e.getElementsByTagName("size").item(0), "x");
					overlays[i].sizeY = ParserUtils.getAttributeValue((Element)e.getElementsByTagName("size").item(0), "y");
					
				} catch (Exception ex1) {
					logger.error("Error extracting scale from screen overlay " 
							+ overlays[i] + " " + ex1);
				}
			}
		}
		return overlays;
	}

	/**
	 * Parse placemarks from ml doc
	 * @param nl Node List of <Placemark> tags
	 * @return Array of {@link PlaceMark}
	 * @throws IOException on I/O error
	 */
	private PlaceMark[] parsePlaceMarks(NodeList nl) 
		throws IOException
	{
		if ( nl.getLength() == 0 ) return null;
		
		Vector<PlaceMark> placemarks = new Vector<PlaceMark>();
		
		for (int i = 0; i < nl.getLength(); i++) {
			final Element e = (Element)nl.item(i);
			
			final String name = ParserUtils.getNodeValue(e, "name");
			final String description = ParserUtils.getNodeValue(e, "description");

			PlaceMark pm = new PlaceMark();
			pm.name = name;
			pm.description = description;
			
			// try optional: <longitude>, <latitude>
			try {
				final double lon = Double.parseDouble(ParserUtils.getNodeValue(e, "longitude"));
				final double lat = Double.parseDouble(ParserUtils.getNodeValue(e, "latitude"));
				pm.lon = lon;
				pm.lat = lat;
			} 
			catch (Exception ex) {
				// Try <coordinates>lon,lat</coordinates>
				try {
					final String[] coords = ParserUtils.getNodeValue(e, "coordinates").split(",");
					pm.lon = Double.parseDouble(coords[0]);
					pm.lat = Double.parseDouble(coords[1]);
				} catch (Exception ex1) {
					// ignore placemark
					logger.warn("Ingoring placemark " + name + " (no lat,lon values).");
					continue;
					//throw new IOException(Messages.getText("err.kml.latlon", new Object[] {name}));
				}
			}
			
			// optional
			try {
				final double range = Double.parseDouble(ParserUtils.getNodeValue(e, "range"));
				final double tilt = Double.parseDouble(ParserUtils.getNodeValue(e, "tilt"));
				final double heading = Double.parseDouble(ParserUtils.getNodeValue(e, "heading"));
				
				pm.range = range;
				pm.tilt = tilt;
				pm.heading = heading;
			} catch (Exception ex) {	}
			
			placemarks.add(pm);
		}
		
		PlaceMark[] pms = new PlaceMark[placemarks.size()];
		placemarks.toArray(pms);
		
		return pms;
	}
	
	/**
	 * 
	 * @return
	 */
	public KMLDocument parse(String docName, InputStream is)
		throws MalformedURLException, IOException, SAXException, ParserConfigurationException
	{
		Document doc = ParserUtils.parse(is);
/*		
		// KML Doc 
		NodeList nl = doc.getElementsByTagName("Document");
		
		if ( nl.getLength() > 0) {
			docName = ParserUtils.getNodeValue((Element)nl.item(0), "name");
		}
			
		KMLDocument kmlDoc = new KMLDocument(docName);
		
		kmlDoc.description	  = ( nl.getLength() > 0) 
				? ParserUtils.getNodeValue((Element)nl.item(0), "description").trim() 
				: null;
		kmlDoc.groundOverlays = parseGroundOverlays(doc.getElementsByTagName("GroundOverlay"));
		kmlDoc.screenOverlays = parseScreenOverlays(doc.getElementsByTagName("ScreenOverlay"));
		kmlDoc.placemarks	  = parsePlaceMarks(doc.getElementsByTagName("Placemark"));
		
		validate(kmlDoc);
		
		return kmlDoc;
*/
		return parse(docName, doc.getDocumentElement());
	}
	
	/**
	 * 
	 * @param docName
	 * @param e
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public KMLDocument parse(String docName, Element e)
		throws MalformedURLException, IOException, SAXException, ParserConfigurationException
	{
		final String name = ParserUtils.getNodeValue((Element)e, "name");
		
		if ( name != null && name.length() > 0) 
			docName = name;
		
		KMLDocument kmlDoc = new KMLDocument(docName);

		kmlDoc.description	  = ParserUtils.getTextContent(e, "description").trim(); 
		kmlDoc.groundOverlays = parseGroundOverlays(e.getElementsByTagName("GroundOverlay"));
		kmlDoc.screenOverlays = parseScreenOverlays(e.getElementsByTagName("ScreenOverlay"));
		kmlDoc.placemarks	  = parsePlaceMarks(e.getElementsByTagName("Placemark"));
		
		validate(kmlDoc);
		
		return kmlDoc;
	}
	
	private void validate (KMLDocument doc) throws IOException
	{
		if ( doc.groundOverlays == null && doc.screenOverlays == null
				&& doc.placemarks == null)
			throw new IOException("Invalid document: " + doc.name 
					+ ". Document contains no <GroundOverlay> <ScreenOverlay> or <Placemark>.");
	}
}
