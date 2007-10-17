package worldwind.contrib.layers.loop;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;

import worldwind.contrib.Messages;
import worldwind.contrib.parsers.ParserUtils;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * A Layer List with a collection of the best loop layers from 
 * the Navy Labs Monterey - http://www.nrlmry.navy.mil/
 * and NOAA's GOES Sat imagery layer list from http://www.ssd.noaa.gov/goes/
 * 
 * <p>There are potentially are a lot of datasets 500+ each with 100+ frames thus only the
 * last MAX_FRAMES are extracted. This images are near real time and change
 * constantly. Thus this layer should be built-in and refreshed every time 
 * a client starts.</p>
 * 
 * @author vsilva
 *
 */
public class HTTPSatLoopLayerList extends LayerList 
{
	private static final Logger logger = Logger.getLogger(HTTPSatLoopLayerList.class);
	
	private static final long serialVersionUID = -1201283860144484556L;
	
	// Extract only the 5 latest frames form each data-set
	public static final int MAX_FRAMES = 5;
	
	public HTTPSatLoopLayerList()
		throws FileNotFoundException
	{
		this(Messages.getInputStream(HTTPSatLoopLayerList.class
						, Messages.getText("layers.loops.xml.file")));
	}
	
	/**
	 * WW Layer list with lots of satellite loop overlays 
	 */
	public HTTPSatLoopLayerList(InputStream stream) {
		try 
		{
			/*
			 * Parse the XML file that contains all the loops
			 */
			Vector<ParserUtils.HTTPDataSet> v = 
				ParserUtils.parseHTTPDatasetXMLFile(stream);
			
			// loop thru all HTTP datasets
			for (int i = 0; i < v.size(); i++) 
			{
				ParserUtils.HTTPDataSet ds = (ParserUtils.HTTPDataSet)v.get(i);
				try 
				{
					// Construct a WW layer
					final HTTPLoopFileLayer layer = new HTTPLoopFileLayer(ds);
					
					// build WW ground overlays for thge latest MAX frames
					layer.buildOverlays(MAX_FRAMES);
					
					logger.debug("Sat Layer: " + layer);
					
					// Add to the WW layer list
					super.add( layer ); 

				} catch (Exception e) {
					logger.error("Unable to load layer " + ds.name  + ": " + e.getMessage() );
				}
			}
		} catch (Exception e) {
			logger.error(e);
			throw new IllegalArgumentException(e);
		}
	}

	
	public void setAllEnabled (boolean enabled)
	{
		for (Iterator<Layer> iterator = iterator(); iterator.hasNext();) {
			Layer l = (Layer) iterator.next();
			l.setEnabled(enabled);
		}
	}
	
}
