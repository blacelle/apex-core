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

import org.junit.Assert;
import org.junit.Test;

public class TestBloomFilterSpy {
	@SuppressWarnings("deprecation")
	@Test
	public void testEstimateCardinality() {
		BloomFilter<Integer> bf = BloomFilter.create(Funnels.integerFunnel(), 1000);

		Assert.assertEquals(0, BloomFilterSpy.estimateCardinality(bf));

		for (int i = 1; i < 1000; i++) {
			bf.put(i);

			if (i <= 86) {
				// Exact match
				Assert.assertEquals("" + i, i, BloomFilterSpy.estimateCardinality(bf));
			} else if (i <= 24) {
				// After 14, we start being slightly underestimating
				Assert.assertEquals("" + i, i - 1, BloomFilterSpy.estimateCardinality(bf));
			} else if (i <= 76) {
				// After 25, we are back on a nice estimation
				Assert.assertEquals("" + i, i, BloomFilterSpy.estimateCardinality(bf));
			} else {
				// Stop checking here
				break;
			}
		}
	}

	@Test
	public void testEstimateCardinalityOnNull() {
		Assert.assertEquals(0, BloomFilterSpy.estimateCardinality(null));
	}
}
