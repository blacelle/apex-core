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

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestApexMemoryHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestApexMemoryHelper.class);

	@Test
	public void testCtor() {
		Assert.assertNotNull(new ApexMemoryHelper());
	}

	@Test
	public void testIntArrayWeight() {
		Assert.assertEquals(56, ApexMemoryHelper.getObjectArrayMemory(new int[9]));
	}

	@Test
	public void testDouble() {
		Assert.assertEquals(24, ApexMemoryHelper.getDoubleMemory());
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

	@Test
	public void testParseMemory_EndsKB() {
		Assert.assertEquals(123 * IApexMemoryConstants.KB, ApexMemoryHelper.memoryAsLong("123kB"));
	}

	@Test
	public void testParseMemory_Edge_B() {
		Assert.assertEquals(0, ApexMemoryHelper.memoryAsLong("B"));
	}

	// Happens on vmmap|pmap. See ApexProcessHelper.getProcessResidentMemory(long)
	@Test
	public void testParseMemory_withDot() {
		Assert.assertEquals((long) (1.2 * IApexMemoryConstants.GB), ApexMemoryHelper.memoryAsLong("1.2G"));
	}

	// happens on tasklist.exe in Windows
	@Test
	public void testParseMemory_windows() {
		Assert.assertEquals(107940 * IApexMemoryConstants.KB, ApexMemoryHelper.memoryAsLong("107,940 K"));
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

	@Test
	public void testStringMemory() {
		long memory = ApexMemoryHelper.getStringMemory("Youpi");
		Assert.assertEquals(48, memory);
	}

	@Test
	public void testStringMemory_huge() {
		CharSequence existingRef = Mockito.mock(CharSequence.class);

		// Consider a very large String
		Mockito.when(existingRef.length()).thenReturn(Integer.MAX_VALUE);

		long memory = ApexMemoryHelper.getStringMemory(existingRef);
		Assertions.assertThat(memory).isGreaterThan(Integer.MAX_VALUE + 1L);
	}
}
