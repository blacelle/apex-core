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
package blasd.apex.core.collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableBiMap;

public class TestApexMapHelper {
	@Test
	public void testMergeOnNullValue() {
		Map<String, Object> map = new HashMap<>();
		map.put("key", null);

		Map<String, Object> newMap = ApexMapHelper.transcodeColumns(ImmutableBiMap.of("key", "newKey"), map);

		Assert.assertEquals(Collections.singletonMap("newKey", null), newMap);
	}

	@Test
	public void testDecoratePutAllOnNullValue() {
		Map<String, Object> first = new HashMap<>();
		first.put("key", null);

		Map<String, Object> second = new HashMap<>();
		second.put("key2", null);

		Map<String, Object> newMap = ApexMapHelper.decoratePutAll(first, second);

		Map<String, Object> merged = new HashMap<>();
		merged.put("key", null);
		merged.put("key2", null);
		Assert.assertEquals(merged, newMap);
	}
}
