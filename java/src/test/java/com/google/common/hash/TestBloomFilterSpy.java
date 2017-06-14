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

import org.junit.Assert;
import org.junit.Test;

public class TestBloomFilterSpy {
	@Test
	public void testEstimateCardinality() {
		BloomFilter<Integer> bf = BloomFilter.create(Funnels.integerFunnel(), 1000);

		Assert.assertEquals(0, BloomFilterSpy.estimateCardinality(bf));

		for (int i = 1; i < 1000; i++) {
			bf.put(i);

			if (i <= 13) {
				// Exact match
				Assert.assertEquals(i, BloomFilterSpy.estimateCardinality(bf));
			} else if (i <= 24) {
				// After 14, we start being slightly underestimating
				Assert.assertEquals(i - 1, BloomFilterSpy.estimateCardinality(bf));
			} else if (i <= 76) {
				// After 25, we are back on a nice estimation
				Assert.assertEquals(i, BloomFilterSpy.estimateCardinality(bf));
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
