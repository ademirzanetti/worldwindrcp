package org.eclipse.plugin.worldwind.utils;


import gov.nasa.worldwind.layers.Layer;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog; 
import org.eclipse.jface.dialogs.IDialogConstants; 
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.plugin.worldwind.operation.AnimationJob;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point; 
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite; 
import org.eclipse.swt.widgets.Control; 
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell; 

import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;
import worldwind.contrib.layers.quadkey.VirtualEarthLayer;

public class LayerControlsDialog extends Dialog
	implements Listener
{ 
	private static final Logger logger	= Logger.getLogger(LayerControlsDialog.class);
	
	// WW layer to control
	private Layer layer;
	
	// Applies for TimeLoops only
	private AnimationJob animation = null;
	
	/**
	 * Layer Controls Constructor
	 * @param parentShell
	 * @param layer
	 */
    public LayerControlsDialog(Shell parentShell, Layer layer) {
        super(parentShell);
        this.layer = layer;
    } 

    /**
     * Configure dialog 
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        
        // set title
        shell.setText(Messages.getString("lyr.ctl.0"));
     }
    
    public void setAnimationJob (AnimationJob job) {
    	animation = job;
    }
    
    /**
     * Dialog GUI
     */
    protected Control createDialogArea(Composite parent) { 
    	
		Composite container = new Composite(parent, SWT.NULL);	
		
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				, GridData.FILL_VERTICAL, true, true)); 
		
    	logger.debug(layer.toString());

    	// Layer
		Label l1 = new Label(container, SWT.NONE);
		l1.setText(Messages.getString("lyr.ctl.1"));
		
		// Layer name
		Label l2 = new Label(container, SWT.NONE);
		l2.setText(layer.getName());

		// Opacity
		Label l3 = new Label(container, SWT.NONE);
		l3.setText(Messages.getString("lyr.ctl.2"));

		Scale opacity = new Scale (container, SWT.NONE);
		opacity.addListener(SWT.Selection, this);
		//opacity.setSize (200, 64);
		opacity.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		opacity.setMaximum (100);
		opacity.setIncrement(10);		
		opacity.setPageIncrement (10);
		opacity.setData("NAME", "OPACITY");
		opacity.setSelection((int)(layer.getOpacity() * 100));

		if  ( layer instanceof TimeLoopGroundOverlay ) 
		{
			// Speed
			Label l4 = new Label(container, SWT.NONE);
			l4.setText(Messages.getString("lyr.ctl.3"));
	
			Scale speed = new Scale (container, SWT.NONE);
			//speed.setSize (200, 64);
			speed.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			speed.setMaximum (100);
			speed.setIncrement (10);
			speed.setPageIncrement (10);
			speed.setData("NAME", "SPEED");
			speed.addListener(SWT.Selection, this);
			
			// start speed
			if ( animation != null)
				speed.setSelection(animation.getSpeed());
		}
		
		// MS VE layer controls: Map type: Aerial, Road or Hybrid
		if ( layer instanceof VirtualEarthLayer )
		{
			VirtualEarthLayer msVe = (VirtualEarthLayer)layer;
			
			GridData gd = new GridData(GridData.FILL_BOTH);
			gd.horizontalSpan = 2;
			gd.grabExcessHorizontalSpace = true;
			
			// road
			Button btnRoad = new Button(container, SWT.RADIO);
			btnRoad.setData("MAP_TYPE", VirtualEarthLayer.MAP_ROAD);
			btnRoad.setText(Messages.getString("lyr.ctl.4"));
			btnRoad.setLayoutData(gd);
		    btnRoad.addListener(SWT.Selection, this);

		    // Aerial
			Button btnAerial = new Button(container, SWT.RADIO);
			btnAerial.setData("MAP_TYPE", VirtualEarthLayer.MAP_AERIAL);
			btnAerial.setText(Messages.getString("lyr.ctl.5"));
			btnAerial.setLayoutData(gd);
			btnAerial.addListener(SWT.Selection, this);

			// Hybrid
			Button btnHybrid = new Button(container, SWT.RADIO);
			btnHybrid.setData("MAP_TYPE", VirtualEarthLayer.MAP_HYBRID);
			btnHybrid.setText(Messages.getString("lyr.ctl.6"));
			btnHybrid.setLayoutData(gd);
			btnHybrid.addListener(SWT.Selection, this);
			
			// set button selection
			final String mapType = msVe.getMapType();
			
			if ( mapType.equals(VirtualEarthLayer.MAP_ROAD))
				btnRoad.setSelection(true);
			else if ( mapType.equals(VirtualEarthLayer.MAP_AERIAL))
				btnAerial.setSelection(true);
			else 
				btnHybrid.setSelection(true);
		}
        return container; 
    }
    
    protected void createButtonsForButtonBar(Composite parent) 
    {
        createButton(parent, IDialogConstants.OK_ID, 
            IDialogConstants.OK_LABEL, true); 
        createButton(parent, IDialogConstants.CANCEL_ID, 
            IDialogConstants.CANCEL_LABEL, false); 
    } 
    
    protected Point getInitialSize() { 
        return new Point(280, 200); 
    }


	public void handleEvent(Event event) 
	{
		// Layer is of type VirtualEarth
		if ( event.widget instanceof Button ) 
		{
			Button btn 		= (Button)event.widget;
			((VirtualEarthLayer)layer).setMapType((String)btn.getData("MAP_TYPE"));
			return;
		}
		
		// handle scale widgets
		Scale scale 		= (Scale)event.widget;
		final String name 	= scale.getData("NAME").toString();
		
		int value = scale.getSelection();
		
		if ( name.equals("OPACITY") ) {
			double opacity = (double)value/100;
			layer.setOpacity(opacity);
		}

		// for time loops only
		if ( name.equals("SPEED") ) {
			//((TimeLoopGroundOverlay)layer).setSpeed(value);
			animation.setSpeed(value);
		}
	} 

}
