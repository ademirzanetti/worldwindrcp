/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package demo;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.formats.georss.GeoRSSParser;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.Earth.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceSector;
import gov.nasa.worldwind.render.UserFacingIcon;
import gov.nasa.worldwind.render.WWIcon;

import org.w3c.dom.*;
import org.xml.sax.*;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.NASAWMSLayerList;
import worldwind.contrib.layers.PlacemarkLayer;
import worldwind.contrib.layers.ScreenOverlayLayer;
import worldwind.contrib.layers.TiledWMSLayerDescriptor;
import worldwind.contrib.layers.TiledWMSLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.parsers.ParserUtils;




import javax.swing.*;
import javax.xml.parsers.*;
import java.awt.*;
import java.awt.font.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * @author Tom Gaskins
 * @version $Id: AWT1Up.java 1772 2007-05-07 23:05:47Z tgaskins $
 */
public class AWT1Up
{
    private static class AWT1UpFrame extends javax.swing.JFrame
    {
        StatusBar statusBar;
        JLabel cursorPositionDisplay;
        WorldWindowGLCanvas wwd;

        public AWT1UpFrame()
        {
            try
            {
                System.out.println(gov.nasa.worldwind.Version.getVersion());

                wwd = new gov.nasa.worldwind.awt.WorldWindowGLCanvas();
                wwd.setPreferredSize(new java.awt.Dimension(800, 600));
                this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);

                this.statusBar = new StatusBar();
                this.getContentPane().add(statusBar, BorderLayout.PAGE_END);

                this.pack();

                java.awt.Dimension prefSize = this.getPreferredSize();
                java.awt.Dimension parentSize;
                java.awt.Point parentLocation = new java.awt.Point(0, 0);
                parentSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
                int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
                this.setLocation(x, y);
                this.setResizable(true);

                Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

                wwd.setModel(m);
                
                LayerList layers =  new LayerList(); 

                layers.add(new BMNGSurfaceLayer());
                layers.add(new EarthNASAPlaceNameLayer());
                
                for (Layer layer : layers)
                {
                    if (layer instanceof TiledImageLayer)
                        ((TiledImageLayer) layer).setShowImageTileOutlines(false);
                    if (layer instanceof LandsatI3)
                        ((TiledImageLayer) layer).setDrawBoundingVolumes(false);
                    if (layer instanceof CompassLayer)
                        ((CompassLayer) layer).setShowTilt(true);
                }

//                VirtualEarthLayer ve = new VirtualEarthLayer();
//                System.out.println(ve.getName());
//                layers.add(ve);
                
                m.setLayers(layers);
                
//                m.getLayers().add(this.buildShapesLayer());
//                m.getLayers().add(this.buildIconLayer());
                m.getLayers().add(this.buildGeoRSSLayer());
                m.getLayers().add(this.buildPlacemarkLayer());
                m.getLayers().add(this.buildScreenOverlayLayer());
                m.getLayers().add(this.buildGroundOverlay());
                
//                m.getLayers().add(this.buildWMSLayer());

//                LayerList collection = buildNASAWMSLayerList();
//                for (Layer layer : collection)
//                {
//	                m.getLayers().add(layer);
//				}

                // anim overlay
//                TimeLoopGroundOverlay anim = buildAnimatedGroundOverlay(wwd);
//
//                for (Layer layer : anim.getOverlays())
//                {
//	                m.getLayers().add(layer);
//				}
//
//                ((TimeLoopGroundOverlay)anim).play();
                
                m.setShowWireframeExterior(false);
                m.setShowWireframeInterior(false);
                

                // Forward events to the status bar to provide the cursor position info.
                this.statusBar.setEventSource(wwd);

                this.wwd.addRenderingListener(new RenderingListener()
                {
                    public void stageChanged(RenderingEvent event)
                    {
                        // Do nothing; just showing how to use it.
                    }
                });

                this.wwd.addSelectListener(new SelectListener()
                {
                    private WWIcon lastToolTipIcon = null;

                    public void selected(SelectEvent event)
                    {
                        if (event.getEventAction().equals(SelectEvent.LEFT_CLICK))
                        {
                            if (event.hasObjects())
                                System.out.println("Single clicked " + event.getTopObject());
                            else
                                System.out.println("Single clicked " + "no object");
                        }
                        else if (event.getEventAction().equals(SelectEvent.LEFT_DOUBLE_CLICK))
                        {
                            if (event.hasObjects())
                                System.out.println("Double clicked " + event.getTopObject());
                            else
                                System.out.println("Double clicked " + "no object");
                        }
                        else if (event.getEventAction().equals(SelectEvent.RIGHT_CLICK))
                        {
                            if (event.hasObjects())
                                System.out.println("Right clicked " + event.getTopObject());
                            else
                                System.out.println("Right clicked " + "no object");
                        }
                        else if (event.getEventAction().equals(SelectEvent.HOVER))
                        {
                            if (lastToolTipIcon != null)
                            {
                                lastToolTipIcon.setShowToolTip(false);
                                this.lastToolTipIcon = null;
                                AWT1UpFrame.this.wwd.repaint();
                            }

                            if (event.hasObjects())
                            {
                                if (event.getTopObject() instanceof WWIcon)
                                {
                                    this.lastToolTipIcon = (WWIcon) event.getTopObject();
                                    lastToolTipIcon.setShowToolTip(true);
                                    AWT1UpFrame.this.wwd.repaint();
                                }
                            }
                        }
                        else if (event.getEventAction().equals(SelectEvent.ROLLOVER))
                        {
                            AWT1UpFrame.this.highlight(event.getTopObject());
                        }
                    }
                });
                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        WWIcon lastPickedIcon;

        private void highlight(Object o)
        {
            if (this.lastPickedIcon == o)
                return; // same thing selected

            if (this.lastPickedIcon != null)
            {
                this.lastPickedIcon.setHighlighted(false);
                this.lastPickedIcon = null;
            }

            if (o != null && o instanceof WWIcon)
            {
                this.lastPickedIcon = (WWIcon) o;
                this.lastPickedIcon.setHighlighted(true);
            }
        }

        private IconLayer buildIconLayer()
        {
            IconLayer layer = new IconLayer();

            for (double lat = 0; lat < 10; lat += 10)
            {
                for (double lon = -180; lon < 180; lon += 10)
                {
                    double alt = 0;
                    if (lon % 90 == 0)
                        alt = 2000000;
                    WWIcon icon = new UserFacingIcon("demo/images/32x32-icon-nasa.png",
                        new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon), alt));
                    icon.setHighlightScale(1.5);
                    icon.setToolTipFont(this.makeToolTipFont());
                    icon.setToolTipText(icon.getImageSource().toString());
                    icon.setToolTipTextColor(java.awt.Color.YELLOW);
                    layer.addIcon(icon);
                }
            }

            return layer;
        }

        private PlacemarkLayer buildPlacemarkLayer() 
        {
        	double lat = 0.0, lon = 0.0, alt =1000;
        	//String iconPath = "demo/images/placemark.png";
        	Position position = new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon), alt);
        	
        	//WWIcon icon = new UserFacingIcon(iconPath, position);
        	
        	PlacemarkLayer l = new PlacemarkLayer();
        	l.addDefaultIcon(position.toString(), position); // addIcon(icon);
        	
        	return l;
        }

        private LayerList buildNASAWMSLayerList() 
        {
        	NASAWMSLayerList list = new NASAWMSLayerList();
        	list.setAllEnabled(false);
        	list.setEnabled(NASAWMSLayerList.US_STATE_BOUNDARIES, true);
        	
        	return list;
        }

        private Layer buildWMSLayer() 
        {
        	Sector bbox = new Sector(Angle.fromDegrees(-18.75), Angle.fromDegrees(28.75),
                    Angle.fromDegrees(-65), Angle.fromDegrees(125));
        	
        	TiledWMSLayer l = new TiledWMSLayer( new TiledWMSLayerDescriptor(
    				"http://svs.gsfc.nasa.gov/cgi-bin/wms"
    				, "3169_21478"			// layer name
    				, "Pinatubo Erupt cloud"
    				, "EPSG:4326"			// SRS
    				, null					// styles
    				, bbox					// bbox
    				, "1991-06-16"			// time
    				, TiledWMSLayer.FMT_PNG 
    				, 1 		// levels
    				, 1024, 256	// w h
    				, TiledWMSLayer.VERSION_1_1_1	//
    				, "Earth/WMS" // WW cache name
        	));
        	
        	return l;
        }

        private Layer buildGroundOverlay() {
//        	Sector bbox = new Sector(Angle.fromDegrees(-18.75), Angle.fromDegrees(28.75),
//                    Angle.fromDegrees(-65), Angle.fromDegrees(125));
        	Sector bbox = new Sector(Angle.fromDegrees(-90.0), Angle.fromDegrees(90.0),
                    Angle.fromDegrees(-180), Angle.fromDegrees(180));
			//"http://svs.gsfc.nasa.gov/cgi-bin/wms?service=WMS&request=GetMap&layers=3169_21478&time=1991-06-16&bbox=-65,-18.75,125,28.75&width=1024&height=256&srs=EPSG:4326&format=image/png&version=1.1.1"

        	try {
            	URL url = new URL(
            		"http://svs.gsfc.nasa.gov/cgi-bin/wms?service=WMS&request=GetMap&layers=3210_22210&time=2004-09-01T03Z&bbox=-180,-90,180,90&width=1000&height=721&srs=EPSG:4326&format=image/png&version=1.1.1"
            			);
            	
            	return new GroundOverlayLayer("Pinatubo"
            			, bbox
            			, url
            			, ParserUtils.getIconSuffix(url.toString()));
			} 
        	catch (Exception e) {
				e.printStackTrace();
			}
			return null;
        }
        
        
        private TimeLoopGroundOverlay buildAnimatedGroundOverlay(WorldWindowGLCanvas world) 
        {
            TimeLoopGroundOverlay ago = 
            	new TimeLoopGroundOverlay("Pinatubo"); //, world);
            
        	Sector bbox = new Sector(Angle.fromDegrees(-18.75), Angle.fromDegrees(28.75),
                    Angle.fromDegrees(-65), Angle.fromDegrees(125));

        	String  pref = "http://svs.gsfc.nasa.gov/cgi-bin/wms?"
        		+ "service=WMS&request=GetMap&layers=3169_21478&bbox=-65,-18.75,125,28.75"
        		+ "&width=1024&height=256&srs=EPSG:4326&format=image/png&version=1.1.1";
        	// &time=1991-06-16
        	
        	try {
        		final String datePref = "1991-06-";
        		
        		for (int i = 16; i <= 29; i++) 
        		{
        			final String icon = pref + "&time=" + datePref + i;

                	ago.add(new GroundOverlayLayer(datePref + i
                			, bbox
                			, new URL(icon)
                			, ParserUtils.getIconSuffix(icon)
                	));
    			}
            	
            	ago.addToModel(world);
            	
			} catch (Exception e) {
				e.printStackTrace();
			}
            return ago;
        }
        
        private ScreenOverlayLayer buildScreenOverlayLayer() 
        {
        	String iconPath = "demo/images/legend.png";
        	return new ScreenOverlayLayer("Legend (NW)", iconPath, ScreenOverlayLayer.NORTHWEST);
        }
        
        private RenderableLayer buildShapesLayer()
        {
            RenderableLayer layer = new RenderableLayer();

            Color interiorColor = new Color(1f, 1f, 0f, 0.3f);
            Color borderColor = new Color(1f, 1f, 0f, 0.4f);

            SurfaceSector quad = new SurfaceSector(new Sector(
                    Angle.fromDegrees(41.0), Angle.fromDegrees(41.6),
                    Angle.fromDegrees(-122.5), Angle.fromDegrees(-121.7)),
                    interiorColor, borderColor);
            layer.addRenderable(quad);

            quad = new SurfaceSector(new Sector(
                    Angle.fromDegrees(38.9), Angle.fromDegrees(39.3),
                    Angle.fromDegrees(-120.2), Angle.fromDegrees(-119.9)),
                    new Color(0f, 1f, 1f, 0.3f), new Color(0.5f, 1f, 1f, 0.4f));
            layer.addRenderable(quad);

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
            layer.addRenderable(polygon);

            return layer;
        }

        private static final String lineTestString =
            "<gml:LineString> <gml:posList>45.256 -110.45 46.46 -109.48 43.84 -109.86</gml:posList></gml:LineString>";
        private static final String itemTestString =
            "<item>    <title>M 3.2, Mona Passage</title>    <link>http://example.org/2005/09/09/atom01</link>    <pubDate>Wed, 17 Aug 2005 07:02:32 GMT</pubDate>    <georss:where>      <gml:Polygon>        <gml:exterior>          <gml:LinearRing>            <gml:posList>    			        45.256 -110.45 46.46 -109.48 43.84 -109.86 45.256 -110.45        	       </gml:posList>          </gml:LinearRing>   </gml:exterior>  </gml:Polygon> </georss:where> </item>";

        private RenderableLayer buildGeoRSSLayer()
        {
            try
            {
                DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
                docBuilderFactory.setNamespaceAware(true);
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(new File("GeoRSSTestData.xml"));
                List<Renderable> shapes = GeoRSSParser.parseShapes(doc);


                RenderableLayer layer = new RenderableLayer();
                if (shapes != null)
                {
                    for (Renderable shape : shapes)
                    {
                        layer.addRenderable(shape);
                    }
                }

                return layer;
            }
            catch (ParserConfigurationException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (SAXException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        private Font makeToolTipFont()
        {
            HashMap<TextAttribute, Object> fontAttributes = new HashMap<TextAttribute, Object>();

            fontAttributes.put(TextAttribute.BACKGROUND, new java.awt.Color(0.4f, 0.4f, 0.4f, 1f));
            return Font.decode("Arial-BOLD-14").deriveFont(fontAttributes);
        }
    }

    static
    {
        if (gov.nasa.worldwind.Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind AWT Canvas App");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }

    public static void main(String[] args)
    {
        System.out.println("Java run-time version: " + System.getProperty("java.version"));

        try
        {
            AWT1UpFrame frame = new AWT1UpFrame();
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
