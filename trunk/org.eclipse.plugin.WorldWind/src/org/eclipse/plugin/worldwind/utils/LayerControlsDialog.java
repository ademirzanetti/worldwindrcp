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
import org.eclipse.swt.widgets.Composite; 
import org.eclipse.swt.widgets.Control; 
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell; 

import worldwind.contrib.layers.loop.TimeLoopGroundOverlay;

public class LayerControlsDialog extends Dialog
	implements Listener
{ 
	private static final Logger logger	= Logger.getLogger(LayerControlsDialog.class);
	
	// WW layer to control
	private Layer layer;
	
	// Applies for TimeLoops only
	private AnimationJob animation = null;
	
	
    public LayerControlsDialog(Shell parentShell, Layer layer) {
        super(parentShell);
        this.layer = layer;
    } 
    
    public void setAnimationJob (AnimationJob job) {
    	animation = job;
    }
    
    protected Control createDialogArea(Composite parent) { 
    	
		Composite container = new Composite(parent, SWT.NULL);		
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL
				, GridData.FILL_HORIZONTAL, true, true, 5, 5));
		
    	logger.debug(layer);

		Label l1 = new Label(container, SWT.NONE);
		l1.setText(Messages.getText("lyr.ctl.1"));
		
		Label l2 = new Label(container, SWT.NONE);
		l2.setText(layer.getName());

		Label l3 = new Label(container, SWT.NONE);
		l3.setText(Messages.getText("lyr.ctl.2"));

		Scale opacity = new Scale (container, SWT.NONE);
		opacity.addListener(SWT.Selection, this);
		opacity.setSize (200, 64);
		opacity.setMaximum (100);
		opacity.setIncrement(10);		
		opacity.setPageIncrement (10);
		opacity.setData("NAME", "OPACITY");
		opacity.setSelection((int)(layer.getOpacity() * 100));

		if  ( layer instanceof TimeLoopGroundOverlay ) 
		{
			Label l4 = new Label(container, SWT.NONE);
			l4.setText(Messages.getText("lyr.ctl.3"));
	
			Scale speed = new Scale (container, SWT.NONE);
			speed.setSize (200, 64);
			speed.setMaximum (100);
			speed.setIncrement (10);
			speed.setPageIncrement (10);
			speed.setData("NAME", "SPEED");
			speed.addListener(SWT.Selection, this);
			
			// start speed
			if ( animation != null)
				speed.setSelection(animation.getSpeed());
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
        return new Point(250, 200); 
    }


	public void handleEvent(Event event) {
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
