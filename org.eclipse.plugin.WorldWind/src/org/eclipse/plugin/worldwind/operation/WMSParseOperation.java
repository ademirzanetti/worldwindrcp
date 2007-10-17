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
package org.eclipse.plugin.worldwind.operation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Display;

import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.SimpleWMSParser;
import worldwind.contrib.parsers.WMS_Capabilities;

/**
 * WMS Parse JFace Operation
 * @author Owner
 *
 */
public class WMSParseOperation implements IRunnableWithProgress
{
	// Unknown progress size?
	private URL url;
	private String serverName;
	private TableViewer viewer;
	private Display display;
	
	private WMS_Capabilities capabilities;
	
	//  Total # of parsed layers
	private int totalParsed;
	
	public WMSParseOperation(String serverName, URL url, TableViewer viewer
			, Display display) 
	{   this.url = url;
	    this.serverName = serverName;
	    this.viewer = viewer;
	    this.display = display;
	}

	
	public void run(IProgressMonitor monitor) 
		throws InvocationTargetException, InterruptedException 
	{
		// Load remote WMS XML from url
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			monitor.beginTask(serverName, IProgressMonitor.UNKNOWN ); 

			// Long operation: load XML via HTTP
			SimpleHTTPClient client = new SimpleHTTPClient(url);
			client.doGet(bos); 
			bos.close();
			
			// parse WMS XML
			SimpleWMSParser parser = new SimpleWMSParser();
			parser.parse(serverName, new ByteArrayInputStream(bos.toString().getBytes()));

			capabilities 	= parser.getCapabilities();
			totalParsed 	= parser.getParsedCount();
			
			display.syncExec(new Runnable() {
				public void run() 
				{
					// remove any prev elements
					viewer.getTable().removeAll();
					
					// Ad layers to the list box
					for (WMS_Capabilities.Layer layer : capabilities.getLayers()) {
						viewer.add(layer.Title);
					}
				}
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			throw new InterruptedException(e.getMessage());
		}

		// done
		monitor.done();
		
	    if (monitor.isCanceled())
	        throw new InterruptedException("WMS parse operation cancelled.");
		
	}

	public WMS_Capabilities getCapabilities() {
		return capabilities;
	}
	
	public int getParsedCount() {
		return totalParsed;
	}
}
