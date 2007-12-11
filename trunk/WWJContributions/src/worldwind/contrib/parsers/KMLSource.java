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
import java.util.zip.ZipOutputStream;

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
 * Sample Usage:
 * 		String file 	= "/path/somefile.xml";
 * 		KMLSource kml 	= new KMLSource(new File(file), SimpleHTTPClient.CT_KML);
 * 		LayerList list 	= kml.toLayerList();
 * 
 * 		for (Layer layer : list) {
 * 			System.out.println(layer));
 * 		}
 * @author Owner
 *
 */
public class KMLSource 
{
	private static final Logger logger = Logger.getLogger(KMLSource.class);
	
	
	private KMLDocument doc;
	
	/**
	 * Constructor 
	 * @param url URL of the KML file
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
	 * Process the KML/KMZ. Parse the document into an object of type {@link KMLDocument}
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
	 * Parse the document into an object of type {@link KMLDocument}
	 * @param docName Document name
	 * @param kml KML string
	 * @throws Exception
	 */
	private void process ( String docName, String kml) throws Exception 
	{
		SimpleKMLParser p 	= new SimpleKMLParser();
		this.doc = p.parse(docName, new java.io.ByteArrayInputStream(kml.getBytes()));
	}
	
	/**
	 * Unzip a file into WW cache
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
	 * Get the {@link KMLDocument} object
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
		// frames
		GroundOverlay[] govs 		=  doc.groundOverlays;
		
		// legend (optional)
		ScreenOverlayLayer legend 	= null;
		
		if ( govs == null || govs.length == 0) return null;
		
		// Cache location
		final String cachePath 		= "Earth/" + Messages.forCachePath(doc.name) + "/";
		
		TimeLoopGroundOverlay aov 	= new TimeLoopGroundOverlay(doc.name);
		
		aov.setDescription(doc.description);
		
		// TimeLoops can only have 1 legend.
		if ( doc.screenOverlays != null )  
		{
			// Get KML screen overlay representing the legend
			final ScreenOverlay so = getLegend(doc.screenOverlays); //  doc.screenOverlays[0];
			
			legend = ( so.icon != null)
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
			
		
		logger.debug("Loop overlay " + doc.name + " " 
				+ govs.length + " frames "
				+ " Legend=" + legend);
		
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
	 * {@link TimeLoopGroundOverlay} can only have 1 legend {@link ScreenOverlayLayer}.
	 * Thus scan the array of screen overlay for the strings: legend, color, or bar
	 * @param overlays array of {@link ScreenOverlayLayer}
	 * @return
	 */
	static private ScreenOverlay getLegend ( ScreenOverlay[] overlays) 
	{
		for (ScreenOverlay overlay : overlays) 
		{
			if ( overlay.name == null) continue;

			final String name = overlay.name.toLowerCase();
			
			if ( name.indexOf("legend") != -1 
					|| name.indexOf("color") != -1 
					|| name.indexOf("bar") != -1)
				return overlay;
		}
		return null;
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
		
		if ( go.description != null )
			layer.setDescription(go.description);
		
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
			if ( doc.groundOverlays.length == 1) 
			{
				// Ground overlay
				GroundOverlayLayer layer= toGroundOverlayLayer(doc.groundOverlays[0]);
				layer.setDescription(doc.description);
				
				list.add(layer);
				
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

	/**
	 * Build a KMZ file for a WW layers: {@link TimeLoopGroundOverlay} or
	 * {@link GroundOverlayLayer}
	 * @param 	kmzFile KMZ {@link File}
	 * @param 	layer World Wind {@link Layer}: {@link TimeLoopGroundOverlay} or
	 * 			{@link GroundOverlayLayer} only
	 * @throws 	Exception
	 */
	static public void buildKMZ (File kmzFile, Layer layer)
		throws Exception
	{
		if ( !(layer instanceof TimeLoopGroundOverlay) &&
				!(layer instanceof GroundOverlayLayer) )
			
			throw new IOException("Invalid layer type:" + layer.getClass().getName());
		
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(kmzFile));
		
		// KML doc within zip
		String kmlDoc;
		
		if (layer instanceof TimeLoopGroundOverlay) 
		{
			// time loop
			final TimeLoopGroundOverlay loop = (TimeLoopGroundOverlay)layer;
			
			// relative paths
			kmlDoc = loop.toKML(false, true);
			
			// add overlay files
			for (GroundOverlayLayer ov : loop.getOverlays()) 
			{
				// file name
				File f 			= new File( ov.getTextureURL().toURI());
				String fName 	= f.getName();
				
				logger.debug("Adding Time Loop Ov to KMZ:" + fName);
				
				// Add Zip entry for this overlay
				out.putNextEntry(new ZipEntry(fName));
				out.write(Messages.readFile(f));
				//writetoStream(new FileInputStream(f), out);
			} 
			
			// Add legend: Legend is optional
			if ( loop.getLegend() != null) {
				File f = loop.getLegend().getFile();
			
				logger.debug("Adding Time Loop LEGEND to KMZ:" + f.getName());
			
				out.putNextEntry(new ZipEntry(f.getName()));
				out.write(Messages.readFile(f));
			}
		}
		else {
			// ground overlay
			final GroundOverlayLayer gov = (GroundOverlayLayer)layer;

			// use relative paths
			kmlDoc = gov.toKML(false, false);

			// Add overlay
			File f 			= new File( gov.getTextureURL().toURI());
			String fName 	= f.getName();

			logger.debug("Adding Ground Overlay to KMZ:" + fName);
			
			out.putNextEntry(new ZipEntry(fName));
			out.write(Messages.readFile(f));
			//writetoStream(new FileInputStream(f), out);
		}
		
		//logger.debug("KML Doc size=" + ( kmlDoc != null ?  kmlDoc.length() : kmlDoc ) );
		//logger.debug("KML Doc " + kmlDoc );
		
		// Add doc kml
		out.putNextEntry(new ZipEntry("doc.kml"));
		out.write(kmlDoc.getBytes());
		
		out.close();
	}
	
/* 
	// test only
	public static void main(String[] args) {
		try {
			//String url = "http://services.google.com/earth/kmz/cumbria_waymarking_n.kmz";
			//String url = "http://gds.rtpnc.epa.gov:9090/geo/wms?request=WMS2KML&tmin_idx=0&tmax_idx=5&layer=3169_21478";
			//String file = "src/demo/xml/KML_Samples.kml";
			//String file = "src/demo/xml/rsig2dviz-1.kmz";
			//String file = "c:/tmp/CCTM_J3a_b313.12km.200109.kml";
			String file = "src/demo/xml/nexrad.kmz";
//			KMLSource kml = new KMLSource(new URL(url));
	
			
			KMLSource kml = new KMLSource(new File(file), SimpleHTTPClient.CT_KMZ);
			
			TimeLoopGroundOverlay overlay = KMLSource.toTimeLoopGroundOverlay(kml.getDocument());
			LayerList list = kml.toLayerList();
			
			System.out.println("List size=" + list.size());
			
			for (Layer layer : list) {
				if ( layer instanceof GroundOverlayLayer) {
					GroundOverlayLayer gol =  (GroundOverlayLayer)layer;
					System.out.println("name=" + gol.getName() + " desc=" + gol.getDescription());
				}
				else
					System.out.println("name=" + layer );
			}
			//System.out.println("kml=" + kml + " ov=" + overlay.getDescription());
			
			
	        //buildKMZ(new File("c:/tmp/junk/moo.kmz"), overlay);
	        
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
	*/
}
