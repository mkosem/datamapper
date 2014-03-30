package org.mkosem.datamapper;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.beanutils.MethodUtils;
import org.mkosem.datamapper.testobjects.IDataModelObject;

public class DataMapper {
	private static final ConcurrentHashMap<Class<?>, DataMapping> classMappings = new ConcurrentHashMap<Class<?>, DataMapping>(16, 1F, 1);
	private static final String SET_PREFIX = "set";


	
	protected DataMapping getMapping(Class<?> argModelClass, String[] argMethodNames, Object[] argMethodParameters) {
		DataMapping mapping = classMappings.get(argModelClass);
		
		// If no mapping was saved, create and add a new one.  Since there is a high likelihood for thread contention here, use putIfAbsent and return existing value if some other thread added a mapping after the initial check was made.
		if (mapping == null) {
			DataMapping newMapping = new DataMapping(argModelClass);
			DataMapping existingMapping = classMappings.putIfAbsent(argModelClass, newMapping);
			mapping = existingMapping != null ? existingMapping : newMapping;
		}
		
		return mapping;
	}
	
	protected Method getMethodForClass(Class<?> argModelClass, String argFieldName, Object argMethodParameter) throws Exception {
		// prepare local variables that will be used while locating methods
		String methodName =  SET_PREFIX + argFieldName;
	    Object methodParameter = argMethodParameter;
	    Class<?> methodParameterClass = methodParameter.getClass();
	    
	    // try to find an exact match first
	    Method modelMethod = getMethodForNameAndType(argModelClass, methodName, methodParameterClass);	;
		
		// if the parameter is not boxed, try the primitive equivalent
		if (modelMethod == null && hasPrimitiveEquivalent(methodParameterClass)) {	
			modelMethod = getMethodForNameAndType(argModelClass, methodName, MethodUtils.getPrimitiveType(methodParameterClass));
		}
		
		// if the parameter is an array of non-boxed values, try the primitive array equivalent
		if (modelMethod == null && methodParameterClass.isArray() && hasPrimitiveEquivalent(methodParameterClass.getComponentType())) {	
			modelMethod = getMethodForNameAndType(argModelClass, methodName, Array.newInstance(MethodUtils.getPrimitiveType(methodParameterClass.getComponentType()), 0).getClass());
		}
		
		if (modelMethod == null) {
			throw new Exception("Could not find a method for " + argModelClass.getName() + " with name of " + methodName);
		}
		
		return modelMethod;
	}
	
	private Method getMethodForNameAndType(Class<?> argClass, String argMethodName, Class<?> argMethodParameter) {
		try {
			return argClass.getDeclaredMethod(argMethodName, argMethodParameter);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
	
	private boolean hasPrimitiveEquivalent(Class<?> argClass) {
		if (argClass.equals(Boolean.class) || argClass.equals(Byte.class) || argClass.equals(Character.class) || argClass.equals(Double.class) || argClass.equals(Float.class) || argClass.equals(Integer.class) || argClass.equals(Long.class) || argClass.equals(Short.class)) {
			return true;
		}
		return false;
	}
	
	protected List<IDataModelObject> mapValuesToObject(Class<?> argModelClass, String[] argFieldNames, Object[][] argMethodParameters) {
		DataMapping mapping = getMapping(argModelClass, argFieldNames, argMethodParameters);
		IDataModelObject[] returnModels = (IDataModelObject[]) Array.newInstance(argModelClass, argMethodParameters.length);
		for (int recordIndex = 0 ; recordIndex < argMethodParameters.length ; recordIndex++) {
			Object modelClass;
			try {
				modelClass = argModelClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Could not create an instance of " + argModelClass.getName(), e);
			}
			Object[] argObjectParameters = argMethodParameters[recordIndex];
			for (int index = 0 ; index < argFieldNames.length ; index++) {
				String fieldName = argFieldNames[index];
				Object methodParameter = argObjectParameters[index];
				Class<?> methodParameterClass = methodParameter.getClass();
				try {
					Method setterMethod = mapping.getSetterForField(fieldName, methodParameter);
					if (setterMethod.getParameterTypes()[0].equals(methodParameterClass) || setterMethod.getParameterTypes()[0].equals(MethodUtils.getPrimitiveType(methodParameterClass)) || setterMethod.getParameterTypes()[0].equals(MethodUtils.getPrimitiveWrapper(methodParameterClass))) {
						// if we have a direct match, or a match that can be autoboxed/autounboxed, invoke it
						setterMethod.invoke(modelClass, methodParameter);
					} else if (methodParameterClass.isArray() && MethodUtils.getPrimitiveType(methodParameterClass.getComponentType()) != null && setterMethod.getParameterTypes()[0].equals(Array.newInstance(MethodUtils.getPrimitiveType(methodParameterClass.getComponentType()), 0).getClass())) {
						// if we have a boxed array, but need a primitive, unbox and invoke
						int arrayLength = Array.getLength(methodParameter);
						Object unboxedArray = Array.newInstance(MethodUtils.getPrimitiveType(methodParameterClass.getComponentType()), arrayLength);
						for (int arrayIndex = 0 ; arrayIndex < arrayLength ; arrayIndex++) {
							Array.set(unboxedArray, arrayIndex, Array.get(methodParameter, arrayIndex));
						}
						setterMethod.invoke(modelClass, unboxedArray);
					} else if (methodParameterClass.isArray() && MethodUtils.getPrimitiveWrapper(methodParameterClass.getComponentType()) != null && setterMethod.getParameterTypes()[0].equals(Array.newInstance(MethodUtils.getPrimitiveWrapper(methodParameterClass.getComponentType()), 0).getClass())) {
						// if we have a primitive array, but need a boxed, box and invoke
						int arrayLength = Array.getLength(methodParameter);
						Object unboxedArray = Array.newInstance(MethodUtils.getPrimitiveWrapper(methodParameterClass.getComponentType()), arrayLength);
						for (int arrayIndex = 0 ; arrayIndex < arrayLength ; arrayIndex++) {
							Array.set(unboxedArray, arrayIndex, Array.get(methodParameter, arrayIndex));
						}
						setterMethod.invoke(modelClass, unboxedArray);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			returnModels[recordIndex] = (IDataModelObject) modelClass;
		}
		return Arrays.asList(returnModels);
	}
	


	class DataMapping {
		private final Class<?> modelClass;
		private final Map<String,Method> fieldMappings = new HashMap<String, Method>();
		
		DataMapping(Class<?> argModelClass) {
			modelClass = argModelClass;
		}
		
		Method getSetterForField(String argFieldName, Object argParameter) {
			Method mappingMethod = fieldMappings.get(argFieldName);
			if (mappingMethod == null) {
				try {
					mappingMethod = getMethodForClass(modelClass, argFieldName, argParameter);
					synchronized (this) {
						fieldMappings.put(argFieldName, mappingMethod);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return mappingMethod;
		}
	}
}
