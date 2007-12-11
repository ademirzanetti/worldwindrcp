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
package worldwind.contrib.layers;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;
import org.apache.log4j.Logger;

import worldwind.contrib.Messages;
import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.WMS_Capabilities;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.cache.TextureCache;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

/**
 * A ground overlay layer, based on the GroundOverlay concept from KML. 
 * Renders a URL representing an surface image on a given lat/lon box (Sector).
 * This layer is used to convert KML's GroundOverlays into WW layers
 * @author Vladimir Silva
 *
 */
public class GroundOverlayLayer extends AbstractLayer 
{
	private static final Logger logger = Logger.getLogger(GroundOverlayLayer.class);

	private String description;
	private TextureTile tile = null;
	
	private String baseCachePath;
	
	// Unique ID (used to cache tile in memory & disk)
	private String tileKey;	
	
	// Texture can be a URL or a file path
	private URL textureURL;
	
	private Sector sector;
	
	// Web Download read timeout
	private boolean loading = false;
	
	private String formatName;
	private String fileSuffix;
	
	// Animation loop status listeners
    private Vector<GroundOverlayListener> listeners = new Vector<GroundOverlayListener>();
	
	/** Ground overlay status messages */
	static public interface GroundOverlayListener
	{
		public void onError ( GroundOverlayLayer layer, Exception ex);
	}
	
	/**
	 * Build a {@link GroundOverlayLayer}
	 * @param description Name of this ground overlay
	 * @param sector Lat/lon box for the overlay
	 * @param textureURL URL of the overlay image
	 * @param fileSuffix Image extension used to download and possible scale texture (e.g .png, .gif, etc)
	 */
	public GroundOverlayLayer (String name, Sector sector, URL textureURL, String fileSuffix) 
	{
		if ( name == null || sector == null)
			throw new IllegalArgumentException("Invalid layer name or sector");
		
        this.tile 		= new TextureTile(sector);
		this.textureURL = textureURL;
		this.sector 	= sector;
		this.fileSuffix = fileSuffix;
		this.baseCachePath = "Earth/";
	
		setName(name);
		
        tileKey	= buildTileKey(); 
		
		// file suffix (used to load texture)
		this.formatName = (fileSuffix != null ) 
			? fileSuffix.replaceAll("\\.", "").toUpperCase() 
			: null;
		
		initCache();
		
		logger.debug("Name=" + name + ", s=" + sector 
				+ " url=" + textureURL + " fs=" + fileSuffix 
				+ " fmt=" + formatName + " tile key=" + tileKey);
	}

	private void initCache () {
        if (!WorldWind.getMemoryCacheSet().containsCache(GroundOverlayLayer.class.getName()))
        {
            long size = Configuration.getLongValue(AVKey.TEXTURE_IMAGE_CACHE_SIZE, 4000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.95 * size), size);
            cache.setName("Texture Tiles");
            WorldWind.getMemoryCacheSet().addCache(GroundOverlayLayer.class.getName(), cache);
        }
	}
	
	private String buildTileKey ()
	{
		return baseCachePath +  Messages.forCachePath(getName()) + fileSuffix;
	}

	/**
	 * Add an overlay status listener
	 * @param listener
	 */
	public void addOverlayListener( GroundOverlayListener listener){
		listeners.add(listener);
	}

	public void removeOverlayListener( GroundOverlayListener listener){
		listeners.remove(listener);
	}
	
	@Override
	/**
	 * Layers draw themselves
	 */
    protected final void doRender(DrawContext dc)
    {
		logger.debug("----> START doRender:" + getName() + " loading tx=" + loading);
		
        if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return; 

        /*
         * If the texture is being loaded show a "load" PMG and return
         */
        if ( loading ) {
        	showLoadingTexture(dc);
			return ;
        }
        
        // If not in memory
        if ( ! isTileInMemory())  
        {
        	logger.debug("Texture " + tileKey + " not in memory. loading from disk");
        	
        	// Load from disk
        	URL url = WorldWind.getDataFileCache().findFile(tileKey, false);

        	if ( url != null  ) 
        	{
        		logger.debug("Loading texture from disk: " + url );
        		loadTexture(dc.getTextureCache(), url); 
        	}
        	else {
        		// not in disk, load from remote URL
        		logger.debug("Tile not in disk. Loading  from url " 
        				+ textureURL  
        				+ " key=" + tileKey); 
        
        		if ( ! fetchOverlay() ) {
        			logger.error("Synch fetch for " + textureURL + " FAILED");
        			//onError(this, new IOException("Synch fetch for " + textureURL + " FAILED"));
        			return;
        		}
        	}
        }
        else {
        	tile = getTileFromMemoryCache(); 
        	logger.debug("Loading tile from memory " + tileKey + " tile=" + tile);
        }
        
        if ( ! loading) {
        	logger.debug("Redering tile=" + tile + " opacity=" + getOpacity());
        	dc.getGeographicSurfaceTileRenderer().renderTile(dc, tile, getOpacity());
        }
        else
        	showLoadingTexture(dc);
        
        logger.debug("----> END doRender:" + getName() );
    }

	/*
	 * Return a "loading.png" texture when
	 */
	private void showLoadingTexture(DrawContext dc)  
	{
		try {
			TextureTile tile = new TextureTile(getSector());
			tile.setTextureData(TextureIO.newTextureData(
						Messages.getInputStream(GroundOverlayLayer.class, "loading.png")
						, false
						, TextureIO.PNG));
			
			dc.getGeographicSurfaceTileRenderer().renderTile(dc
					, tile 
					, getOpacity());
			
		} catch (Exception e) {
		}
	}
	
	
	/*
	 * Fetch ground overlay from a URL: http:/ or file:/
	 */
    private synchronized boolean loadTexture(TextureCache tc, URL textureURL) 
    {
//        TextureData textureData = null;
    	Texture texture = null;
    	
        try {
        	// load from URL
        	logger.debug("Fetching texture from URL " + textureURL);
        	//textureData = TextureIO.newTextureData(textureURL, true, null);
        	texture = TextureIO.newTexture(textureURL, true, null);
        }
        catch (Exception e) {
        	// notify listeners of error
        	//onError(this, e);
        	return false;
        }
       
//        Texture texture = TextureIO.newTexture(textureData);
//
//        logger.debug("Got texture: " 
//        		+ tileKey 
//        		+ " TexData (w,h)=" + textureData.getWidth() + "," + textureData.getHeight()
//        		+ " Texture w=" + texture.getWidth() + " h=" + texture.getHeight() 
//        		+ " from " + textureURL //( textureURL != null ? textureURL : textureFile ) 
//        		+ " Est mem size:" + textureData.getEstimatedMemorySize());
        
        /*
         * Texture/TextureData w/h mismatch bug: sometimes W/H don't match
         * between Texture & TextureData. This causes a messed up img to display.
         * A solution? ... resize img to 1024x1024
         */
//        if ( textureData.getWidth() != texture.getWidth() 
//        		|| textureData.getHeight() != texture.getHeight() )
//        {
//        	try 
//        	{
//            	final URL url = WorldWind.getDataFileCache().findFile(tileKey, false);
//            	
//            	// texture exists?
//            	File file = ( url == null ) 
//            		? WorldWind.getDataFileCache().newFile(tileKey)
//            		: new File(url.toURI())	;
//            	
//            	logger.error("Texture/TextureData w/h don't match. Saving to disk & resizing " 
//            			+ file + " file exists " + file.exists());
//        		
//        		// Fetch manually & store on disk
//            	if ( !file.exists())
//            		downloadResource(textureURL, file);
//        		
//            	// scale file from disk to 1024x1024 (preserving alpha)
//            	scaleImage(file, formatName, 1024, 1024);
//            	
//            	// reload texture
//            	texture = TextureIO.newTexture(file, true);
//        	} 
//        	catch (Exception e) {
//        		logger.error(e);
//			}
//        }
        
        tile.setTexture(tc, texture);
        
        this.addTileToMemoryCache();

        return true;
    }

    /**
     * Download a remote resource
     * @param resourceURL remote url
     * @param outFile file to save the resource to
     */
    private void downloadResource(URL resourceURL, final File outFile) throws Exception 
    {
    	try {
    		loading = true;
    		
			SimpleHTTPClient client = new SimpleHTTPClient(resourceURL);
			client.doGet(new FileOutputStream(outFile));
			
        	final String contentType 	= client.getContentType();
        	final int respCode			= client.getStatus();
        	String errorMessage 		= null;

            logger.debug("Web download for " + resourceURL 
            		+ " status=" + respCode 
            		+ " ct=" + client.getContentType());
        	
        	// check response content type (http status 200)
        	if ( contentType != null && contentType.indexOf("image") == -1 ) 
        	{
        		// Invalid CT (not an image)
        		final String buf =  new String(Messages.readFile(outFile));
        		
        		logger.debug("Invalid resp content type " + contentType + " buffer " + buf);
        		
        		// XML error response 
        		if ( contentType.equalsIgnoreCase("application/vnd.ogc.se_xml")) {
        			// WMS XML response?...extract message from: 
        			// <ServiceException>MESSAGE</ServiceException>
        			final String xml 	= buf;
        			errorMessage			= ( xml != null && xml.indexOf("<ServiceException>") != -1) 
        				? xml.substring(xml.indexOf("<ServiceException>") + 18, xml.indexOf("</ServiceException>"))
        				: xml;
        		}
        		else {
        			// just set the err msg to whatever is on the byte buffer
        			errorMessage = buf;
        		}
        	}
        	
        	if ( errorMessage != null ) 
                throw new IOException("Download failed: " + errorMessage );
        	
        	client.close();
		} 
    	catch ( Exception e) 
    	{
    		// remove file from disk
            if ( outFile != null && outFile.exists()) 
            {
            	logger.error("Download failed" 
            			+ e.getMessage() 
            			+ ". Deleting cache file " + outFile);
            	
            	outFile.delete();
            }
            throw new Exception(e);
    	}
    	finally {
    		loading = false;
    	}
    }
    
    
    private void addTileToMemoryCache()   
    {
        if (this.getTileFromMemoryCache() == null ) { 
        	logger.debug("Adding tile " + tile + " key=" + tileKey + " to memory cache.");
            WorldWind.getMemoryCache(GroundOverlayLayer.class.getName()).add(tileKey, tile);
        }
    }

    private TextureTile getTileFromMemoryCache() 
    {
    	TextureTile tile = (TextureTile) WorldWind.getMemoryCache(GroundOverlayLayer.class.getName()).getObject(tileKey); //tileKey);
    	logger.debug("Got tile from memory " + tile + " key=" + tileKey);
    	return tile;
    }
    
    public boolean isTileInMemory () {
    	boolean bool = WorldWind.getMemoryCache(GroundOverlayLayer.class.getName()).contains(tileKey);
    	logger.debug("Tile in memory? " + bool);
    	return bool;
    }
    
    public boolean isTileInCache () {
    	boolean bool = WorldWind.getDataFileCache().findFile(tileKey, false) != null;
    	logger.debug("Tile in cache? " + bool);
    	return bool;
    }

    public Texture getTexture(TextureCache tc)
    {
        if (tc == null)
        {
            String message = Logging.getMessage("nullValue.TextureCacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        return tc.get(tile.getTileKey());
    }
    

    @Override
    public String toString()
    {
    	return getName();
    }

	/**
	 * @param description the description to set
	 */
	public void setDescription(String title) {
		this.description = title;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	public void setTextureURL(URL textureURL) {
		this.textureURL = textureURL;
	}

	public URL getTextureURL() {
		return textureURL;
	}

	
	/**
	 * Utility to scale an image preserving alpha transparency
	 */
	static void scaleImage(File src, String formatName, int newW, int newH) {
		try {
			// Anything besides PNG will create a NULL image????... when setting transp
			if ( !formatName.equalsIgnoreCase("png"))
				formatName = "PNG";
			
			logger.debug("Src file: " + src + " format " + formatName + " WxH=" + newW + "," + newH);

			BufferedImage img1 = ImageIO.read(src);
			int w = img1.getWidth();   
		    int h = img1.getHeight();   
		    
			BufferedImage img2 =
			      new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g = img2.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);   
	        g.drawImage(img1, 0, 0, newW, newH, 0, 0, w, h, null);   
			
			g.dispose();
			
			ImageIO.write(img2 , formatName , src);
			
		} 
		catch (IOException e) {
			//e.printStackTrace();
			logger.error(e);
		}
	}

	/**
	 * Erase a color from a {@link BufferedImage}
	 * @param source
	 * @param color
	 * @return
	 */
//	static BufferedImage eraseColor(BufferedImage source, Color color)
//    {
//        int w 		= source.getWidth();
//        int h 		= source.getHeight();
//        int type 	= BufferedImage.TYPE_INT_ARGB;
//        
//        BufferedImage out = new BufferedImage(w, h, type);
//        Graphics2D g2 = out.createGraphics();
//
//        g2.setComposite(AlphaComposite.Src);   
//        g2.drawImage(source, null, 0, 0);   
//        g2.dispose();   
//        for(int i = 0; i < out.getHeight(); i++) {   
//            for(int j = 0; j < out.getWidth(); j++) {   
//                if(out.getRGB(j, i) == color.getRGB()) {   
//                	out.setRGB(j, i, 0x8F1C1C);   
//                }   
//            }   
//        }          
//        return out;
//    }


	/**
	 * Set WW base path where image will be cached 
	 * @param path
	 */
	public void setBaseCachePath(String path) {
		baseCachePath = path;
		
		// rebuild tile key
		tileKey	= buildTileKey();
		
	}

	/**
	 * Get the base cache location of this overlay
	 * @return base cache path string
	 */
	public String getBaseCachePath() {
		return baseCachePath;
	}
	
	/**
	 * Fetch overlay URL into WW cache using the asynch task service
	 * to improve GUI response & prevent the globe from freezing
	 */
	public boolean fetchOverlay() 
	{
		try 
		{
			if ( tileKey == null )
				throw new IOException("Invalid(NULL) tile key for layer " + getName() 
						+ " Cache path=" + baseCachePath);
			
			logger.debug("Looking for tile key " + tileKey + " in WW cache");
			
			if ( WorldWind.getDataFileCache().findFile(tileKey, false) == null ) 
			{ 
				// Tile not in cache
				final File file = WorldWind.getDataFileCache().newFile(tileKey);
				
				// if remote url http://
				if  ( textureURL.toString().startsWith("http")) 
				{
					//logger.debug("Synchronously fetching "+ textureURL+ " into " + file);
					//downloadResource(textureURL, file);
					
					logger.debug("Sending load request for " + textureURL);
					
					// Use the WW task service to load the URL asynchronously
					WorldWind.getTaskService().addTask(new Runnable() 
					{
						public void run() {
							try {
								downloadResource(textureURL, file);
							} 
							catch (Exception e) {
								onError(GroundOverlayLayer.this, e);
							}
						}
					});					
				}
				// Local path or file:/ URL
				else 
				{
					if ( ! file.exists() ) {
						File src = new File(textureURL.toURI());
						
						logger.debug("Copying " + src + " to " + file );
						Messages.copyResource(src, file);
					}
					else {
						logger.debug("Texture " + file + " already in cache.");
					}
				}
				
				// Texture in cache now. Update texture URL
				if ( file.exists()) {
					logger.debug("Success. Updating texture url to " + file.toURL());
					textureURL = file.toURL();

					if ( textureURL.toString().indexOf(" ") != -1) {
						logger.debug("Invalid characters detected on " + textureURL);
						textureURL = new URL(textureURL.toString().replaceAll(" ", "%20"));
					}
				}
			}
			return true;
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			onError(this, ex);
			return false;
		}
	}

	/*
	 * Notify listeners of a Layer error
	 */
	private void onError (GroundOverlayLayer layer, Exception ex) 
	{
		logger.debug("# of overlay listeners:" + listeners.size());
		
		for (GroundOverlayListener listener : listeners) {
			listener.onError(layer, ex);
		}
	}
	
	/**
	 * Get overlay center {@link Point}
	 * @param globe {@link Globe}
	 * @return
	 */
	public Vec4 getCentroid(Globe globe) {
		if (tile == null) return null;
		return tile.getCentroidPoint(globe);
	}

	public Sector getSector() {
		return sector;
	}
	
	public String toKML() {
		return toKML(true, false);
	}

	/**
	 * Build KML fragment for this {@link GroundOverlayLayer}
	 * @param 	useAbsolutePaths if true will use absolute paths for URLs
	 * @param 	nameIsTimeSpan if true use the frame name as a time span
	 * 		  	(is this overlay is part of a loop)
	 * @return
	 */
	public String toKML(boolean useAbsolutePaths, boolean nameIsTimeSpan) 
	{
		Sector bbox = tile.getSector();
		
		if ( bbox == null ) return null;

		String icon;
		
		// Absolute paths?
		try {
			icon = useAbsolutePaths 
				? textureURL.toString() 
				: new File(textureURL.toURI()).getName() ;
		} catch (URISyntaxException e) {
			icon = textureURL.toString();
			logger.error("Unable to build icon with abs path: " + useAbsolutePaths 
					+ ":" + e.getMessage() );
		}

		// Time Spans?
		String isoDate = null;
		if ( nameIsTimeSpan) {
			try {
				isoDate = WMS_Capabilities.isoTime2String(
						WMS_Capabilities.splitISOTime(getName()) );
			} catch (Exception e) {
				logger.error("Unable to build ISO date from layer name:" + getName());
			}
		}
		
		return "<GroundOverlay><name>" + getName() + "</name>" + Messages.NL
			+ "<description><![CDATA[" + 
				 ( description != null ? description : getName() ) 
				+ "]]>" 
			+ "</description>" + Messages.NL
			
			// Use the frame name as time span
			+ ( nameIsTimeSpan && isoDate != null
					? "<TimeSpan><begin>" + isoDate + "</begin></TimeSpan>" + Messages.NL 
					: "")
					
			+ "<Icon><href><![CDATA[" 
				+ icon  
				+ "]]></href></Icon>" + Messages.NL
			+ "<LatLonBox><north>" + bbox.getMaxLatitude().degrees + "</north>"
			+ "<south>" + bbox.getMinLatitude().degrees + "</south>"
			+ "<east>" + bbox.getMaxLongitude().degrees + "</east>"
			+ "<west>" + bbox.getMinLongitude().degrees + "</west></LatLonBox>" + Messages.NL
			+ "</GroundOverlay>" + Messages.NL;
	}
	
	/**
	 * Delete overlat from WW cache
	 */
	public void deleteFromCache() 
	{
		logger.debug("Deleting tile " + tileKey + " from WW cache.");
		
		URL url = WorldWind.getDataFileCache().findFile(tileKey, false);
		
		if ( url == null) return;

		try {
			File f = new File(url.toURI());
			
			if ( ! f.exists()) return;
			
			// remove from memory
			WorldWind.getMemoryCache(GroundOverlayLayer.class.getName()).remove(tileKey);
			
			logger.debug("Removing file from disk/memory cache " + f);
			f.delete();
		} 
		catch (URISyntaxException e) {
			logger.error("Unable to delete cache folder: " + url + ":" + e.getMessage());
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
//		deleteFromCache();
	}
}
