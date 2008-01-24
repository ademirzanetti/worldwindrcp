/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package demo;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.nasa.worldwind.retrieve.RetrievalService;

import javax.swing.*;

import java.awt.Point;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: StatusBar.java 1764 2007-05-07 20:01:57Z tgaskins $
 */
public class StatusBar extends JPanel implements PositionListener
{
    private WorldWindow eventSource;
    private final JLabel latDisplay = new JLabel("");
    private final JLabel lonDisplay = new JLabel("Off globe");
    private final JLabel eleDisplay = new JLabel("");

//    private AnnotationLayer netStatus = new AnnotationLayer();
//    
//    private void initAnnotation () 
//    {
//    	ScreenAnnotation sa = new ScreenAnnotation("", new Point(10, 10));
//    	
//    	sa.getAttributes().setCornerRadius(0);
//    	sa.getAttributes().setDrawOffset(new Point(55, 20)); // screen point is annotation bottom left corner
//    	sa.getAttributes().setOpacity(0);
//    	
//    	netStatus.addAnnotation(sa);
//    }
    
    public StatusBar()
    {
        super(new java.awt.GridLayout(1, 0));

        //initAnnotation();
        
        final JLabel heartBeat = new JLabel("Downloading");

        latDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        lonDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        eleDisplay.setHorizontalAlignment(SwingConstants.CENTER);

        this.add(new JLabel("")); // dummy label to visually balance with heartbeat
        this.add(latDisplay);
        this.add(lonDisplay);
        this.add(eleDisplay);
        this.add(heartBeat);

        heartBeat.setHorizontalAlignment(SwingConstants.CENTER);
        heartBeat.setForeground(new java.awt.Color(255, 0, 0, 0));

        Timer downloadTimer = new Timer(50, new ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent actionEvent)
            {

                java.awt.Color color = heartBeat.getForeground();

                int alpha = color.getAlpha();
                
                RetrievalService service = WorldWind.getRetrievalService();
                
                if (service.hasActiveTasks())
                {
                    if (alpha == 255)
                        alpha = 255;
                    else
                        alpha = alpha < 16 ? 16 : Math.min(255, alpha + 20);
                }
                else
                {
                    alpha = Math.max(0, alpha - 20);
                }
                heartBeat.setForeground(new java.awt.Color(255, 0, 0, alpha));
                heartBeat.setText("Downloading " 
                		+ service.getNumRetrieversPending() + "/"
                		+ service.getRetrieverPoolSize());
                
//                if ( netStatus != null ) 
//                {
//                	double opacity = ((double)alpha/255);
//                
//                	Annotation sa = netStatus.getAnnotations().iterator().next();
//                	
//                	if ( sa != null ) {
//                		sa.setText("Pending requests " + service.getNumRetrieversPending());
//                		sa.getAttributes().setOpacity(opacity);
//                		eventSource.redraw();
//                	}
//                }
                
            }
        });
        downloadTimer.start();
    }

    public void setEventSource(WorldWindow newEventSource)
    {
        if (this.eventSource != null)
            this.eventSource.removePositionListener(this);

        if (newEventSource != null)
            newEventSource.addPositionListener(this);

        this.eventSource = newEventSource;
        //eventSource.getModel().getLayers().add(netStatus);
    }

    public void moved(PositionEvent event)
    {
        this.handleCursorPositionChange(event);
    }

    public WorldWindow getEventSource()
    {
        return this.eventSource;
    }

    private void handleCursorPositionChange(PositionEvent event)
    {
        Position newPos = (Position) event.getPosition();
        if (newPos != null)
        {
            String las = String.format("Latitude %7.3f\u00B0",
                newPos.getLatitude().getDegrees());
            String los = String.format("Longitude %7.3f\u00B0",
                newPos.getLongitude().getDegrees());
            String els = String.format("Elevation %7d meters", (int) newPos.getElevation());
            latDisplay.setText(las);
            lonDisplay.setText(los);
            eleDisplay.setText(els);
        }
        else
        {
            latDisplay.setText("");
            lonDisplay.setText("Off globe");
            eleDisplay.setText("");
        }
    }
}
