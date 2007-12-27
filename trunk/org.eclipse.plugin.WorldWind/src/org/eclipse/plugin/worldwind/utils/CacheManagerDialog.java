package org.eclipse.plugin.worldwind.utils;

import gov.nasa.worldwind.WorldWind;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
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


	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);	
		
		container.setLayout(new GridLayout(1, true));
		container.setLayoutData(new GridData(GridData.FILL_BOTH)); 

		
		GridData gd = new GridData(GridData.FILL_BOTH);

		Label l1 = new Label(container, SWT.NONE);
		l1.setText("Cache location");
		
		combo =  new Combo(container, SWT.BORDER);
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		combo.addSelectionListener(new SelectionAdapter()
        {
			public void widgetSelected(SelectionEvent e) {
				final String path = ((Combo)e.getSource()).getText();
				
				viewer.getTable().removeAll();
				
				System.out.println(path);
				loadTable(new File(path));
			}
		});

		
		viewer = new TableViewer(container, SWT.BORDER);
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
	 * Load table data
	 */
	private void loadTable ( File folder)
	{
		File[] filelist = folder.listFiles();

    	if ( filelist == null ) 
    		return ;
    	
		for (int i = 0; i < filelist.length; i++) 
		{
			viewer.add(filelist[i] + " - (" 
					+ nf.format((double)(getFileSize(filelist[i])/1e6)) 
					+ " MB)" );
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
    protected void createButtonsForButtonBar(Composite parent) 
    {
        Button btn = createButton(parent, IDialogConstants.NO_ID, 
            "Delete Selected", true);
        
        btn.addSelectionListener(new SelectionAdapter()
        {
			public void widgetSelected(SelectionEvent e) {
				// Delete selected
				try {
					final String sel = (String)((IStructuredSelection)viewer.getSelection()).getFirstElement();
					
					if ( sel == null )
						return;
					
					// get path
					final String path = sel.split("-")[0].trim();
					
					//System.out.println("Delete " + path);
					deleteDirectory(new File(path));
					
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
        
        
        // close dlg
        createButton(parent, IDialogConstants.CANCEL_ID, 
            IDialogConstants.CANCEL_LABEL, false); 
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
     * Delete a folder recursively
     */
    public static void deleteDirectory(File dir)  
    {	       
    	if ((dir == null) || !dir.isDirectory())
			return; 

		final File[] files = dir.listFiles();
		final int size = files.length;

		for (int i = 0; i < size; i++) 
		{
			if (files[i].isDirectory()) 
				deleteDirectory(files[i]);
			else
				System.out.println("del file " + files[i]);// files[i].delete();
		}
		System.out.println("del dir " + dir); //dir.delete();
    }
    

}
