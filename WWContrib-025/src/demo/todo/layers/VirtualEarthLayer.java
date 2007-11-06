/**
 * 
 */
package demo.todo.layers;

import java.net.MalformedURLException;
import java.net.URL;

import worldwind.contrib.Messages;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import gov.nasa.worldwind.view.BasicOrbitView;

/**
 * @author Owner
 *
 */
public class VirtualEarthLayer extends BasicTiledImageLayer  
{
	/**
	 * Constructor 
	 */
	public VirtualEarthLayer(WorldWindowGLCanvas world) {
        super(makeLevels(new URLBuilder(world))); //builder));
        this.setUseTransparentTextures(true);
	}

    private static LevelSet makeLevels(URLBuilder urlBuilder)
    {
        AVList params = new AVListImpl();

        params.setValue(AVKey.TILE_WIDTH, 256);
        params.setValue(AVKey.TILE_HEIGHT, 256);
        params.setValue(AVKey.DATA_CACHE_NAME, "Earth/MS Virtual Earth");
        
        // dummy values
        params.setValue(AVKey.SERVICE, "http://dummy.com/");
        params.setValue(AVKey.DATASET_NAME, "dummy");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
        
        params.setValue(AVKey.NUM_LEVELS, 19);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(36d), Angle.fromDegrees(36d)));
        params.setValue(AVKey.SECTOR, Sector.FULL_SPHERE);
        params.setValue(AVKey.TILE_URL_BUILDER, urlBuilder);

        return new LevelSet(params);
    }
	
    /**
     * 
     * @author Owner
     *
     */
    private static class URLBuilder implements TileUrlBuilder
    {
    	private String mapType = "h";
    	private String mapExtension = ".jpeg";

    	private WorldWindowGLCanvas world;
    	
		private final double earthRadius = 6378137;
		private final double earthCircum = earthRadius * 2.0 * Math.PI;
		private final double earthHalfCirc = earthCircum / 2;
    	
    	public URLBuilder(WorldWindowGLCanvas world){ 
            this.world = world;
    	}
    	
    	public URL getURL(Tile tile) throws MalformedURLException
        {
    		Sector s 		= tile.getSector();
    		LatLon latLon 	= s.getCentroid();
    		
    		BasicOrbitView view = (BasicOrbitView)world.getView();
    		//Globe globe 		= world.getModel().getGlobe();
    		
    		Position eyePoint = view.getEyePosition();// globe.computePositionFromPoint(view.getEyePoint());
    		
    		double altitude 	= eyePoint.getElevation();
    		double factor 		= altitude / earthRadius;
    		double distance 	= view.getAltitude();
    		
    		Angle trueViewRange; // = view.getFieldOfView().degrees;
    		
    		if ( factor < 1 )
    			trueViewRange = Angle.fromRadians(Math.abs(Math.asin((distance) / earthRadius))*2);
    		else
    			trueViewRange = Angle.fromRadians(Math.PI);
    				
    		
    		int zoomLevel =  GetZoomLevelByTrueViewRange(trueViewRange.degrees);
    		
    		double lat = eyePoint.getLatitude().degrees; // latLon.getLatitude().degrees;
    		double lon = eyePoint.getLongitude().degrees; // latLon.getLongitude().degrees;
//    		double alt = eyePoint.getElevation();
    		
			int meterY = LatitudeToYAtZoom(lat, zoomLevel);
			int meterX = LongitudeToXAtZoom(lon, zoomLevel);

			final int imageSize = 256; //constant
			int col = meterX / imageSize;
			int row = meterY / imageSize;

System.out.println("altitude=" + altitude 
		+ " trueViewRange=" + trueViewRange + " Sector:" + s 
		+ " zoom lev=" + zoomLevel	+ " lat=" + lat 
		+ " lon=" + lon + " mY=" + meterY + " mX=" + meterX
		+ " col=" + col + " row=" + row);

			String quadKey = TileToQuadKey(col, row, zoomLevel);
			
    		if ( mapType.equals("h") || mapType.equals("a")) 
    			mapExtension = ".jpeg";
    		else if ( mapType.equals("r"))
    			mapExtension = ".png";


            String sb = "http://" + getMapType()
            	+ quadKey.charAt(quadKey.length() - 1)
            	+ ".ortho.tiles.virtualearth.net/tiles/"
            	+ mapType + quadKey + mapExtension 
            	+ "?g=1";

//System.out.println("quad key=" + quadKey + " m type=" + mapType + " ext=" + mapExtension + " url=" + sb);
            return new java.net.URL(sb);
        }

		private static String TileToQuadKey(int tx, int ty, int zl)
		{
			String quad = "";
			for (int i = zl; i > 0; i--)
			{
				int mask = 1 << (i - 1);
				int cell = 0;
				if ((tx & mask) != 0)
				{
					cell++;
				}
				if ((ty & mask) != 0)
				{
					cell += 2;
				}
				quad += cell;
			}
			return quad;
		}
    	
		private static double DegToRad(double d)
		{
			return d * Math.PI / 180.0;
		}
    	
		private int LatitudeToYAtZoom(double lat, int zoom)
		{
			double arc = earthCircum / ((1 << zoom) * 256);
			double sinLat = Math.sin(DegToRad(lat));
			double metersY = earthRadius / 2 * Math.log((1 + sinLat) / (1 - sinLat));
			int y = (int)Math.round((earthHalfCirc - metersY) / arc);
			return y;
		}

		private int LongitudeToXAtZoom(double lon, int zoom)
		{
			double arc = earthCircum / ((1 << zoom) * 256);
			double metersX = earthRadius * DegToRad(lon);
			int x = (int)Math.round((earthHalfCirc + metersX) / arc);
			return x;
		}

		private int GetZoomLevelByTrueViewRange(double trueViewRange)
		{
			int maxLevel = 3;
			int minLevel = 19;
			int numLevels = minLevel - maxLevel + 1;
			int retLevel = maxLevel;
			for(int i=0; i<numLevels; i++)
			{
				retLevel = i + maxLevel;

				double viewAngle = 180;
				for(int j=0; j<i; j++)
				{
					viewAngle = viewAngle / 2.0;
				}
				if(trueViewRange >= viewAngle)
				{
					break;
				}
			}
			return retLevel;
		}

    	
		/**
		 * @param mapType the mapType to set
		 */
		public void setMapType(String mapType) {
			this.mapType = mapType;
		}

		/**
		 * @return the mapType
		 */
		public String getMapType() {
			return mapType;
		}

    }

    @Override
    public String toString()
    {
    	// load from props
        return Messages.getText("layers.Earth.MSVE.Name");
    }

	/**
	 * @param mapType the mapType to set
	 */
	public void setMapType(String mapType) {
		// TODO: builder.setMapType(mapType);
	}

	/**
	 * @return the mapType
	 */
	public String getMapType() {
		return ""; // TODO: builder.getMapType();
	}
	
}
