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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

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
		Assert.assertEquals("1#/days", ApexLogHelper.getNiceRate(10, 10, TimeUnit.DAYS).toString());
	}

	@Test
	public void testBigTimeVeryLowRate() {
		Assert.assertEquals("10#/sec", ApexLogHelper.getNiceRate(1, 100, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testBigTimeVeryLowRate1() {
		Assert.assertEquals("30#/min", ApexLogHelper.getNiceRate(5, 10 * 1000, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testBigTimeHIghRate() {
		Assert.assertEquals("2#/ms", ApexLogHelper.getNiceRate(Integer.MAX_VALUE, 10, TimeUnit.DAYS).toString());
	}

	@Test
	public void testLowTimeLowRate() {
		Assert.assertEquals("1#/ms", ApexLogHelper.getNiceRate(10, 10, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testLowTimeHighRate() {
		Assert.assertEquals("214#/ns",
				ApexLogHelper.getNiceRate(Integer.MAX_VALUE, 10, TimeUnit.MILLISECONDS).toString());
	}

	@Test
	public void testRightUnderRatePerSecond() {
		Assert.assertEquals("999#/sec", ApexLogHelper.getNiceRate(999, 1000, TimeUnit.MILLISECONDS).toString());
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

	@Test
	public void testGetNiceTimeMillis() {
		Assert.assertEquals("912ms", ApexLogHelper.getNiceTime(912).toString());
	}

	@Test
	public void testGetNiceTimeSecondsAndMillis() {
		Assert.assertEquals("9sec 600ms", ApexLogHelper.getNiceTime(9600).toString());
	}

	@Test
	public void testGetNiceTimeSecondsAndMillis_NoHundredsInMillis() {
		Assert.assertEquals("9sec 60ms", ApexLogHelper.getNiceTime(9060).toString());
	}

	@Test
	public void testGetNiceTimeMinAndSeconds() {
		Assert.assertEquals("2min 11sec", ApexLogHelper.getNiceTime(131, TimeUnit.SECONDS).toString());
	}

	@Test
	public void testGetNiceTimeRoundMinutes() {
		Assert.assertEquals("2min", ApexLogHelper.getNiceTime(120, TimeUnit.SECONDS).toString());
	}

	@Test
	public void testGetNiceTimeHoursAndMinutes() {
		Assert.assertEquals("2hours 11min", ApexLogHelper.getNiceTime(131, TimeUnit.MINUTES).toString());
	}

	@Test
	public void testGetNiceDays() {
		Assert.assertEquals("5days", ApexLogHelper.getNiceTime(5, TimeUnit.DAYS).toString());
	}

	@Test
	public void testGetNiceDaysAndHours() {
		Assert.assertEquals("4days 4hours", ApexLogHelper.getNiceTime(100, TimeUnit.HOURS).toString());
	}

	@Test
	public void testGetNiceTimeFromNanos() {
		Assert.assertEquals("1sec",
				ApexLogHelper.getNiceTime(TimeUnit.SECONDS.toNanos(1), TimeUnit.NANOSECONDS).toString());
	}

	@Test
	public void testCollectionLimit_under() {
		Assert.assertEquals("[0, 1]", ApexLogHelper.getToStringWithLimit(Arrays.asList(0, 1), 3).toString());
	}

	@Test
	public void testCollectionLimit_same() {
		Assert.assertEquals("[0, 1, 2]", ApexLogHelper.getToStringWithLimit(Arrays.asList(0, 1, 2), 3).toString());
	}

	@Test
	public void testCollectionLimit_above() {
		Assert.assertEquals("[0, 1, (3 more elements)]",
				ApexLogHelper.getToStringWithLimit(Arrays.asList(0, 1, 2, 3, 4), 2).toString());
	}

	@Test
	public void testLimitChars() {
		Assert.assertEquals("'12345...(4 more chars)'", ApexLogHelper.getFirstChars("123456789", 5).toString());
	}

	@Test
	public void testLimitChars_underlimit() {
		Assert.assertEquals("123456789", ApexLogHelper.getFirstChars("123456789", 15).toString());
	}

	@Test
	public void testSingleRow() {
		Assert.assertEquals("a b", ApexLogHelper.getSingleRow("a\rb", true).toString());
		Assert.assertEquals("a b", ApexLogHelper.getSingleRow("a\nb", true).toString());
		Assert.assertEquals("a b", ApexLogHelper.getSingleRow("a\r\nb", true).toString());

		// \n\r leads to 2 whitespaces
		Assert.assertEquals("a  b", ApexLogHelper.getSingleRow("a\n\rb", true).toString());

		Assert.assertEquals(" a b c ", ApexLogHelper.getSingleRow("\na\rb\r\nc\r", true).toString());

		Assert.assertEquals("a\\rb", ApexLogHelper.getSingleRow("a\rb", false).toString());
		Assert.assertEquals("a\\nb", ApexLogHelper.getSingleRow("a\nb", false).toString());
		Assert.assertEquals("a\\r\\nb", ApexLogHelper.getSingleRow("a\r\nb", false).toString());
	}

	@Test
	public void testObjectAndClass() {
		Assert.assertEquals("{k=v(java.lang.String), k2=2(java.lang.Long)}",
				ApexLogHelper.getObjectAndClass(ImmutableMap.of("k", "v", "k2", 2L)).toString());
	}

	@Test
	public void testObjectAndClass_recursive() {
		Map<Object, Object> map = new LinkedHashMap<>();
		Assert.assertEquals("{}", ApexLogHelper.getObjectAndClass(map).toString());

		// Add itself as value
		map.put("k", map);

		// Legimitate use-case as handle by AsbtractMap.toString()
		Assert.assertEquals("{k=(this Map)}", map.toString());
		Assert.assertEquals("{k=(this Map)}", ApexLogHelper.getObjectAndClass(map).toString());

		// Add another value
		map.put("k2", "v2");

		Assert.assertEquals("{k=(this Map), k2=v2(java.lang.String)}", ApexLogHelper.getObjectAndClass(map).toString());
	}
}
