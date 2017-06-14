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
package blasd.apex.shared.zip;

import org.junit.Assert;
import org.junit.Test;

public class TestGZipStringBuilder {
	@Test
	public void testAppend() {
		GZipStringBuilder sb = new GZipStringBuilder();

		sb.append("Azaz");
		sb.append(new StringBuilder("Zeze"));

		Assert.assertEquals("AzazZeze", sb.toString());

		sb.clear();
		Assert.assertEquals("", sb.toString());
	}

	@Test
	public void testAppendSub() {
		GZipStringBuilder sb = new GZipStringBuilder();

		sb.append("Azaz", 1, 3);

		Assert.assertEquals("za", sb.toString());
	}

	@Test
	public void testNull() {
		GZipStringBuilder sb = new GZipStringBuilder();

		sb.append(null);

		Assert.assertEquals("null", sb.toString());
	}

	@Test
	public void testCopyInflated() {
		GZipStringBuilder sb = new GZipStringBuilder();

		sb.append("Azaz");

		Assert.assertTrue(sb.copyInflatedByteArray().length > 0);
	}
}
