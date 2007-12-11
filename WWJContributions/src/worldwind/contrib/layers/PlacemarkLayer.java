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
   
}
