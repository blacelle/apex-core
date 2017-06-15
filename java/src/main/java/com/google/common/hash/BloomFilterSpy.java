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
package com.google.common.hash;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.google.common.hash.BloomFilterStrategies.BitArray;

/**
 * Additional methods for BloomFilter
 * 
 * @author Benoit Lacelle
 *
 */
public class BloomFilterSpy {

	protected static final Logger LOGGER = LoggerFactory.getLogger(BloomFilterSpy.class);

	private static final Field BIT_ARRAY_FIELD = ReflectionUtils.findField(BloomFilter.class, "bits", BitArray.class);
	private static final Field NUM_HASH_FUNCTIONS_FIELD =
			ReflectionUtils.findField(BloomFilter.class, "numHashFunctions", int.class);
	static {
		// Ensure the field is writtable
		try {
			if (BIT_ARRAY_FIELD != null) {
				ReflectionUtils.makeAccessible(BIT_ARRAY_FIELD);
			}
			if (NUM_HASH_FUNCTIONS_FIELD != null) {
				ReflectionUtils.makeAccessible(NUM_HASH_FUNCTIONS_FIELD);
			}
		} catch (Throwable t) {
			LOGGER.warn("Issue with BloomFilter introspection", t);
		}
	}

	protected BloomFilterSpy() {
		// BloomFilterSpy
	}

	/**
	 * https://en.wikipedia.org/wiki/Bloom_filter# Approximating_the_number_of_items_in_a_Bloom_filter
	 * 
	 * @param bloomFilter
	 * @return an estimation of the number of entries inserted in this BloomFilter
	 */
	public static long estimateCardinality(BloomFilter<?> bloomFilter) {
		if (bloomFilter == null) {
			return 0L;
		} else if (BIT_ARRAY_FIELD == null || NUM_HASH_FUNCTIONS_FIELD == null) {
			LOGGER.warn("BloomFilter.estimateCardinality is not available");
			return -1L;
		}

		try {
			BitArray bitArray = (BitArray) ReflectionUtils.getField(BIT_ARRAY_FIELD, bloomFilter);

			long x = bitArray.bitCount();

			if (x == 0L) {
				return 0L;
			} else {

				long m = bitArray.bitSize();
				long k = NUM_HASH_FUNCTIONS_FIELD.getInt(bloomFilter);

				long theory = (long) (-1L * m * Math.log(1 - (double) x / (double) m)) / k;

				// As we have as least one bit set, the count must be at least 1;
				return Math.max(1L, theory);
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
