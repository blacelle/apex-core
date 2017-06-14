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

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.collection.ArrayWithHashcodeEquals;

public class TestArrayWithHashcodeEquals {
	@Test
	public void testHashCodeEquals() {
		ArrayWithHashcodeEquals first = new ArrayWithHashcodeEquals(new Object[] { "a", "b" });
		ArrayWithHashcodeEquals second = new ArrayWithHashcodeEquals(new Object[] { new String("a"), "b" });

		// NotSame
		Assert.assertNotSame(first, second);
		Assert.assertNotSame(first.array, second.array);
		Assert.assertNotSame(first.array[0], second.array[0]);

		// But equals
		Assert.assertEquals(first.hashCode(), second.hashCode());
		Assert.assertEquals(first, second);
	}

}
