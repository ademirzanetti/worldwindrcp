package org.eclipse.plugin.worldwind.utils;

import gov.nasa.worldwind.WorldWind;

import java.io.File;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * A simple cache manager dialog for WW
 * @author vsilva
 *
 */
public class CacheManagerDialog extends Dialog
{
	private TableViewer viewer;
	private Combo combo;
	private NumberFormat nf = NumberFormat.getInstance();

	/**
	 * Constructor
	 * @param parentShell
	 */
	public CacheManagerDialog(Shell parentShell) {
		super(parentShell);
		nf.setMaximumFractionDigits(2);
	}

    /**
     * Configure dialog 
     */
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        
        // set title
        shell.setText(Messages.getText("CacheManagerDialog.0")); //$NON-NLS-1$
    }
	

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);	
		
		container.setLayout(new GridLayout(1, true));
		container.setLayoutData(new GridData(GridData.FILL_BOTH)); 

		
		GridData gd = new GridData(GridData.FILL_BOTH);

		Label l1 = new Label(container, SWT.NONE);
		l1.setText(Messages.getText("CacheManagerDialog.1")); //$NON-NLS-1$
		
		combo =  new Combo(container, SWT.BORDER);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.addSelectionListener(new SelectionAdapter()
        {
			public void widgetSelected(SelectionEvent e) {
				final String path = ((Combo)e.getSource()).getText();
				
				viewer.getTable().removeAll();
				
				//System.out.println(path);
				loadTable(new File(path));
			}
		});

		
		viewer = new TableViewer(container, SWT.BORDER | SWT.MULTI);
		viewer.getTable().setLayoutData(gd);
		
		loadData();
		
		return container;
	}
	
	/*
	 * Load cache locations
	 */
	private void loadData () {
		// load combo with WW caches
		List<File> caches =  WorldWind.getDataFileCache().getCacheLocations();
		
		for (File file : caches) {
			combo.add(file.toString());
		}
		
		// load table w/ 1st location files
		//loadTable(caches.get(0));
	}
	
	/*
	 * Load table with All directory names/sizes within a location
	 */
	private void loadTable ( File folder)
	{
		File[] filelist = folder.listFiles();

    	if ( filelist == null ) 
    		return ;
    	
		for (int i = 0; i < filelist.length; i++) 
		{
			if ( filelist[i].isDirectory()) {
				viewer.add(filelist[i] + " - ("  //$NON-NLS-1$
					+ nf.format((double)(getFileSize(filelist[i])/1e6)) 
					+ " MB)" ); //$NON-NLS-1$
				
				loadTable(filelist[i]);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
    protected void createButtonsForButtonBar(Composite parent) 
    {
        Button btn = createButton(parent, IDialogConstants.NO_ID, 
            Messages.getText("CacheManagerDialog.2"), true); //$NON-NLS-1$
        
        btn.addSelectionListener(new SelectionAdapter()
        {
			@SuppressWarnings("unchecked") //$NON-NLS-1$
			public void widgetSelected(SelectionEvent e) 
			{
				// Delete selected
				try {
					IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
					
					if ( selection == null || selection.isEmpty() )
						return;
					
					Iterator iter = selection.iterator();
					
					// delete selected items
					while (iter.hasNext()) 
					{
						final String sel = (String)iter.next();
						// get path
						final String path = sel.split("-")[0].trim(); //$NON-NLS-1$
						
						//System.out.println("Delete " + path);
						deleteResource(new File(path));
					}
					
					// refresh table
					final String path = combo.getText();
					
					viewer.getTable().removeAll();
					
					loadTable(new File(path));
				} 
				catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
        
        
        // close dlg
        createButton(parent, IDialogConstants.CANCEL_ID, 
        		IDialogConstants.CLOSE_LABEL, false); 
    } 
    
    protected Point getInitialSize() { 
        return new Point(550, 320); 
    }
	
    /*
     * Simple File Utilities
     */
    
    /*
     * Get folder size
     */
    static long getFileSize(File folder)
	{
    	long foldersize = 0;
 
    	File[] filelist = folder.listFiles();

    	if ( filelist == null ) return foldersize;
    	
		for (int i = 0; i < filelist.length; i++) {
			if (filelist[i].isDirectory()) {
				foldersize += getFileSize(filelist[i]);
			} else {
				foldersize += filelist[i].length();
			}
		}

		return foldersize;
	}     
    
    /*
     * Delete a file or folder recursively
     */
    public static void deleteResource(File dir)  
    {	       
    	if (dir == null) 
			return; 

    	if (dir.isFile()) {
    		dir.delete();
    		return;
    	}
    	
		final File[] files = dir.listFiles();
		final int size = files.length;

		for (int i = 0; i < size; i++) 
		{
			if (files[i].isDirectory()) 
				deleteResource(files[i]);
			else
				files[i].delete();  
		}
		dir.delete();
    }
    

}
