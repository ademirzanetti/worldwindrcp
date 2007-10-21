package worldwind.contrib.layers.loop;


import gov.nasa.worldwind.geom.Sector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.apache.log4j.Logger;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.parsers.ParserUtils;

public class HTTPLoopFileLayer extends TimeLoopGroundOverlay 
{
	private static final Logger logger = Logger.getLogger(HTTPLoopFileLayer.class);
	
	private Sector bbox;
	
	private Vector<ParserUtils.HTTPImageFrame> frames;
	
	public HTTPLoopFileLayer(ParserUtils.HTTPDataSet ds)  
		throws MalformedURLException , IOException
	{
		super(ds.name);
		
		this.bbox 	= ds.bbox;
        this.frames = loadFrames(ds); 
		
	}

	/*
	 * Load frames from HTTP directory. Each frame maps to a GroundOverlay
	 */
	private Vector<ParserUtils.HTTPImageFrame> loadFrames(ParserUtils.HTTPDataSet ds)
		throws IOException
	{
		logger.debug("HTTP Dataset=" + ds);
		
		if ( ds.accessType == null ) {
			return loadAsNRL(ds);
		}
		else {
			return loasAsGOES(ds);
		}
	}
	
	/**
	 * 
	 * @param ds
	 * @return
	 * @throws IOException
	 */
	private Vector<ParserUtils.HTTPImageFrame> loadAsNRL(ParserUtils.HTTPDataSet ds)
		throws IOException
	{
	    // NRL frames are of the form: 
	    // 20061017.0600.multisat.ir.cloudtop.global.nogaps.jpg
	    // A Date extraction string is used to extract each frame's date.
	    // It means: For each HTTP directory entry:
		// Split by "."  Year = field:0, substr(0,4), Month = field:0 substr(4,6),....
		final String qryStr = "SPLIT=.&Y=0 0 4&M=0 4 6&D=0 6 8&h=1 0 2&m=1 2 4&s=NULL";
		
		logger.debug("Loading as NRL " + ds);
		
	    return ParserUtils.parseHTTPDirectory(ds.directoryURL	// HTML to parse
	    		, ds.directoryURL.toString()			// base URL ( to construct frames)
	    		, ds.filter								// frame name filters 
	    		, ParserUtils.httpQryStr2Properties(qryStr)		// Date extraction string (from frameurl)
	    		);
	}

	/**
	 * 
	 * @param ds
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private Vector<ParserUtils.HTTPImageFrame> loasAsGOES(ParserUtils.HTTPDataSet ds)
		throws MalformedURLException , IOException
	{
		logger.debug("Loading as GOES " + ds);
		
		return ds.accessType.equalsIgnoreCase("loopfile")
			? ParserUtils.handleLoopFile(ds.directoryURL.toString()
				, ds.loopFile
				, 0)
			: ParserUtils.parseHTTPDirectory(ds.directoryURL
				, ds.directoryURL.toString()
				, ds.filter
				,  ParserUtils.httpQryStr2Properties(ds.dateFormat));
	}

	/**
	 * Build ground overlays for the last max frames in the HTTP directory of this data set.
	 * @param maxFrames Extract only a number of the latest frames 
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public void buildOverlays(int maxFrames)  
		throws MalformedURLException , IOException
	{
		int start =  ( frames.size() < maxFrames)
			? 0
			: frames.size() - maxFrames;
	
		logger.debug("Building overlay range " + start + "," + frames.size() 
				+ " for " + getName());
		
	    for (int i = start; i < frames.size(); i++) {
	    	final GroundOverlayLayer layer = new GroundOverlayLayer(
					frames.get(i).date
					, this.bbox
					, new URL(frames.get(i).frameUrl)
					, frames.get(i).frameExt 
					);
	    	
	    	layer.setBaseCachePath("Earth/" + getName().replaceAll(" ", "") + "/");
	    	add(layer);
		}
	}
	
	public Vector<ParserUtils.HTTPImageFrame> getFrames() {
		return frames;
	}

	/**
	 * Get names for each HTTP frame (ussually a date)
	 * @return
	 */
	public String[] getFrameNames() {
		String[] names = new String[frames.size()];
		for (int i = 0; i < names.length; i++) {
			names[i] = frames.get(i).date;
		}
		return names;
	}
	
}

