/*******************************************************************************
 *	World Wind Contribution Layers
/*******************************************************************************

To use this project in your eclipse work space you need the following libraries
in your class path:

1) SWT from http://www.eclipse.org/swt/
2) An eclipse version of the WWJ SDK. (The source built as a eclipse project)
	or worldwind.jar plus Open GL jars: gluegen-rt.jar, jogl.jar in your class path
3) The JOGL native libraries in your VM arguments 
	-Djava.library.path=${workspace_loc:org.eclipse.plugin.WorldWind}/lib/native/win32
	
	
Contributed Layers:
	1) Ground Overlays (based on similar to KML's <GroundOverlay>
	2) TimeLoop overlays. Basically a layer with a list of ground overlay children
		used to create animations
	3) WMS Layers using a SAX parser for large WMS documents (the WWJ SDK XPath WMS parser 
		is too slow).
	4) HTTP Loop layers to display NOAA GOES imagery.	
	