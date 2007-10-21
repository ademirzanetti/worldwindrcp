package worldwind.contrib.layers;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Vector;

import javax.imageio.ImageIO;
import org.apache.log4j.Logger;

import worldwind.contrib.Messages;
import worldwind.contrib.parsers.WMS_Capabilities;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureData;
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
import gov.nasa.worldwind.render.GeographicSurfaceTileRenderer;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

/**
 * A ground overlay layer. Renders a URL on a given latlon box (a.k.a Sector) 
 * @author Vladimir Silva
 *
 */
public class GroundOverlayLayer extends AbstractLayer 
{
	private static final Logger logger = Logger.getLogger(GroundOverlayLayer.class);

	private String title;
	private TextureTile tile = null;
	
	private String baseCachePath;
	
	// Unique ID (used to cache tile in memory & disk)
	private String tileKey;	
	
	// Texture can be a URL or a file path
	private URL textureURL;
	
	
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
	 * @param title Name of this ground overlay
	 * @param sector Lat/lon box for the overlay
	 * @param textureURL URL of the overlay image
	 * @param fileSuffix Image extension used to download and possible scale texture (e.g .png, .gif, etc)
	 */
	public GroundOverlayLayer (String title, Sector sector, URL textureURL, String fileSuffix) 
	{
		if ( title == null || sector == null)
			throw new IllegalArgumentException("Invalid layer name or sector");
		
        this.tile 		= new TextureTile(sector);
		this.textureURL = textureURL;
		this.title 		= title;
		this.fileSuffix = fileSuffix;
		this.baseCachePath = "Earth/";
		
        tileKey	= buildTileKey(); 
		
		// file suffix (used to load texture)
		this.formatName = (fileSuffix != null ) 
			? fileSuffix.replaceAll("\\.", "").toUpperCase() 
			: null;
		
		initCache();
		
		logger.debug("title=" + title + ", s=" + sector 
				+ " url=" + textureURL + " fs=" + fileSuffix + " fmt=" + formatName);
	}

	private void initCache () {
        if (!WorldWind.getMemoryCacheSet().containsCache(GroundOverlayLayer.class.getName()))
        {
            long size = Configuration.getLongValue(AVKey.TEXTURE_IMAGE_CACHE_SIZE, 3000000L);
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("Texture Tiles");
            WorldWind.getMemoryCacheSet().addCache(GroundOverlayLayer.class.getName(), cache);
        }
	}
	
	private String buildTileKey ()
	{
		return baseCachePath + title.replaceAll("[:]", "") + fileSuffix;
	}

	/**
	 * Add an overlay status listener
	 * @param listener
	 */
	public void addOverlayListener( GroundOverlayListener listener){
		listeners.add(listener);
	}
	
	@Override
	/**
	 * Layers draw themselves
	 */
    protected final void doRender(DrawContext dc)
    {
		logger.debug("----> START doRender:" + title);
		
        if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return; 

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(true);
        
        if ( ! isTileInMemory()) // isTextureInMemory(dc.getTextureCache()))//tile)) 
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
        
        		//downloadResource(getTextureURL(), WorldWind.dataFileCache().newFile(tileKey));
        		synchFetch();
        		
        		// return the "loading.png" texture
        		try {
        			tile.setTexture(dc.getTextureCache(), getLoadingTexture());
					
				} catch (Exception e) {
					System.err.println(e);
				}
				
        	}
        }
        else {
        	tile = getTileFromMemoryCache(); //tileKey);
        	logger.debug("Loading tile from memory " + tileKey + " tile=" + tile);
        }
        
        logger.debug("Redering tile=" + tile);
        
        new GeographicSurfaceTileRenderer().renderTile(dc, tile);
        logger.debug("----> END doRender:" + title);
    }

	/*
	 * Return a "loading.png" texture when
	 */
	private Texture getLoadingTexture() throws IOException
	{
		return TextureIO.newTexture(
				Messages.getInputStream(GroundOverlayLayer.class, "loading.png")
				, false, TextureIO.PNG);		
	}
	
	
	/*
	 * Fetch ground overlay from a URL: http:/ or file:/
	 */
    private synchronized boolean loadTexture(TextureCache tc, URL textureURL) 
    {
        TextureData textureData = null;

        try {
        	// load from URL
        	logger.debug("Fetching texture from URL " + textureURL);
        	textureData = TextureIO.newTextureData(textureURL, false, null);
        }
        catch (Exception e) {
        	// notify listeners of error
        	onError(this, e);
        	return false;
        }
        
        Texture texture = TextureIO.newTexture(textureData);

        logger.debug("Got texture: " 
        		+ tileKey 
        		+ " TexData (w,h)=" + textureData.getWidth() + "," + textureData.getHeight()
        		+ " Texture w=" + texture.getWidth() + " h=" + texture.getHeight() 
        		+ " from " + textureURL //( textureURL != null ? textureURL : textureFile ) 
        		+ " Est mem size:" + textureData.getEstimatedMemorySize());
        
        /*
         * Texture/TextureData w/h mismatch bug: sometimes W/H don't match
         * between Texture & TextureData. This causes a messed up img to display.
         * A solution? ... resize img to 1024x1024
         */
        if ( textureData.getWidth() != texture.getWidth() 
        		|| textureData.getHeight() != texture.getHeight() )
        {
        	try 
        	{
            	final URL url = WorldWind.getDataFileCache().findFile(tileKey, false);
            	
            	// texture exists?
            	File file = ( url == null ) 
            		? WorldWind.getDataFileCache().newFile(tileKey)
            		: new File(url.toURI())	;
            	
            	logger.error("Texture/TextureData w/h don't match. Saving to disk & resizing " 
            			+ file + " file exists " + file.exists());
        		
        		// Fetch manually & store on disk
            	if ( !file.exists())
            		downloadResource(textureURL, file);
        		
            	// scale file from disk to 1024x1024 (preserving alpha)
            	scaleImage(file, formatName, 1024, 1024);
            	
            	// reload texture
            	texture = TextureIO.newTexture(file, false);
        	} 
        	catch (Exception e) {
				e.printStackTrace();
			}
        }
        
        tile.setTexture(tc, texture);
        
        this.addTileToMemoryCache(); //tileKey, tile);

        return true;
    }

    /**
     * Download a remote resource
     * @param resourceURL remote url
     * @param outFile file to save the resource to
     */
    void downloadResource(URL resourceURL, final File outFile)
    {
        try
        {
            URLRetriever retriever = new HTTPRetriever(resourceURL,
                new RetrievalPostProcessor() 
                {
                    public ByteBuffer run(Retriever retriever)
                    {
                        if (!retriever.getState().equals(Retriever.RETRIEVER_STATE_SUCCESSFUL))
                            return null;

                        HTTPRetriever htr = (HTTPRetriever) retriever;
                        if (htr.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
                        {
                            return null;
                        }

                        URLRetriever r = (URLRetriever) retriever;
                        ByteBuffer buffer = r.getBuffer();

                        try
                        {
                            WWIO.saveBuffer(buffer, outFile);
                            return buffer;
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace(); 
                            return null;
                        }
                    }
                });

            retriever.call();
        }
        catch (Exception e)
        {
        	// Notify listeners of the error
        	onError(this, e);
            //e.printStackTrace(); 
        }
    }
    
    
    private void addTileToMemoryCache() //Object key, TextureTile tile)  
    {
        if (this.getTileFromMemoryCache() == null ) { //key) == null) {
        	logger.debug("Adding tile " + tile + " key=" + tileKey + " to memory cache.");
            WorldWind.getMemoryCache(GroundOverlayLayer.class.getName()).add(tileKey, tile);
        }
    }

    private TextureTile getTileFromMemoryCache() //Object key)
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
    	return getTitle();
    }

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
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
			e.printStackTrace();
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
	 * 
	 * @param src
	 * @param color
	 * @throws IOException
	 */
//	static void makeColorTransparent(File src, Color color)
//		throws IOException
//	{
//		logger.debug("File=" + src + " Color=" + color);
//		
//		String formatName = "PNG";
//		BufferedImage source = ImageIO.read(src);
//	
//		BufferedImage target = eraseColor(source, color);
//		ImageIO.write(target, formatName , src);
//	}
	
	

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
	 * Fetch overlay URL into WW file cache to improve GUI response
	 */
	public void synchFetch() 
	{
		try {
			if ( WorldWind.getDataFileCache().findFile(tileKey, false) == null ) 
			{ 
				File file = WorldWind.getDataFileCache().newFile(tileKey);
				
				if  ( textureURL.toString().startsWith("http")) {
					logger.debug("Synchronously fetching "+ textureURL+ " into " + file);
					downloadResource(textureURL, file);
				}
				else {
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
		}
		catch (Exception ex) {
			//ex.printStackTrace();
			onError(this, ex);
		}
	}

	/**
	 * Fetch asynchronously using a separate thread
	 */
	public void asynchFetch() 
	{
		if ( WorldWind.getDataFileCache().findFile(tileKey, false) == null ) 
		{
			final File file = WorldWind.getDataFileCache().newFile(tileKey);
			
			new Thread(new Runnable() {
				public void run() 
				{
					logger.debug("Asynchronously fetching " 
							+ textureURL //( textureURL != null ? textureURL : textureFile) 
							+ " into " + file);
					try {
						if  ( textureURL.toString().startsWith("http") ) {//  != null) {
							downloadResource(textureURL, file);
						}
						else {
							//Messages.copyResource(textureFile, file);
							Messages.copyResource(new File(textureURL.toURI()), file);
						}
					}
					catch ( Exception ex) {
						ex.printStackTrace();
					}
					
					notifyAll();
				}
			});
		}
	}
	
	/*
	 * Notify listeners of a Layer error
	 */
	private void onError (GroundOverlayLayer layer, Exception ex) {
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

	public String toKML() {
		return toKML(true, false);
	}

	/**
	 * Build KML for this {@link GroundOverlayLayer}
	 * @param useAbsolutePaths if true will use absolute paths for URLs
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
			+ "<description>" + getTitle() 
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
	
	
}
