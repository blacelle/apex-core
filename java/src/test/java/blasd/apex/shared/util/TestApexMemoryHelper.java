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

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class TestApexMemoryHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestApexMemoryHelper.class);

	static class NotFinalField {
		public String oneString;

		public NotFinalField(String oneString) {
			this.oneString = oneString;
		}

	}

	static class FinalField {
		public String oneString;

		public FinalField(String oneString) {
			this.oneString = oneString;
		}

	}

	static class DerivedClass extends FinalField {

		public DerivedClass(String oneString) {
			super(oneString);
		}

	}

	@Test
	public void testNullRef() {
		ApexMemoryHelper.dictionarize(null);
		ApexMemoryHelper.dictionarizeArray(null);
		ApexMemoryHelper.dictionarizeIterable(null);
	}

	@Test
	public void testDictionarisationOnFinal() {
		FinalField left = new FinalField("Youpi");
		FinalField right = new FinalField(new String("Youpi"));

		// Not same ref
		Assert.assertNotSame(left.oneString, right.oneString);

		ApexMemoryHelper.dictionarize(left);
		ApexMemoryHelper.dictionarize(right);

		Assert.assertSame(left.oneString, right.oneString);
	}

	@Test
	public void testDictionarisationOnNotFinal() {
		NotFinalField left = new NotFinalField("Youpi");
		NotFinalField right = new NotFinalField(new String("Youpi"));

		// Not same ref
		Assert.assertNotSame(left.oneString, right.oneString);

		ApexMemoryHelper.dictionarize(left);
		ApexMemoryHelper.dictionarize(right);

		Assert.assertSame(left.oneString, right.oneString);
	}

	@Test
	public void testDictionarisationOnDerived() {
		DerivedClass left = new DerivedClass("Youpi");
		DerivedClass right = new DerivedClass(new String("Youpi"));

		// Not same ref
		Assert.assertNotSame(left.oneString, right.oneString);

		ApexMemoryHelper.dictionarize(left);
		ApexMemoryHelper.dictionarize(right);

		Assert.assertSame(left.oneString, right.oneString);
	}

	@Test
	public void testStringWeight() {
		Assert.assertEquals(56, ApexMemoryHelper.recursiveSize("Youpi"));

		if (false) {
			// Adding a single char add 2 bytes. As the JVM packes by block of 8 bytes, it may not be enough to grow the
			// estimated size
			Assert.assertTrue(ApexMemoryHelper.recursiveSize("Youpi") < ApexMemoryHelper.recursiveSize("Youpi+"));
		}
		// Adding 4 chars leads to adding 8 bytes: the actual JVM size is increased
		Assert.assertTrue(ApexMemoryHelper.recursiveSize("Youpi") < ApexMemoryHelper.recursiveSize("Youpi1234"));
	}

	@Test
	public void testImmutableMapWeight() {
		Assert.assertEquals(144, ApexMemoryHelper.recursiveSize(ImmutableMap.of("key", "Value")));
	}

	@Test
	public void testRecursiveMapWeight() {
		// Consider a Map referencing itself
		Map<String, Object> recursiveMap = new HashMap<>();
		recursiveMap.put("myself", recursiveMap);

		long recursiveSize = ApexMemoryHelper.recursiveSize(recursiveMap);
		Assert.assertEquals(216, recursiveSize);

		// Change the Map so it does not reference itself: the object graph should have the same size
		Map<String, Object> withoutRecursivity = new HashMap<>();
		withoutRecursivity.put("myself", null);

		long notRecursiveSize = ApexMemoryHelper.recursiveSize(withoutRecursivity);
		Assert.assertEquals(notRecursiveSize, recursiveSize);
	}

	@Test
	public void testArrayWeight() {
		Object[] array = new Object[2];

		long sizeEmpty = ApexMemoryHelper.recursiveSize(array);
		Assert.assertEquals(24, sizeEmpty);

		array[0] = new LocalDate();
		array[1] = new LocalDate();

		long sizeFull = ApexMemoryHelper.recursiveSize(array);

		// We have different memory consumptions depending on the env/jdk
		Assertions.assertThat(sizeFull).isIn(9136L, 9112L);

		Assert.assertTrue(sizeFull > sizeEmpty);
	}

	@Test
	public void testIntArrayWeight() {
		Assert.assertEquals(44, ApexMemoryHelper.getObjectArrayMemory(new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
	}

	@Test
	public void testBitPacking() {
		// We use custom bit packing in order to produce positive integers if the input long is positive
		// Consider positive longs, as they typically comes from dates
		for (long i = 0; i < Integer.MAX_VALUE * 1024L; i += Integer.MAX_VALUE / 10) {
			// We want to ensure the integers are positive: false as unpack2 will cover the whole integer range:
			// MIN_VALUE -> MAX_VALUE
			LOGGER.trace("Testing bit-packing for {}", i);
			Assert.assertEquals(i,
					ApexMemoryHelper.positivePack(ApexMemoryHelper.positiveUnpack1(i),
							ApexMemoryHelper.positiveUnpack2(i)));
		}
	}
}
