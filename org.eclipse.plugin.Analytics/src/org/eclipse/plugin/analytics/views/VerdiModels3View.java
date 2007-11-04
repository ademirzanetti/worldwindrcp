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
package org.eclipse.plugin.analytics.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.plugin.analytics.AnalyticsActivator;
import org.eclipse.plugin.analytics.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import anl.verdi.core.Project;
import anl.verdi.core.VerdiApplication;
import anl.verdi.core.VerdiGUI;
import anl.verdi.data.DataLoader;
import anl.verdi.data.DataManager;
import anl.verdi.data.Dataset;
import anl.verdi.formula.FormulaVariable;
import anl.verdi.formula.Formula.Type;
import anl.verdi.gui.DataSetPanel;
import anl.verdi.gui.DatasetListModel;
import anl.verdi.gui.FormulaListElement;
import anl.verdi.gui.FormulaListModel;
import anl.verdi.gui.FormulasPanel;
import anl.verdi.loaders.Models3Loader;
import anl.verdi.loaders.dap.OpenDAPLoader;
import anl.verdi.plot.gui.DefaultPlotCreator;
import anl.verdi.plot.gui.Plot;
import anl.verdi.plot.gui.PlotPanel;
import anl.verdi.plot.gui.ScatterPlotCreator;
import anl.verdi.plot.gui.VectorPlotCreator;

/**
 * See
 * Package for Analysis and Visualization of Environmental Data (PAVE)
 * Visualization Environment for Rich Data Interpretation (VERDI)
 * @author Owner
 *
 */
public class VerdiModels3View extends ViewPart 
{
	public static final String ID = VerdiModels3View.class.getName();

	// Init swing look & feel
	static {
		AnalyticsActivator.initSwingLookAndFeel();
	}
	
	// Verdi App
	private Verdi verdi;
	
	/**
	 * This is an implementation of VERDI
	 * Visualization Environment for Rich Data Interpretation:
	 * Developed by ANL for the EPA  to replace the 
	 * Package for Analysis and Visualization of Environmental Data (PAVE).
	 * @author Vladimir Silva
	 *
	 */
	private static class Verdi extends VerdiApplication  
	{
		private JFrame mainFrame;

		/**
		 * Listen to dialog close events  
		 * @author Owner
		 */
		private class DialogListener extends WindowAdapter {

			JFrame dialog;
			PlotPanel plotPanel;
			
			public DialogListener(JFrame dialog, PlotPanel plotPanel) {
				this.dialog = dialog;
				this.plotPanel = plotPanel;
			}

			@Override
			public void windowClosing(WindowEvent e) {
				plotPanel = null;
				dialog.dispose();
				dialog = null;
				System.gc();
			}
		}
		
		/**
		 * Plot Action  
		 * @author vsilva
		 */
		class PlotAction extends AbstractAction 
		{
			private static final long serialVersionUID = 6430683618103939142L;
			
			private Type type;
			
			public PlotAction(Type plotType) {
				type = plotType;
			}

			public void actionPerformed(ActionEvent e) {
				FormulaListElement formulaList =  getProject().getSelectedFormula();
				
				if ( formulaList == null) {
					getGui().showMessage(Messages.getString("VerdiModels3View.0"), Messages.getString("VerdiModels3View.1")); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
				
				try {
					// plot 1st var from selected formula
				    FormulaVariable formula = formulaList.variables().iterator().next();
					
					Plot plot = createPlot(type, formula);
					
					if ( plot == null) return ; // canceled?
					
					// A window needs to be created for non contour plots
					if ( type != Type.CONTOUR) 
					{
						PlotPanel plotPanel = new PlotPanel(plot, formula.getDataset().getName());
						
						JFrame frame = newJFrame(formula.getDataset().getName() + " - " + formula.getName()
							, 500
							, 500
							, plotPanel 
							);  
						
						frame.addWindowListener(new DialogListener(frame, plotPanel));
					}
				} catch (Exception ex) {
					getGui().showMessage(Messages.getString("VerdiModels3View.3")
							, Messages.getString("VerdiModels3View.4") 
								+ ex.getClass().getName() 
								+ ": "  + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
			}
		}
		
		/**
		 * Constructor
		 * @throws Exception
		 */
		public Verdi() throws Exception {
			super(createDataManager());
			
			mainFrame = new JFrame(Messages.getString("VerdiModels3View.5")); //$NON-NLS-1$
			
		    mainFrame.setPreferredSize(new Dimension(500, 800));
			mainFrame.setContentPane(initComponents());
		}

		
		/**
		 * Init data loaders
		 * @return
		 */
		static private DataManager createDataManager () {
			List<DataLoader> dataLoaders = new ArrayList<DataLoader>();
			
			dataLoaders.add(new OpenDAPLoader());
			dataLoaders.add(new Models3Loader());

			return new DataManager(dataLoaders);
		}
		
		/**
		 * Local Actions
		 */
		private Action openDatasetAction = new AbstractAction() {
			private static final long serialVersionUID = -6350607647745319459L;

			public void actionPerformed(ActionEvent e) {
				addDataset();
			}
		};
		
		/**
		 * Init GUI components: Plot buttons and data panels
		 * @return
		 */
		private JPanel initComponents() {
			// Button bar: hold all plot buttons (NORTH)
		    JPanel buttonBar = new JPanel();
		    buttonBar.setLayout(new FlowLayout());

			// Contour btn
		    JButton button1 = new JButton(Messages.getString("VerdiModels3View.6"));  //$NON-NLS-1$
		    button1.addActionListener(new PlotAction(Type.CONTOUR));	    
			buttonBar.add(button1);
			
			// Time Series btn
		    JButton button2 = new JButton(Messages.getString("VerdiModels3View.7"));  //$NON-NLS-1$
		    button2.addActionListener(new PlotAction(Type.TIME_SERIES_LINE));	    
			buttonBar.add(button2);
			
			// Tile btn
		    JButton button3 = new JButton(Messages.getString("VerdiModels3View.8"));  //$NON-NLS-1$
		    button3.addActionListener(new PlotAction(Type.TILE));	    
			buttonBar.add(button3);

			// Scatter plot btn
		    JButton button4 = new JButton(Messages.getString("VerdiModels3View.9"));  //$NON-NLS-1$
		    button4.addActionListener(new PlotAction(Type.SCATTER_PLOT));	    
			buttonBar.add(button4);

			// Vector plot btn
		    JButton button5 = new JButton(Messages.getString("VerdiModels3View.10"));  //$NON-NLS-1$
		    button5.addActionListener(new PlotAction(Type.VECTOR));	    
			buttonBar.add(button5);

			
			// Main panel
			JPanel mainPanel = new JPanel(new BorderLayout());
			
			// button bar (NORTH)
			mainPanel.add(buttonBar, BorderLayout.NORTH);
			
			// Dataset/Formula panel: (CENTER)
			mainPanel.add(initDataPanel());
			
			return mainPanel;
		}
		
		/**
		 * Init app data panels: Datasets & Formulas
		 * @return
		 */
		JTabbedPane initDataPanel () 
		{
			DatasetListModel datasetModel = new DatasetListModel();
			FormulaListModel formulaModel = new FormulaListModel();
			formulaModel.addListDataListener(this);
			Project project = new Project(datasetModel, formulaModel);

			// dataset panel
			DataSetPanel datasetPanel = new DataSetPanel(project, getDomainPanelContext());
			datasetPanel.addOpenDatasetAction(openDatasetAction);
			datasetModel.addDatasetModelListener(this);
			JScrollPane pane1 = new JScrollPane(datasetPanel);
			
			// formulas panel
			FormulasPanel formulasPanel = new FormulasPanel(formulaModel);
			formulasPanel.addFormulaSelectionListener(this);
			formulasPanel.setFormulaCreator(this);
			// this is necessary to avoid the horizontal scrollbar
			// showing up before it should
			formulasPanel.setPreferredSize(formulasPanel.getMinimumSize());
			JScrollPane pane2 = new JScrollPane(formulasPanel);

			JTabbedPane tabbedPanel = new JTabbedPane();
			
			tabbedPanel.addTab(Messages.getString("VerdiModels3View.12"), pane1); //$NON-NLS-1$
			tabbedPanel.addTab(Messages.getString("VerdiModels3View.13"), pane2);  //$NON-NLS-1$
			
			datasetPanel.addFormulaCallbacks(this, formulasPanel.getFormulaEditor());

			init(new VerdiGUI(null, datasetPanel, formulasPanel), project);
			return tabbedPanel; 
		}
		
		/**
		 * Create a default plot
		 * @param file
		 * @return
		 * @throws MalformedURLException
		 */
		private Plot createPlot( Type type, FormulaVariable formula)
			throws MalformedURLException
		{
			if ( type == Type.SCATTER_PLOT) {
				ScatterPlotCreator creator = new ScatterPlotCreator(this);
				return creator.doCreatePlot();
			}

			if ( type == Type.VECTOR) {
				VectorPlotCreator creator = new VectorPlotCreator(this);
				return creator.doCreatePlot();
			}
			
			DefaultPlotCreator plotCreator = new DefaultPlotCreator(this, type);
			return plotCreator.doCreatePlot();
		}
		
		/**
		 * Utility to build a frame
		 * @param title
		 * @param width
		 * @param height
		 * @param contents
		 */
		private JFrame newJFrame(String title
				, int width, int height, JPanel contents) 
		{
			JFrame frame = new JFrame(title);
		    frame.setPreferredSize(new Dimension(width, height));
		    frame.setContentPane(contents);
		    frame.pack();
		    frame.setVisible(true);
		    return frame;
		}
		
		
		public JFrame getMainFrame () {
			return mainFrame;
		}
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		Composite top = new Composite(parent, SWT.EMBEDDED);
		
		java.awt.Frame verdiFrame = SWT_AWT.new_Frame(top);
		java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout());
		panel.setBackground(java.awt.Color.GRAY);
		
		verdiFrame.add(panel);
		
		try {
			verdi = new Verdi();
			
			panel.add( verdi.getMainFrame().getContentPane(), BorderLayout.WEST);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFocus() {
		
	}

	/**
	 * Load a remote dataset (OpeNDAP) fro example:
	 * http://motherlode.ucar.edu:8080/thredds/dodsC/fmrc/NCEP/GFS/Alaska_191km/runs/NCEP-GFS-Alaska_191km_RUN_2007-11-02T12:00:00Z.dods  
	 * @param url remote dataset url
	 */
	public void addDatasetFromUrl (String url) throws MalformedURLException 
	{
		DataManager manager 	= verdi.getDataManager(); 
		List<Dataset> dataSets	= null;
		
		try {
			dataSets = manager.createDatasets(new URL(url));
		} 
		catch (Exception e) {
			// Create the required Status object
	        Status status = new Status(IStatus.ERROR
	        		, AnalyticsActivator.PLUGIN_ID, 0
	        		, e.getMessage()
	        		, e);

	        // Display the dialog
	        ErrorDialog.openError(getViewSite().getShell()
	        		, AnalyticsActivator.PLUGIN_NAME
	        		, Messages.getString("VerdiModels3View.14") + " " + url
	        		, status);
	        return;
		}
		 
		for (Dataset dataset : dataSets) {
			verdi.getGui().loadDataset(dataset);
		}
	}
}
