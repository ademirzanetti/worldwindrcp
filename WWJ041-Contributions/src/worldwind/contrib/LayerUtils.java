package worldwind.contrib;

import org.apache.log4j.Logger;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.view.FlyToOrbitViewStateIterator;
import gov.nasa.worldwind.view.OrbitView;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;

/**
 * World Wind Contribution utilities
 * @author Owner
 *
 */
public class LayerUtils 
{
	private static final Logger logger = Logger.getLogger(LayerUtils.class);
	
	/**
	 * Move globe view to the centroid of a WW layer at a random heading, pitch & zoom
	 * @param view {@link View}
	 * @param globe {@link Globe}
	 * @param layer {@link Layer}
	 */
	static public void moveViewTo (final View view, final Globe globe, Layer layer) 
	{ 
		Vec4 point 		= null;
		Sector sector 	= null;
		
		if ( layer instanceof TimeLoopGroundOverlay) {
			point = ((TimeLoopGroundOverlay)layer).getCentroid(globe);
			sector  = ((TimeLoopGroundOverlay)layer).getSector();
		}
		else if ( layer instanceof GroundOverlayLayer) {
			point = ((GroundOverlayLayer)layer).getCentroid(globe);
			sector = ((GroundOverlayLayer)layer).getSector();
		}
		
		if ( point == null ) return;
		
		Position position = globe.computePositionFromPoint(point);
		Position eyePoint = globe.computePositionFromPoint(view.getEyePoint());
		
		// Compute a very basic altitude depending on the width of the BBOX 
		double delta 	= (sector.getDeltaLonDegrees() + sector.getDeltaLatDegrees())/2;
		double altitude	= 1.91e7; // Full view alt (default) //eyePoint.getElevation();
		
		// New altitude (zoom) based on the sector delta
		if ( delta > 50 && delta < 100  ) altitude /= 2;
		if ( delta > 10 && delta < 50  ) altitude /= 4;
		if ( delta >  5 && delta < 10 ) altitude /= 6;
		if ( delta < 5 ) altitude = 100000;
		
		logger.debug("Move to=" + position + " Eye point=" +  eyePoint
				+ " Sector" + sector + " Sector delta=" + delta + " alt=" + altitude);
		
        view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator(
        		(OrbitView)view
        		, globe
        		, new LatLon(position.getLatitude(), position.getLongitude())
        		, Angle.ZERO
        		, Angle.ZERO
        		, altitude)
        		);
	}
	

}
