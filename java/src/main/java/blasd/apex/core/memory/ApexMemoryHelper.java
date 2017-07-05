/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package blasd.apex.core.memory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntPredicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.google.common.base.CharMatcher;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.util.concurrent.AtomicLongMap;

import blasd.apex.core.agent.InstrumentationAgent;

/**
 * This class helps reference-sharing
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexMemoryHelper implements IApexMemoryConstants {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexMemoryHelper.class);

	// TODO: we should compute the actual memory of all Strings, in order to reject very long Strings
	public static final int NB_STRING_BEFORE_CLEAR = 10000;

	private static final ConcurrentMap<Class<?>, List<Field>> CLASS_TO_ELECTED_FIELDS =
			new ConcurrentHashMap<Class<?>, List<Field>>();
	private static final ConcurrentMap<Field, ConcurrentMap<Object, Object>> DICTIONARY =
			new ConcurrentHashMap<Field, ConcurrentMap<Object, Object>>();
	private static final AtomicLongMap<Field> FIELD_TO_DICTIONARY_SIZE = AtomicLongMap.create();

	protected ApexMemoryHelper() {
		// hidden
	}

	/**
	 * This method will replace fields of given object with reference used by other objects. It is typically usefull to
	 * POJO which, except the spaceKey, have low cardinality
	 * 
	 * @param data
	 */
	public static void dictionarize(Object data) {
		if (data == null) {
			return;
		}

		Class<?> clazz = data.getClass();

		List<Field> fields = computeDictionarizableFields(clazz);

		if (fields.isEmpty()) {
			// This class has no field to dictionarize
			return;
		}

		for (Field f : fields) {
			dictionarizeFieldValue(f, data);
		}

	}

	/**
	 * 
	 * @param field
	 * @param object
	 */
	protected static void dictionarizeFieldValue(Field field, Object object) {
		if (field == null || object == null) {
			return;
		}

		ConcurrentMap<Object, Object> fieldDictionary = DICTIONARY.get(field);

		if (fieldDictionary == null) {
			// First encounter of given field
			DICTIONARY.putIfAbsent(field, new ConcurrentHashMap<Object, Object>());

			// Retrieve the elected cache
			fieldDictionary = DICTIONARY.get(field);
		}

		try {
			Object currentRef = field.get(object);

			if (currentRef != null) {
				// Try to find an existing object equal to the new object
				Object existingRef = fieldDictionary.putIfAbsent(currentRef, currentRef);
				if (existingRef != null && existingRef != currentRef) {
					// We have found an existing equals object, but with a different ref: share the existing ref
					field.set(object, existingRef);
				} else {
					if (existingRef instanceof CharSequence) {
						FIELD_TO_DICTIONARY_SIZE.addAndGet(field, getStringMemory((CharSequence) existingRef));
					}

					// fieldDictionary has grown
					if (fieldDictionary.size() > NB_STRING_BEFORE_CLEAR) {
						// We consider the cache is too big: clear it and start from scratch. One may prefer to use
						// an LRU, but it would cost more CPU to maintain
						fieldDictionary.clear();

						// Check more advanced hit/miss ratio
						// {
						// long missCount = fieldStats.get(1);
						// long hitCount = fieldStats.get(0);
						// if (missCount > 100000 && missCount / 100L > hitCount) {
						// // We have at least 100.000 misses and less than 1% hits: this field is not
						// // interesting
						// // as it have too high cardinality
						// LOGGER.warn("Stop dictionarizing " + field
						// + " because of very-bad miss-hit ratio after having considered "
						// + (hitCount + missCount)
						// + " entries");
						// stopDictionarizing(fields, field);
						// }
						// }
					}
				}
			}

		} catch (IllegalArgumentException e) {
			stopDictionarizing(object.getClass(), field);
		} catch (IllegalAccessException e) {
			stopDictionarizing(object.getClass(), field);
		}
	}

	// http://java-performance.info/overview-of-memory-saving-techniques-java/
	public static final int JVM_MEMORY_CHUNK = 8;
	public static final int JVM_BYTES_PER_CHAR = 2;
	public static final int JVM_STRING_HEADER = 45;

	public static long getStringMemory(CharSequence existingRef) {
		// http://www.javamex.com/tutorials/memory/string_memory_usage.shtml
		// Object Header
		int nbChars = existingRef.length();

		// String are essentially char[], 8 for char
		return JVM_MEMORY_CHUNK * (int) ((nbChars * JVM_BYTES_PER_CHAR + JVM_STRING_HEADER) / JVM_MEMORY_CHUNK);
	}

	protected static void stopDictionarizing(Class<?> clazz, Field field) {
		if (clazz == null || field == null) {
			return;
		}

		// This field is not candidate for dictionarisation anymore
		List<Field> electedFields = CLASS_TO_ELECTED_FIELDS.get(clazz);
		if (electedFields != null) {
			// Might be null if CLASS_TO_ELECTED_FIELDS cleared concurrently
			electedFields.remove(field);
		}
		DICTIONARY.remove(field);
	}

	protected static List<Field> computeDictionarizableFields(Class<?> clazz) {
		List<Field> fields = CLASS_TO_ELECTED_FIELDS.get(clazz);

		if (fields != null) {
			// Already computed
			return fields;
		}

		// First encounter of given class
		// CopyOnWriteArrayList as some Field may be removed later
		final List<Field> preparingFields = new CopyOnWriteArrayList<Field>();

		ReflectionUtils.doWithFields(clazz, field -> {
			// Make accessible as we will read and write it
			ReflectionUtils.makeAccessible(field);

			preparingFields.add(field);
		}, field -> {
			if (Modifier.isStatic(field.getModifiers())) {
				// Do not touch static fields
				return false;
			}

			if (field.getType() != String.class) {
				// For now, we consider only String fields
				return false;
			}

			return true;
		});

		CLASS_TO_ELECTED_FIELDS.putIfAbsent(clazz, preparingFields);

		// handle concurrent fields computations
		return CLASS_TO_ELECTED_FIELDS.get(clazz);
	}

	public static <T> void dictionarizeArray(T[] array) {
		if (array == null) {
			return;
		}

		for (Object item : array) {
			dictionarize(item);
		}
	}

	public static void dictionarizeIterable(Iterable<?> iterable) {
		if (iterable == null) {
			return;
		}

		for (Object item : iterable) {
			dictionarize(item);
		}
	}

	/**
	 * @deprecated renamed to deepSize
	 */
	@Deprecated
	public static long recursiveSize(Object object) {
		return deepSize(object);
	}

	/**
	 * 
	 * @param object
	 *            the object to analyze
	 * @return the number of bytes consumed by given objects, taking in account the references objects
	 */
	public static long deepSize(Object object) {
		// http://stackoverflow.com/questions/1063068/how-does-the-jvm-ensure-that-system-identityhashcode-will-never-change
		return deepSizeWithBloomFilter(object, Integer.MAX_VALUE / 1024);
	}

	public static long recursiveSize(Object object, IntPredicate identityPredicate) {
		return deepSize(object, identityPredicate);
	}

	/**
	 * 
	 * @param object
	 * @param identityPredicate
	 *            a predicate returning true if it is the first encounter of given object. It may return false even if
	 *            an object has not been considered before, woult it be because the identity policy is not guaranteed
	 *            (e.g. we rely on a BloomFilter) or if we want to exclude some objects
	 * @return 0 if the Instrumentation agent is not available. Else an estimation of the memory consumption.
	 */
	public static long deepSize(Object object, IntPredicate identityPredicate) {
		if (object == null) {
			return 0L;
		} else {
			Instrumentation instrumentation = InstrumentationAgent.safeGetInstrumentation();

			if (instrumentation == null) {
				LOGGER.debug("Instrumentation is not available");
				return 0L;
			}

			LongAdder totalSize = new LongAdder();

			deepSize(instrumentation, identityPredicate, totalSize, object);

			return totalSize.sum();
		}
	}

	@Deprecated
	public static long recursiveSizeWithBloomFilter(Object object, long expectedObjectCardinality) {
		return deepSizeWithBloomFilter(object, expectedObjectCardinality);
	}

	public static long deepSizeWithBloomFilter(Object object, long expectedObjectCardinality) {
		BloomFilter<Integer> identities = BloomFilter.create(Funnels.integerFunnel(), expectedObjectCardinality);

		return recursiveSize(object, identities::put);
	}

	@Deprecated
	public static void recursiveSize(Instrumentation instrumentation,
			IntPredicate identities,
			LongAdder totalSize,
			Object object) {
		deepSize(instrumentation, identities, totalSize, object);
	}

	/**
	 * 
	 * @param instrumentation
	 *            an {@link Instrumentation} able to provide the memory weight of given object
	 * @param identities
	 *            an identityHashSet where to collect already handlded objects
	 * @param totalSize
	 *            the LongAdder where to accumulate the memory
	 * @param object
	 *            the object to analyse
	 */
	// see https://github.com/jbellis/jamm
	//
	public static void deepSize(Instrumentation instrumentation,
			IntPredicate identities,
			LongAdder totalSize,
			Object object) {
		if (object == null) {
			return;
		} else {
			// http://stackoverflow.com/questions/4930781/how-do-hashcode-and-identityhashcode-work-at-the-back-end
			// see FastHashCode in
			// http://hg.openjdk.java.net/jdk6/jdk6/hotspot/file/tip/src/share/vm/runtime/synchronizer.cpp
			// -> Random value unrelated to memory address
			if (identities.test(System.identityHashCode(object))) {
				long currentSize = instrumentation.getObjectSize(object);
				totalSize.add(currentSize);

				if (object instanceof Object[]) {
					// For arrays, it would not work to iterate on its fields
					Arrays.stream((Object[]) object)
							.forEach(element -> recursiveSize(instrumentation, identities, totalSize, element));
				} else {
					ReflectionUtils.doWithFields(object.getClass(),
							field -> recursiveSize(instrumentation, identities, totalSize, field.get(object)),
							field -> {
								if (Modifier.isStatic(field.getModifiers())) {
									// Do not add static fields in memory graph
									return false;
								} else if (field.getType().isPrimitive()) {
									// Primitive fields has already been captured by Instrumentation.getObjectSize
									return false;
								} else {
									// Ensure the field is accessible as we are going to read it
									ReflectionUtils.makeAccessible(field);

									return true;
								}
							});
				}
			}
		}
	}

	public static long getDoubleMemory() {
		// Double are essentially a double in an Object
		long charArrayWeight = DOUBLE + OBJECT;

		return charArrayWeight;
	}

	public static long getObjectArrayMemory(Object[] asArray) {
		if (asArray == null) {
			return 0L;
		}
		long footprint = OBJECT;

		// The weight of the array
		footprint += OBJECT * asArray.length;

		return footprint;
	}

	public static long getObjectArrayMemory(Object asArray) {
		if (asArray == null) {
			return 0L;
		}
		Class<?> arrayClass = asArray.getClass();
		if (!arrayClass.isArray()) {
			return 0L;
		}

		Class<?> elementClass = arrayClass.getComponentType();

		// Object header
		long footprint = OBJECT;
		final long componentWeight;
		if (elementClass == int.class) {
			componentWeight = INT;
		} else if (elementClass == float.class) {
			componentWeight = FLOAT;
		} else {
			// Long, Double, Object
			componentWeight = OBJECT;
		}

		// The weight of the array
		footprint += componentWeight * Array.getLength(asArray);

		return footprint;
	}

	private static final long MASK = 0xFFFFFFFFL;
	private static final int SHIFT = 32;

	/**
	 * It might be useful to have an long<->(int,int) packing guaranteeing both integers to be positive if the long is
	 * positive
	 */
	public static final long positivePack(int i1, int i2) {
		long packed1 = (long) i1 << SHIFT;
		long packed2 = Integer.rotateLeft(i2, 1) & MASK;
		return Long.rotateRight(packed1 | packed2, 1);
	}

	public static final int positiveUnpack1(long packed) {
		// Move the higher bit as lower bit: if packed >= 0, we then are sure to have a 0 as first bit
		return (int) (Long.rotateLeft(packed, 1) >>> SHIFT);
	}

	public static final int positiveUnpack2(long packed) {
		// Move the higher bit as lower bit: if packed >= 0, we then are sure to have a 0 as first bit
		// Then, this 0 bit it put back as last bit: the integer is guaranteed to be positive
		return Integer.rotateRight((int) (Long.rotateLeft(packed, 1) & MASK), 1);
	}

	public static long memoryAsLong(String targetMax) {
		// https://stackoverflow.com/questions/1098488/jvm-heap-parameters

		if (targetMax.isEmpty()) {
			throw new UnsupportedOperationException("Can not be empty");
		}

		String digits;
		long multiplier;

		char lastChar = targetMax.charAt(targetMax.length() - 1);
		if (CharMatcher.javaDigit().matches(lastChar)) {
			multiplier = 1L;
			digits = targetMax;
		} else {
			if (lastChar == 'k' || lastChar == 'K') {
				multiplier = IApexMemoryConstants.KB;
			} else if (lastChar == 'm' || lastChar == 'M') {
				multiplier = IApexMemoryConstants.MB;
			} else if (lastChar == 'g' || lastChar == 'G') {
				multiplier = IApexMemoryConstants.GB;
			} else {
				throw new IllegalArgumentException(
						"Can not parse " + targetMax + ". It should end by a digit or one of 'k', 'm','g'");
			}

			digits = targetMax.substring(0, targetMax.length() - 1);
		}

		return Long.parseLong(digits) * multiplier;
	}

	public static String memoryAsString(long bytes) {
		String string = "";

		int unitsDone = 0;
		if (unitsDone < 2) {
			long gb = bytes / IApexMemoryConstants.GB;
			if (gb > 0) {
				unitsDone++;
				string += gb + "G";
				bytes -= gb * IApexMemoryConstants.GB;
			}
		}

		if (unitsDone < 2) {
			long mb = bytes / IApexMemoryConstants.MB;
			if (mb > 0) {
				unitsDone++;
				string += mb + "M";
				bytes -= mb * IApexMemoryConstants.MB;
			}
		}
		if (unitsDone < 2) {
			long kb = bytes / IApexMemoryConstants.KB;
			if (kb > 0) {
				unitsDone++;
				string += kb + "K";
				bytes -= kb * IApexMemoryConstants.KB;
			}
		}

		if (unitsDone < 2) {
			if (bytes > 0) {
				string += bytes + "B";
			}
		}

		return string;
	}
}
