/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jst.j2ee.internal.web.operations;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jem.util.UIContextDetermination;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.jem.util.logger.proxy.Logger;
import org.eclipse.jst.j2ee.application.internal.operations.IAnnotationsDataModel;
import org.eclipse.jst.j2ee.common.CommonFactory;
import org.eclipse.jst.j2ee.common.Listener;
import org.eclipse.jst.j2ee.internal.common.operations.INewJavaClassDataModelProperties;
import org.eclipse.jst.j2ee.model.IModelProvider;
import org.eclipse.jst.j2ee.model.ModelProviderManager;
import org.eclipse.jst.j2ee.webapplication.WebApp;
import org.eclipse.jst.javaee.core.JavaeeFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wst.common.componentcore.internal.operation.ArtifactEditProviderOperation;
import org.eclipse.wst.common.componentcore.internal.operation.IArtifactEditOperationDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.AbstractDataModelOperation;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

/**
 * This class, AddListenerOperation is a IDataModelOperation following the IDataModel wizard and
 * operation framework.
 * @see org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation
 * @see org.eclipse.wst.common.frameworks.datamodel.IDataModelProvider
 * 
 * This operation subclasses the ArtifactEditProviderOperation so the changes made to the deployment descriptor
 * models are saved to the artifact edit model.
 * @see org.eclipse.wst.common.componentcore.internal.operation.ArtifactEditProviderOperation
 * 
 * It is the operation which should be used when adding a new application lifecycle listener to
 * a web app, whether that be an annotated listener or a non annotated listener.  This uses the
 * NewListenerClassDataModelProvider to retrieve properties set by the user in order to create the custom
 * listener class.
 * @see org.eclipse.jst.j2ee.internal.web.operations.NewListenerClassDataModelProvider
 * 
 * In the non annotated case, this operation will add the metadata necessary into the web deployment
 * descriptor. In the annotated case, it will not, it will leave this up to the parsing of the
 * annotations to build the deployment descriptor artifacts. To actually create the java class for
 * the listener, the operation uses the NewListenerClassOperation. The NewListenerClassOperation 
 * shares the same data model provider.
 * @see org.eclipse.jst.j2ee.internal.web.operations.NewListenerClassOperation
 * 
 * Clients may subclass this operation to provide their own behavior on listener creation. The execute
 * method can be extended to do so. Also, generateListenerMetaData and creteListenerClass are exposed.
 * 
 * The use of this class is EXPERIMENTAL and is subject to substantial changes.
 */
public class AddListenerOperation extends AbstractDataModelOperation implements IArtifactEditOperationDataModelProperties{
	
	private IModelProvider provider = null;
	/**
	 * This is the constructor which should be used when creating the operation.
	 * It will not accept null parameter.  It will not return null.
	 * @see ArtifactEditProviderOperation#ArtifactEditProviderOperation(IDataModel)
	 * 
	 * @param dataModel 
	 * @return AddListenerOperation
	 */
	public AddListenerOperation(IDataModel dataModel) {
		super(dataModel);
		provider = ModelProviderManager.getModelProvider( getTargetProject() );
	}

	/**
	 * Subclasses may extend this method to add their own actions during execution.
	 * The implementation of the execute method drives the running of the operation. This
	 * implementation will create the listener class, and then if the listener is not
	 * annotated, it will create the listener metadata for the web deployment descriptor.
	 * This method will accept null as a parameter.
	 * @see org.eclipse.core.commands.operations.AbstractOperation#execute(org.eclipse.core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 * @see AddListenerOperation#createListenerClass()
	 * @see AddListenerOperation#generateListenerMetaData(IDataModel, String)
	 * 
	 * @param monitor IProgressMonitor
	 * @param info IAdaptable
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
	public IStatus doExecute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		//Retrieve values set in the NewListenerClassDataModel
		boolean useExisting = model.getBooleanProperty(INewListenerClassDataModelProperties.USE_EXISTING_CLASS);
		String qualifiedClassName = model.getStringProperty(INewJavaClassDataModelProperties.CLASS_NAME);
		
		if (!useExisting)
			qualifiedClassName = createListenerClass();

		// If the listener is not annotated, generate the listener metadata for the DD
		if (!model.getBooleanProperty(IAnnotationsDataModel.USE_ANNOTATIONS))
			generateListenerMetaData(model, qualifiedClassName);
		
		return OK_STATUS;
	}
	
	/**
	 * Subclasses may extend this method to add their own creation of the actual listener java class.
	 * This implementation uses the NewListenerClassOperation which is a subclass of the NewJavaClassOperation.
	 * The NewListenerClassOperation will use the same NewListenerClassDataModelProvider to retrieve the properties in
	 * order to create the java class accordingly.  This method will not return null.
	 * @see NewListenerClassOperation
	 * @see org.eclipse.jst.j2ee.internal.common.operations.NewJavaClassOperation
	 * @see NewListenerClassDataModelProvider
	 * 
	 * @return String qualified listener class name
	 */
	protected String createListenerClass() {
		//	Create listener java class file using the NewListenerClassOperation. The same data model is shared.
		NewListenerClassOperation op = new NewListenerClassOperation(model);
		try {
			op.execute(new NullProgressMonitor(), null);
		} catch (Exception e) {
			Logger.getLogger().log(e);
		} 
		// Return the qualified class name of the newly created java class for the listener
		return getQualifiedClassName();
	}
	
	/**
	 * This method will return the qualified java class name as specified by the class name
	 * and package name properties in the data model.
	 * This method should not return null.
	 * @see INewJavaClassDataModelProperties#CLASS_NAME
	 * @see INewJavaClassDataModelProperties#JAVA_PACKAGE
	 * 
	 * @return String qualified java class name
	 */
	public final String getQualifiedClassName() {
		// Use the java package name and unqualified class name to create a qualified java class name
		String packageName = model.getStringProperty(INewJavaClassDataModelProperties.JAVA_PACKAGE);
		String className = model.getStringProperty(INewJavaClassDataModelProperties.CLASS_NAME);
		//Ensure the class is not in the default package before adding package name to qualified name
		if (packageName != null && packageName.trim().length() > 0)
			return packageName + "." + className; //$NON-NLS-1$
		return className;
	}

	/**
	 * Subclasses may extend this method to add their own generation steps for the creation of the
	 * metadata for the web deployment descriptor.  This implementation uses the J2EE models to create
	 * the Listener model instance. It then adds these to the web application model. This will then be 
	 * written out to the deployment descriptor file. This method does not accept null parameters.
	 * @see Listener
	 * @see AddListenerOperation#createListener(String)
	 * 
	 * @param aModel
	 * @param qualifiedClassName
	 */
	protected void generateListenerMetaData(IDataModel aModel, String qualifiedClassName) {
		// Set up the listener modeled object
		createListener(qualifiedClassName);
	}
	
	/**
	 * This method is intended for private use only. This method is used to create the listener
	 * modeled object, to set any parameters specified in the data model, and then to add the
	 * listener instance to the web application model. This method does not accept null parameters.
	 * It will not return null.
	 * @see AddListenerOperation#generateListenerMetaData(IDataModel, String)
	 * @see CommonFactory#createListener()
	 * @see Listener
	 * 
	 * @param qualifiedClassName
	 * @return Listener instance
	 */
	private Object createListener(String qualifiedClassName) {
		Object modelObject = provider.getModelObject();
		if (modelObject instanceof org.eclipse.jst.j2ee.webapplication.WebApp ){
			// Create the listener instance and set up the parameters from data model
			Listener listener = CommonFactory.eINSTANCE.createListener();
			listener.setListenerClassName(qualifiedClassName);

			// Add the listener to the web application model
			WebApp webApp = (WebApp) modelObject;
			webApp.getListeners().add(listener);
			return listener;
		} else if (modelObject instanceof org.eclipse.jst.javaee.web.WebApp ){
			// Create the listener instance and set up the parameters from data model
			org.eclipse.jst.javaee.core.Listener listener = JavaeeFactory.eINSTANCE.createListener();
			listener.setListenerClass(qualifiedClassName);

			// Add the listener to the web application model
			org.eclipse.jst.javaee.web.WebApp webApp = (org.eclipse.jst.javaee.web.WebApp) modelObject;
			webApp.getListeners().add(listener);	
			return listener;
		}
		
		return null;
	}
	
	public IProject getTargetProject() {
		String projectName = model.getStringProperty(IArtifactEditOperationDataModelProperties.PROJECT_NAME);
		return ProjectUtilities.getProject(projectName);
	}
	
	@Override
	public IStatus execute(final IProgressMonitor monitor, final IAdaptable info) throws ExecutionException {
		Runnable runnable = null;
		try {
			Object ctx = null;
			if( UIContextDetermination.getCurrentContext() == UIContextDetermination.UI_CONTEXT ){
				Display display = Display.getCurrent();
				if(display != null )
					ctx = display.getActiveShell();
			}

			if (provider.validateEdit(null,ctx).isOK()){
				runnable = new Runnable(){

					public void run() {
						try {
							doExecute(monitor, info);
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
					}
				};
				provider.modify(runnable, IModelProvider.FORCESAVE);
			}
			//return doExecute(monitor, info);
			return Status.CANCEL_STATUS;
		} finally {

		}		
	}
}