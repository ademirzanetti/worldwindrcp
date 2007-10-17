/**
 * 
 */
package worldwind.contrib.layers;

import org.apache.log4j.Logger;

import gov.nasa.worldwind.geom.Sector;

/**
 * @author Owner
 *
 */
public class TiledWMSLayerDescriptor 
{
	private static final Logger logger = Logger.getLogger(TiledWMSLayerDescriptor.class);
	
	static public class Style
	{
		public String name;
		public String description;
		public String legendUrl;
		@Override
		public String toString() {
			return name;
		}
	}

	public String name;
	public String title;
	public String description;
	
	public Style style;
	
	public String format;
	public int levels = 1;
	public int width;
	public int height;
	public String cacheName;
	public String srs;
	public Sector bbox;
	public String time;
	public String version;
	public String serviceUrl;
	
	public TiledWMSLayerDescriptor ( 
			String serviceUrl	// wms url
			, String name 		// layer name
			, String title 		// Friendly Name
			, String srs		// srs
			, String style		// layer styles 
			, Sector bbox
			, String time
			, String format			// layer fmt
			, int levels			// WW zoom levels
			, int width, int height // w h
			, String version
			, String cacheName		// WW cache for layer
			)	
	{
		setTitle(title);
		setLayerName(name);
		setFormat(format); 
		
		setSrs(srs);
		setLevels(levels);
		setWidth(width);
		setHeight(height);
		setCacheName(cacheName);
		setBBox(bbox);
		setTime(time);
		setVersion(version);
		this.style  = new Style();
		this.style.name = style;
		this.serviceUrl = serviceUrl;
		
		logger.debug(this.toString());
	}

	public void setLayerName(String name) {
		this.name = name;
	}

	public String getLayerName() {
		return name;
	}

	public String getServiceUrl() {
		return serviceUrl; 
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getFormat() {
		return format;
	}

	public void setLevels(int levels) {
		this.levels = levels;
	}

	public int getLevels() {
		return levels;
	}

	public void setStyle(Style style) {
		this.style = style;
	}

	public Style getStyle() {
		return style;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getWidth() {
		return width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getHeight() {
		return height;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String getCacheName() {
		return cacheName;
	}

	public void setSrs(String srs) {
		this.srs = srs;
	}

	public String getSrs() {
		return srs;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setBBox(Sector bbox) {
		this.bbox = bbox;
	}

	public Sector getBBox() {
		return bbox;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getTime() {
		return time;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public String toString() {
		return "Name=" + name + " Title=" + title  + " u=" + serviceUrl
			+ " fmt=" + format + " levs=" + levels + " style=" + style
			+ " bbox=" + bbox + " cache=" + cacheName;
	}
}
