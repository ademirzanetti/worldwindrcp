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
package org.eclipse.plugin.worldwind.views;

import gov.nasa.worldwind.layers.Layer;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import org.eclipse.plugin.worldwind.Activator;
import org.eclipse.plugin.worldwind.ApplicationActionBarAdvisor;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.*;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;

import worldwind.contrib.LayerUtils;
import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.layers.GroundOverlayLayer;

import org.eclipse.plugin.worldwind.operation.AnimationJob;
import org.eclipse.plugin.worldwind.operation.Check4UpdatesJob;
import org.eclipse.plugin.worldwind.operation.LayerLoaderJob;
import org.eclipse.plugin.worldwind.utils.LayerControlsDialog;
import org.eclipse.plugin.worldwind.utils.LayersToolTipSupport;
import org.eclipse.plugin.worldwind.views.EarthView;
import org.eclipse.plugin.worldwind.views.tree.WWTreeViewer;
import org.eclipse.plugin.worldwind.views.tree.TreeObject;
import org.eclipse.plugin.worldwind.views.tree.TreeParent;


/**
 * Layer Navigator View 
 * @author Vladimir Silva
 *
 */
public class LayersView extends ViewPart
{
	private static final Logger logger = Logger.getLogger(LayersView.class);
	
	static public String ID = LayersView.class.getName();
	
	private WWTreeViewer treeViewer;

	// click action: navigate to layer centroid
	private Action clickAction;

	// Layer controls
	private Action actionLayerControls;
	
	// Status line
	private StatusLine statusLine;
	
	private LayersToolTipSupport tipSupport;
	
	
	// A hashtable to track animations
	private ConcurrentHashMap<String, AnimationJob> animatedJobs 
		= new ConcurrentHashMap<String, AnimationJob>();
	

	/*
	 * Provides content to the layers tree
	 */
	static class LayersContentProvider 
		implements IStructuredContentProvider, ITreeContentProvider 
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
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
	static class LayersLabelProvider extends CellLabelProvider 
	{
		LayersToolTipSupport tipSupport;

		public void setTipSupport (LayersToolTipSupport tipSupport ) {
			this.tipSupport = tipSupport;
		}
		
		@Override
		public Image getToolTipImage(Object object) {
			return ((TreeObject)object).getImage();
		}

		public void update(ViewerCell cell) {
			cell.setText(cell.getElement().toString());
		}
		
		@Override
		public String getToolTipText(Object element) 
		{
			final Layer layer 	= ((TreeObject)element).getLayer();
			String description 	= ((TreeObject)element).toString();
			
			// Get layer description: only in GroundOverlay & TimeLoop Ovs
			if ( layer instanceof TimeLoopGroundOverlay )
				description = ((TimeLoopGroundOverlay)layer).getDescription();

			if ( layer instanceof GroundOverlayLayer )
				description = ((GroundOverlayLayer)layer).getDescription();
				
				
			final String toolTip = "<html>" 
				+ "<style>body, table {font-family:Arial;font-size=12px;background-color='#FFFFCC'}"
				+ "</style>"
				+ "<body bgcolor='#FFFFCC'>" 
				+ description 
				+ "</body></html>";

			if ( tipSupport != null) 
			{
				// very crappy tip size logic
				final int len = description.length();
				int w = 200 , h = 100;
				
				if ( len > 30  && len < 100) { w = 300; h = 200;} 
				else if ( len > 100 && len < 200 ){ w = 300; h = 300;}
				else if ( len > 200 ){ w = 600; h = 400;}
				
				tipSupport.setTipSize(w, h);
			}
			return toolTip;
		}
		
	}
	

	/**
	 * The constructor.
	 */
	public LayersView() {
	}

	
	/*
	 * Process a check state event
	 */
	private void handleCheckState (boolean checked, final TreeObject to)
	{
		to.setEnabled(checked);

		// WW Layer
		Layer layer = to.getLayer();
	  
		logger.debug("Layer " + layer.getName() + " type=" + layer.getClass().getName() );

		// The real time weather node has too many layers. it cannot be checked
		// Only its children can be checked
		if ( layer.getName().equalsIgnoreCase(Messages.getText("remote.layers.tree.lbl")))
		{
			Messages.showErrorMessage(getViewSite().getShell()
					, Messages.getText("err.dialog.title")
					, Messages.getText("err.msg.realtime.sat", new Object[] { layer.getName()} ));

			treeViewer.setChecked(to, false);
			return;
		}
		
		// check all node children
		treeViewer.setSubtreeChecked(to, checked);
	  
		// Animated Overlay, play it
		if ( layer instanceof TimeLoopGroundOverlay ) 
		{
			final TimeLoopGroundOverlay overlay = (TimeLoopGroundOverlay)layer;

			if ( checked ) 
			{
				overlay.addToModel(EarthView.world);
				
				// Listen for loop updates
				//overlay.addLoopListener(this);
				//overlay.play(); 

				Display display = getViewSite().getShell().getDisplay();
				
				// Use an eclipse animation job for smoothness
				// It won't hang the UI
				AnimationJob job = new AnimationJob(display, overlay, statusLine);
				
				// Save the job, so it can be stopped
				animatedJobs.put(to.getID(), job);
				
				logger.debug("Starting animated job " + job + " id=" + to.getID());
				job.play();
				
				
			}
			else { 
				//overlay.stop();
//				overlay.removeLoopListener(this);
				
				// Get job from pool
				AnimationJob job = animatedJobs.get(to.getID());
				job.stop();

				logger.debug("Stopped animated job " + job + " id=" + to.getID());
				animatedJobs.remove(to.getID());
			}
		}
		// If layer has children
		else if ( to instanceof TreeParent && ((TreeParent)to).hasChildren() )
		{
			logger.debug("Layer w/ children parent=" + to.getParent() );
			
			// Grab parent layer. 
			Layer parent = (to.getParent() != null ) 
				? to.getParent().getLayer()
				: null ;
			
			parent.setEnabled(checked);
			
			// if parent is an animated overlay (set child visibility)
			if ( parent != null && parent instanceof TimeLoopGroundOverlay ) 
			{
				((TimeLoopGroundOverlay)parent).setVisible(layer, checked);
			}
			// enable/disable respective children layers
			else  
			{
				TreeObject[] children = ((TreeParent)to).getChildren();
				
				if ( children != null) 
				{
					logger.debug("Children size=" + children.length);
					for (int i = 0; i < children.length; i++) 
					{
						logger.debug("Setting visibility for " + children[i] + " to " + checked);
						
						children[i].setEnabled(checked);
						
						// is the child a TimeLoopGroundOverlay?
						if ( children[i].getLayer() instanceof TimeLoopGroundOverlay)
							handleCheckState(checked, children[i]);
					}
				}
			}
		}
		// leaf layer
		else 
		{
			logger.debug("Leaf layer "+ layer.getName());
			
//			if ( layer instanceof GroundOverlayLayer)
//				((GroundOverlayLayer)layer).addOverlayListener(this);
			
			// set tree check state
			to.setEnabled(checked);
		}

		// repaint WW
		EarthView.world.repaint();
		
		// check state not preserved when adding/removing nodes
		treeViewer.trackCheckState(to, checked);
		
	}
	
	
	/**
	 * This is a callback that will allow us
	 * to create the treeViewer and initialize it.
	 */
	public void createPartControl(Composite parent) 
	{
		final Composite composite = new Composite(parent, SWT.NONE);
		
        GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
		
		composite.setLayout(layout);
		
		// Tooltip support
		LayersLabelProvider labelProvider = new LayersLabelProvider();
		
		treeViewer = new WWTreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		
		treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.setContentProvider(new LayersContentProvider());

		tipSupport = LayersToolTipSupport.enablefor(treeViewer
				, ToolTip.NO_RECREATE
				, getViewSite().getWorkbenchWindow());
		
		labelProvider.setTipSupport(tipSupport);
		treeViewer.setLabelProvider(labelProvider);

		treeViewer.initialize();

		
		// When user checks a checkbox in the tree, check all its children
	    treeViewer.addCheckStateListener(new ICheckStateListener() {
	      public void checkStateChanged(CheckStateChangedEvent event) 
	      {
	    	  boolean checked = event.getChecked(); 
	    	  
	    	  // check node
	    	  TreeObject to = (TreeObject)event.getElement();
	    	  
	    	  // handle state
	    	  handleCheckState(checked, to);
	      }
	    });
		
		makeActions();
		hookClickAction();
		contributeToActionBars();

		// Set initially checked layers
		treeViewer.updateCheckState();
		
		// initialize status line
		statusLine = ApplicationActionBarAdvisor.getDefaultStatusLine();
		
		//createProgressIndicator(composite);
		
		// load remote real time weather layers 
		LayerLoaderJob job = new LayerLoaderJob(getViewSite().getShell().getDisplay()
				, treeViewer
				, statusLine );
		
		job.setProperty(IProgressConstants.ICON_PROPERTY, Activator.ICON_NOAA);
		job.setUser(false);
		job.schedule();
		
		// Fire update check job in 5 mins.
		new Check4UpdatesJob(getViewSite().getWorkbenchWindow()).schedule(350000);
	}

/*
    private void createProgressIndicator(final Composite parent) {
        
        final Composite comp = new Composite(parent, SWT.NONE);
        comp.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        final GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gd.heightHint = 1;
        comp.setLayoutData(gd);
        
        
        final IWorkbenchWindow window = getViewSite().getWorkbenchWindow(); 
        	//PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        ProgressRegion progressRegion = new ProgressRegion();
        
        progressRegion.createContents(parent, (WorkbenchWindow)window);
        progressRegion.getControl().setVisible(true);
        progressRegion.getControl().setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
        
    }
*/
	
	/*
	 * Very simple logic to load an icon from a layer name
	 */
	static public final Image guessIcon(String layerName) 
	{
    	Image icon = null;
    	String name = layerName.toLowerCase();
    	
    	// set some icons
    	if ( name.indexOf("usgs") != -1 )
    		icon = Activator.ICON_USGS;
    	else if ( name.indexOf("us ") != -1)
    		icon = Activator.ICON_FLAG_US;
    	else if ( name.indexOf("nasa") != -1
    			|| name.indexOf("blue marble") != -1 
    			|| name.indexOf("compass") != -1)
    		icon = Activator.ICON_NASA;
    	else
    		icon = Activator.getSharedImage(ISharedImages.IMG_DEF_VIEW);
    	return icon;
	}

	

	/* View local actions */
	private void makeActions() 
	{
		// Tree double click action
		clickAction = new Action() {
			public void run() 
			{
				ISelection selection = treeViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				
				// move globe position to layer centroid
				LayerUtils.moveViewTo(EarthView.world.getView()
						, EarthView.world.getModel().getGlobe()
						, ((TreeObject)obj).getLayer());
			}
		};

		actionLayerControls = new Action() {
			public void run() 
			{
				try {
					showLayerControls();
				} catch (Exception e) {
					logger.error(e);
				}
			}
		};
		actionLayerControls.setToolTipText(Messages.getText("layer.action.controls"));
		actionLayerControls.setImageDescriptor(Activator.getSharedImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		
	}
	
	/*
	 * Show controls for the selected layer 
	 */
	private void showLayerControls() 
	{
		ISelection selection 	= treeViewer.getSelection();
		Object obj 				= ((IStructuredSelection)selection).getFirstElement();
		TreeObject to 			= (TreeObject)obj;
		
		if ( obj == null ) return;

		Shell shell = getViewSite().getShell();
		Layer layer = to.getLayer();

		if ( ! (layer instanceof TimeLoopGroundOverlay) ) {
			MessageDialog.openInformation(shell
					, Messages.getText("info.dialog.title")
					, Messages.getText("ctl.dlg.invalid.layer"
							, new Object[] {layer.getName()} ));
			return;
		}
		
		LayerControlsDialog dialog = new LayerControlsDialog(shell, layer);
		
		if ( layer instanceof TimeLoopGroundOverlay)
			dialog.setAnimationJob(animatedJobs.get(to.getID()));
		
		dialog.open();
	}
	
	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}
	
	private void fillLocalToolBar(IToolBarManager manager) 
	{
		manager.add(actionLayerControls);
	}

	/**
	 * On click move globe to the centroid of the BBOX dataset
	 */
	private void hookClickAction () {
		treeViewer.getTree().addSelectionListener(new SelectionListener() 
		{
			public void widgetDefaultSelected(SelectionEvent e) {
				clickAction.run();
			}

			public void widgetSelected(SelectionEvent e) {
				clickAction.run();
			}
		});
	}
	
	
	
	/**
	 * Passing the focus request to the treeViewer's control.
	 */
	public void setFocus() {
		treeViewer.getControl().setFocus();
	}

	
	/**
	 * Add a World Wind Layer to the view. This view accepts {@link TimeLoopGroundOverlay} only 
	 */
	public void addOverlays (Layer[] layers, boolean enabled) 
	{
		if ( layers == null ) return;
		
		Layer first = layers[0];
		
		// Time Loop layer
		if ( first instanceof TimeLoopGroundOverlay) {
			treeViewer.addTimeLoopGroundOverlays( (TimeLoopGroundOverlay[])layers);
			return;
		}
		// regular layer
		else {
			statusLine.setErrorMessage(Messages.getText("layers.view.1") + first.getClass().getName());
		}
	}

	/**
	 * Add a World Wind {@link Layer} to the tree
	 * @param layer
	 * @param icon
	 * @param enabled
	 */
	public void addLayer (Layer layer, Image icon, boolean enabled) {
		layer.setEnabled(enabled);
		treeViewer.addTreeObject(new TreeParent(layer, icon	)
				, null
				, true		// Make layer renderable
				, enabled); // checked
		
	}
	
	
	public void setStausMessage(String message){
		statusLine.setMessage(message);
	}
	
	public void setStatusErrorMessage (String message) {
		statusLine.setErrorMessage(message);
	}
}

