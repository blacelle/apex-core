/**
 * The MIT License
 * Copyright (c) ${project.inceptionYear} Benoit Lacelle
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
