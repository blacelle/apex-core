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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import blasd.apex.core.io.ApexSerializationHelper;

/**
 * Various utility methods specific to JMX
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexJMXHelper {

	// By default, the JConsole prefill String arguments with this value
	public static final String JMX_DEFAULT_STRING = "String";

	// This is by what the default JMX String is replaced by
	public static final String STANDARD_DEFAULT_STRING = "";

	// By default, the JConsole prefill String arguments with this value
	public static final int JMX_DEFAULT_INT = 0;

	// If we query a huge result, by default it will be truncated to this result size
	public static final int DEFAULT_LIMIT = 400;

	protected ApexJMXHelper() {
		// hidden
	}

	public static String convertToString(String asString) {
		if (asString == null || asString.isEmpty() || ApexJMXHelper.JMX_DEFAULT_STRING.equals(asString)) {
			// If the user left the default JMX String, we consider he expects a
			// full-range search
			return STANDARD_DEFAULT_STRING;
		} else {
			// We prefer to clean whitespaces as it is typically input provided
			// by a human
			return asString.trim();
		}
	}

	public static Map<String, String> convertToMap(String asString) {
		asString = convertToString(asString);

		if (asString.isEmpty()) {
			// If the user left the default JMX String, we consider he expects a
			// full-range search
			return Collections.emptyMap();
		} else {
			return ApexSerializationHelper.convertToMapStringString(asString);
		}
	}

	public static Map<String, List<String>> convertToMapList(String asString) {
		asString = convertToString(asString);

		if (asString.isEmpty()) {
			// If the user left the default JMX String, we consider he expects a
			// full-range search
			return Collections.emptyMap();
		} else {
			return ApexSerializationHelper.convertToMapStringListString(asString);
		}
	}

	public static List<? extends Map<String, String>> convertToJMXListMapString(
			Iterable<? extends Map<String, ?>> iterator) {
		// Convert to brand HashMap of String for JMX compatibility
		Iterable<? extends Map<String, String>> asString = Iterables.transform(iterator, input -> {
			// Lexicographical order over String
			return new TreeMap<>(Maps.transformValues(input, String::valueOf));
		});

		// Convert to brand ArrayList for JMX compatibility
		return Lists.newArrayList(asString);
	}

	public static List<String> convertToList(String asString) {
		asString = convertToString(asString);

		if (asString.isEmpty()) {
			// If the user left the default JMX String, we consider he expects a
			// full-range search
			return Collections.emptyList();
		} else {
			return ApexSerializationHelper.convertToListString(asString);
		}
	}

	public static Set<?> convertToSet(String asString) {
		asString = convertToString(asString);

		if (asString.isEmpty()) {
			return Collections.emptySet();
		} else {
			return ApexSerializationHelper.convertToSet(asString);
		}
	}

	public static Set<? extends String> convertToSetString(String asString) {
		asString = convertToString(asString);

		if (asString.isEmpty()) {
			return Collections.emptySet();
		} else {
			return ApexSerializationHelper.convertToSetString(asString);
		}
	}

	public static Map<String, String> convertToJMXMapString(Map<?, ?> someMap) {
		// Maintain order, and use a JMX compatible implementation
		Map<String, String> idToQueryString = new LinkedHashMap<>();

		for (Entry<?, ?> entry : someMap.entrySet()) {
			idToQueryString.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
		}

		return idToQueryString;
	}

	public static <T> Map<String, T> convertToJMXMapKeyString(Map<?, T> someMap) {
		// Maintain order, and use a JMX compatible implementation
		Map<String, T> idToQueryString = new LinkedHashMap<>();

		for (Entry<?, T> entry : someMap.entrySet()) {
			idToQueryString.put(String.valueOf(entry.getKey()), entry.getValue());
		}

		return idToQueryString;
	}

	public static <S, T> Map<S, T> convertToJMXMap(Map<S, T> asMap) {
		Map<S, T> cleanerMap;

		// We accept to break ordering, and prefer return a user-friendly order
		try {
			cleanerMap = new TreeMap<S, T>(asMap);
		} catch (RuntimeException e) {
			// TreeMap ctor could fail if the key is not comparable (e.g. if the
			// key is itself a Map)
			cleanerMap = asMap;
		}
		return convertToJMXValueOrderedMap(cleanerMap, Optional.empty());
	}

	public static <S, T extends Comparable<T>> Map<S, T> convertToJMXValueOrderedMap(Map<S, T> map) {
		return convertToJMXValueOrderedMap(map, false);
	}

	public static <S, T extends Comparable<T>> Map<S, T> convertToJMXValueOrderedMap(Map<S, T> map,
			final boolean reverse) {

		Comparator<Entry<S, T>> comparator;

		if (reverse) {
			comparator = Map.Entry.<S, T>comparingByValue().reversed();
		} else {
			comparator = Map.Entry.comparingByValue();
		}

		return convertToJMXValueOrderedMap(map, Optional.of(comparator));
	}

	public static <S, T> Map<S, T> convertToJMXValueOrderedMap(Map<S, T> map,
			Optional<? extends Comparator<? super Entry<S, T>>> comparator) {

		Stream<Entry<S, T>> entries = map.entrySet().stream();

		Supplier<Map<S, T>> mapSupplier;
		if (comparator.isPresent()) {
			entries = entries.sorted(comparator.get());
			mapSupplier = LinkedHashMap::new;
		} else {
			if (map instanceof TreeMap) {
				mapSupplier = TreeMap::new;
			} else {
				mapSupplier = LinkedHashMap::new;
			}
		}

		// http://stackoverflow.com/questions/29567575/sort-map-by-value-using-java-8

		return entries.collect(StackOverflowExampleCollectors.toMap(e -> {
			if (e.getKey() instanceof List<?>) {
				return (S) convertToJMXList((List) e.getKey());
			} else {
				return e.getKey();
			}
			// LinkedHashMap to maintain order (as defined by values), and JMX
			// Compatible
		}, Entry::getValue, mapSupplier));
	}

	public static <T extends Comparable<T>> Set<T> convertToJMXSet(Iterable<? extends T> elements) {
		// TreeSet: JMX-compatible and lexicographically ordered
		return Sets.newTreeSet(elements);
	}

	public static <T> List<T> convertToJMXList(Iterable<? extends T> elements) {
		// ArrayList: JMX-compatible
		return Lists.newArrayList(elements);
	}

	public static List<String> convertToJMXStringList(Iterable<?> elements) {
		return convertToJMXList(Iterables.transform(elements, Functions.toStringFunction()));
	}

	public static List<String> convertToJMXStringSet(Set<?> elements) {
		return convertToJMXList(Iterables.transform(elements, Functions.toStringFunction()));
	}

	public static int convertToLimit(int limit) {
		if (limit == ApexJMXHelper.JMX_DEFAULT_INT) {
			return ApexJMXHelper.DEFAULT_LIMIT;
		} else {
			return limit;
		}
	}
}
