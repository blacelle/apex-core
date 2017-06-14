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
package blasd.apex.shared.util;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import blasd.apex.shared.monitoring.setstatic.SetStaticMBean;

/**
 * Various utility method related to Serialization, as conversion from/to String to/from Collections and Map
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexSerializationHelper {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexSerializationHelper.class);

	// Excel accepts only 32,767 chars per cell: we accept up to 4 MDX in a row
	// https://support.office.com/en-us/article/Excel-specifications-and-limits-16c69c74-3d6a-4aaf-ba35-e6eb276e8eaa
	public static final int MAX_CHARS_PER_COLUMN = 8192;

	public static final char MAP_KEY_VALUE_SEPARATOR = '=';

	// This should be the same as IPostProcessor.SEPARATOR
	public static final char MAP_ENTRY_SEPARATOR = ',';

	public static final MapSplitter MAP_TO_STRING_SPLITTER =
			Splitter.on(MAP_ENTRY_SEPARATOR).trimResults().withKeyValueSeparator(MAP_KEY_VALUE_SEPARATOR);

	// TODO
	// This should be the same as IPostProcessor.SEPARATOR
	public static final char COLLECTION_SEPARATOR = '|';

	public static final char FORCE_SEPARATOR = '#';

	// TODO: Not useful at all?
	@Deprecated
	protected static final Function<Object, CharSequence> OBJECT_TO_STRING = input -> {

		if (input == null) {
			// An empty String is a nice reflect of null
			return "";
		} else {
			return input.toString();
		}
	};

	private static final Function<Object, CharSequence> OBJECT_TO_QUOTED_STRING = input -> {
		if (input == null) {
			// An empty String is a nice reflect of null
			return "";
		} else {
			String asString = input.toString();

			if (input instanceof CharSequence && ((CharSequence) input).length() >= 2
					&& ((CharSequence) input).charAt(0) == '"'
					&& ((CharSequence) input).charAt(((CharSequence) input).length() - 1) == '"') {
				// Already quoted
				return asString;
			} else {
				String replaceDoubleQuotes = asString.replace("\"", "\"\"");

				// Wrap between quotes
				return "\"" + replaceDoubleQuotes + "\"";
			}
		}
	};

	protected ApexSerializationHelper() {
		// hidden
	}

	/**
	 * 
	 * @param asString
	 *            a String with the form key1=value1,key2=value2
	 * @return a {@link Map}
	 */
	public static Map<String, Object> convertToMap(CharSequence asString) {
		Map<String, String> mapStringString = convertToMapStringString(asString);
		return ImmutableMap.copyOf(Maps.transformValues(mapStringString, input -> convertStringToObject(input)));
	}

	public static Map<String, String> convertToMapStringString(CharSequence asString) {
		if (asString == null || asString.length() == 0) {
			return Collections.emptyMap();
		} else {
			Map<String, String> notFullyTrimmed = MAP_TO_STRING_SPLITTER.split(asString);

			// Linked to maintain the order of the String
			Map<String, String> fullyTrimmed = new LinkedHashMap<>();

			// .trimResults does not work from Maps
			for (Entry<String, String> notTrimmed : notFullyTrimmed.entrySet()) {
				String valueAsObject = notTrimmed.getValue().trim();
				fullyTrimmed.put(notTrimmed.getKey().trim(), valueAsObject);
			}

			return fullyTrimmed;
		}
	}

	public static Map<String, List<String>> convertToMapStringListString(String asString) {
		// Separate keys from values
		Map<String, String> mapStringString = convertToMapStringString(asString);

		// Convert value from String to List of String
		return Maps.transformValues(mapStringString, value -> convertToListString(value));
	}

	public static Set<?> convertToSet(CharSequence asString) {
		// Linked to maintain order, typically to match secondary indexes
		return new LinkedHashSet<>(convertToList(asString));
	}

	public static Set<? extends String> convertToSetString(CharSequence asString) {
		// Linked to maintain order, typically to match secondary indexes
		return new LinkedHashSet<>(convertToListString(asString));
	}

	public static List<Object> convertToList(CharSequence asString) {
		if (CharMatcher.is(COLLECTION_SEPARATOR).indexIn(asString) >= 0) {
			return convertToList(asString, COLLECTION_SEPARATOR);
		} else {
			return convertToList(asString, MAP_ENTRY_SEPARATOR);
		}
	}

	public static List<String> convertToListString(CharSequence asString) {
		if (CharMatcher.is(COLLECTION_SEPARATOR).indexIn(asString) >= 0) {
			return convertToListString(asString, COLLECTION_SEPARATOR);
		} else {
			return convertToListString(asString, MAP_ENTRY_SEPARATOR);
		}
	}

	public static List<Object> convertToList(CharSequence asString, char separator) {
		List<String> stringList = convertToListString(asString, separator);
		return ImmutableList.copyOf(Lists.transform(stringList, (input) -> {
			return convertStringToObject(input);
		}));
	}

	public static List<String> convertToListString(CharSequence asString, char separator) {
		return Splitter.on(separator).trimResults().splitToList(asString);
	}

	public static String convertToString(Map<?, ?> asMap) {
		return Joiner.on(MAP_ENTRY_SEPARATOR).withKeyValueSeparator(Character.toString(MAP_KEY_VALUE_SEPARATOR)).join(
				Maps.transformValues(asMap, input -> {
					if (input == null) {
						return "";
					} else if (input instanceof Iterable<?>) {
						// convertToString would use MAP_ENTRY_SEPARATOR
						// which is already used by Map
						return convertToString2((Iterable<?>) input);
					} else if (input instanceof CharSequence) {
						return input.toString();
					} else {
						return convertObjectToString(input);
					}
				}));
	}

	public static String convertToString(Iterable<?> asList) {
		return Joiner.on(MAP_ENTRY_SEPARATOR).join(asList);
	}

	public static String convertToString2(Iterable<?> asList) {
		return Joiner.on(COLLECTION_SEPARATOR).join(asList);
	}

	public static String convertObjectToString(Object object) {
		if (object == null) {
			return "";
		} else if (object instanceof CharSequence) {
			return object.toString();
		} else {
			return object.getClass().getName() + FORCE_SEPARATOR + object;
		}
	}

	public static Object convertStringToObject(CharSequence charSequence) {
		if (charSequence == null || charSequence.length() == 0) {
			return "";
		} else {
			String string = charSequence.toString();
			final int indexofForceSep = string.indexOf(FORCE_SEPARATOR);
			if (indexofForceSep >= 0) {
				String className = string.substring(0, indexofForceSep);
				try {
					Class<?> clazz = Class.forName(className);
					String subString = string.substring(indexofForceSep + 1);

					Object asObject = SetStaticMBean.safeTrySingleArgConstructor(clazz, subString);

					if (asObject != null) {
						// Success
						return asObject;
					} else {
						// Fallback on String
						return string;
					}
				} catch (ClassNotFoundException e) {
					LOGGER.trace("No class for {}", className);

					// Return as String
					return string;
				}
			} else {
				return string;
			}
		}
	}

	@Beta
	// TODO
	public static Object toDoubleLowDigits(Object value) {
		// if (value instanceof Float || value instanceof Double) {
		// //
		// http://stackoverflow.com/questions/703396/how-to-nicely-format-floating-numbers-to-string-without-unnecessary-decimal-0
		//
		//
		// double asDouble = ((Number) value).doubleValue();
		//
		// if (asDouble >= 1) {
		// // Get ride of decimals
		// return (double) ((long) asDouble);
		// } else {
		// String asString = String.format("%f", asDouble);
		//
		// int indexOfDot = asString.indexOf('.');
		// if (indexOfDot == -1) {
		// return asDouble;
		// } else {
		// int notZeroOrDot = 0;
		// for (int i = 0 ; i < )
		//
		// if (asString.length() > indexOfDot + 4)
		// }
		// if (asString.)
		//
		// String subString = asString.substring(0, + 4);
		//
		// return Double.parseDouble(Double.toString(asDouble));
		// }
		// } else {
		// return super.cleanValue(value);
		// }
		// TODO Auto-generated method stub
		return value;
	}

	/**
	 * Easy way to append a single CSV row in a file
	 * 
	 * @param file
	 * @param row
	 * @throws IOException
	 */
	// synchronized to prevent interlaced rows
	// TODO: one lock per actual file
	@Beta
	public static synchronized void appendLineInCSVFile(Path file, Iterable<?> row) throws IOException {
		appendLineInCSVFile(new FileWriter(file.toFile(), true), row);
	}

	@Beta
	public static void appendLineInCSVFile(Writer writer, Iterable<?> row) throws IOException {
		// Ensure the writer is buffered
		try (BufferedWriter bufferedWriter = new BufferedWriter(writer) {
			@Override
			public void close() throws IOException {
				// Skip closing as we received a Writer from somewhere else
				super.flush();
			};
		}) {
			// By default, we wrap in quotes
			rawAppendLineInCSVFile(bufferedWriter, row, true, MAX_CHARS_PER_COLUMN);
			// Prepare the next line
			bufferedWriter.newLine();
		}
	}

	@Beta
	public static void rawAppendLineInCSVFile(Writer writer,
			Iterable<?> row,
			final boolean wrapInQuotes,
			final int maxLength) throws IOException {
		// Get ride of null references
		Iterable<CharSequence> asString = Iterables.transform(row, OBJECT_TO_QUOTED_STRING);

		asString = Iterables.transform(asString, input -> {

			if (input == null || maxLength < 0) {
				// No transformation
				return input;
			} else if (!wrapInQuotes && input.length() > maxLength) {
				// simple SubSequence
				return input.subSequence(0, maxLength);
			} else {
				// We do '-2' to prevent an overflow if maxLength == Integer.MAX_VALUE
				if (wrapInQuotes && input.length() - 2 > maxLength) {
					// SubSequence between quotes
					return "\"" + input.subSequence(1, maxLength + 1) + '\"';
				} else {
					return input;
				}
			}

		});

		// Append the row
		Joiner.on(';').appendTo(writer, asString);
	}

	@Beta
	public static void appendLineInCSVFile(FileOutputStream outputFileIS, Iterable<?> row) throws IOException {
		// Use a filelock to prevent several process having their rows being interlaced
		java.nio.channels.FileLock lock = outputFileIS.getChannel().lock();
		try {
			appendLineInCSVFile(new OutputStreamWriter(outputFileIS, Charsets.UTF_8), row);
		} finally {
			lock.release();
		}
	}

	public static List<String> parseList(String asString) {
		return Splitter.on(',')
				.trimResults()
				.splitToList(asString.substring(asString.indexOf('[') + 1, asString.lastIndexOf(']')));
	}

	/**
	 * Read the object from Base64 string.
	 */
	// http://stackoverflow.com/questions/134492/how-to-serialize-an-object-into-a-string
	public static <T extends Serializable> T fromString(String s) throws IOException, ClassNotFoundException {
		byte[] data = Base64.getDecoder().decode(s);

		return fromBytes(data);
	}

	public static <T extends Serializable> T fromBytes(byte[] data) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		Object o = ois.readObject();
		ois.close();
		return (T) o;
	}

	/**
	 * Write the object to a Base64 string.
	 */
	// http://stackoverflow.com/questions/134492/how-to-serialize-an-object-into-a-string
	public static String toString(Serializable o) throws IOException {
		return Base64.getEncoder().encodeToString(toBytes(o));
	}

	public static byte[] toBytes(Serializable o) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(o);
		oos.close();
		return baos.toByteArray();
	}

}
