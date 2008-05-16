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
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * Layer representing a WMS 1.1.1 Capabilities<br/>
 * From the NASA WMS server at: <br/>
 * 	<pre>http://worldwind21.arc.nasa.gov/geoserver/wms?service=WMS&request=GetCapabilities</pre><br/>
 * <b>This layer represents US Counties: </b> <pre>
 *          String url		= "http://worldwind21.arc.nasa.gov/geoserver/wms";
 *        	String layer	= "topp:us_counties"; 
 *        	String styles	= "borders_black"; 
 *        	String cacheName	= "Earth/USCountyBoundaries";
 *        	
 *       	TiledWMSLayer.LayerDescriptor service = new TiledWMSLayer.LayerDescriptor(url
 *        				, layer		// layer name
 *        				, styles	// styles
 *        				, TiledWMSLayer.LayerDescriptor.FMT_PNG 
 *        				, 13 		// levels
 *        				, 512, 512	// w h
 *        				, cacheName// WW cache name for this layer
 *        				);
 *       	TiledWMSLayer l = new TiledWMSLayer(service);</pre>
 * For political boundaries use:<br/> <pre>
 *	layer = "topp:cia"
 *	cache name = "Earth/PoliticalBoundaries"; 
 * </pre>
 * @author Vladimir Silva
 */
public class TiledWMSLayer extends BasicTiledImageLayer //extends TiledImageLayer
{
	private static final Logger logger = Logger.getLogger(TiledWMSLayer.class);
			
	private final static String KEY_STYLES = "wms.layer.styles";
	private final static String KEY_FORMAT = "wms.layer.fmt";
	private final static String KEY_SRS = "wms.layer.srs";
	private final static String KEY_TIME = "wms.layer.time";
	private final static String KEY_VERSION = "wms.layer.version";

	public final static String VERSION_1_1_1 = "1.1.1";
	public final static String VERSION_1_3_0 = "1.3.0";

	private TiledWMSLayerDescriptor descriptor;

	static public final String FMT_PNG = "image/png";
	static public final String FMT_GIF = "image/gif";
	static public final String FMT_JPG = "image/jpg";
	
	public TiledWMSLayer (TiledWMSLayerDescriptor descriptor)
	{
        super(makeLevels(descriptor, new WMSURLBuilder()));
        setDescriptor(descriptor);
        setUseTransparentTextures(true);
		logger.debug(this.descriptor);
	}


    private static LevelSet makeLevels(TiledWMSLayerDescriptor service, WMSURLBuilder urlBuilder )
    {
        AVList params = new AVListImpl();
        
		logger.debug("WMS Descriptor=" + service);

		params.setValue(AVKey.DISPLAY_NAME, service.name);
        params.setValue(AVKey.TILE_WIDTH, service.getWidth());
        params.setValue(AVKey.TILE_HEIGHT, service.getHeight());
        params.setValue(AVKey.DATA_CACHE_NAME, service.getCacheName());
        params.setValue(AVKey.SERVICE, service.getServiceUrl());
        params.setValue(AVKey.DATASET_NAME, service.getLayerName());
        params.setValue(AVKey.FORMAT_SUFFIX, service.getFormat().replaceAll("image/", "."));
        params.setValue(AVKey.NUM_LEVELS, service.getLevels());
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));
        params.setValue(AVKey.SECTOR, service.getBBox());
        params.setValue(AVKey.TILE_URL_BUILDER, urlBuilder);
        params.setValue(KEY_STYLES, service.getStyle().name);
        params.setValue(KEY_FORMAT, service.getFormat());
        params.setValue(KEY_SRS, service.getSrs());
        params.setValue(KEY_TIME, service.getTime());
        params.setValue(KEY_VERSION, service.getVersion());
        
        return new LevelSet(params);
    }

    private static class WMSURLBuilder implements TileUrlBuilder
    {
        public URL getURL(Tile tile, String imageFormat) throws MalformedURLException
        {
        	AVList params =  tile.getLevel().getParams();
        	
            StringBuffer sb = new StringBuffer(tile.getLevel().getService());
            if (sb.lastIndexOf("?") != sb.length() - 1)
                sb.append("?");
            sb.append("service=WMS&request=GetMap");
            sb.append("&layers=");
            sb.append(tile.getLevel().getDataset());
            sb.append("&srs=" + params.getStringValue(KEY_SRS)); 
            sb.append("&width=");
            sb.append(tile.getLevel().getTileWidth());
            sb.append("&height=");
            sb.append(tile.getLevel().getTileHeight());

        	//Sector bbox = (Sector)params.getValue(AVKey.SECTOR);
            Sector s = tile.getSector();

            final String time = params.getStringValue(KEY_TIME);
             
            if ( time != null ) {
            	sb.append("&time=" + time);
            }

        	// static layer: use tile sector
            sb.append("&bbox=");
            sb.append(s.getMinLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMinLatitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLongitude().getDegrees());
            sb.append(",");
            sb.append(s.getMaxLatitude().getDegrees());
            
            final String styles = params.getStringValue(KEY_STYLES);
            if ( styles != null ) sb.append("&styles=" + styles);

            final String format = params.getStringValue(KEY_FORMAT);
            if ( format != null ) sb.append("&format=" + format);

            final String version = params.getStringValue(KEY_VERSION);
            if ( version != null ) sb.append("&version=" + version);
            
            sb.append("&transparent=true");
            
            logger.debug("TiledWMSLayer: " + tile.getPath() + " u=" + sb);            
            return new java.net.URL(sb.toString());
        }
    }

    @Override
    public String toString()
    {
    	return (descriptor != null ? descriptor.getTitle() : null ) ;
    }


	/**
	 * @param descriptor the descriptor to set
	 */
	public void setDescriptor(TiledWMSLayerDescriptor descriptor) {
		this.descriptor = descriptor;
	}


	/**
	 * @return the descriptor
	 */
	public TiledWMSLayerDescriptor getDescriptor() {
		return descriptor;
	}
	
}
