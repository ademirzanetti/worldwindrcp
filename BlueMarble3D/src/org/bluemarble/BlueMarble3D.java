package org.bluemarble;

import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;

import org.bluemarble.gui.BlueMarbleModel;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.examples.ClickAndGoSelectListener;
import gov.nasa.worldwind.layers.Earth.WorldMapLayer;

/**
 * Blue Marble 3D
 * @author vsilva
 *
 */
public class BlueMarble3D extends JFrame
{
	private static final long serialVersionUID 	= 4176551277009947767L;
	public static final String APP_NAME 		= "Blue Marble 3D";
	
	// for full screen mode
	private final GraphicsEnvironment env 	= GraphicsEnvironment.getLocalGraphicsEnvironment();
	private final GraphicsDevice[] devices 	= env.getScreenDevices();
	
	// Multi-monitor full-screen mode not yet supported
	private final GraphicsDevice device = devices[0];
	private final boolean isFullScreen 	= device.isFullScreenSupported();

	static
    {
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", APP_NAME);
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
    }
	
	private WorldWindowGLCanvas wwd;
	
	/**
	 * Initialize
	 */
	private void initialize () 
	{
		// WWJ GKL canvas
        wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize( getPreferredSize());
        
        // Create the default model as described in the current worldwind properties.
        this.wwd.setModel(new BlueMarbleModel(wwd)); //, display));

        
        // Hook Listeners
        hookGlobeListeners();
        
        this.getContentPane().add(wwd, BorderLayout.CENTER);
        
        setFullScreen();
	}
	
	
	/**
	 * Init full screen (if supported)
	 */
	private void setFullScreen() 
	{
        // Init full screen mode if supported
//        setUndecorated(isFullScreen);
//        setResizable(!isFullScreen);
//
//        if (isFullScreen) {
//            device.setFullScreenWindow(this);
//            validate();
//        }
//        else {
			pack();
			setVisible(true);			
//         }
	}
	
	/**
	 * Hook WWJ globe lsiteners
	 */
	private void hookGlobeListeners() 
	{
        // Setup a select listener for the worldmap click-and-go feature
        this.wwd.addSelectListener(new ClickAndGoSelectListener(wwd, WorldMapLayer.class));
        
        // Key listener
        this.wwd.addKeyListener(new KeyAdapter()
        {
			public void keyReleased(KeyEvent e) 
			{
				// Repaint UI for widgets that use key strokes
				wwd.redraw();

				// F2 = Toggle UI
				if ( e.getKeyCode() == KeyEvent.VK_F2) {
					toggleUI ();
				}
				
				// F10 = Quit
				if ( e.getKeyCode() == KeyEvent.VK_F10) {
					quit();
				}
			}
        });
	}
	
	/**
	 * Quit application
	 */
	public static void quit () {
		System.exit(0);
	}
	
	/**
	 * Toggle visibility of the master UI layer affecting all Widgets
	 */
	private void toggleUI ()
	{
		((BlueMarbleModel)wwd.getModel()).toogleGUI();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			BlueMarble3D frame = new BlueMarble3D();
			
			frame.setTitle(APP_NAME);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setPreferredSize(Toolkit.getDefaultToolkit().getScreenSize());
			frame.initialize();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
