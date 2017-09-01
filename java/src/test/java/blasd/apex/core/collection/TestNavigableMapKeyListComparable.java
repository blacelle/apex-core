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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.collection.NavigableMapListValueComparator;

public class TestNavigableMapKeyListComparable {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Comparator<NavigableMap> makeComparator() {
		return (Comparator) new NavigableMapListValueComparator();
	}

	@Test
	public void testNavigableMapListValueComparatorDepth1() {
		@SuppressWarnings("rawtypes")
		Comparator<NavigableMap> c = makeComparator();

		NavigableMap<String, List<String>> aa = new TreeMap<>();
		{
			aa.put("a", Arrays.asList("a"));
		}

		Assert.assertEquals(0, c.compare(aa, aa));

		NavigableMap<String, List<String>> zz = new TreeMap<>();
		{
			zz.put("z", Arrays.asList("z"));
		}

		Assert.assertTrue(-1 >= c.compare(aa, zz));
		Assert.assertTrue(1 <= c.compare(zz, aa));

		NavigableMap<String, List<String>> az = new TreeMap<>();
		{
			az.put("a", Arrays.asList("z"));
		}

		Assert.assertTrue(-1 >= c.compare(aa, az));
		Assert.assertTrue(1 <= c.compare(az, aa));

		NavigableMap<String, List<String>> za = new TreeMap<>();
		{
			za.put("z", Arrays.asList("a"));
		}

		Assert.assertTrue(-1 >= c.compare(az, za));
		Assert.assertTrue(1 <= c.compare(za, az));
	}

	@Test
	public void testNavigableMapListValueComparatorDepth2() {
		@SuppressWarnings("rawtypes")
		Comparator<NavigableMap> c = makeComparator();

		NavigableMap<String, List<String>> a_ab = new TreeMap<>();
		{
			a_ab.put("a", Arrays.asList("a", "b"));
		}

		Assert.assertEquals(0, c.compare(a_ab, a_ab));

		NavigableMap<String, List<String>> z_ac = new TreeMap<>();
		{
			z_ac.put("z", Arrays.asList("a", "c"));
		}

		Assert.assertTrue(-1 >= c.compare(a_ab, z_ac));
		Assert.assertTrue(1 <= c.compare(z_ac, a_ab));

		NavigableMap<String, List<String>> a_ac = new TreeMap<>();
		{
			a_ac.put("a", Arrays.asList("a", "c"));
		}

		Assert.assertTrue(-1 >= c.compare(a_ab, a_ac));
		Assert.assertTrue(1 <= c.compare(a_ac, a_ab));

		NavigableMap<String, List<String>> z_ab = new TreeMap<>();
		{
			z_ab.put("z", Arrays.asList("a", "b"));
		}

		Assert.assertTrue(-1 >= c.compare(a_ac, z_ab));
		Assert.assertTrue(1 <= c.compare(z_ab, a_ac));
	}

	@Test
	public void testNavigableMapListValueComparatorDepth1And2() {
		@SuppressWarnings("rawtypes")
		Comparator<NavigableMap> c = makeComparator();

		NavigableMap<String, List<String>> a_a = new TreeMap<>();
		{
			a_a.put("a", Arrays.asList("a"));
		}

		Assert.assertEquals(0, c.compare(a_a, a_a));

		NavigableMap<String, List<String>> z_ac = new TreeMap<>();
		{
			z_ac.put("z", Arrays.asList("a", "c"));
		}

		Assert.assertTrue(-1 >= c.compare(a_a, z_ac));
		Assert.assertTrue(1 <= c.compare(z_ac, a_a));

		NavigableMap<String, List<String>> a_ac = new TreeMap<>();
		{
			a_ac.put("a", Arrays.asList("a", "c"));
		}

		Assert.assertTrue(-1 >= c.compare(a_a, a_ac));
		Assert.assertTrue(1 <= c.compare(a_ac, a_a));

		NavigableMap<String, List<String>> z_b = new TreeMap<>();
		{
			z_b.put("z", Arrays.asList("b"));
		}

		Assert.assertTrue(-1 >= c.compare(a_ac, z_b));
		Assert.assertTrue(1 <= c.compare(z_b, a_ac));
	}

	@Test
	public void testNavigableMapListKey() {
		@SuppressWarnings("rawtypes")
		Comparator<NavigableMap> c = makeComparator();

		NavigableMap<String, List<String>> a_a = new TreeMap<>();
		{
			a_a.put("a", Arrays.asList("a"));
		}

		Assert.assertEquals(0, c.compare(a_a, a_a));

		NavigableMap<String, List<String>> a_a_b_a = new TreeMap<>();
		{
			a_a_b_a.put("a", Arrays.asList("a"));
			a_a_b_a.put("b", Arrays.asList("a"));
		}

		Assert.assertTrue(-1 >= c.compare(a_a, a_a_b_a));
		Assert.assertTrue(1 <= c.compare(a_a_b_a, a_a));
	}
}
