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
package org.eclipse.plugin.worldwind.contrib.layers;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import gov.nasa.worldwind.formats.georss.GeoRSSParser;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Renderable;

public class GeoRSSLayer extends RenderableLayer 
{

	/**
	 * A WW GeoRSS layer used to render shapes from a given document 
	 * @param fileGeoRSS XML file
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public GeoRSSLayer(String name, File file) 
		throws IOException, SAXException, ParserConfigurationException
	
	{
		super.setName(name);
		
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file);
        
        List<Renderable> shapes = GeoRSSParser.parseShapes(doc);

        if (shapes != null)
        {
            for (Renderable shape : shapes)
            {
                addRenderable(shape);
            }
        }
	}
}
