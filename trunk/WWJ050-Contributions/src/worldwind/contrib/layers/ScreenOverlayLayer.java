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

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

import javax.media.opengl.GL;

import org.apache.log4j.Logger;

import worldwind.contrib.Messages;
import worldwind.contrib.layers.GroundOverlayLayer.OverlayListener;

import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureIO;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.Logging;

/**
 * A class used to render legends. It is exactly the same as the
 * compass layer but the image doesn't rotate with the globe
 * @author Owner
 *
 */
public class ScreenOverlayLayer extends AbstractLayer
{
	private static final Logger logger = Logger.getLogger(ScreenOverlayLayer.class);
	
    public final static String NORTHWEST = "gov.nasa.worldwind.CompassLayer.NorthWest";
    public final static String SOUTHWEST = "gov.nasa.worldwind.CompassLayer.SouthWest";
    public final static String NORTHEAST = "gov.nasa.worldwind.CompassLayer.NorthEast";
    public final static String SOUTHEAST = "gov.nasa.worldwind.CompassLayer.SouthEast";
	
    /**
     * On window resize, scales the compass icon to occupy a constant relative size of the viewport.
     */
    public final static String RESIZE_STRETCH = "gov.nasa.worldwind.CompassLayer.ResizeStretch";
    /**
     * On window resize, scales the compass icon to occupy a constant relative size of the viewport, but not larger than
     * the icon's inherent size scaled by the layer's icon scale factor.
     */
    public final static String RESIZE_SHRINK_ONLY = "gov.nasa.worldwind.CompassLayer.ResizeShrinkOnly";
    /**
     * Does not modify the compass icon size when the window changes size.
     */
    public final static String RESIZE_KEEP_FIXED_SIZE = "gov.nasa.worldwind.CompassLayer.ResizeKeepFixedSize";
    
    private File iconFile = null; 
    
    private double compassToViewportScale = 0.27; 
    private double iconScale = 1.0;
    private int borderWidth = 20; 
    private String position = SOUTHEAST; 
    private String resizeBehavior = RESIZE_SHRINK_ONLY;
    private Texture iconTexture = null;
    private Vec4 locationCenter = null;
    
    private URL iconURL = null;

    // Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
    private OrderedIcon orderedIcon = new OrderedIcon();

    // status listeners
    private Vector<GroundOverlayLayer.OverlayListener> listeners = new Vector<OverlayListener>();

    private class OrderedIcon implements OrderedRenderable
    {
        public double getDistanceFromEye()
        {
            return 0;
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            drawIcon(dc);
        }

        public void render(DrawContext dc)
        {
            drawIcon(dc);
        }
    }
    
    /**
     * ScreenOverlayLayer
     */
    public ScreenOverlayLayer(){
    	super();
    }
    
    /**
     * ScreenOverlayLayer
     * @param name
     * @param iconFilePath
     * @param position
     */
    public ScreenOverlayLayer(String name, String iconFilePath, String position) 
    {
    	super();
    	setName(name);

    	// does iconFilePath exist?
    	boolean iconFound = WorldWind.getDataFileCache().findFile(iconFilePath, false) != null;
    	
    	try {
        	if ( ! iconFound ) {
        		setIconFilePath(new File(iconFilePath));
        	}
        	else {
        		setIconFilePath(WorldWind.getDataFileCache().newFile(iconFilePath));
        	}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	setPosition(position);
    	
    	logger.debug("Screen ov=" + name + " Path=" + iconFilePath 
    			+ " file=" + iconFile + " Pos=" + position + " u=" + iconURL);
    }

    public ScreenOverlayLayer(String name, URL iconURL, String position) 
    {
    	logger.debug(name + "," + iconURL + "," + position);
    	
    	setName(name);
    	setPosition(position);
    	this.iconURL = iconURL;

    }

    /**
	 * Add an overlay status listener
	 * @param listener
	 */
	public void addOverlayListener( OverlayListener listener){
		listeners.add(listener);
	}
    
    @Override
    protected void doRender(DrawContext dc) {
    	dc.addOrderedRenderable(orderedIcon);
    }

    public void drawIcon(DrawContext dc)
    {
        if (this.iconFile == null && iconURL == null)
            return;

        GL gl = dc.getGL();

        boolean attribsPushed = false;
        boolean modelviewPushed = false;
        boolean projectionPushed = false;

        try
        {
            gl.glPushAttrib(GL.GL_DEPTH_BUFFER_BIT
                | GL.GL_COLOR_BUFFER_BIT
                | GL.GL_ENABLE_BIT
                | GL.GL_TEXTURE_BIT
                | GL.GL_TRANSFORM_BIT
                | GL.GL_VIEWPORT_BIT
                | GL.GL_CURRENT_BIT);
            attribsPushed = true;

            if (this.iconTexture == null)
                this.initializeTexture(dc);

            gl.glEnable(GL.GL_TEXTURE_2D);
            iconTexture.bind();

            gl.glColor4d(1d, 1d, 1d, this.getOpacity());
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glDisable(GL.GL_DEPTH_TEST);

            double width = this.getScaledIconWidth();
            double height = this.getScaledIconHeight();

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

            double scale = this.computeScale(viewport);

            Vec4 locationSW = this.computeLocation(viewport, scale);

            gl.glTranslated(locationSW.x(), locationSW.y(), locationSW.z());
            gl.glScaled(scale, scale, 1);

            gl.glTranslated(width / 2, height / 2, 0);
            
            //if (this.showTilt) // formula contributed by Ty Hayden
            //    gl.glRotated(70d * (dc.getView().getPitch().getDegrees() / 90.0), 1d, 0d, 0d);

            //gl.glRotated(dc.getView().getHeading().getDegrees(), 0d, 0d, 1d);
            
            gl.glTranslated(-width / 2, -height / 2, 0);

            TextureCoords texCoords = this.iconTexture.getImageTexCoords();
            gl.glScaled(width, height, 1d);
            dc.drawUnitQuad(texCoords);
        }
        catch (Exception e) {
        	//logger.error(e.getMessage());
        	Error(this, e);
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
        }
    }

    private void initializeTexture(DrawContext dc) throws IOException
    {
        if (this.iconTexture != null )
            return;

        try
        {
        	// look in cache first
        	URL url = WorldWind.getDataFileCache().findFile(getName(), false);
        	
        	if ( url != null ) 
        	{
        		try {
        			logger.debug("Looking for screen ov " + getName() + " in cache");
        			
        			iconTexture = TextureIO.newTexture(new File(url.toURI()), true);
        			return;
				} catch (Exception e) {
					logger.error(e + " for cached screen ov " + getName());
				}
        	}
        	
        	logger.debug("Initializing texture " + getName() 
        			+ " from " + (iconFile != null ? iconFile : iconURL));
        	
        	if ( iconFile != null ) 
        	{
        		// try classpath
	            InputStream iconStream = this.getClass().getResourceAsStream("/" + this.iconFile.getName());
	            
	            if (iconStream == null)
	            {
	            	// not in CP. Try full path
	            	iconStream = this.getClass().getResourceAsStream("/" + this.iconFile);
	            	
	                if (iconStream == null && iconFile.exists()) {
	                    iconStream = new FileInputStream(iconFile);
	                }
	            }
	            this.iconTexture = TextureIO.newTexture(iconStream, true, null);
        	}
        	else {
        		// load from URL
        		iconTexture = TextureIO.newTexture(iconURL, true, null);
        	}
            
        	// cache texture
        	if ( iconTexture != null) {
        		final File f = WorldWind.getDataFileCache().newFile(getName() + ".png");
        		
        		logger.debug("Caching screen ov as " + f);
        		TextureIO.write(iconTexture, f);
        	}
        	
            this.iconTexture.bind();
        }
        catch (IOException e)
        {
            String msg = "Error initializing texture " + iconFile + " " + e.getMessage();
            throw new IOException(msg);
        }

        GL gl = dc.getGL();
        gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        // Enable texture anisotropy, improves "tilted" compass quality.
        int[] maxAnisotropy = new int[1];
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy, 0);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy[0]);
    }

    private double getScaledIconWidth()
    {
        return this.iconTexture.getWidth() * this.iconScale;
    }

    private double getScaledIconHeight()
    {
        return this.iconTexture.getHeight() * this.iconScale;
    }
   
    private Vec4 computeLocation(java.awt.Rectangle viewport, double scale)
    {
        double width = this.getScaledIconWidth();
        double height = this.getScaledIconHeight();

        double scaledWidth = scale * width;
        double scaledHeight = scale * height;

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
        if (this.resizeBehavior.equals(RESIZE_SHRINK_ONLY))
        {
            return Math.min(1d, (this.compassToViewportScale) * viewport.width / this.getScaledIconWidth());
        }
        else if (this.resizeBehavior.equals(RESIZE_STRETCH))
        {
            return (this.compassToViewportScale) * viewport.width / this.getScaledIconWidth();
        }
        else if (this.resizeBehavior.equals(RESIZE_KEEP_FIXED_SIZE))
        {
            return 1d;
        }
        else
        {
            return 1d;
        }
    }
    
    /**
     * Sets the relative viewport location to display the compass icon. Can be one of {@link #NORTHEAST} (the default),
     * {@link #NORTHWEST}, {@link #SOUTHEAST}, or {@link #SOUTHWEST}. These indicate the corner of the viewport to place
     * the icon.
     *
     * @param position the desired compass position
     */
    public void setPosition(String position)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.CompassPositionIsNull");
            Logging.logger().log(java.util.logging.Level.FINE, message);
            throw new IllegalArgumentException(message);
        }
        this.position = position;
    }

    /**
     * Sets the compass icon's image location. The layer first searches for this location in the current Java classpath.
     * If not found then the specified path is assumed to refer to the local file system. found there then the
     *
     * @param iconFilePath the path to the icon's image file
     */
    public void setIconFilePath(File iconFile)
    {
        if (iconFile == null)
        {
            String message = Logging.getMessage("nullValue.IconFilePath");
            Logging.logger().log(java.util.logging.Level.FINE, message);
            throw new IllegalArgumentException(message);
        }
        this.iconFile = iconFile;
    }

    /**
     * Returns the icon scale factor. See {@link #setIconScale(double)} for a description of the scale factor.
     *
     * @return the current icon scale
     */
    public double getIconScale()
    {
        return iconScale;
    }

    /**
     * Sets the scale factor defining the displayed size of the compass icon relative to the icon's width and height in
     * its image file. Values greater than 1 magify the image, values less than one minify it. If the layer's resize
     * behavior is other than {@link #RESIZE_KEEP_FIXED_SIZE}, the icon's displayed sized is further affected by the
     * value specified by {@link #setCompassToViewportScale(double)} and the current viewport size.
     *
     * @param iconScale the icon scale factor
     */
    public void setIconScale(double iconScale)
    {
        this.iconScale = iconScale;
    }

    /**
     * Returns the layer's compass-to-viewport scale factor.
     *
     * @return the compass-to-viewport scale factor
     */
    public double getCompassToViewportScale()
    {
        return compassToViewportScale;
    }

    /**
     * Sets the scale factor applied to the viewport size to determine the displayed size of the compass icon. This
     * scale factor is used only when the layer's resize behavior is {@link #RESIZE_STRETCH} or {@link
     * #RESIZE_SHRINK_ONLY}. The icon's width is adjusted to occupy the proportion of the viewport's width indicated by
     * this factor. The icon's height is adjusted to maintain the compass image's native aspect ratio.
     *
     * @param compassToViewportScale the compass to viewport scale factor
     */
    public void setCompassToViewportScale(double compassToViewportScale)
    {
        this.compassToViewportScale = compassToViewportScale;
    }
    
    public String getResizeBehavior () {
    	return resizeBehavior;
    }

    public void setResizeBehavior (String resizeBehavior) {
    	this.resizeBehavior = resizeBehavior;
    }
 
    public String toKML(boolean useAbsolutePaths) 
    {
    	String icon = null;
    	
    	if ( iconURL != null ) 
    		icon = iconURL.toString();
    	
    	if ( icon == null && iconFile != null && iconFile.exists() && useAbsolutePaths)
    		icon =  iconFile.toString();
    	
    	if ( icon == null && iconFile != null && iconFile.exists() && ! useAbsolutePaths)
    		icon = iconFile.getName();
    	
    	if ( icon == null ) {
    		logger.debug("Unable to get icon href for screen overlay " + getName());
    		return null;
    	}
    	
    	return " <ScreenOverlay>" + Messages.NL
    		+ "<name>" + getName() + "</name>" + Messages.NL
    		+ "<Icon><href><![CDATA[" 
			+ icon  
			+ "]]></href></Icon>" + Messages.NL
			
			// position: top left
			+ "<screenXY  x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>" + Messages.NL
			+ "<overlayXY  x=\"0\" y=\"1\" xunits=\"fraction\" yunits=\"fraction\"/>" + Messages.NL
			
			// size
			+ ( resizeBehavior.equals(RESIZE_KEEP_FIXED_SIZE) 
					? "<size x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>" 
							+ Messages.NL
					: "<size x=\"0.1\" y=\"0.1\" xunits=\"fraction\" yunits=\"fraction\"/>" 
							+ Messages.NL
				)
			+ "</ScreenOverlay>" + Messages.NL ;
    }
    
    /**
     * Get the overlay cache file
     * @return
     * @throws Exception
     */
    public File getFile () throws Exception 
    {
    	if (iconFile != null ) return iconFile;
    	
    	// in cache?
    	URL url = WorldWind.getDataFileCache().findFile(getName(), false);
    	if ( url != null) return new File( url.toURI());
    	
    	if (iconURL.getProtocol().startsWith("http"))
    		throw new IOException("No overlay " + getName() + " in cache.");
    	
    	return  new File( iconURL.toURI());
    }
    
    
    /*
	 * Notify listeners of a Layer error
	 */
	private void Error (ScreenOverlayLayer layer, Exception ex) 
	{
		logger.debug("# of overlay listeners:" + listeners.size());
		
		for (OverlayListener listener : listeners) {
			listener.onError(layer, ex);
		}
	}	
}
