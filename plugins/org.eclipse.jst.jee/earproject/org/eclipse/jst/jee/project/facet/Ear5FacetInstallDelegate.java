/******************************************************************************
 * Copyright (c) 2005 BEA Systems, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Konstantin Komissarchik - initial API and implementation
 ******************************************************************************/

package org.eclipse.jst.jee.project.facet;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jem.util.logger.proxy.Logger;
import org.eclipse.jst.common.project.facet.WtpUtils;
import org.eclipse.jst.j2ee.internal.earcreation.IEarFacetInstallDataModelProperties;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.FacetDataModelProvider;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

public final class Ear5FacetInstallDelegate implements IDelegate {

	public void execute(final IProject project, final IProjectFacetVersion fv, final Object cfg, final IProgressMonitor monitor) throws CoreException {
		if (monitor != null) {
			monitor.beginTask("", 1); //$NON-NLS-1$
		}

		try {
			IDataModel model = (IDataModel) cfg;

			if (monitor != null) {
				monitor.worked(1);
			}
			// Add WTP natures.

			WtpUtils.addNaturestoEAR(project);

			final IVirtualComponent c = ComponentCore.createComponent(project);
			c.create(0, null);

			final IVirtualFolder earroot = c.getRootFolder();
			earroot.createLink(new Path("/" + model.getStringProperty(IEarFacetInstallDataModelProperties.CONTENT_DIR)), 0, null); //$NON-NLS-1$

// TODO commented on 12/13/2006 do not want to create ear xml file right now			
//			if (!project.getFile(JEEConstants.APPLICATION_DD_URI).exists()) {
//				String ver = fv.getVersionString();
//				int nVer = JEEVersionUtil.convertVersionStringToInt(ver);
//				EARArtifactEdit.createDeploymentDescriptor(project, nVer);
//			}

			try {
				((IDataModelOperation) model.getProperty(FacetDataModelProvider.NOTIFICATION_OPERATION)).execute(monitor, null);
			} catch (ExecutionException e) {
				Logger.getLogger().logError(e);
			}
		}

		finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}

}