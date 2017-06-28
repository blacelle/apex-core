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
package blasd.apex.shared.logging;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.logging.ApexLogHelper;

public class TestApexLogHelper {
	@Test
	public void lazyToString() {
		// Not the String
		Assert.assertNotEquals("Youpi", ApexLogHelper.lazyToString(() -> "Youpi"));

		// But same .toString
		Assert.assertEquals("Youpi", ApexLogHelper.lazyToString(() -> "Youpi").toString());
	}

	@Test
	public void testLazyToString() {
		// The lazyToString should not be a String
		Assert.assertNotEquals("Youpi", ApexLogHelper.lazyToString(() -> "Youpi"));

		Assert.assertEquals("Youpi", ApexLogHelper.lazyToString(() -> "Youpi").toString());
	}

	@Test
	public void getPercentage() {
		Assert.assertEquals("10%", ApexLogHelper.getNicePercentage(100, 1000).toString());
	}

	@Test
	public void getPercentageDivideBy0() {
		Assert.assertEquals("-%", ApexLogHelper.getNicePercentage(100, 0).toString());
	}

	@Test
	public void getSmallPercentage() {
		Assert.assertEquals("0.3%", ApexLogHelper.getNicePercentage(3, 1000).toString());
	}

	@Test
	public void getVerySmallPercentage() {
		Assert.assertEquals("0.03%", ApexLogHelper.getNicePercentage(3, 10000).toString());
	}

	@Test
	public void getProgressAboveMax() {
		Assert.assertEquals("1000%", ApexLogHelper.getNicePercentage(1000, 100).toString());
	}

	@Test
	public void testBigTimeLowRate() {
		Assert.assertEquals("1440#/minute", ApexLogHelper.getNiceRate(10, 10, TimeUnit.DAYS).toString());
	}

	@Test
	public void testBigTimeHIghRate() {
		Assert.assertEquals("309237644160#/minute",
				ApexLogHelper.getNiceRate(Integer.MAX_VALUE, 10, TimeUnit.DAYS).toString());
	}

	@Test
	public void testLowTimeLowRate() {
		Assert.assertEquals("1#/ms", ApexLogHelper.getNiceRate(10, 10, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testLowTimeHighRate() {
		Assert.assertEquals("3579#/minute",
				ApexLogHelper.getNiceRate(Integer.MAX_VALUE, 10, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testRightUnderRatePerSecond() {
		Assert.assertEquals("999#/second", ApexLogHelper.getNiceRate(999, 1000, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testZeroTime() {
		Assert.assertEquals("999#/0SECONDS", ApexLogHelper.getNiceRate(999, 0, TimeUnit.SECONDS).toString());
	}

	@Test
	public void testPercentageNoDecimals() {
		Assert.assertEquals("100370%", ApexLogHelper.getNicePercentage(123456, 123).toString());
	}

	@Test
	public void testPercentage() {
		Assert.assertEquals("100370%", ApexLogHelper.getNicePercentage(123456, 123).toString());

		Assert.assertEquals("0.09%", ApexLogHelper.getNicePercentage(123, 123456).toString());
	}

	@Test
	public void testPercentage2() {
		Assert.assertEquals("9.8%", ApexLogHelper.getNicePercentage(98, 1000).toString());
	}

}
