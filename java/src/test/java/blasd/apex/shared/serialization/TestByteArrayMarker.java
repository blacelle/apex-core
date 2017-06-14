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
package blasd.apex.shared.serialization;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.shared.util.ApexSerializationHelper;

public class TestByteArrayMarker {
	@Test
	public void testSerialization() throws IOException, ClassNotFoundException {
		ByteArrayMarker marker = new ByteArrayMarker(123);

		String asString = ApexSerializationHelper.toString(marker);
		Assert.assertEquals(123L, ((ByteArrayMarker) ApexSerializationHelper.fromString(asString)).getNbBytes());
	}
}
