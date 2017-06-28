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
package blasd.apex.core.jvm;

import org.junit.Assert;
import org.junit.Test;

public class TestApexMathHelper {
	@Test
	public void testNextFloat() {
		Assert.assertEquals(1F, ApexMathHelper.nextFloat(1F), 0.000001F);
	}

	@Test
	public void testNextFloat_Nan() {
		Assert.assertTrue(Float.isNaN(ApexMathHelper.nextFloat(Float.NaN)));
	}

	@Test
	public void testNextFloat_Infinite() {
		float greaterThanInfinity = ApexMathHelper.nextFloat(Float.POSITIVE_INFINITY);
		Assert.assertTrue(Float.isInfinite(greaterThanInfinity));
		Assert.assertTrue(greaterThanInfinity > 0);
	}

	@Test
	public void testNextFloat_NegativeInfinite() {
		float greaterThanInfinity = ApexMathHelper.nextFloat(Float.NEGATIVE_INFINITY);
		Assert.assertTrue(Float.isInfinite(greaterThanInfinity));
		Assert.assertTrue(greaterThanInfinity < 0);
	}
}
