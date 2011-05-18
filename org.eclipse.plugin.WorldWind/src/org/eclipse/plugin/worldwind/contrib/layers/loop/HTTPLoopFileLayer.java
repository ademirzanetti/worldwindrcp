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
package org.eclipse.plugin.worldwind.contrib.layers.loop;

import gov.nasa.worldwind.geom.Sector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.eclipse.plugin.worldwind.contrib.Messages;
import org.eclipse.plugin.worldwind.contrib.layers.GroundOverlayLayer;
import org.eclipse.plugin.worldwind.contrib.parsers.ParserUtils;

/**
 * A WW layer to fetch images from a HTTP image server such as Navy Labs Monterey
 * http://www.nrlmry.navy.mil/ or NOAA GOES Sat images - http://www.ssd.noaa.gov/goes/
 * 
 * <p>The loops are created by parsing XML catalogs using WW .NET NRL XML format.
 * For example:</p>
 * <pre>
 * 	&lt;NRL_DataSet Name="Cloud Top Heights">
 * 		&lt;Directory_Url>http://www.nrlmry.navy.mil/archdat/global/stitched/cloudtop/&lt;/Directory_Url>
 * 		&lt;Description>Global Cloud Top Heights&lt;/Description>
 * 		&lt;GeographicBoundingBox>
 * 			&lt;North>&lt;Value>75&lt;/Value>&lt;/North>
 * 			&lt;South>&lt;Value>-75&lt;/Value>&lt;/South>
 * 			&lt;West>&lt;Value>-180&lt;/Value>&lt;/West>
 * 			&lt;East>&lt;Value>180&lt;/Value>&lt;/East>
 * 		&lt;/GeographicBoundingBox>
 * 	&lt;/NRL_DataSet>
 * 	</pre>
 * <p>The catalog are located in the config folder: MRL_Monterey.xml, goes.noaa.xml
 * , and sat.loops.xml
 * @author Owner
 *
 */
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
		
		buildOverlays(start, frames.size());
	}

	/**
	 * Build ground overlays for a frame range in the HTTP directory of this data set.
	 * @param startFrame
	 * @param endFrame
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public void buildOverlays(int startFrame, int endFrame)  
		throws MalformedURLException , IOException
	{
	    for (int i = startFrame; i < endFrame; i++) {
	    	final GroundOverlayLayer layer = new GroundOverlayLayer(
					frames.get(i).date
					, this.bbox
					, new URL(frames.get(i).frameUrl)
					, frames.get(i).frameExt 
					);
	    	
	    	layer.setBaseCachePath("Earth/" 
	    			+ Messages.forCachePath(getName()) + "/");
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

