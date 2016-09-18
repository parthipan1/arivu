package org.arivu.data;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
//import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.arivu.datastructure.Amap;
import org.arivu.datastructure.DoublyLinkedList;
import org.arivu.utils.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author P
 *
 * @param <T>
 * @param <R>
 */
public final class RowMapper<T extends Annotation, R> {
	/**
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(RowMapper.class);
	
	public static interface Identifier{
		String getIdentity(Field field);
	}

	/**
	 * 
	 */
	static final Map<Class<?>,Map<Class<?>, Map<Field, Method>>> allGetterMethodFieldMaps = new Amap<Class<?>,Map<Class<?>, Map<Field, Method>>>();
	/**
	 * 
	 */
	static final Map<Class<?>,Map<Class<?>, Map<Field, Method>>> allSetterMethodFieldMaps = new Amap<Class<?>,Map<Class<?>, Map<Field, Method>>>();

	/**
	 * 
	 */
	private static final Object[] objectsInputParams = new Object[] {};

	/**
	 * @param valueObject
	 * @param annotationClass
	 * @param identifier
	 * @param returnClass
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public R set(final Map<String, Object> valueObject, final Class<T> annotationClass, final Identifier identifier, final Class<R> returnClass)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InstantiationException{
		return set(valueObject, annotationClass, true, identifier, returnClass);
	}
	
	/**
	 * @param valueObject
	 * @param annotationClass
	 * @param useCache
	 * @param identifier
	 * @param returnClass
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public R set(final Map<String, Object> valueObject, final Class<T> annotationClass, final boolean useCache, final Identifier identifier, final Class<R> returnClass)
			throws IllegalAccessException, InvocationTargetException, InstantiationException {
		final R values = returnClass.newInstance();
		if (valueObject!=null) {
			Class<?> klass = returnClass;
			while (klass != null && klass!=Object.class) {
				setReflectionValues(valueObject, values, klass, annotationClass, useCache, identifier);
				klass = klass.getSuperclass();
			} 
		}
		return values;
	}
	
	/**
	 * @param valueObject
	 * @param values
	 * @param returnClass
	 * @param annotationClass
	 * @param useCache
	 * @param identifier
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private void setReflectionValues(final Map<String, Object> valueObject, final R values,
			final Class<?> returnClass, final Class<T> annotationClass, final boolean useCache, final Identifier identifier)
					throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (useCache) {
			Map<Class<?>, Map<Field, Method>> map3 = allSetterMethodFieldMaps.get(annotationClass);
			final Map<Class<?>, Map<Field, Method>> map2 = ((map3 == null) ? new Amap<Class<?>, Map<Field, Method>>() : map3);
			Map<Field, Method> map = map2.get(returnClass);
			final Map<Field, Method> methodToFieldMap = ((map == null) ? new Amap<Field, Method>() : map);
			if (NullCheck.isNullOrEmpty(methodToFieldMap)) {
				final Collection<Field> declaredFields = new DoublyLinkedList<Field>(Arrays.asList(returnClass.getDeclaredFields()));
				final Collection<Method> declaredMethods = new DoublyLinkedList<Method>(Arrays.asList(returnClass.getDeclaredMethods()));
				for (final Field field : declaredFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						logger.debug(getClass().getName(), "field:" + field.getName() + " static field ignored!");
					} else {
						final T annotation = field.getAnnotation(annotationClass);
						if (annotation != null) {
							setterMethod(valueObject, values, identifier, methodToFieldMap, declaredMethods, field);
						}else{
							final String methodName = "set" + field.getName();
							for (final Method m : declaredMethods) {
								if (m.getName().equalsIgnoreCase(methodName)) {
									if (methodToFieldMap!=null) {
										methodToFieldMap.put(field, m);
									}
									break;
								}
							}
						}
					}
				}
				map2.put(returnClass, Collections.unmodifiableMap(methodToFieldMap));
				allSetterMethodFieldMaps.put(annotationClass, map2);
			} else {
				for (Entry<Field, Method> entry : methodToFieldMap.entrySet()) {
					final T annotation = entry.getKey().getAnnotation(annotationClass);
					if (annotation != null) {
						String identity = identifier.getIdentity(entry.getKey());
						entry.getValue().invoke(values, new Object[]{valueObject.get(identity)});
					}
				}
			} 
		}else{
			final Collection<Field> declaredFields = new DoublyLinkedList<Field>(Arrays.asList(returnClass.getDeclaredFields()));
			final Collection<Method> declaredMethods = new DoublyLinkedList<Method>(Arrays.asList(returnClass.getDeclaredMethods()));
			for (final Field field : declaredFields) {
				final T annotation = field.getAnnotation(annotationClass);
				if (annotation != null) {
					setterMethod(valueObject, values, identifier, null, declaredMethods, field);
				}
			}
		}
	}

	/**
	 * @param valueObject
	 * @param values
	 * @param identifier
	 * @param methodToFieldMap
	 * @param declaredMethods
	 * @param field
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void setterMethod(final Map<String, Object> valueObject, final R values, final Identifier identifier,
			final Map<Field, Method> methodToFieldMap, final Collection<Method> declaredMethods, final Field field)
					throws IllegalAccessException, InvocationTargetException {
		String identity = identifier.getIdentity(field);
		Object object = valueObject.get(identity);
		final String methodName = "set" + field.getName();
		boolean methodDeclr = false;
		for (final Method m : declaredMethods) {
			if (m.getName().equalsIgnoreCase(methodName)) {
				m.invoke(values, new Object[]{object});
				if (methodToFieldMap!=null) {
					methodToFieldMap.put(field, m);
				}
				methodDeclr = true;
				break;
			}
		}
		if (!methodDeclr)
			throw new IllegalStateException("No public setter method declared for field " + field.getName()
					+ " in class " + valueObject.getClass());
	}
	
	/**
	 * @param valueObject
	 * @param annotationClass
	 * @param identifier
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Map<String, Object> get(final R valueObject, final Class<T> annotationClass, final Identifier identifier)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		return get(valueObject, annotationClass, true, identifier);
	}
	
	/**
	 * @param valueObject
	 * @param annotationClass
	 * @param useCache
	 * @param identifier
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public Map<String, Object> get(final R valueObject, final Class<T> annotationClass, final boolean useCache, final Identifier identifier)
			throws IllegalAccessException, InvocationTargetException {
		final Map<String, Object> values = new Amap<String, Object>();
		if (valueObject!=null) {
			Class<?> klass = valueObject.getClass();
			while (klass != null && klass!=Object.class) {
				getReflectionValues(valueObject, values, klass, annotationClass, useCache, identifier);
				klass = klass.getSuperclass();
			} 
		}
		return values;
	}

	/**
	 * @param valueObject
	 * @param values
	 * @param klass
	 * @param annotationClass
	 * @param useCache
	 * @param identifier
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private void getReflectionValues(final R valueObject, final Map<String, Object> values,
			final Class<?> klass, final Class<T> annotationClass, final boolean useCache, final Identifier identifier) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		if (useCache) {
			Map<Class<?>, Map<Field, Method>> map3 = allGetterMethodFieldMaps.get(annotationClass);
			final Map<Class<?>, Map<Field, Method>> map2 = ((map3 == null) ? new Amap<Class<?>, Map<Field, Method>>() : map3);
			Map<Field, Method> map = map2.get(klass);
			final Map<Field, Method> methodToFieldMap = ((map == null) ? new Amap<Field, Method>() : map);
			if (NullCheck.isNullOrEmpty(methodToFieldMap)) {
				final Collection<Field> declaredFields = new DoublyLinkedList<Field>(Arrays.asList(klass.getDeclaredFields()));
				final Collection<Method> declaredMethods = new DoublyLinkedList<Method>(Arrays.asList(klass.getDeclaredMethods()));
				for (final Field field : declaredFields) {
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						logger.debug(getClass().getName(), "field:" + field.getName() + " static field ignored!");
					} else {
						final T annotation = field.getAnnotation(annotationClass);
						if (annotation != null) {
							getMethod(valueObject, values, klass, declaredMethods, methodToFieldMap, annotation, field, identifier);
						}else{
							final String methodName = "get" + field.getName();
							for (final Method m : declaredMethods) {
								if (m.getName().equalsIgnoreCase(methodName)) {
									if (methodToFieldMap!=null) {
										methodToFieldMap.put(field, m);
									}
									break;
								}
							}

						}
					}
				}
				map2.put(klass, Collections.unmodifiableMap(methodToFieldMap));
				allGetterMethodFieldMaps.put(annotationClass, map2);
			} else {
				for (Entry<Field, Method> entry : methodToFieldMap.entrySet()) {
					final T annotation = entry.getKey().getAnnotation(annotationClass);
					setMapValue(valueObject, values, klass, annotation, entry.getValue(), identifier, entry.getKey());
				}
			} 
		}else{
			final Collection<Field> declaredFields = new DoublyLinkedList<Field>(Arrays.asList(klass.getDeclaredFields()));
			final Collection<Method> declaredMethods = new DoublyLinkedList<Method>(Arrays.asList(klass.getDeclaredMethods()));
			for (final Field field : declaredFields) {
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					logger.debug(getClass().getName(), "field:" + field.getName() + " static field ignored!");
				} else {
					final T annotation = field.getAnnotation(annotationClass);
					if (annotation != null) {
						getMethod(valueObject, values, klass, declaredMethods, null, annotation, field, identifier);
					}
				}
			}
		}

	}

	/**
	 * @param valueObject
	 * @param values
	 * @param klass
	 * @param declaredMethods
	 * @param methodToFieldMap
	 * @param annotation
	 * @param field
	 * @param identifier
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void getMethod(final Object valueObject, final Map<String, Object> values, final Class<?> klass,
			final Collection<Method> declaredMethods, final Map<Field, Method> methodToFieldMap, final T annotation,
			final Field field, final Identifier identifier) throws IllegalAccessException, InvocationTargetException {
		boolean methodDeclr = false;
		final String methodName = ("get" + field.getName());//.toUpperCase(Locale.ENGLISH);
		for (final Method m : declaredMethods) {
//			if (m.getName().toUpperCase(Locale.ENGLISH).equals(methodName)) {
			if (m.getName().equalsIgnoreCase(methodName)) {
				setMapValue(valueObject, values, klass, annotation, m, identifier, field);
				if (methodToFieldMap!=null) {
					methodToFieldMap.put(field, m);
				}
				methodDeclr = true;
				break;
			}
		}
		if (!methodDeclr)
			throw new IllegalStateException("No public getter method declared for field " + field.getName()
					+ " in class " + valueObject.getClass());
	}

	/**
	 * @param valueObject
	 * @param values
	 * @param klass
	 * @param annotation
	 * @param method
	 * @param identifier
	 * @param field
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private void setMapValue(final Object valueObject, final Map<String, Object> values, final Class<?> klass,
			final T annotation, final Method method, final Identifier identifier, final Field field) throws IllegalAccessException, InvocationTargetException {
//		logger.debug(this.getClass().getName(), "Using Reflection(" + klass.getName() + ") name "
//				+ annotation.name() );
//		System.out.println("Using Reflection(" + klass.getName() + ") method " + method.getName() );
		Object value = method.invoke(valueObject, objectsInputParams);
		if (value != null) {
			String name = identifier.getIdentity(field);//annotation.name();
			Object object = values.get(name);
			if (object != null) {
				logger.debug(this.getClass().getName(), "Using Reflection(" + klass.getName() + ") name "
						+ name + " value " + value + " skipping already with value " + object);
			} else {
				values.put(name, value);
				logger.debug(this.getClass().getName(),
						"Using Reflection(" + klass.getName() + ") name " + name + " value " + value);
			}
		}
	}
	
}
