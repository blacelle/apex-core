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
