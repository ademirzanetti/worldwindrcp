package worldwind.contrib.parsers;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;


import worldwind.contrib.Messages;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.PlacemarkLayer;
import worldwind.contrib.layers.ScreenOverlayLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.SimpleKMLParser.GroundOverlay;
import worldwind.contrib.parsers.SimpleKMLParser.KMLDocument;
import worldwind.contrib.parsers.SimpleKMLParser.PlaceMark;
import worldwind.contrib.parsers.SimpleKMLParser.ScreenOverlay;

/**
 * A class to convert KML/KMZ documents into World Wind layers
 * @author Owner
 *
 */
public class KMLSource 
{
	private static final Logger logger = Logger.getLogger(KMLSource.class);
	
	
	private KMLDocument doc;
	
	/**
	 * 
	 * @param url
	 */
	public KMLSource(URL url) throws Exception {
		download(url);
	}

	public KMLSource(File file, String contentType) throws Exception
	{
		process(file, contentType);
	}

	public KMLSource( String kml) throws Exception
	{
		process("doc.kml", kml);
	}

	public KMLSource(String docName, Element e) throws Exception
	{
		SimpleKMLParser p 	= new SimpleKMLParser();
		this.doc 			= p.parse(docName, e);
		
		logger.debug("KML doc=" + doc);
	}
	
	/*
	 * Download url into WW cache
	 */
	private void download(URL url) throws Exception 
	{
		SimpleHTTPClient client = new SimpleHTTPClient(url);
		
		// download url into WW cache
		final String fileName = url.toString().substring(
				url.toString().lastIndexOf("/") + 1, url.toString().length());
		
		File file = WorldWind.getDataFileCache().newFile(fileName);

		if ( ! file.exists()) {
			// file couldn't be created. Perhaps URL contains a query string?
			// Use a generic name
			file = WorldWind.getDataFileCache().newFile("download-" + System.currentTimeMillis());
			logger.debug(file + " couldn't be created. Using " + file);
		}
		
		logger.debug("Downloading " + url + " to " + file);
		
		client.doGet( new FileOutputStream( file ));
		
		process(file, client.getContentType());
		
		// done w/ file, remove
		file.delete();
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private void process(File file, String contentType) throws Exception
	{
		SimpleKMLParser p 	= new SimpleKMLParser();
		KMLDocument doc 	= null;
		
		// unzip kmz
		if ( contentType.equals(SimpleHTTPClient.CT_KMZ))  
		{
			final String docName = unZip(file);
			if ( docName == null)
				throw new IOException("No KML document within zip " + file);
			
			doc = p.parse(docName, new FileInputStream(WorldWind.getDataFileCache().newFile(docName)));
		}
		else {
			doc = p.parse(file.getName(), new java.io.FileInputStream(file));
		}
		
		this.doc = doc;
		logger.debug("Got KML " + doc);
	}

	/**
	 * 
	 * @param docName
	 * @param kml
	 * @throws Exception
	 */
	private void process ( String docName, String kml) throws Exception 
	{
		SimpleKMLParser p 	= new SimpleKMLParser();
		this.doc = p.parse(docName, new java.io.ByteArrayInputStream(kml.getBytes()));
	}
	
	/*
	 * Unzip file into WW cache dir 
	 */
	private String unZip (File zipFile) throws Exception
	{
		ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));
		ZipEntry ze;
		
		String kmlDoc = null;	// KML doc within zip
		
		while ( (ze = in.getNextEntry()) != null ) 
		{
			if ( ze.getName().indexOf(".kml") != -1) 
				kmlDoc = ze.getName();
			
			final File zeFile = WorldWind.getDataFileCache().newFile(ze.getName());
			
			logger.debug("Unzipping " + ze.getName() + " to " + zeFile);
			
			FileOutputStream fos = new FileOutputStream(zeFile);
			
			byte[] buf = new byte[1024];
			int len;
			
			while ((len = in.read(buf)) > 0) {
				fos.write(buf, 0, len);
			}
			fos.close();
		}
		in.close();
		return kmlDoc;
	}

	/**
	 * get kml document object
	 * @return
	 */
	public KMLDocument getDocument () {
		return doc;
	}
	
	/**
	 * Convert {@link KMLDocument} {@link GroundOverlay} to World Wind {@link TimeLoopGroundOverlay}
	 * @return
	 */
	static public TimeLoopGroundOverlay toTimeLoopGroundOverlay(KMLDocument doc) 
	{
		GroundOverlay[] govs =  doc.groundOverlays;
		
		if ( govs == null || govs.length == 0) return null;
		
		// Cache location
		final String cachePath 		= "Earth/" + Messages.forCachePath(doc.name) + "/";
		
		TimeLoopGroundOverlay aov 	= new TimeLoopGroundOverlay(doc.name);
		
		aov.setDescription(doc.description);
		
		// the 1st screen overlay is assumed to be a legend
		if ( doc.screenOverlays != null && doc.screenOverlays.length == 1) 
		{
			final ScreenOverlay so = doc.screenOverlays[0];
			
			ScreenOverlayLayer legend = ( so.icon != null)
				? new ScreenOverlayLayer(so.name, so.icon, so.position)
				: new ScreenOverlayLayer(so.name, so.iconPath, so.position);

			// scale legend?
			if ( so.sizeX != null 
					&& so.sizeY != null 
					&& so.sizeX.equals("0") 
					&& so.sizeY.equals("0") )
				legend.setResizeBehavior(ScreenOverlayLayer.RESIZE_KEEP_FIXED_SIZE);
			
			aov.setLegend(legend);
		}
			
		
		logger.debug("Loop overlay " + doc.name + " " + govs.length + " frames");
		
		for (GroundOverlay go : govs) {
			final Sector sector = new Sector(
					Angle.fromDegrees(go.bbox.south)
					, Angle.fromDegrees(go.bbox.north)
					, Angle.fromDegrees(go.bbox.west)
					, Angle.fromDegrees(go.bbox.east));
			
			final GroundOverlayLayer overlay =  
				  new GroundOverlayLayer(
					go.name
					, sector
					, go.icon 
					, ParserUtils.getIconSuffix(go.icon.toString())
					);
			
			overlay.setBaseCachePath(cachePath);
			
			aov.add(overlay);
		}
		
		return aov;
	}

	/**
	 * Convert a KML {@link GroundOverlay} object to World Wind {@link GroundOverlayLayer}
	 * @return {@link GroundOverlayLayer}
	 */
	static public GroundOverlayLayer toGroundOverlayLayer(GroundOverlay go) 
	{
		logger.debug("GroundOverlay: " + go);
		
		final Sector sector = new Sector(
				Angle.fromDegrees(go.bbox.south)
				, Angle.fromDegrees(go.bbox.north)
				, Angle.fromDegrees(go.bbox.west)
				, Angle.fromDegrees(go.bbox.east));
		
		GroundOverlayLayer layer =  
			 new GroundOverlayLayer(
				go.name
				, sector
				, go.icon
				, ParserUtils.getIconSuffix(go.icon.toString())
				);

		return layer;
	}
	
	/**
	 * Convert {@link PlaceMark} to World Wind {@link PlacemarkLayer}
	 * @return
	 */
	private PlacemarkLayer toPlaceMarks() 
	{
		PlaceMark[] placemarks 	= doc.placemarks;
		
		if ( placemarks == null || placemarks.length == 0) 
			return null;
		
		PlacemarkLayer layer = new PlacemarkLayer();
		
		for (PlaceMark placeMark : placemarks) 
		{
			final Position position = 
				new Position(Angle.fromDegrees(placeMark.lat)
						, Angle.fromDegrees(placeMark.lon)
						, 0.0);
				
			layer.addDefaultIcon(placeMark.name	, position);
		}
		return layer;
	}
	
	/**
	 * Convert {@link ScreenOverlay}s into WW {@link ScreenOverlayLayer}s
	 * @return array of {@link ScreenOverlayLayer}
	 */
	private ScreenOverlayLayer[] toScreenOverlays()
	{
		ScreenOverlay[] overlays = doc.screenOverlays;
		
		if ( overlays == null || overlays.length == 0) return null;
		
		ScreenOverlayLayer[] layers = new ScreenOverlayLayer[overlays.length];
		
		for (int i = 0; i < layers.length; i++) 
		{
			final ScreenOverlay overlay = overlays[i];
			
			final ScreenOverlayLayer sol = (overlay.icon != null ) 
					? new ScreenOverlayLayer(overlay.name
							, overlay.icon
							, overlay.position )
					: new ScreenOverlayLayer(overlay.name
							, overlay.iconPath
							, overlay.position);
					
			// scale overlay?	
			if ( overlay.sizeX != null 
					&& overlay.sizeY != null 
					&& overlay.sizeX.equals("0") 
					&& overlay.sizeY.equals("0")
				)
				sol.setResizeBehavior(ScreenOverlayLayer.RESIZE_KEEP_FIXED_SIZE);
			
			layers[i] = sol;
		}
		return layers;
	}
	
	/**
	 * Get KML source as a World Wind {@link LayerList}
	 * @return WW {@link LayerList}
	 */
	public LayerList toLayerList() {
		LayerList list = new LayerList();
		
		// Add ground Overlays
		if ( doc.groundOverlays != null ) {
			if ( doc.groundOverlays.length == 1) {
				// Ground overlay
				list.add(toGroundOverlayLayer(doc.groundOverlays[0]));
				
			}
			else {
				// Animated ground overlays
				list.add(toTimeLoopGroundOverlay(doc));
			}
		}
		
		PlacemarkLayer pmLayer 			= toPlaceMarks();
		ScreenOverlayLayer[] screenOvs 	= toScreenOverlays();

		// Add screen overlays
		if ( screenOvs != null && screenOvs.length > 1) {
			for (ScreenOverlayLayer screenOverlay : screenOvs) {
				list.add(screenOverlay);
			}
		}
		
		// Add placemarks
		if ( pmLayer != null ) list.add(pmLayer);
		
		return list;
	}

	public boolean hasScreenOverlays() {
		return doc.screenOverlays != null;
	}

	public boolean hasGroundOverlays() {
		return doc.groundOverlays != null;
	}

	public boolean hasPlaceMarks() {
		return doc.placemarks != null;
	}
	
	public int groundOverlaysSize () {
		if ( doc.groundOverlays == null) return -1;
		return doc.groundOverlays.length;
	}
	
	public String toKML() 
	{
		StringBuffer buf = new StringBuffer("<Document>" + Messages.NL);
		buf.append("<name>" + doc.name + "</name>" + Messages.NL);
		buf.append("<description>" + doc.name + "</description>" + Messages.NL);

		LayerList list = toLayerList();
		 
		for (Layer layer : list) {
			if ( layer instanceof GroundOverlayLayer )
				buf.append( ((GroundOverlayLayer)layer).toKML() + Messages.NL);
			else if ( layer instanceof TimeLoopGroundOverlay )
				buf.append( ((TimeLoopGroundOverlay)layer).toKML() + Messages.NL);
		}
		buf.append("</Document>");
		
		return buf.toString();
	}
	
	// test only
	public static void main(String[] args) {
		try {
			//String url = "http://services.google.com/earth/kmz/cumbria_waymarking_n.kmz";
			//String url = "http://gds.rtpnc.epa.gov:9090/geo/wms?request=WMS2KML&tmin_idx=0&tmax_idx=5&layer=3169_21478";
			//String file = "src/demo/xml/KML_Samples.kml";
			//String file = "C:/Documents and Settings/All Users/Application Data/WorldWindData/rsig2dviz_16162_.kml";
			//String file = "src/demo/xml/rsig2dviz-1.kmz";
			String file = "c:/tmp/CCTM_J3a_b313.12km.200109.kml";
			//KMLSource kml = new KMLSource(new URL(url));
			
			KMLSource kml = new KMLSource(new File(file), SimpleHTTPClient.CT_KML);// .CT_KML);
			
			System.out.println(KMLSource.toTimeLoopGroundOverlay(kml.getDocument()).toKML());
			
			//System.out.println(kml.toKML());
//			LayerList list = kml.toLayerList();
//			
//			for (Layer layer : list) {
//				if ( layer instanceof TimeLoopGroundOverlay ){
//					System.out.println(((TimeLoopGroundOverlay)layer).toKML());					
//				}
//				else if ( layer instanceof GroundOverlayLayer ) {
//					System.out.println(((GroundOverlayLayer)layer).toKML());
//				}
//				else
//					System.out.println(layer);
//			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
