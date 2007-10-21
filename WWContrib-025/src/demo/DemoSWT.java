package demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Sphere;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Polyline;
import gov.nasa.worldwind.render.SurfaceQuad;


import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import demo.todo.mesh.Mesh;
import demo.todo.mesh.MeshDATReader;
import demo.todo.mesh.MeshReader;



import worldwind.contrib.Messages;
import worldwind.contrib.LayerUtils;
import worldwind.contrib.layers.GroundOverlayLayer;
import worldwind.contrib.layers.NASAWMSLayerList;
import worldwind.contrib.layers.ScreenOverlayLayer;
import worldwind.contrib.layers.loop.HTTPSatLoopLayerList;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay.GroundOverlayLoopListener;
import worldwind.contrib.parsers.KMLSource;
import worldwind.contrib.parsers.ParserUtils;
import worldwind.contrib.parsers.SimpleHTTPClient;
import worldwind.contrib.parsers.SimpleWMSParser;
import worldwind.contrib.parsers.WMS_Capabilities;


public class DemoSWT 
	implements GroundOverlayLoopListener
{
	private static final Logger logger = Logger.getLogger(DemoSWT.class);
	
	private WorldWindowGLCanvas world;
	private Display display;
	private Shell shell;
	
	CheckboxTreeViewer treeViewer;
	
	// initially cheked elements
	Object[] initialChecked;
	
	// default WW layers
	public NavigatorLayer[] navigatorLayers; 

	/*
	 * A thread to load remote layers. Makes the GUI irresponsive.
	 * A better way is t use a Wiz & let the user select them
	 */
	private static class RemoteLayerLoader implements Runnable
	{
		TreeViewer treeViewer;
		WorldWindowGLCanvas world;
		Display display;
		
		public RemoteLayerLoader(Display display, TreeViewer treeViewer, WorldWindowGLCanvas world) {
			this.treeViewer = treeViewer;
			this.world = world;
			this.display = display;
		}
		public void run() 
		{

			final TreeParent tp = buildSatLoopLayerList(); //world);
			
			display.syncExec(new Runnable() {
				public void run() {
					if ( tp != null ) 
						treeViewer.add(treeViewer.getInput(), tp);
				}
			});
			
		}
	}
	
	private static class NavigatorLayer
	{
        private Layer layer;
        private boolean enabled;
        private Image image;
        
        public NavigatorLayer(Layer layer, Image image, boolean enabled) {
            this.layer = layer;
            this.image = image;
            this.enabled = enabled;
            this.layer.setEnabled(this.enabled);
        }
        public Layer getLayer() { return layer;}
        public String getName() { return layer.getName();}
        public Image getImage() { return image; }
        public boolean getEnabled() { return enabled; }
	}
	
	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content 
	 * (like Task List, for example).
	 */
	 
	static class TreeObject implements IAdaptable {
		private NavigatorLayer  layer;
		private TreeParent parent;
		private boolean checked;
		
		public TreeObject( NavigatorLayer layer) { 
			this.setLayer(layer);
			checked =layer.getEnabled();
		}
		public String getName() {
			return getLayer().getName();
		}
		public void setParent(TreeParent parent) {
			this.parent = parent;
		}
		public TreeParent getParent() {
			return parent;
		}
		public String toString() {
			return getName();
		}
		public Object getAdapter(Class key) {
			return null;
		}
		public void setEnabled(boolean enabled) {
			getLayer().getLayer().setEnabled(enabled);
		}
		public Image getImage () {
			return getLayer().getImage();
		}
		public boolean getChecked () {
			return checked;
		}
		public void setLayer(NavigatorLayer layer) {
			this.layer = layer;
		}
		public NavigatorLayer getLayer() {
			return layer;
		}
	}
	
	static class TreeParent extends TreeObject {
		private ArrayList<TreeObject> children;
		public TreeParent(NavigatorLayer layer) { 
			super(layer);
			children = new ArrayList<TreeObject>();
		}
		public void addChild(TreeObject child) {
			children.add(child);
			child.setParent(this);
		}
		public void removeChild(TreeObject child) {
			children.remove(child);
			child.setParent(null);
		}
		public TreeObject [] getChildren() {
			return (TreeObject [])children.toArray(new TreeObject[children.size()]);
		}
		public boolean hasChildren() {
			return children.size()>0;
		}
	}

	/*
	 * Provides content to the layes tree
	 */
	static class LayersContentProvider 
		implements IStructuredContentProvider, ITreeContentProvider 
	{
//		private TreeParent invisibleRoot;

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) 
		{
			return getChildren(parent);
		}
		
		public Object getParent(Object child) {
			if (child instanceof TreeObject) {
				return ((TreeObject)child).getParent();
			}
			return null;
		}
		public Object [] getChildren(Object parent) {
			if (parent instanceof TreeParent) {
				return ((TreeParent)parent).getChildren();
			}
			return new Object[0];
		}
		public boolean hasChildren(Object parent) {
			if (parent instanceof TreeParent)
				return ((TreeParent)parent).hasChildren();
			return false;
		}

	}
	
	/*
	 * Provides Labels/images to the tree
	 */
	static class LayersLabelProvider extends LabelProvider {

		public String getText(Object obj) {
			return obj.toString();
		}
		
	}

	/*
	 * NASA WMS Layers
	 */
    private LayerList buildNASAWMSLayerList() 
    {
    	NASAWMSLayerList list = new NASAWMSLayerList();
    	list.setAllEnabled(false);
    	return list;
    }

    private static TreeParent buildSatLoopLayerList()  
    {
		/*
		 * Navy res Labs Layers (animated ground overlays)
		 */
    	RenderableLayer topLayer = new RenderableLayer();
		topLayer.setName( "Satellite Real-time Weather");
		
		TreeParent top = new TreeParent(new NavigatorLayer(topLayer
					, null 
					, false));

		// NRL layer list
		try {
			HTTPSatLoopLayerList list = new HTTPSatLoopLayerList();
			list.setAllEnabled(false);

			// Add children layers (Animated overlays) to the tree
	        for (Layer layer : list)
	        {
	        	
	        	final TreeParent tp = new TreeParent(new NavigatorLayer(layer
	        			, null 
	        			, false));
	        	
	        	top.addChild(tp);
	        	
	        	// Add frames from each overlay
	        	for ( GroundOverlayLayer go : ((TimeLoopGroundOverlay)layer).getOverlays() )
	        	{
	        		tp.addChild(new TreeObject(new NavigatorLayer(go
	        						, null
	        						, false)));
	        	}
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return top;
    }
    


    /*
     * Add an animated overlay from the NASA SVS
     */
    private static TreeParent  buildSVSLayer(WorldWindowGLCanvas world) 
    {
    	try {
    		final URL svsUrl = new URL (
    				"http://svs.gsfc.nasa.gov/cgi-bin/wms?SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities"
    				);
    		
    		logger.debug("Loading XML from " + svsUrl);
    		
			// Load remote WMS XML from url
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			SimpleHTTPClient client = new SimpleHTTPClient(svsUrl);
			
			client.doGet(bos); 
			bos.close();

			logger.debug("Parsing xml...");
			
			// Parse WMS XML
			SimpleWMSParser parser = new SimpleWMSParser();
			parser.parse("NASA SVS", new ByteArrayInputStream(bos.toString().getBytes()));
			
			WMS_Capabilities capabilities = parser.getCapabilities();
			
			// Pick Layer #11: TRMM Viz of Hurr Katrina (Animated Overlay)
			WMS_Capabilities.Layer layer = capabilities.getLayers().get(11);

			// Split time steps
			String[] frames = (layer.ISOTimeSpan != null)
				? WMS_Capabilities.buildWMSTimeList(layer.ISOTimeSpan).split(",")
				: null;
			
			// must be an animated overlay
			if ( frames == null )
				throw new Exception("Layer " + layer.Title + " has no time span.");
			
			logger.debug("Layer " + layer.Title + " has "
					+ frames.length + " time steps. box" + layer.bbox);
			
			// Frame format
			String format = "image/png";
			
			TimeLoopGroundOverlay aov = ParserUtils
				.newTimeLoopGroundOverlay(layer, frames, format);

			//String format = layer.getCapabilities().GetMapFormat;
    		
    		if ( format != null) {
    			format = "." + format.split("/")[1];
    		}

			// Add to the tree: tree parent
			final TreeParent top = new TreeParent(new NavigatorLayer(aov
        			, null 
        			, false));
			
			// overlays
			for (GroundOverlayLayer ov : aov.getOverlays()) {
	        	final TreeObject child = new TreeParent(new NavigatorLayer(ov
	        			, null 
	        			, false));
	        	top.addChild(child);
			}
			
			return top;
		} 
    	catch (Exception e) 
		{
			e.printStackTrace();
			return null;
		}
    }
    
    
    /*
     * TRMM after Hur Katrina
     */
    private TimeLoopGroundOverlay buildKatrinaAnimation (){
    	Sector bbox = new Sector(Angle.fromDegrees(10.0), Angle.fromDegrees(40.0),
                Angle.fromDegrees(-100), Angle.fromDegrees(-70));

    	return buildAnimatedGroundOverlay("TRMM Mesaurament after Katrina", bbox, "http://svs.gsfc.nasa.gov/cgi-bin/wms?"
    			+ "service=WMS&request=GetMap&layers=3250_22668&bbox=-100,10,-70,40"
    			+ "&width=512&height=512&crs=CRS:84&format=image/png&version=1.3.0"
    		, new String[] {"2005-08-23T05Z", "2005-08-23T23Z", "2005-08-24T06Z", "2005-08-24T22Z"
    			, "2005-08-25T05Z", "2005-08-25T23Z", "2005-08-26T04Z", "2005-08-26T22Z"
    			, "2005-08-27T04Z", "2005-08-27T21Z", "2005-08-28T04Z", "2005-08-28T22Z"}
    	);
    }
    
    /*
     * Toxic cloud of mount pinatubo eruption
     */
    private TimeLoopGroundOverlay buildPinatuboAnimation (){
    	Sector bbox = new Sector(Angle.fromDegrees(-18.75), Angle.fromDegrees(28.75),
                Angle.fromDegrees(-65), Angle.fromDegrees(125));
    	
    	return buildAnimatedGroundOverlay("Pinatubo Toxic Cloud", bbox
    		, "http://svs.gsfc.nasa.gov/cgi-bin/wms?"
    			+ "service=WMS&request=GetMap&layers=3169_21478&bbox=-65,-18.75,125,28.75"
    			+ "&width=1024&height=256&srs=EPSG:4326&format=image/png&version=1.1.1&styles=opaque"
    		, new String[] {"1991-06-16", "1991-06-17", "1991-06-18", "1991-06-19", "1991-06-20", "1991-06-21"}
    	);
    }
    
    
    /*
     * Animated screen overlay example:
     */
    private TimeLoopGroundOverlay buildAnimatedGroundOverlay(String title
    		, Sector bbox, String urlPref, String[] frames)  
    {
        TimeLoopGroundOverlay overlay = 
        	new TimeLoopGroundOverlay(title);  
        
        overlay.addLoopListener(this);
        
    	try {
    		for (String frame : frames ) 
    		{
    			final String icon = urlPref + "&time=" + frame; 
    			
    			final GroundOverlayLayer gov = new GroundOverlayLayer(frame 
            			, bbox
            			, new URL(icon)
            			, ParserUtils.getIconSuffix(icon));
    			
    			gov.setBaseCachePath("Earth/" + Messages.forCachePath(title) + "/"); 
    			
            	overlay.add(gov);
			}
    		
		} 
    	catch (Exception e) {
			e.printStackTrace();
		}
        return overlay;
    }
    
    /*
     * Mesh test
     */
    private Layer buildMesh() {
    	RenderableLayer layer = new RenderableLayer();
    	
    	try {
//    		MeshReader.Sinc("c:/tmp/mysinc.dat", 10, 10);
//        	MeshReader reader = new MeshDATReader("c:/tmp/mysinc.dat");
//        	Mesh mesh = new Mesh(reader);
        	
        	layer.setName("Mesh View");
//        	layer.addRenderable(mesh);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
    	return layer;
    }
    
    /*
     * Ground Overlay sample: Global Large-scale Precipitation during Hurricane Frances
     * (Full sphere)
     */
    private Layer buildGroundOverlay() 
    {
    	Sector bbox = new Sector(Angle.fromDegrees(-90.0), Angle.fromDegrees(90.0),
                Angle.fromDegrees(-180), Angle.fromDegrees(180));
    	
    	final String title = "Global Surface Latent Heat Flux during Hurricane Frances";
    	try {

    		// Global Surface Latent Heat Flux during Hurricane Frances
        	URL url = new URL(
        			"http://svs.gsfc.nasa.gov/cgi-bin/wms?service=WMS&request=GetMap&layers=3199_22125&time=2004-09-01T03Z&bbox=-180,-90,180,90&width=1000&height=721&srs=EPSG:4326&format=image/png&version=1.1.1&transparent=true"
            			);
        	
        	return new GroundOverlayLayer(title
        			, bbox
        			, url
        			, ParserUtils.getIconSuffix(url.toString()));
		} 
    	catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }

    /*
     * Screen Overlay example: Renders a legend image from classpath: demo/images/legend.png
     */
    private ScreenOverlayLayer buildScreenOverlayLayer() 
    {
    	try {
			URL iconURL = new URL(
				"http://svs.gsfc.nasa.gov/vis/a000000/a003100/a003169/so2bar.png");
				
			return new ScreenOverlayLayer("Sample legend (NorthWest)", iconURL, ScreenOverlayLayer.NORTHWEST);	
		} 
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
    
    /*
     * 
     */
    Object[] getInitialChecked() 
    {
    	return null;
    }
    
    /*
     * Initial tree data
     */
	public TreeParent getInitialInput () 
	{
		/*
		 * Built-in WorldWind Layers (already in the WW model - contructor)
		 */
		RenderableLayer topLayer = new RenderableLayer();
		topLayer.setName("WorldWind Built-in Layers");
		
		
		TreeParent top = new TreeParent(new NavigatorLayer(topLayer, null, false));

		int chekedCount = navigatorLayers.length + 1;
		
		initialChecked = new Object[chekedCount];
		initialChecked[0] = top;
		
		// Add layers to the tree
		for (int i = 0; i < navigatorLayers.length; i++) 
		{
			final TreeObject to = new TreeObject(navigatorLayers[i]);
			top.addChild(to);
			initialChecked [i+1] = to;
		}
		
		// the root tree node is invisible
		RenderableLayer rl = new RenderableLayer();
		rl.setName("invisible");
		
		final TreeParent invisibleRoot = new TreeParent(new NavigatorLayer(rl, null, false));
		invisibleRoot.addChild(top);

		try 
		{
			/*
			 * Some layers from NASA WMS server. Must be added to the model
			 */
			topLayer = new RenderableLayer();
			topLayer.setName("Layers from NASA's WMS");
			top = new TreeParent(new NavigatorLayer(topLayer, null, false));
			
			LayerList list = buildNASAWMSLayerList();
	        for (Layer layer : list)
	        {
	        	// Must add layer to both tree & world wind model
	        	world.getModel().getLayers().add(layer);
	        	top.addChild(new TreeObject(new NavigatorLayer(layer, null, false)));
	        }
			
			invisibleRoot.addChild(top);
			
	        
	        /*
	         * Animated Ground overlay (toxic cloud from pinatubo in filipines - from NASA WMS)
	         */
			TimeLoopGroundOverlay anim = buildPinatuboAnimation();
			
			// set legend
			anim.setLegend(new URL("http://svs.gsfc.nasa.gov/vis/a000000/a003100/a003169/so2bar.png"));
			
			// add frames to the tree
			top = new TreeParent(new NavigatorLayer(anim, null, false));
			
	        for (Layer layer : anim.getOverlays())
	        {
	        	top.addChild(new TreeObject(new NavigatorLayer(layer, null, false)));
	        }
	
	        invisibleRoot.addChild(top);
	
	        /*
	         * Animated Ground overlay (Katrina TRMM - from NASA WMS)
	         */
			TimeLoopGroundOverlay kat = buildKatrinaAnimation();
			
			// add frames to the tree
			top = new TreeParent(new NavigatorLayer(kat, null, false));
			
	        for (Layer layer : kat.getOverlays())
	        {
	        	top.addChild(new TreeObject(new NavigatorLayer(layer, null, false)));
	        }

	        invisibleRoot.addChild(top);
	        
	        // NASA SVS
	        //invisibleRoot.addChild(buildSVSLayer(world));
	        	        
	        /*
	         * Ground overlay Example
	         */ 
	        Layer layer = buildGroundOverlay();
	    	world.getModel().getLayers().add(layer);
	    	top = new TreeParent(new NavigatorLayer(layer, null, false));
	        
			invisibleRoot.addChild(top);

			/*
			 * Example of Screen Overlay: Sample Legend
			 */
			layer = buildScreenOverlayLayer();
			world.getModel().getLayers().add(layer);
			invisibleRoot.addChild(new TreeParent(new NavigatorLayer(layer, null, false)));

			/*
			 * Mesh test
			 */
			layer = buildMesh();
			world.getModel().getLayers().add(layer);
			invisibleRoot.addChild(new TreeParent(new NavigatorLayer(layer, null, false)));
			
		}
		catch (Exception ex) {
			ex.printStackTrace();
			Messages.showErrorMessage(shell, "Error", ex.getClass() + ":" + ex.getMessage());
		}
		return invisibleRoot;
	}

	/*
	 * Create GUI 
	 */
	void createContents(Composite parent, final WorldWindowGLCanvas world )
	{
	
		SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
		sash.setLayout(new GridLayout(2, false));
		sash.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Layers tree
		treeViewer = new CheckboxTreeViewer(sash, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.setContentProvider(new LayersContentProvider());
		treeViewer.setLabelProvider(new LayersLabelProvider());
		treeViewer.setInput(getInitialInput());
		treeViewer.setCheckedElements(initialChecked);
		
		// Fires when user checks a checkbox in the tree
	    treeViewer.addCheckStateListener(new ICheckStateListener() {
	      public void checkStateChanged(CheckStateChangedEvent event) 
	      {
	    	  boolean checked = event.getChecked(); 
	    	  
	    	  // check node
	    	  TreeObject to = (TreeObject)event.getElement();
	    	  to.setEnabled(checked);

	    	  // WW Layer
	    	  Layer layer = to.getLayer().getLayer();
	    	  
	    	  // check all tree children
	          treeViewer.setSubtreeChecked(event.getElement(), checked);
	          
	          // Play any Animated Overlays
	    	  if ( layer instanceof TimeLoopGroundOverlay ) 
	    	  {
	    		  logger.debug("Clicked Animated Overlay. Checked=" + checked);
	    		  TimeLoopGroundOverlay aov = (TimeLoopGroundOverlay)to.getLayer().getLayer();
	    		  
	    		  if ( checked ) {
		    		  // Add frames to WW model
	    			  aov.asynchFetchSome();
	    			  aov.addToModel(world);

		    		  logger.debug("Playing...");
		    		  aov.play();
	    		  }
	    		  else { 
	    			  aov.stop();
	    			  //aov.removeFromModel();
	    		  }
	    	  }
	    	  else 
	    	  {
	    		  Layer parent = to.getParent().getLayer().getLayer();
	    		  
	    		  // if parent is an animated overlay (set child visibility)
	    		  if ( parent instanceof TimeLoopGroundOverlay ) 
	    		  {
	    			  logger.debug("Setting visibility for frame:" + layer + " to " + checked);
	    			  ((TimeLoopGroundOverlay)parent).setVisible(layer, checked);
	    		  }
	    		  // enable/disable respective children layers
	    		  else if ( to instanceof TreeParent ) 
		          {
		        	  TreeObject[] children = ((TreeParent)event.getElement()).getChildren();
		        	  
		        	  for (int i = 0; i < children.length; i++) {
		        		  children[i].setEnabled(checked);
		        	  }
		          }
	    	  }
	    	  
	    	  // repaint WW
		      world.repaint();
	      }
	    });

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection = treeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				System.out.println("Double-click detected on " + obj.toString());
				
				Layer layer = ((TreeObject)obj).getLayer().getLayer();
				LayerUtils.moveViewTo(world.getView(), world.getModel().getGlobe(), layer);
			}
		});
	    
		//World Window
		Composite composite = new Composite(sash, SWT.EMBEDDED);
		composite.setLayout(new GridLayout(1,true));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
				
		java.awt.Frame worldFrame = SWT_AWT.new_Frame(composite);
		java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout());
		
		worldFrame.add(panel);
		panel.add(world, BorderLayout.CENTER);

		// sash children weight
		sash.setWeights(new int[]{1, 3});

		loadLayers();
		
		// load remote layers 
		//new Thread(new RemoteLayerLoader(display, treeViewer, world)).start();
		
	}

	/**
	 * Constructor
	 */
	public DemoSWT () 
	{
		// Init WW
		world = new WorldWindowGLCanvas();  
        world.setPreferredSize(new Dimension(800, 600));

        // Add layers to WW
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        
        // get built-n layers
        LayerList layerList = m.getLayers();
        
        
        // Add default layers (all enabled)
        navigatorLayers = new NavigatorLayer[layerList.size()];
        
        int i = 0;
        for (Layer layer : layerList) {
        	navigatorLayers[i++] = new NavigatorLayer(layer, null, true);
        }
        
        m.setLayers(layerList);
        m.setShowWireframeExterior(false);
        m.setShowWireframeInterior(false);
        m.setShowTessellationBoundingVolumes(false);
        world.setModel(m);
        
        GlobeSelectionListener listener = new GlobeSelectionListener(world);
        world.addSelectListener(listener);
        
		// init app
		display = new Display();
		shell = new Shell(display);
		
		
		shell.setBounds(display.getClientArea());
		
		shell.setText("World Wind SWT Example");
		shell.setLayout(new GridLayout(1, true));
		shell.setLayoutData(new GridData(GridData.FILL_BOTH));

	}
	
	
	void run()
	{
		// init events
		Listener exitListener = new Listener() {
			public void handleEvent(Event e) {
				MessageBox dialog = new MessageBox(shell, SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
				dialog.setText("Question");
				dialog.setMessage("Exit?");
				if (e.type == SWT.Close) e.doit = false;
				if (dialog.open() != SWT.OK) return;
				
				saveLayers();
				
				shell.dispose();
			}
		};	

		// Init shell
		shell.addListener(SWT.Close, exitListener);
		
		createContents(shell, world);
		
		shell.open();
		while(!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
		display.dispose();
		
	}
	
	/**
	 * Cache layers
	 */
	private void saveLayers() 
	{
		TreeObject[] top = ((TreeParent)treeViewer.getInput()).getChildren();
		
		StringBuffer buf = new StringBuffer("<xml>" + Messages.NL);
		
		for (TreeObject treeObject : top) {
			final Layer layer = treeObject.getLayer().getLayer();
			
			if ( layer instanceof TimeLoopGroundOverlay) {
				buf.append("<kml>" + Messages.NL 
						+ ((TimeLoopGroundOverlay)layer).toKML() + Messages.NL
						+ "</kml>" + Messages.NL);
			}
			else if (layer instanceof GroundOverlayLayer) {
				buf.append("<kml>" + Messages.NL
						+ ((GroundOverlayLayer)layer).toKML() + Messages.NL
						+ "</kml>" + Messages.NL);
			}
		}
		buf.append("</xml>");
		
		// save XML in WW cache folder
		System.out.println("Layers not saved.");
//		File file = WorldWind.dataFileCache().newFile("layers.xml");
//		try {
//			Messages.writeToFile(file, buf.toString().getBytes());
//		} catch (Exception e) {
//			// Unable to save file
//			e.printStackTrace();
//		}
		
	}
	
	/**
	 * Load layers from WW default cache
	 */
	private void loadLayers() {
		try {
			// Read kml from cache
			URL url 	= WorldWind.getDataFileCache().findFile("layers.xml", false);
			
			if ( url == null ) return;
			
			File file 	= new File(url.toURI());
			String xml 	= new String(worldwind.contrib.Messages.readFile(file));
			
			// Each <kml> element represents a WW layer
			Document doc = ParserUtils.parse(new ByteArrayInputStream(xml.getBytes()));
			NodeList nl = doc.getElementsByTagName("kml");

			// loop thru layers
			for (int i = 0; i < nl.getLength(); i++) 
			{
				// convert kml to a WW layer list
				KMLSource kml = new KMLSource("doc.kml", (Element) nl.item(i));
				LayerList list = kml.toLayerList();

				for (Layer layer : list) 
				{
					// Add layer to the tree
					System.out.println("Add layer " + layer + " of type " + layer.getClass().getName());					
					if ( layer instanceof TimeLoopGroundOverlay) {
						addTimeLoopGroundOverlays(
								new TimeLoopGroundOverlay[]{ (TimeLoopGroundOverlay)layer } );
					}
					else if ( layer instanceof GroundOverlayLayer) 
					{
					}
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * Add a rime loop overlay to the tree
	 */
	private void addTimeLoopGroundOverlays(TimeLoopGroundOverlay[] ovs) 
	{
		for (TimeLoopGroundOverlay aov : ovs) 
		{
			NavigatorLayer layer = new NavigatorLayer(aov
					, null
					, false);
			
			// View children
			NavigatorLayer[] children = new NavigatorLayer[aov.getOverlays().size()];
			
			// Children as ground overlays
			GroundOverlayLayer[] childOvs = new GroundOverlayLayer[aov.getOverlays().size()];
			
			aov.getOverlays().toArray(childOvs);
			
			for (int i = 0; i < children.length; i++) {
				children[i] = new NavigatorLayer(childOvs[i]
						, null
						, false);  
			}
			
			// Add to view
			addLayer(layer, children, false);
		}
	}
	
	/**
	 * Add a layer to the view
	 * @param layer {@link NavigatorLayer} parent object to add to the view
	 * @param children Array of {@link NavigatorLayer} children
	 * @param addToModel Add layers to the World Wind model so they will render
	 */
	public void addLayer(NavigatorLayer layer, NavigatorLayer[] children
			, boolean addToModel) 
	{
		TreeParent parent = new TreeParent(layer);
		
		if ( children != null ) 
		{
			for (NavigatorLayer navigatorLayer : children) 
			{
				System.out.println("LV: add child:" + navigatorLayer.getName());
				parent.addChild(new TreeObject(navigatorLayer));
				
				// Render? The parent should not be added cuz it has nothing to render
				if ( addToModel ) {
					System.out.println("LV: adding child to WW model " + navigatorLayer.getName() );
					world.getModel().getLayers().add(navigatorLayer.getLayer());					
				}
			}
		}
		
		// Add only single Layers
		// AnimatedGroundOverlay(s) are not added to the model for performance
		if ( ! (layer.getLayer() instanceof TimeLoopGroundOverlay) 
				&& addToModel && children == null)
		{
			System.out.println("LV: adding top layer to WW model " + layer.getName() );
			world.getModel().getLayers().add(layer.getLayer());					
		}
		
		TreeParent root = (TreeParent)treeViewer.getInput();
		root.addChild(parent);
		
		//treeViewer.add(treeViewer.getInput(), parent);
		treeViewer.setInput(root);
		treeViewer.refresh(root); 
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		try {
			new DemoSWT().run();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		System.exit(0);
	}


	public synchronized void statusChanged(int current, int total, GroundOverlayLayer layer) {
		System.out.println("statusChanged " + current + "/" + total + " " + layer.getName());
	}


	public void onError(GroundOverlayLayer layer, Exception ex) {
		System.err.println("Error " + layer.getName() + " " + ex.getMessage());
	}
}
