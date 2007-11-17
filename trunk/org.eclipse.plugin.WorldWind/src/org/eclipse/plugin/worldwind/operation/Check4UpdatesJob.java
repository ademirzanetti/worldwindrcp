package org.eclipse.plugin.worldwind.operation;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.plugin.worldwind.Messages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.ILocalSite;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.OperationsManager;

/**
 * A Job to check for software updates or new features.
 * For security reasons, features will not be installed without
 * the user's permission.
 *  
 * The update manager can be used to do the same thing. However the user
 *  needs to know the update site.
 *  
 * @author Owner
 *
 */
public class Check4UpdatesJob extends Job
{
	private static final Logger logger	= Logger.getLogger(Check4UpdatesJob.class);
	
	private Display display;
	
	public Check4UpdatesJob(Display display) { 
		super("Check for updates");
		this.display = display;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) 
	{
		final String updateSite = Messages.getText("upd.site");
		
		String features = "";
		final String footer	= Messages.getText("upd.footer.msg");
		
		try {
			logger.debug("Checking 4 updates in " + updateSite);

			// Get remote & local features
			ISite remoteSite 	 = SiteManager.getSite(new URL(updateSite), monitor);
			ILocalSite localSite = SiteManager.getLocalSite();

			IConfiguredSite configuredSite 		= localSite.getCurrentConfiguration().getConfiguredSites()[0];
			
			IFeatureReference[] remoteFeatures 	= remoteSite.getFeatureReferences();
			IFeatureReference[] localFeatures 	= configuredSite.getConfiguredFeatures();
			
			// Feature install operations
			final List<IInstallFeatureOperation> installOps = new ArrayList<IInstallFeatureOperation>();

			logger.debug("Remote features refs len=" + remoteFeatures.length 
					+ " Configured(Local) features len=" + localFeatures.length);
			
			// if remote features == 0 nothing to install
			if ( remoteFeatures.length == 0 ) {
				logger.debug("No remote featues found in " + remoteSite);
				return Status.OK_STATUS;
			}

			// if remote features size > 0 && local feats == 0 then there are new features to be installed
			if ( remoteFeatures.length > 0 && localFeatures.length == 0) 
			{
				logger.debug("New features to install=" + remoteFeatures.length);
				
				for (int i = 0; i < remoteFeatures.length; i++) 
				{
					features += "\n" + remoteFeatures[i].getName();
					
					// Add install operation
					installOps.add(OperationsManager.getOperationFactory().createInstallOperation(
							configuredSite, remoteFeatures[i].getFeature(monitor), null, null, null));					
				}
				
				final String message =  Messages.getText("upd.job.1")
				 					+ "\n" + features + "\n\n" + footer;
				
				// Ask if new features should be installed
				display.syncExec(new Runnable() {
					public void run() 
					{
						final boolean install = MessageDialog.openQuestion(display.getActiveShell()
								, Messages.getText("info.dialog.title")
								, message) ;
						
						// install new features
						if ( install ) 
							installFeatures(installOps, monitor);
						
					}
				});
				return Status.OK_STATUS;
			}
			
			// There are updates for installed features
			for (int i = 0; i < remoteFeatures.length; i++) 
			{
				logger.debug("Looking 4 update of remote feature=" +  remoteFeatures[i].getName() + " in local site.");
				
				for (int j = 0; j < localFeatures.length; j++) {

					VersionedIdentifier rfrsVi = remoteFeatures[i].getVersionedIdentifier();
					VersionedIdentifier lfrsVi = localFeatures[j].getVersionedIdentifier();
					
					logger.debug("Remote feat vid=" + rfrsVi + " local feature vid="  + lfrsVi);
					
					if (rfrsVi.getIdentifier().equals(lfrsVi.getIdentifier())) {
						features += "\n" + localFeatures[j].getName();
						logger.debug("Update found for feature " + lfrsVi.getIdentifier());
						
						if (rfrsVi.getVersion().isGreaterThan(lfrsVi.getVersion())) {
							installOps.add(OperationsManager.getOperationFactory().createInstallOperation(
								configuredSite, remoteFeatures[i].getFeature(monitor), null, null, null));
						}
					}
				}
			}
			
			// Ask if the user wants to update
			//if ( features.length() >  0)
			if ( installOps.size() > 0 )
			{
				final String message = Messages.getText("upd.job.2")
				 						+ "\n" + features + "\n\n" + footer;
				
				display.syncExec(new Runnable() {
					public void run() 
					{
						final boolean update = MessageDialog.openQuestion(display.getActiveShell()
								, Messages.getText("info.dialog.title")
								, message);
						
						// Update features
						if ( update ) 
							installFeatures(installOps, monitor);
						
					}
				});	
			}
		} 
		catch (Exception e) {
			//e.printStackTrace();
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}


	/**
	 * Install features
	 * @param installOps
	 * @param monitor
	 */
	void installFeatures( List<IInstallFeatureOperation> installOps, IProgressMonitor monitor) 
	{
		try {
			ILocalSite ls = SiteManager.getLocalSite();
			
			if (installOps.size() > 0) {
				for (Iterator<IInstallFeatureOperation> iter = installOps.iterator(); iter.hasNext();) 
				{
					IInstallFeatureOperation op = iter.next();
					
					logger.debug("Installing feature=" + op.getFeature().getLabel());
					op.execute(monitor, null);
				}
				ls.save();
			}
		} catch (Exception e) {
			Messages.showErrorMessage(display.getActiveShell()
					, Messages.getText("err.dialog.title")
					, e.getMessage());
		}
	}
	
}