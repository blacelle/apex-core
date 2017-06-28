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
package blasd.apex.core.jmx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import blasd.apex.core.jmx.ApexJMXHelper;

public class TestApexJMXHelper {

	/**
	 * Check we split correctly the template as String
	 */
	@Test
	public void testSearchWithTemplate() {
		Assert.assertEquals(Maps.newHashMap(), ApexJMXHelper.convertToMap(ApexJMXHelper.JMX_DEFAULT_STRING));
		Assert.assertEquals(Lists.newArrayList(), ApexJMXHelper.convertToList(ApexJMXHelper.JMX_DEFAULT_STRING));
		Assert.assertEquals(Sets.newHashSet(), ApexJMXHelper.convertToSet(ApexJMXHelper.JMX_DEFAULT_STRING));

		Assert.assertEquals(Collections.singletonMap("key", "value"), ApexJMXHelper.convertToMap("key=value"));
		Assert.assertEquals(Lists.newArrayList("key", "value"), ApexJMXHelper.convertToList("key,value"));
		Assert.assertEquals(Sets.newHashSet("key", "value"), ApexJMXHelper.convertToSet("key,value"));

		Assert.assertEquals(Maps.newHashMap(), ApexJMXHelper.convertToMap(""));
		Assert.assertEquals(Lists.newArrayList(), ApexJMXHelper.convertToList(""));
		Assert.assertEquals(Sets.newHashSet(), ApexJMXHelper.convertToSet(""));
	}

	@Test
	public void testSearchWithNotTrimmed() {
		Assert.assertEquals(ImmutableMap.of("key", "value", "key2", "value"),
				ApexJMXHelper.convertToMap(" key = value , key2 = value "));
		Assert.assertEquals(Lists.newArrayList("key", "value"), ApexJMXHelper.convertToList(" key , value "));
		Assert.assertEquals(Sets.newHashSet("key", "value"), ApexJMXHelper.convertToSet(" key , value"));
	}

	@Test
	public void testConvertToJMX() {
		Assert.assertTrue(ApexJMXHelper.convertToJMXMap(ImmutableMap.of()) instanceof TreeMap<?, ?>);
		Assert.assertTrue(
				ApexJMXHelper.convertToJMXMapString(ImmutableMap.of(new Date(), 3L)) instanceof LinkedHashMap<?, ?>);
		Assert.assertTrue(ApexJMXHelper.convertToJMXSet(ImmutableSet.of(new Date(), new Date())) instanceof TreeSet<?>);
		Assert.assertTrue(
				ApexJMXHelper.convertToJMXValueOrderedMap(ImmutableMap.of(new Date(), 3L)) instanceof HashMap<?, ?>);
	}

	@Test
	public void testConvertJMXLimit() {
		// We keep negative as explicitly set to it
		Assert.assertEquals(-1, ApexJMXHelper.convertToLimit(-1));

		// Convert the default JMX int to the default limit
		Assert.assertEquals(ApexJMXHelper.DEFAULT_LIMIT, ApexJMXHelper.convertToLimit(ApexJMXHelper.JMX_DEFAULT_INT));

		// Keep positive values as they are
		Assert.assertEquals(1, ApexJMXHelper.convertToLimit(1));
	}

	@Test
	public void testConvertToMapOrdered() {
		Map<String, Long> reverse = ImmutableMap.of("A", 2L, "B", 1L);

		Map<String, Long> decreasing = ApexJMXHelper.convertToJMXValueOrderedMap(reverse);

		// Check we re-ordered by value
		Assert.assertEquals(Arrays.asList("B", "A"), Lists.newArrayList(decreasing.keySet()));
	}

	@Test
	public void testConvertToMapOrderedReversed() {
		Map<String, Long> reverse = ImmutableMap.of("A", 1L, "B", 2L);

		Map<String, Long> decreasing = ApexJMXHelper.convertToJMXValueOrderedMap(reverse, true);

		// Check we re-ordered by value
		Assert.assertEquals(Arrays.asList("B", "A"), Lists.newArrayList(decreasing.keySet()));
	}

	@Test
	public void testConvertToMapOrderedReversedListKey() {
		Map<List<String>, Long> reverse = ImmutableMap.of(ImmutableList.of("A"), 1L, ImmutableList.of("B"), 2L);

		Map<List<String>, Long> decreasing = ApexJMXHelper.convertToJMXValueOrderedMap(reverse, true);

		// Check we re-ordered by value
		Assert.assertEquals(Arrays.asList(Arrays.asList("B"), Arrays.asList("A")),
				Lists.newArrayList(decreasing.keySet()));

		// CHeckthe key has been makde JMX compatible
		Assert.assertTrue(decreasing.keySet().iterator().next() instanceof ArrayList);
	}
}
