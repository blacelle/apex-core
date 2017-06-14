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
package blasd.apex.core.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Properties
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexPropertiesHelper {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexPropertiesHelper.class);

	protected ApexPropertiesHelper() {
		// hidden
	}

	public static void checkProperties(Properties properties) {
		checkProperties(properties, Collections.emptySet());
	}

	public static void checkProperties(Properties properties, Set<String> exceptionsKeys) {
		if (properties != null) {
			try {
				Enumeration<?> keys = properties.propertyNames();

				while (keys.hasMoreElements()) {
					Object key = keys.nextElement();
					Object value = properties.get(key);

					if (value == null) {
						if (key instanceof String) {
							// Try to find the value in default properties
							String valueAsString = properties.getProperty((String) key);

							if (valueAsString == null) {
								// The not-String object in the default
								// properties
								// is not accessible
								LOGGER.warn(
										"It is unsafe to associate the key {} to !String (in base or default Properties)",
										key);
							}
						} else {
							LOGGER.warn("It is unsafe to associate the key {} to null", key);
						}
					} else if (!(value instanceof String)) {
						if (exceptionsKeys.contains(key)) {
							// This bad case is introduced by ActivePivot core
							// com.qfs.pivot.cube.provider.impl.AggregateProviderBuilder.build(IEpoch)
							LOGGER.debug("It is unsafe to associate the key {} to !String: {}({})",
									key,
									value.getClass(),
									value);
						} else {
							LOGGER.warn("It is unsafe to associate the key {} to !String: {}({})",
									key,
									value.getClass(),
									value);
						}
					}
				}
			} catch (Exception e) {
				LOGGER.warn("Something is wrong in properties: " + properties, e);
			}
		}
	}
}
