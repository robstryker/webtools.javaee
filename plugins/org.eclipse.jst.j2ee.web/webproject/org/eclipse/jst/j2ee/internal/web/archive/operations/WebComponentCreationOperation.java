/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Nov 6, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.eclipse.jst.j2ee.internal.web.archive.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jem.util.logger.proxy.Logger;
import org.eclipse.jst.j2ee.application.operations.J2EEComponentCreationDataModel;
import org.eclipse.jst.j2ee.application.operations.J2EEComponentCreationOperation;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
import org.eclipse.jst.j2ee.internal.J2EEVersionUtil;
import org.eclipse.jst.j2ee.internal.web.util.WebArtifactEdit;
import org.eclipse.wst.common.modulecore.ModuleCore;
import org.eclipse.wst.common.modulecore.WorkbenchComponent;
import org.eclipse.wst.common.modulecore.internal.util.IModuleConstants;

public class WebComponentCreationOperation extends J2EEComponentCreationOperation {
	public WebComponentCreationOperation(WebComponentCreationDataModel dataModel) {
		super(dataModel);
	}

	public WebComponentCreationOperation() {
		super();
	}


	protected void createDeploymentDescriptor(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		
		
		String moduleName = (String)operationDataModel.getProperty(WebComponentCreationDataModel.MODULE_NAME);

		
		IFolder moduleFolder = getProject().getFolder( moduleName );

		if (!moduleFolder.exists()) {
			moduleFolder.create(true, true, null);
		}

		IFolder javaSourceFolder = moduleFolder.getFolder( "JavaSource" );
		if (!javaSourceFolder.exists()) {
			javaSourceFolder.create(true, true, null);
		}
		
		IFolder webContentFolder = moduleFolder.getFolder( "WebContent" );
		if (!webContentFolder.exists()) {
			webContentFolder.create(true, true, null); 
		}
		
		IFolder metainf = webContentFolder.getFolder(J2EEConstants.META_INF);
		if (!metainf.exists()) {
			IFolder parent = metainf.getParent().getFolder(null);
			if (!parent.exists()) {
				parent.create(true, true, null);
			}
			metainf.create(true, true, null);
		}
		
		
		IFolder webinf = webContentFolder.getFolder(J2EEConstants.WEB_INF);
		if (!webinf.exists()) {
			webinf.create(true, true, null);
		}
		
		IFolder lib = webinf.getFolder("lib"); //$NON-NLS-1$
		if (!lib.exists()) {
			lib.create(true, true, null);
		}

		
		//should cache wbmodule when created instead of  searching ?
        ModuleCore moduleCore = null;
        WorkbenchComponent wbmodule = null;
        try {
            moduleCore = ModuleCore.getModuleCoreForRead(getProject());
            wbmodule = moduleCore.findWorkbenchModuleByDeployName(operationDataModel.getStringProperty(WebComponentCreationDataModel.MODULE_DEPLOY_NAME));
        } finally {
            if (null != moduleCore) {
                moduleCore.dispose();
            }
        }		


        WebArtifactEdit webEdit = null;
       	try{

       		webEdit = WebArtifactEdit.getWebArtifactEditForWrite( wbmodule );
       		String projPath = getProject().getLocation().toOSString();
       		//projPath += this.getDeploymentDescriptorFolder() + IPath.SEPARATOR + J2EEConstants.WEBAPP_DD_SHORT_NAME;       		

       		
       		projPath += operationDataModel.getProperty( WebComponentCreationDataModel.DD_FOLDER );
       		projPath +=IPath.SEPARATOR + J2EEConstants.WEBAPP_DD_SHORT_NAME;
//       		projPath += IPath.SEPARATOR + moduleName + IPath.SEPARATOR + "WebContent" + IPath.SEPARATOR + J2EEConstants.WEB_INF + IPath.SEPARATOR + J2EEConstants.WEBAPP_DD_SHORT_NAME;

       		

       		
       		IPath webxmlPath = new Path(projPath);
       		boolean b = webxmlPath.isValidPath(webxmlPath.toString());
       		if(webEdit != null) {
       			int moduleVersion = operationDataModel.getIntProperty(WebComponentCreationDataModel.J2EE_MODULE_VERSION);
  			
           		webEdit.createModelRoot( getProject(), webxmlPath, moduleVersion );
       		}
       	}
       	catch(Exception e){
       		Logger.getLogger().logError(e);
       	} finally {
       		if(webEdit != null)
       			webEdit.dispose();
       	}					
	
	
	
	}

	protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
		
		super.execute( IModuleConstants.JST_WEB_MODULE, monitor );
	}
	
	protected  void addResources( WorkbenchComponent component ){
		addResource(component, getModuleRelativeFile(getWebContentSourcePath(), getProject()), getWebContentDeployPath());
		addResource(component, getModuleRelativeFile(getJavaSourceSourcePath(), getProject()), getJavaSourceDeployPath());		
	}
	
	/**
	 * @return
	 */
	public String getJavaSourceSourcePath() {
		return "/" + getModuleName() +"/JavaSource"; //$NON-NLS-1$
	}
	
	/**
	 * @return
	 */
	public String getJavaSourceDeployPath() {
		return "/WEB-INF/classes"; //$NON-NLS-1$
	}
	
	/**
	 * @return
	 */
	public String getWebContentSourcePath() {
		return "/" + getModuleName() + "/WebContent"; //$NON-NLS-1$
	}
	
	/**
	 * @return
	 */
	public String getWebContentDeployPath() {
		return "/"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jst.j2ee.application.operations.FlexibleJ2EEModuleCreationOperation#createProjectStructure()
	 */
	protected void createProjectStructure() throws CoreException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jst.j2ee.application.operations.J2EEComponentCreationOperation#getVersion()
	 */
	protected String getVersion() {
		int version = operationDataModel.getIntProperty(J2EEComponentCreationDataModel.J2EE_MODULE_VERSION);
		return J2EEVersionUtil.getServletTextVersion(version);

	}
}