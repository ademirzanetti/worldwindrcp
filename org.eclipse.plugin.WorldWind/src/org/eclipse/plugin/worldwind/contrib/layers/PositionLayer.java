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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL;

import org.eclipse.plugin.worldwind.contrib.Messages;

import com.sun.opengl.util.j2d.TextRenderer;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;

/**
 * Position Layer based on the ScalebarLayer.
 * It uses an annotation to let the users know about network pending requests.
 * By default, it displays the message: Pointer LAT LON ELEVATION EYE-ALTITUDE
 * on the south west side of the view port. 
 * @author Owner
 * @deprecated doesn't work properly w/ wwj 0.6
 */
public class PositionLayer extends RenderableLayer
	implements PositionListener
{
	// Positioning constants
    public final static String NORTHWEST = "gov.nasa.worldwind.ScalebarLayer.NorthWest";
    public final static String SOUTHWEST = "gov.nasa.worldwind.ScalebarLayer.SouthWest";
    public final static String NORTHEAST = "gov.nasa.worldwind.ScalebarLayer.NorthEast";
    public final static String SOUTHEAST = "gov.nasa.worldwind.ScalebarLayer.SouthEast";

    private String resizeBehavior = AVKey.RESIZE_SHRINK_ONLY;
    private double toViewportScale = 0.2;
    
	private Dimension size = new Dimension(300, 1);
	private Color color = Color.white;
    
	private String position = SOUTHWEST;
	private int borderWidth = 20;

	private Vec4 locationCenter 		= null;
	private TextRenderer textRenderer 	= null;
	private Font defaultFont 			= Font.decode("Arial-12-BOLD");

	// Position string: Pointer LAT LON ELEVATION EYE-ALTITUDE
	private String pointerMessage;
	
	private final WorldWindowGLCanvas canvas;
	
	// The status of the network is displayed using an annotation on the lower left
   // private AnnotationLayer netStatus = new AnnotationLayer();

    // Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
/*    private OrderedIcon orderedImage = new OrderedIcon();

    private class OrderedIcon implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            draw(dc);
        }

        public void render(DrawContext dc)
        {
            draw(dc);
        }
    } */
	
	
    private OrderedLayer orderedLayer = new OrderedLayer();

    private class OrderedLayer implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            draw(dc);
        }

        public void render(DrawContext dc)
        {
            draw(dc);
        }
    }

	
    /*
     * Init the net status annotation
     */
/*    
    private void initAnnotation () 
    {
    	ScreenAnnotation sa = new ScreenAnnotation("", new Point(10, 10));
    	
    	sa.getAttributes().setCornerRadius(0);
    	sa.getAttributes().setDrawOffset(new Point(60, 20)); // screen point is annotation bottom left corner
    	sa.getAttributes().setOpacity(0);
    	
    	netStatus.addAnnotation(sa);
    	netStatus.setName("Network");
    } */
    
    /**
     * Constructor
     * @param canvas
     */
	public PositionLayer(final WorldWindowGLCanvas canvas) 
	{
		this.canvas = canvas;
		this.setName("Pointer");

		// listen for position events
		canvas.addPositionListener(this);
		
		// init the network status annotation
//		initAnnotation();

		// "Pending requests"
		final String netStatusLabel = Messages.getText("layers.position.1");
/*		
		// Network status probe
        Timer downloadTimer = new Timer(50, new ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent actionEvent)
            {
            	if ( netStatus == null ) return;
            	
            	Annotation sa = netStatus.getAnnotations().iterator().next();
            	
            	if ( sa == null ) return;
            	
                int alpha = (int)(sa.getAttributes().getOpacity() * 255);
                
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
                
            	double opacity = ((double)alpha/255);
            	
        		sa.setText(netStatusLabel + " " + service.getNumRetrieversPending());
        		sa.getAttributes().setOpacity(opacity);
        		canvas.redraw();
            }
        });
        downloadTimer.start();
		*/
	}

	@Override
	public void setEnabled(boolean enabled) 
	{
		super.setEnabled(enabled);
//		netStatus.setEnabled(enabled);
		
		if ( enabled ) canvas.addPositionListener(this);
		else canvas.removePositionListener(this);
	}
	
	@Override
	public void dispose() {
	}
	    
    // fires when position changes
    public void moved(PositionEvent event)
    {
        Position newPos = (Position) event.getPosition();
        String position = "";
        if (newPos != null)
        {
        	double lat = newPos.getLatitude().getDegrees();
        	double lon = newPos.getLongitude().getDegrees();
        	
        	String NS = ( lat > 0) ? "N" : "S";
        	String EW = ( lon > 0) ? "E" : "W";
        	
            String Lat = String.format("%7.2f\u00B0", Math.abs(lat) ) + " " + NS;
            String Lon = String.format("%7.2f\u00B0", Math.abs(lon) ) + " " + EW;
            
            String elv = String.format("Elev %5d m", (int) newPos.getElevation());
            
            position = Lat + " " + Lon + "   " + elv; // "Pointer " + 
        }
        pointerMessage = position;
    }
	
	private Vec4 computeLocation(java.awt.Rectangle viewport, double scale)
	{
		double scaledWidth = scale * this.size.width;
		double scaledHeight = scale * this.size.height;

		double x;
		double y;

		if (this.locationCenter != null)
		{
			x = viewport.getWidth() - scaledWidth / 2 - this.borderWidth;
			y = viewport.getHeight() - scaledHeight / 2 - this.borderWidth;
		}
		else if (this.position.equals(NORTHEAST))
		{
			x = viewport.getWidth() - scaledWidth - this.borderWidth;
			y = viewport.getHeight() - scaledHeight - this.borderWidth;
		}
		else if (this.position.equals(SOUTHEAST))
		{
			x = viewport.getWidth() - scaledWidth - this.borderWidth;
			y = 0d + this.borderWidth;
		}
		else if (this.position.equals(NORTHWEST))
		{
			x = 0d + this.borderWidth;
			y = viewport.getHeight() - scaledHeight - this.borderWidth;
		}
		else if (this.position.equals(SOUTHWEST))
		{
			x = 0d + this.borderWidth;
			y = 0d + this.borderWidth;
		}
		else // use North East
		{
			x = viewport.getWidth() - scaledWidth / 2 - this.borderWidth;
			y = viewport.getHeight() - scaledHeight / 2 - this.borderWidth;
		}

		return new Vec4(x, y, 0);
	}
	
	private double computeScale(java.awt.Rectangle viewport)
	{
		if (this.resizeBehavior.equals(AVKey.RESIZE_SHRINK_ONLY))
		{
			return Math.min(1d, (this.toViewportScale) * viewport.width / this.size.width);
		}
		else if (this.resizeBehavior.equals(AVKey.RESIZE_STRETCH))
		{
			return (this.toViewportScale) * viewport.width / this.size.width;
		}
		else if (this.resizeBehavior.equals(AVKey.RESIZE_KEEP_FIXED_SIZE))
		{
			return 1d;
		}
		else
		{
			return 1d;
		}
	}
	
    // Draw the scale label
	private void drawLabel(String text, Vec4 screenPoint)
    {
		if (this.textRenderer == null) {
			this.textRenderer =  new TextRenderer(this.defaultFont, true, true);
		}

		Rectangle2D nameBound = this.textRenderer.getBounds(text);
		int x = (int) (screenPoint.x() - nameBound.getWidth() / 2d);
		int y = (int) screenPoint.y();

		this.textRenderer.begin3DRendering();

        this.textRenderer.setColor(this.getBackgroundColor(this.color));
        this.textRenderer.draw(text, x + 1, y - 1);
		this.textRenderer.setColor(this.color);
		this.textRenderer.draw(text, x, y);

		this.textRenderer.end3DRendering();

	}
	
	private final float[] compArray = new float[4];
	
    // Compute background color for best contrast
    private Color getBackgroundColor(Color color)
    {
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), compArray);
        if (compArray[2] > 0.5)
            return new Color(0, 0, 0, 0.7f);
        else
            return new Color(1, 1, 1, 0.7f);
    }
	
	
	@Override
	public void doRender(DrawContext dc) 
	{
		//addAnnotation();
		dc.addOrderedRenderable(orderedLayer);
	}
	
    @Override
    public void doPick(DrawContext dc, Point pickPoint)
    {
        // Delegate drawing to the ordered renderable list
        dc.addOrderedRenderable(this.orderedLayer);
    }
	
/*	
	// Rendering
    public void draw(DrawContext dc)
    {
    	if ( pointerMessage == null ) return;
    	
        GL gl = dc.getGL();

        OGLStackHandler ogsh = new OGLStackHandler();

        try
        {
            ogsh.pushAttrib(gl, GL.GL_TRANSFORM_BIT);

            gl.glDisable(GL.GL_DEPTH_TEST);

            double width = this.size.width;
            double height = this.size.height;

            // Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
            // into the GL projection matrix.
            java.awt.Rectangle viewport = dc.getView().getViewport();
            ogsh.pushProjectionIdentity(gl);
            double maxwh = width > height ? width : height;
            gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

            ogsh.pushModelviewIdentity(gl);

            // Scale to a width x height space
            // located at the proper position on screen
            double scale = this.computeScale(viewport);
            Vec4 locationSW = this.computeLocation(viewport, scale);
            gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
            gl.glScaled(scale, scale, 1);
            
            
			double divWidth = size.width * 500;
			
            // Set color using current layer opacity
            Color backColor = this.getBackgroundColor(this.color);
            float[] colorRGB = backColor.getRGBColorComponents(null);
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], (double)backColor.getAlpha() / 255d * this.getOpacity());
            
            // Draw label ( Add eye altitude)
            Position eye 	= dc.getGlobe().computePositionFromPoint(dc.getView().getEyePoint());
            
            if ( pointerMessage != null && pointerMessage.length() > 0 )
            	pointerMessage += String.format("  Alt %8.1f Km", eye.getElevation()/1e3);
            
            String label = pointerMessage ;
            
            gl.glLoadIdentity();
            gl.glDisable(GL.GL_CULL_FACE);
            drawLabel(label, locationSW.add3(
            		new Vec4(divWidth * scale / 2 + (width - divWidth) / 2 + 5
                    			, height * scale - 10
                    			, 0)));
            
        }
        finally
        {
            gl.glColor4d(1d, 1d, 1d, 1d); // restore the default OpenGL color
            gl.glEnable(GL.GL_DEPTH_TEST);

            if (!dc.isPickingMode())
            {
                gl.glBlendFunc(GL.GL_ONE, GL.GL_ZERO); // restore to default blend function
                gl.glDisable(GL.GL_BLEND); // restore to default blend state
            }
            
            ogsh.pop(gl);
        }
    }
*/	
	
	private void draw(DrawContext dc)
	{
		if ( pointerMessage == null ) return;
		
		GL gl = dc.getGL();

		boolean attribsPushed = false;
		boolean modelviewPushed = false;
		boolean projectionPushed = false;
		
		try {
			gl.glPushAttrib( GL.GL_DEPTH_BUFFER_BIT
					| GL.GL_COLOR_BUFFER_BIT
					| GL.GL_ENABLE_BIT
					| GL.GL_TEXTURE_BIT
					| GL.GL_TRANSFORM_BIT  
					| GL.GL_VIEWPORT_BIT
					| GL.GL_CURRENT_BIT);
			attribsPushed = true;

			gl.glDisable(GL.GL_TEXTURE_2D);		// no textures
			
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDisable(GL.GL_DEPTH_TEST);
			
			double width = this.size.width;
			double height = this.size.height;

			// Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
			// into the GL projection matrix.
			java.awt.Rectangle viewport = dc.getView().getViewport();
			gl.glMatrixMode(javax.media.opengl.GL.GL_PROJECTION);
			
			gl.glPushMatrix();
			
			projectionPushed = true;
			gl.glLoadIdentity();
			
			double maxwh = width > height ? width : height;
			gl.glOrtho(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

			gl.glMatrixMode(GL.GL_MODELVIEW);
			gl.glPushMatrix();
			modelviewPushed = true;
			gl.glLoadIdentity();
			
			// Scale to a width x height space
			// located at the proper position on screen
			double scale = 1.0;
			Vec4 locationSW = this.computeLocation(viewport, scale);
			gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
			gl.glScaled(scale, scale, 1);

			double divWidth = size.width * 500;
			
            // Set color using current layer opacity
            Color backColor = this.getBackgroundColor(this.color);
            float[] colorRGB = backColor.getRGBColorComponents(null);
            gl.glColor4d(colorRGB[0], colorRGB[1], colorRGB[2], (double)backColor.getAlpha() / 255d * this.getOpacity());
            
            // Draw label ( Add eye altitude)
            Position eye 	= dc.getGlobe().computePositionFromPoint(dc.getView().getEyePoint());
            
            if ( pointerMessage != null && pointerMessage.length() > 0 )
            	pointerMessage += String.format("  Alt %8.1f Km", eye.getElevation()/1e3);
            
            //String label = pointerMessage ;
            
            gl.glLoadIdentity();
            gl.glDisable(GL.GL_CULL_FACE);
            drawLabel(pointerMessage, locationSW.add3(
            		new Vec4(divWidth * scale / 2  + (width - divWidth) / 2 - 20 // + 5
                    			, height * scale - 10
                    			, 0)));
			
		} 
		finally
		{
			if (projectionPushed)
			{
				gl.glMatrixMode(GL.GL_PROJECTION);
				gl.glPopMatrix();
			}
			if (modelviewPushed)
			{
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glPopMatrix();
			}
			if (attribsPushed)
				gl.glPopAttrib();
			
			pointerMessage = null;
		}
	}

	/**
	 * Add the net status annotation to the layer model
	 */
/*	
	private void addAnnotation () 
	{
		if ( canvas.getModel() == null ) return;
		
		// Add the anotations layer to the model for rendering
		if (!canvas.getModel().getLayers().contains(netStatus)) {
			canvas.getModel().getLayers().add(netStatus);
		}
	} */
	
}
