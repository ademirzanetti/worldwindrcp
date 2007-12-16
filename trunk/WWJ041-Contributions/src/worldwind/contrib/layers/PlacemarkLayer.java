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

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.font.TextAttribute;
import java.util.Collection;
import java.util.HashMap;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.IconLayer;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.IconRenderer;
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
	
	// Annotation attributes
	private static AnnotationAttributes attribs = new AnnotationAttributes();

	private IconRenderer renderer = new IconRenderer();

	static {
    	attribs.setTextColor(Color.BLACK);
    	attribs.setBackgroundColor(Color.WHITE);
    	attribs.setBorderColor(Color.BLACK);
	}

	/**
	 * Placemark icon w/ annotation
	 * @author Owner
	 *
	 */
	public static class PlacemarkIcon extends UserFacingIcon 
	{
		GlobeAnnotation bubble;
		
		public PlacemarkIcon(String iconPath, Position iconPosition, String description) {
			super(iconPath, iconPosition);
			
			if ( description != null)
				bubble = new GlobeAnnotation(description, iconPosition, attribs);
		}
		
		public Annotation getAnnotation(){
			return bubble;
		}
	}
	
	/**
	 * Contructor
	 */
	public PlacemarkLayer() {
		super();
	}

    @Override
    protected void doRender(DrawContext dc)
    {
    	Collection<WWIcon> icons = getIcons();
    	
    	renderer.render(dc, icons);
    	for (WWIcon icon : icons) 
    	{
    		final Annotation a = ((PlacemarkIcon)icon).getAnnotation();
    		if ( a != null )
    			a.render(dc);
		}
    }

    @Override
    protected void doPick(DrawContext dc, Point pickPoint) {
    	super.doPick(dc, pickPoint);
    	
    	Collection<WWIcon> icons = getIcons();
    	
    	for (WWIcon icon : icons) 
    	{
    		final Annotation a = ((PlacemarkIcon)icon).getAnnotation();
    		
    		if (a != null)
    			a.pick(dc, pickPoint);
		}
    }
    
    
    public void addIcon(PlacemarkIcon icon) { 
    	super.addIcon(icon);
    }
    
    public void addDefaultIcon(String name, Position iconPosition, String description) {
    	//WWIcon icon = new UserFacingIcon(defaultIconPath, iconPosition);
    	WWIcon icon = new PlacemarkIcon(defaultIconPath, iconPosition, description);
    	
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
   
}
