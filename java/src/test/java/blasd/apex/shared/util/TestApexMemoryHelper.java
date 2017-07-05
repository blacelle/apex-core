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

import blasd.apex.core.memory.ApexMemoryHelper;
import blasd.apex.core.memory.IApexMemoryConstants;

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
		Assert.assertEquals(56, ApexMemoryHelper.deepSize("Youpi"));

		if (false) {
			// Adding a single char add 2 bytes. As the JVM packes by block of 8 bytes, it may not be enough to grow the
			// estimated size
			Assert.assertTrue(ApexMemoryHelper.deepSize("Youpi") < ApexMemoryHelper.deepSize("Youpi+"));
		}
		// Adding 4 chars leads to adding 8 bytes: the actual JVM size is increased
		Assert.assertTrue(ApexMemoryHelper.deepSize("Youpi") < ApexMemoryHelper.deepSize("Youpi1234"));
	}

	@Test
	public void testImmutableMapWeight() {
		Assertions.assertThat(ApexMemoryHelper.deepSize(ImmutableMap.of("key", "Value"))).isBetween(100L, 250L);
	}

	@Test
	public void testRecursiveMapWeight() {
		// Consider a Map referencing itself
		Map<String, Object> recursiveMap = new HashMap<>();
		recursiveMap.put("myself", recursiveMap);

		long deepSize = ApexMemoryHelper.deepSize(recursiveMap);
		Assert.assertEquals(216, deepSize);

		// Change the Map so it does not reference itself: the object graph should have the same size
		Map<String, Object> withoutRecursivity = new HashMap<>();
		withoutRecursivity.put("myself", null);

		long notdeepSize = ApexMemoryHelper.deepSize(withoutRecursivity);
		Assert.assertEquals(notdeepSize, deepSize);
	}

	@Test
	public void testArrayWeight() {
		Object[] array = new Object[2];

		long sizeEmpty = ApexMemoryHelper.deepSize(array);
		Assert.assertEquals(24, sizeEmpty);

		array[0] = new LocalDate();
		array[1] = new LocalDate();

		long sizeFull = ApexMemoryHelper.deepSize(array);

		// We have different memory consumptions depending on the env/jdk/run
		Assertions.assertThat(sizeFull).isBetween(900L, 9200L);

		Assert.assertTrue(sizeFull > sizeEmpty);
	}

	@Test
	public void testIntArrayWeight() {
		Assert.assertEquals(44, ApexMemoryHelper.getObjectArrayMemory(new int[9]));
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

	@Test
	public void testParseMemory() {
		Assert.assertEquals(123, ApexMemoryHelper.memoryAsLong("123"));
		Assert.assertEquals(123 * IApexMemoryConstants.KB, ApexMemoryHelper.memoryAsLong("123k"));
		Assert.assertEquals(123 * IApexMemoryConstants.MB, ApexMemoryHelper.memoryAsLong("123M"));
		Assert.assertEquals(123 * IApexMemoryConstants.GB, ApexMemoryHelper.memoryAsLong("123g"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseMemoryFailsOnUnknownEndChars() {
		ApexMemoryHelper.memoryAsLong("123A");
	}

	@Test(expected = NumberFormatException.class)
	public void testParseMemoryFailsOnNotDigitsFirst() {
		ApexMemoryHelper.memoryAsLong("12a3m");
	}

	@Test
	public void testMemoryToString() {
		Assert.assertEquals("123B", ApexMemoryHelper.memoryAsString(123));
		Assert.assertEquals("1K206B", ApexMemoryHelper.memoryAsString(1230));
		Assert.assertEquals("1M177K", ApexMemoryHelper.memoryAsString(1230000));
		Assert.assertEquals("1G149M", ApexMemoryHelper.memoryAsString(1230000000));
	}
}
