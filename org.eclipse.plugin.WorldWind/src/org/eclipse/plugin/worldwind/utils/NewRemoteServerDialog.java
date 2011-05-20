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
package org.eclipse.plugin.worldwind.utils;

import java.io.IOException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Remote server Name and URL input dialog (Used to get a new WMS Server URL)
 * @author vsilva
 *
 */
public class NewRemoteServerDialog extends Dialog
{
	private Text server;
	private Text url;
	
	private String name, Url;
	
	public NewRemoteServerDialog(Shell parentShell) {
		super(parentShell);
	}

    /**
     * Configure dialog 
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        
        // set title
        shell.setText(Messages.getString("NewRemoteServerDialog.0")); //$NON-NLS-1$
    }

    @Override
    protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);	
		
		container.setLayout(new GridLayout(2, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label l1 = new Label(container, SWT.NONE);
		l1.setText(Messages.getString("NewRemoteServerDialog.1")); //$NON-NLS-1$
		
		server = new Text(container, SWT.BORDER);
		server.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label l2 = new Label(container, SWT.NONE);
		l2.setText(Messages.getString("NewRemoteServerDialog.2")); //$NON-NLS-1$
		
		url = new Text (container, SWT.BORDER);
		url.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		return container;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
    	Button ok = createButton(parent, IDialogConstants.NO_ID, 
        		IDialogConstants.OK_LABEL, false); 
    	
        ok.addSelectionListener(new SelectionAdapter()
        {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void widgetSelected(SelectionEvent e) 
			{
				try {
					name 	= server.getText();
					Url 	= url.getText();

					if (name == null || Url == null || !Url.startsWith("http")) //$NON-NLS-1$
						throw new IOException(Messages.getString("NewRemoteServerDialog.4")); //$NON-NLS-1$
					
					// add Capabilities request (if missing)
					if ( Url.indexOf("?") == -1 ) //$NON-NLS-1$
						Url += "?service=WMS&request=GetCapabilities"; //$NON-NLS-1$
					
					close();
				} 
				catch (Exception ex) {
					Messages.showErrorMessage(getShell(), ex.getMessage());
				}
			}
        });
        
        createButton(parent, IDialogConstants.CANCEL_ID, 
        		IDialogConstants.CANCEL_LABEL, false); 
    }
    
    public String getName (){
    	return name;
    }
    
    public String getUrl (){
    	return Url;
    }
    
    protected Point getInitialSize() { 
        return new Point(450, 130); 
    }
    
}
