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
package blasd.apex.core.util;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

public class TestApexTimeHelper {

	@Test
	public void testDetectOutlier() {
		ApexTimeHelper.NB_LOG_FOR_OUTLIER.set(0);

		AtomicLong nb = new AtomicLong();
		AtomicLong max = new AtomicLong(Long.MIN_VALUE);

		// First call: this is a max
		String simpleName = getClass().getSimpleName();
		{
			Assert.assertTrue(ApexTimeHelper.updateOutlierDetectorStatistics(nb, max, 0, simpleName, "methodName"));

			Assert.assertEquals(1L, nb.get());
			Assert.assertEquals(0L, max.get());
		}

		// Second call: 1 > 0
		{
			Assert.assertTrue(ApexTimeHelper.updateOutlierDetectorStatistics(nb, max, 1, simpleName, "methodName"));

			Assert.assertEquals(2L, nb.get());
			Assert.assertEquals(1L, max.get());
		}

		// Third call: 1 == 1
		{
			Assert.assertFalse(ApexTimeHelper.updateOutlierDetectorStatistics(nb, max, 1, simpleName, "methodName"));

			Assert.assertEquals(3L, nb.get());
			Assert.assertEquals(1L, max.get());
		}

		// Third call: 0 < 1
		{
			Assert.assertFalse(ApexTimeHelper.updateOutlierDetectorStatistics(nb, max, 0, simpleName, "methodName"));

			Assert.assertEquals(4L, nb.get());
			Assert.assertEquals(1L, max.get());
		}

		{
			Assert.assertTrue(ApexTimeHelper.updateOutlierDetectorStatistics(nb, max, 2, simpleName, "methodName"));

			Assert.assertEquals(5L, nb.get());
			Assert.assertEquals(2L, max.get());
		}

		// We should not have logged as not enough occurrences
		Assert.assertEquals(0L, ApexTimeHelper.NB_LOG_FOR_OUTLIER.get());
	}

	@Test
	public void testDetectOutlierMoreInfos() {
		ApexTimeHelper.NB_LOG_FOR_OUTLIER.set(0);

		AtomicLong nb = new AtomicLong(ApexTimeHelper.NB_OCCURENCES_FOR_INFO);
		AtomicLong max = new AtomicLong(128);

		String simpleName = getClass().getSimpleName();

		// First call: this is a max
		Assert.assertTrue(ApexTimeHelper
				.updateOutlierDetectorStatistics(nb, max, 317, simpleName, "methodName", "more", "evenMore"));

		// We should have logged once as nb was NB_OCCURENCES_FOR_INFO
		Assert.assertEquals(1L, ApexTimeHelper.NB_LOG_FOR_OUTLIER.get());
	}
}
