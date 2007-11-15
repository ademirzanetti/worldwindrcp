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

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;

/**
 * A WW layer to represent a KML Placemark or point on earth.
 * @author Owner
 *
 */
public class PlacemarkLayer extends IconLayer 
{
	// Default icon path
	static final String defaultIconPath = "worldwind/contrib/layers/placemark.png"; 
	
	
	public PlacemarkLayer() {
		super();
	}

    @Override
    protected void doRender(DrawContext dc)
    {
    	super.doRender(dc);
    	//renderShapes(dc);
    }

    public void addDefaultIcon(String name, Position iconPosition) {
    	WWIcon icon = new UserFacingIcon(defaultIconPath, iconPosition);
    	
    	icon.setToolTipText(name);
    	icon.setToolTipFont(makeToolTipFont());
    	icon.setToolTipTextColor(java.awt.Color.BLACK);
    	addIcon(icon);
    }

    /*
     * Deafult font
     */
    private Font makeToolTipFont()
    {
        HashMap<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>();

        fontAttributes.put(TextAttribute.BACKGROUND, java.awt.Color.YELLOW); //new java.awt.Color(0.4f, 0.4f, 0.4f, 1f));
        return Font.decode("Arial-12").deriveFont(fontAttributes);
    }
   
    /**
     * TODO: Build a text bubble fro the placemark info
     */
    
    /*
    private void renderShapes(DrawContext dc)
    {

        Color interiorColor = new Color(1f, 1f, 0f, 0.3f);
        Color borderColor = new Color(1f, 1f, 0f, 0.4f);

        SurfaceQuadrilateral quad = new SurfaceQuadrilateral(new Sector(
            Angle.fromDegrees(41.0), Angle.fromDegrees(41.6),
            Angle.fromDegrees(-122.5), Angle.fromDegrees(-121.7)),
            interiorColor, borderColor);
        quad.render(dc);
        

        quad = new SurfaceQuadrilateral(new Sector(
            Angle.fromDegrees(38.9), Angle.fromDegrees(39.3),
            Angle.fromDegrees(-120.2), Angle.fromDegrees(-119.9)),
            new Color(0f, 1f, 1f, 0.3f), new Color(0.5f, 1f, 1f, 0.4f));

        quad.render(dc);
        
        double originLat = 28;
        double originLon = -82;
        ArrayList<LatLon> positions = new ArrayList<LatLon>();
        positions.add(new LatLon(Angle.fromDegrees(originLat + 5.0), Angle.fromDegrees(originLon + 2.5)));
        positions.add(new LatLon(Angle.fromDegrees(originLat + 5.0), Angle.fromDegrees(originLon - 2.5)));
        positions.add(new LatLon(Angle.fromDegrees(originLat + 2.5), Angle.fromDegrees(originLon - 5.0)));
        positions.add(new LatLon(Angle.fromDegrees(originLat - 2.5), Angle.fromDegrees(originLon - 5.0)));
        positions.add(new LatLon(Angle.fromDegrees(originLat - 5.0), Angle.fromDegrees(originLon - 2.5)));
        positions.add(new LatLon(Angle.fromDegrees(originLat - 5.0), Angle.fromDegrees(originLon + 2.5)));
        positions.add(new LatLon(Angle.fromDegrees(originLat - 2.5), Angle.fromDegrees(originLon + 5.0)));
        positions.add(new LatLon(Angle.fromDegrees(originLat + 2.5), Angle.fromDegrees(originLon + 5.0)));

        SurfacePolygon polygon = new SurfacePolygon(positions,
            new Color(1f, 0.11f, 0.2f, 0.4f), new Color(1f, 0f, 0f, 0.6f));
        polygon.setStroke(new BasicStroke(2f));
        polygon.render(dc);
    }
    */
    
}
