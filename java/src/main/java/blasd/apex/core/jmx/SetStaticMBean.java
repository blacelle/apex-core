/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.core.jmx;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.ReflectionUtils;

/**
 * This MBean enables the modification of primitive static variables, like DEBUG modes
 * 
 * @author Benoit Lacelle
 * 
 */
@ManagedResource
public class SetStaticMBean {
	protected static final Logger LOGGER = LoggerFactory.getLogger(SetStaticMBean.class);

	// THere might be a way to change private final fields... but it seems not
	// to work on a unit-test :|
	protected boolean forceForPrivateFinal = true;

	@ManagedOperation
	public void setStatic(String className, String fieldName, String newValueAsString)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> classToSet = Class.forName(className);

		Field field = getField(classToSet, fieldName);

		Class<?> fieldType = field.getType();
		if (fieldType == Boolean.class || fieldType == boolean.class) {
			field.set(null, Boolean.parseBoolean(newValueAsString));
		} else if (fieldType == Float.class || fieldType == float.class) {
			field.set(null, Float.parseFloat(newValueAsString));
		} else if (fieldType == Double.class || fieldType == double.class) {
			field.set(null, Double.parseDouble(newValueAsString));
		} else if (fieldType == Integer.class || fieldType == int.class) {
			field.set(null, Integer.parseInt(newValueAsString));
		} else if (fieldType == Long.class || fieldType == long.class) {
			field.set(null, Long.parseLong(newValueAsString));
		} else if (fieldType == String.class) {
			field.set(null, newValueAsString);
		} else {
			Object asObject = safeTrySingleArgConstructor(fieldType, newValueAsString);

			if (asObject != null) {
				// Instantiation succeeded
				field.set(null, asObject);
				return;
			}

			throw new RuntimeException("The field " + fieldType + " is not managed");
		}
	}

	@ManagedOperation
	public String getStaticAsString(String className, String fieldName)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		return String.valueOf(getStatic(className, fieldName));
	}

	public Object getStatic(String className, String fieldName)
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		Class<?> classToSet = Class.forName(className);

		Field field = getField(classToSet, fieldName);

		// Instantiation succeeded
		return field.get(null);
	}

	private Field getField(Class<?> classToSet, String fieldName)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field field = ReflectionUtils.findField(classToSet, fieldName);

		if (forceForPrivateFinal) {
			// http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection
			ReflectionUtils.makeAccessible(field);

			// It may not work for primitive fields
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			ReflectionUtils.makeAccessible(modifiersField);
			modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		}

		return field;
	}

	public static Object safeTrySingleArgConstructor(Class<?> fieldType, Object argument) {
		if (argument == null) {
			// TODO: try to find any Constructor accepting any Object
			return null;
		} else {
			// iterate through classes and interfaces
			{
				Class<?> classToTry = argument.getClass();

				while (classToTry != null) {
					Object asObject = safeTrySingleArgConstructor(fieldType, classToTry, argument);

					if (asObject != null) {
						// Instantiation succeeded
						return asObject;
					} else {
						classToTry = classToTry.getSuperclass();
					}
				}
			}

			for (Class<?> classToTry : argument.getClass().getInterfaces()) {
				Object asObject = safeTrySingleArgConstructor(fieldType, classToTry, argument);

				if (asObject != null) {
					// Instantiation succeeded
					return asObject;
				} else {
					classToTry = classToTry.getSuperclass();
				}
			}

			// Found nothing
			return null;
		}
	}

	/**
	 * We expect this method not to throw because of an invalid class, invalid type, etc
	 * 
	 * @param fieldType
	 * @param constructorClass
	 * @param argument
	 * @return
	 */
	public static Object safeTrySingleArgConstructor(Class<?> fieldType, Class<?> constructorClass, Object argument) {
		// Unknown field: we will try to call the constructor taking a single String
		// It will work for joda LocalDate for instance
		try {
			Constructor<?> stringConstructor = fieldType.getConstructor(constructorClass);

			return stringConstructor.newInstance(argument);
		} catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException
				| RuntimeException e) {
			LOGGER.trace("No constructor for {} with {} argumennt", fieldType, constructorClass);
			return null;
		}
	}

	/**
	 * One could write "com.quartetfs.biz.pivot.IActivePivot" One could write
	 * "com/quartetfs/biz/pivot/IActivePivot.class"
	 */
	@ManagedOperation
	public List<String> getResourcesFor(String path) throws IOException {
		List<String> resources = new ArrayList<>();

		Enumeration<URL> urlEnum = this.getClass().getClassLoader().getResources(path);
		if (!urlEnum.hasMoreElements()) {
			// Transform "com.quartetfs.biz.pivot.IActivePivot" to
			// "com/quartetfs/biz/pivot/IActivePivot.class"
			urlEnum = this.getClass().getClassLoader().getResources(path.replace('.', '/') + ".class");
		}
		while (urlEnum.hasMoreElements()) {
			resources.add(urlEnum.nextElement().toString());
		}

		return resources;
	}

	public static void main(String[] args) throws IOException {
		String className = SetStaticMBean.class.getName();

		System.out.println("From " + className);
		for (String url : new SetStaticMBean().getResourcesFor(className)) {
			System.out.println(url);
		}
		System.out.println();

		String shortPath = className.replace('.', '/');
		System.out.println("From " + shortPath);
		for (String url : new SetStaticMBean().getResourcesFor(shortPath)) {
			System.out.println(url);
		}
		System.out.println();

		String path = shortPath + ".class";
		System.out.println("From " + path);
		for (String url : new SetStaticMBean().getResourcesFor(path)) {
			System.out.println(url);
		}
		System.out.println();
	}
}
