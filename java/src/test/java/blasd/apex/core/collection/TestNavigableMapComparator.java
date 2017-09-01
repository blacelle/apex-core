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
package blasd.apex.core.collection;

import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.collection.NavigableMapComparator;

public class TestNavigableMapComparator {
	@Test
	public void testComparator() {
		NavigableMapComparator c = new NavigableMapComparator();

		NavigableMap<String, String> aa = new TreeMap<>();
		{
			aa.put("a", "a");
		}

		Assert.assertEquals(0, c.compare(aa, aa));

		NavigableMap<String, String> zz = new TreeMap<>();
		{
			zz.put("z", "z");
		}

		Assert.assertTrue(-1 >= c.compare(aa, zz));
		Assert.assertTrue(1 <= c.compare(zz, aa));

		NavigableMap<String, String> az = new TreeMap<>();
		{
			az.put("a", "z");
		}

		Assert.assertTrue(-1 >= c.compare(aa, az));
		Assert.assertTrue(1 <= c.compare(az, aa));

		NavigableMap<String, String> za = new TreeMap<>();
		{
			za.put("z", "a");
		}

		Assert.assertTrue(-1 >= c.compare(az, za));
		Assert.assertTrue(1 <= c.compare(za, az));
	}
}
