package org.eclipse.jem.internal.proxy.ide;
/*******************************************************************************
 * Copyright (c)  2001, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $RCSfile: IDEMethodTypeProxy.java,v $
 *  $Revision: 1.2 $  $Date: 2004/02/03 23:18:36 $ 
 */


/**
 * @version 	1.0
 * @author
 */
public class IDEMethodTypeProxy extends IDEBeanTypeProxy {
	
	IDEMethodTypeProxy(IDEProxyFactoryRegistry registry) {
		super(registry, java.lang.reflect.Method.class);
	}

	/*
	 * @see IDEBeanTypeProxy#newBeanProxy(Object)
	 */
	protected IIDEBeanProxy newBeanProxy(Object anObject) {
		return new IDEMethodProxy(fProxyFactoryRegistry, (java.lang.reflect.Method) anObject);
	}

}
