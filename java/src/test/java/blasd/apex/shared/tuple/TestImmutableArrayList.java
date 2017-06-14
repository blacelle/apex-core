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
package blasd.apex.shared.tuple;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TestImmutableArrayList {
	@Test
	public void testSubSet() {
		List<?> subset = new ImmutableArrayList<>(new Object[] { 13, 27 }, new int[1]);

		Assert.assertEquals(1, subset.size());
		Assert.assertEquals(13, subset.get(0));
		Assert.assertEquals(Arrays.asList(13).hashCode(), subset.hashCode());

		// Check both equals
		Assert.assertEquals(Arrays.asList(13), subset);
		Assert.assertEquals(subset, Arrays.asList(13));
	}

	@Test
	public void testPlain() {
		List<?> subset = new ImmutableArrayList<>(new Object[] { 13, 27 });

		Assert.assertEquals(2, subset.size());
		Assert.assertEquals(13, subset.get(0));
		Assert.assertEquals(27, subset.get(1));
		Assert.assertEquals(Arrays.asList(13, 27).hashCode(), subset.hashCode());

		// Check both equals
		Assert.assertEquals(Arrays.asList(13, 27), subset);
		Assert.assertEquals(subset, Arrays.asList(13, 27));
	}
}
