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
package com.google.common.hash;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Additional methods for BloomFilter
 * 
 * @author Benoit Lacelle
 *
 */
public class BloomFilterSpy {

	protected static final Logger LOGGER = LoggerFactory.getLogger(BloomFilterSpy.class);

	protected BloomFilterSpy() {
		// BloomFilterSpy
	}

	/**
	 * https://en.wikipedia.org/wiki/Bloom_filter# Approximating_the_number_of_items_in_a_Bloom_filter
	 * 
	 * @param bloomFilter
	 * @return an estimation of the number of entries inserted in this BloomFilter. It is guaranteed to be at least one
	 *         if at least one put happened
	 * @deprecated BloomFilter.approximateElementCount has been introduced in Guava 23.0
	 */
	@Deprecated
	public static long estimateCardinality(BloomFilter<?> bloomFilter) {
		return Optional.ofNullable(bloomFilter).map(bf -> bf.approximateElementCount()).orElse(0L);
	}
}
