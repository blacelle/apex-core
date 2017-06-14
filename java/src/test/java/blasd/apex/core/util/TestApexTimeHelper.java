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
		Assert.assertTrue(ApexTimeHelper.updateOutlierDetectorStatistics(nb,
				max,
				317,
				simpleName,
				"methodName",
				"more",
				"evenMore"));

		// We should have logged once as nb was NB_OCCURENCES_FOR_INFO
		Assert.assertEquals(1L, ApexTimeHelper.NB_LOG_FOR_OUTLIER.get());
	}
}
