/* Copyright (C) 2001, 2006 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package demo;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.Earth.*;

import javax.swing.*;
import javax.swing.border.*;

import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
//import worldwind.contrib.layers.todo.VirtualEarthLayer;
import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.todo.layers.VirtualEarthLayer;


import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * @author tag
 * @version $Id: WWPieceMaker.java 1764 2007-05-07 20:01:57Z tgaskins $
 */
public class DemoBasic
{

    private static class AppFrame extends JFrame
    {

        private final WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
    	
        private DemoBasic.LayerAction[] layers = new DemoBasic.LayerAction[] {
            new DemoBasic.LayerAction(new BMNGSurfaceLayer(), true, wwd),
            new DemoBasic.LayerAction(new LandsatI3(), false, wwd),
            new DemoBasic.LayerAction(new USGSDigitalOrtho(), false, wwd),
            new DemoBasic.LayerAction(new USGSUrbanAreaOrtho(), false, wwd),
            new DemoBasic.LayerAction(new EarthNASAPlaceNameLayer(), true, wwd),
            new DemoBasic.LayerAction(new CompassLayer(), false, wwd),
            new DemoBasic.LayerAction(new VirtualEarthLayer(wwd), false, wwd),
            null
        };
        //new DemoBasic.LayerAction(new VirtualEarthLayer(), false)
    	
        public AppFrame() //DemoBasic.LayerAction[] layers)
        {
            LayerList layerList = new LayerList();

            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            m.setShowWireframeExterior(false);
            m.setShowWireframeInterior(false);
            m.setShowTessellationBoundingVolumes(false);
            m.setLayers(layerList);
            wwd.setModel(m);
            
            try
            {
                JPanel mainPanel = new JPanel();
                mainPanel.setLayout(new BorderLayout());
                wwd.setPreferredSize(new Dimension(800, 600));
                mainPanel.add(wwd, BorderLayout.CENTER);

                StatusBar statusBar = new StatusBar();
                statusBar.setEventSource(wwd);
                mainPanel.add(statusBar, BorderLayout.PAGE_END);
                this.getContentPane().add(mainPanel, BorderLayout.CENTER);
            
                TimeLoopGroundOverlay anim = buildAnimatedGroundOverlay(wwd);
                layers[layers.length - 1] = new DemoBasic.LayerAction(anim, false, wwd);
            	
                JPanel westContainer = new JPanel(new BorderLayout());
                {
                    JPanel westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
                    westPanel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));
                    {
                        JPanel layersPanel = new JPanel(new GridLayout(0, 1, 0, 15));
                        layersPanel.setBorder(new TitledBorder("Layers"));
                        
                        for (DemoBasic.LayerAction action : layers)
                        {
                            JCheckBox jcb = new JCheckBox(action);
                            jcb.setSelected(action.selected);
                            layersPanel.add(jcb);
                            layerList.add(action.layer);

                            if (action.layer instanceof TiledImageLayer)
                                ((TiledImageLayer) action.layer).setShowImageTileOutlines(false);

                            if (action.layer instanceof LandsatI3)
                                ((TiledImageLayer) action.layer).setDrawBoundingVolumes(false);

                            if (action.layer instanceof USGSDigitalOrtho)
                                ((TiledImageLayer) action.layer).setDrawTileIDs(false);
                        }
                        
                        // add VE layer
//                        DemoBasic.LayerAction ve = new DemoBasic.LayerAction(
//                        		new VirtualEarthLayer(wwd), false, wwd);

//                		anim.addToModel(wwd.getModel());
//                		anim.setWindowCanvas(wwd);
                        
//                        JCheckBox jcb = new JCheckBox(ve);
//                        jcb.setSelected(ve.selected);
                        
//                        layersPanel.add(jcb);
//                        layerList.add(ve.layer);
                        
                        westPanel.add(layersPanel);
                        westContainer.add(westPanel, BorderLayout.NORTH);
                    }
                }

                this.getContentPane().add(westContainer, BorderLayout.WEST);
                this.pack();

                Dimension prefSize = this.getPreferredSize();
                prefSize.setSize(prefSize.getWidth(), 1.1 * prefSize.getHeight());
                this.setSize(prefSize);

                // Center the app on the user's screen.
                Dimension parentSize;
                Point parentLocation = new Point(0, 0);
                parentSize = Toolkit.getDefaultToolkit().getScreenSize();
                int x = parentLocation.x + (parentSize.width - prefSize.width) / 2;
                int y = parentLocation.y + (parentSize.height - prefSize.height) / 2;
                this.setLocation(x, y);
                this.setResizable(true);

                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /*
     * Animated screen overlay example: Toxic clod of mount pinatubo eruption
     */
    static private TimeLoopGroundOverlay buildAnimatedGroundOverlay(WorldWindowGLCanvas canvas) 
    {
        TimeLoopGroundOverlay overlay = 
        	new TimeLoopGroundOverlay("Pinatubo Toxic Cloud", canvas); 
        
    	Sector bbox = new Sector(Angle.fromDegrees(-18.75), Angle.fromDegrees(28.75),
                Angle.fromDegrees(-65), Angle.fromDegrees(125));

    	String  pref = "http://svs.gsfc.nasa.gov/cgi-bin/wms?"
    		+ "service=WMS&request=GetMap&layers=3169_21478&bbox=-65,-18.75,125,28.75"
    		+ "&width=1024&height=256&srs=EPSG:4326&format=image/png&version=1.1.1";
    	
    	// Pinatubo cloud time span: 1991-06-16/1991-06-29/P1D
    	try {
    		final String datePref = "1991-06-";
    		
    		for (int i = 16; i <= 29; i++) 
    		{
    			final String icon = pref + "&time=" + datePref + i;

            	overlay.add(new GroundOverlayLayer(datePref + i
            			, bbox
            			, new URL(icon)
            			, ParserUtils.getIconSuffix(icon)
            	));
			}
		} 
    	catch (Exception e) {
			e.printStackTrace();
		}
        return overlay;
    }

    private static class LayerAction extends AbstractAction
    {
        private Layer layer;
        private boolean selected;
        private WorldWindowGLCanvas canvas;
        
        public LayerAction(Layer layer, boolean selected, WorldWindowGLCanvas canvas)
        {
            super(layer.getName());
            this.layer = layer;
            this.selected = selected; 
            this.layer.setEnabled(this.selected);
            this.canvas = canvas;
        }

        public void actionPerformed(ActionEvent actionEvent)
        {
        	
            if (((JCheckBox) actionEvent.getSource()).isSelected()) 
            {
                this.layer.setEnabled(true);
                if (layer instanceof TimeLoopGroundOverlay) 
                {
                	System.out.println("play");
                	((TimeLoopGroundOverlay)layer).addToModel(canvas);
	    			((TimeLoopGroundOverlay)layer).play();
                }
            }
            else {
                this.layer.setEnabled(false);
                if (layer instanceof TimeLoopGroundOverlay) {
	    			((TimeLoopGroundOverlay)layer).stop();
	    			//((TimeLoopGroundOverlay)layer).removeFromModel(canvas.getModel());
              }
            }

            appFrame.wwd.repaint();
        }
    }

    static
    {
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Basic Demo");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
        }
    }

    private static DemoBasic.AppFrame appFrame;

    public static void main(String[] args)
    {
        System.out.println("Java run-time version: " + System.getProperty("java.version"));
        System.out.println(gov.nasa.worldwind.Version.getVersion());

        try
        {
            DemoBasic demo = new DemoBasic();
            appFrame = new DemoBasic.AppFrame(); //demo.layers);
            appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            appFrame.setVisible(true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
