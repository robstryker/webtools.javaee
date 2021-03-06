/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jem.internal.java.adapters;
/*


 */
import java.util.*;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;

import org.eclipse.jem.java.*;
import org.eclipse.jem.java.adapters.IJavaReflectionKey;
import org.eclipse.jem.java.adapters.IJavaReflectionKeyExtension;
import org.eclipse.jem.util.CharacterUtil;
import org.eclipse.jem.util.CharacterUtil.StringIterator;

/**
 * This key handles Java Reflection. It creates the appropriate type of entries if
 * they are not already in the resource. There is an extension mechanism for other
 * types of ids that are not recognized by this key. They are registered with the
 * JavaXMIFactory that is used to create these keys, and the factory will pass in
 * a List of these registered extension processers (IJavaReflectionKeyExtension).
 * 
 * Creation date: (10/4/2000 8:24:36 AM)
 * @author: Administrator
 */
public class JavaReflectionKey implements IJavaReflectionKey {
	private static final Collection PRIMITIVES = new ArrayList(8);

	static { initializePrimitivesCollection(); }
	
	protected JavaXMIFactoryImpl.JavaXMIResource resource; //FB
	protected List extensions;

public JavaReflectionKey(List extensions, JavaXMIFactoryImpl.JavaXMIResource resource) {
  this.extensions = extensions;
  this.resource = resource;
}
//FB END

/* 
 * Create an ArrayType instance in the current document
 * Handles the push and pop of the current document.
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#createArrayType()
 */
public ArrayType createArrayType() {
	return getJavaFactory().createArrayType();
}
/* 
 * Create a JavaClass instance in the current document
 * Handles the push and pop of the current document.
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#createJavaClass()
 */
public JavaClass createJavaClass() {
	return getJavaFactory().createJavaClass();
}
/**
 * Create a data type and put it in the primitives resource
 */
protected EClassifier getJavaDataType(String typeName) {
	JavaDataType result = getJavaFactory().createJavaDataType();
	result.setName(typeName);
	// ((InternalEObject)result).eSetID(typeName);
	//result.eSetUUID((UUID) null);
	//result.setEPackage(getPackage());
	getPackage().getEClassifiers().add(result);
	resource.setID(result, typeName);	
	return result;
}
/* 
 * Create a Field instance in the current document
 * Handles the push and pop of the current document.
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#createJavaField()
 */
public Field createJavaField() {
	return getJavaFactory().createField();
}
/* 
 * Create a Method instance in the current document
 * Handles the push and pop of the current document.
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#createJavaMethod()
 */
public Method createJavaMethod() {
	return getJavaFactory().createMethod();
}
/* 
 * Create a JavaParameter instance in the current document
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#createJavaParameter()
 */
public JavaParameter createJavaParameter() {
	return getJavaFactory().createJavaParameter();
}
//FB/**
//FB * Override to try to introspect objects
//FB * that aren't found using the default mechanism.
//FB */
//FBpublic Object get(Object key) {
//FB 	Object javaObject = super.get(key);
//FB 	if (javaObject == null) {
//FB 		javaObject = getObjectFromExtensions((String) key);
//FB 		if (javaObject == null)
//FB 			javaObject = getJavaObject((String) key);
//FB 	}
//FB	return javaObject;
//FB}

//FB ADDED
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#get(java.lang.String)
 */
public Object get(String key) {
	Object javaObject = getObjectFromExtensions(key);
	if (javaObject == null)
		javaObject = getJavaObject(key);
	return javaObject;
}
//FB END

/*
 * Instantiate the named class.
 * If we make it to this point, the class has not yet been instantiated or reflected.
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#getArrayType(java.lang.String)
 */
public EClassifier getArrayType(String typeName) {
	ArrayType arrayType = this.createArrayType();
	arrayType.setName(typeName);
	// ((InternalEObject)arrayType).eSetID(typeName);
	//arrayType.eSetUUID((UUID) null);
	int pos = -1, braces = 0;
	do {
		pos = typeName.indexOf("[", pos+1);//$NON-NLS-1$
		if (pos != -1)
			braces++;
	} while (pos != -1);
	arrayType.setArrayDimensions(braces);
	//arrayType.setEPackage(getPackage());
	getPackage().getEClassifiers().add(arrayType);
	resource.setID(arrayType, typeName);	
	return arrayType;
}
/*
 * Instantiate the named class.
 * If we make it to this point, teh class has not yet been instantiated or reflected.
 */
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#getJavaClass(java.lang.String)
 */
public EClassifier getJavaClass(String typeName) {
	JavaClass javaClass = this.createJavaClass();
	javaClass.setName(typeName);
	// ((InternalEObject)javaClass).eSetID(javaClass.getName());
	//javaClass.eSetUUID((UUID) null);
	// javaClass.setEPackage(getPackage());
	getPackage().getEClassifiers().add(javaClass);
	resource.setID(javaClass, typeName);
	return javaClass;
}
/* Get the Java Factory
 */
protected static JavaRefFactory getJavaFactory() {
	return JavaRefFactory.eINSTANCE;
}
/**
 * This allows for the direct retrieval of fields with the following notation:
 * field: java:/com.ibm.foo#Test.foo
 * 
 */
protected Field getJavaField(String keyValue) {
	Field result = null;
	String className, memberName;
	int memberDelimiter = keyValue.indexOf(ReflectionAdaptor.C_CLASS_MEMBER_DELIMITER);
	className = keyValue.substring(0, memberDelimiter);
	memberName = keyValue.substring(memberDelimiter + 1);
	EClassifier javaClass = (EClassifier) primGet(className);
	// if we haven't already instantiated the containing class, then go do it.
	if (javaClass == null)
		javaClass = getJavaType(className);
	if ((javaClass != null) && !((JavaHelpers) javaClass).isPrimitive()) {
		// We have a class. Now we need to force reflection, this will cause
		// the JavaReflectionKey to be updated and have the member added to the
		// list.
		((JavaClass) javaClass).getFields();
		//FB result = (Field) super.get(keyValue); // It should now be registered.
		result = (Field) primGet(keyValue); // It should now be registered.
		
		if (result == null) {
			Field tempField = createJavaField();
			tempField.setName(memberName);
			// ((InternalEObject)tempField).eSetID(keyValue);
			//tempField.eSetUUID((UUID) null);
			((JavaClass) javaClass).getFields().add(tempField);
			result = tempField;
		}
	}
	return result;
}
/**
 * This allows for the retrieval of methods with the following notation:
 * method: java:/com.ibm.foo#Test.foofoo(
 * 
 */
protected Method getJavaMethod(String keyValue) {
	Method result = null;
	String className, memberName;
	int memberDelimiter = keyValue.indexOf(ReflectionAdaptor.C_CLASS_MEMBER_DELIMITER);
	className = keyValue.substring(0, memberDelimiter);
	int parmDelimiter = keyValue.indexOf(ReflectionAdaptor.C_METHOD_PARM_DELIMITER, memberDelimiter);
	memberName = keyValue.substring(memberDelimiter + 1, parmDelimiter);
	EClassifier javaClass = (EClassifier) primGet(className);
	// if we haven't already instantiated the containing class, then go do it.
	if (javaClass == null)
		javaClass = getJavaType(className);
	if ((javaClass != null) && !((JavaHelpers) javaClass).isPrimitive()) {
		// We have a class. Now we need to force reflection, this will cause
		// the JavaReflectionKey to be updated and have the member added to the
		// list.
		((JavaClass) javaClass).getMethods();
		result = (Method) primGet(keyValue); // It should now be registered.
		
		if (result == null) {
			Method tempMethod = createJavaMethod();
			tempMethod.setName(memberName);
			resource.setID(tempMethod, keyValue);
			((JavaClass) javaClass).getMethods().add(tempMethod);
			result = tempMethod;
		}
	}
	return result;
}
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#getJavaObject(java.lang.String)
 */
public EObject getJavaObject(String keyValue) {
	EObject result = null;
	int index = keyValue.indexOf(ReflectionAdaptor.C_CLASS_MEMBER_DELIMITER);
	if (index > 0) {
		// The reference is to a member: field, method, or parameter.
		// Take the most specific case first, a parameter.
		// Only a parameter should contain the methodID to parmID delimiter.
		boolean isParameter = (keyValue.indexOf(ReflectionAdaptor.C_METHODID_PARMID_DELIMITER, index) != -1);
		if (isParameter) {
			result = getJavaParameter(keyValue);
		} else {
			// The target is a field or method. If it has a "(", then it is a method.
			// If it is not found, then we will create a temporary object.
			int parmStart = keyValue.indexOf(ReflectionAdaptor.C_METHOD_PARM_DELIMITER, index);
			boolean isField = parmStart == -1;
			if (isField) {
				result = getJavaField(keyValue);
			} else {
				result = getJavaMethod(keyValue);
			}
		}
	} else {
		result = getJavaType(keyValue);
	}
	return result;
}

/**
 * See if the extensions can resolve this first.
 */
protected Object getObjectFromExtensions(String keyValue) {
	if (extensions != null) {
		for (Iterator iter = extensions.iterator(); iter.hasNext();) {
			IJavaReflectionKeyExtension extension = (IJavaReflectionKeyExtension) iter.next();
			Object obj = extension.getObject(keyValue, this);
			if (obj != null)
				return obj;
		}
	}

	return null;
}
/**
 * This allows for the retrieval of parameters with the following notation:
 * parameter: java:/com.ibm.foo#Test.foofoo(-arg0
 * 
 */
protected JavaParameter getJavaParameter(String keyValue) {
	JavaParameter result = null;
	String parmName;
	int memberDelimiter = keyValue.indexOf(ReflectionAdaptor.C_CLASS_MEMBER_DELIMITER);
	int parmDelimiter = keyValue.indexOf(ReflectionAdaptor.C_METHODID_PARMID_DELIMITER, memberDelimiter);
	parmName = keyValue.substring(parmDelimiter + 1);
	// getMethod() will find or create a method
	Method method = this.getJavaMethod(keyValue.substring(parmDelimiter));
	if (method != null) {
		result = method.getParameter(parmName);
		if (result == null) {
			JavaParameter tempParameter = createJavaParameter();
			tempParameter.setName(parmName);
			// ((InternalEObject)tempParameter).eSetID(keyValue);
			// tempParameter.eSetUUID((UUID) null);
			method.getParameters().add(tempParameter);
			result = tempParameter;
		}
	}
	return result;
}
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#getJavaType(java.lang.String)
 */
public EClassifier getJavaType(String typeName) {
	if (isPrimitive(typeName))
		return getJavaDataType(typeName);
	else if (isArray(typeName))
		return getArrayType(typeName);
	else if (isValidJavaIdentifier(typeName))
		return getJavaClass(typeName);
	return null;
}

protected boolean isValidJavaIdentifier(String typeName) {
	if (typeName.length() > 0) {
		StringIterator charIter = new StringIterator(typeName);
		if (CharacterUtil.isJavaIdentifierStart(charIter.next())) {
			// Starts with a valid java identifier character.
			// Now see if the rest are valid.
			while (charIter.hasNext()) {
				if (!CharacterUtil.isJavaIdentifierPart(charIter.next()))
					return false; // Not valid
			}
			return true;
		}
	}
	return false;
}

/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#primGet(java.lang.String)
 */
public Object primGet(String key) {
	return resource.primGetEObject(key);
}
protected EPackage getPackage() {
	//FB return (EPackage) super.get(JavaPackage.PACKAGE_ID);
	// return (EPackage) resource.getEObject(JavaPackage.PACKAGE_ID); //FB
	//return (EPackage) resource.getEObject(JavaPackage.PACKAGE_ID); //FB
	return (EPackage)EcoreUtil.getObjectByType(resource.getContents(), EcorePackage.eINSTANCE.getEPackage());
}
/**
 * Insert the method's description here.
 * Creation date: (10/4/2000 9:56:53 AM)
 */
private static void initializePrimitivesCollection() {
	PRIMITIVES.add(N_VOID);
	PRIMITIVES.add(N_BOOLEAN);
	PRIMITIVES.add(N_BYTE);
	PRIMITIVES.add(N_CHAR);
	PRIMITIVES.add(N_DOUBLE);
	PRIMITIVES.add(N_FLOAT);
	PRIMITIVES.add(N_INT);
	PRIMITIVES.add(N_LONG);
	PRIMITIVES.add(N_SHORT);
}
/* (non-Javadoc)
 * @see org.eclipse.jem.internal.java.adapters.IJavaReflectionKey#isArray(java.lang.String)
 */
public boolean isArray(String typeName) {
	return typeName.endsWith("[]");//$NON-NLS-1$
}
/**
 * Insert the method's description here.
 * Creation date: (10/4/2000 9:54:23 AM)
 * @return boolean
 * @param typeName java.lang.String
 */
private boolean isPrimitive(String typeName) {
	return PRIMITIVES.contains(typeName);
}
}




