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
package blasd.apex.shared.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.core.io.ApexSerializationHelper;

public class TestApexSerializationHelper {
	@Test
	public void testConvertEmptyToMap() {
		String asString = "";

		Assert.assertEquals(Collections.emptyMap(), ApexSerializationHelper.convertToMap(asString));
	}

	@Test
	public void testConvertToMap() {
		String asString = "a=b,c=d";

		Assert.assertEquals(ImmutableMap.<String, String>of("a", "b", "c", "d"),
				ApexSerializationHelper.convertToMap(asString));
	}

	@Test
	public void testConvertList() {
		Assert.assertEquals(Arrays.asList("EUR", "USD"), ApexSerializationHelper.convertToList("EUR,USD"));
		Assert.assertEquals(Arrays.asList("EUR", "USD"), ApexSerializationHelper.convertToList("EUR|USD"));

		Assert.assertEquals(Sets.newHashSet("EUR", "USD"), ApexSerializationHelper.convertToSet("EUR,USD"));
		Assert.assertEquals(Sets.newHashSet("EUR", "USD"), ApexSerializationHelper.convertToSet("EUR|USD"));
	}

	@Test
	public void testConvertMapIterableToMap() {
		Assert.assertEquals("a=b|c",
				ApexSerializationHelper.convertToString(ImmutableMap.of("a", Arrays.asList("b", "c"))));
	}

	@Test
	public void testConvertMapOfObject() {
		ImmutableMap<String, LocalDate> objectMap = ImmutableMap.of("key", new LocalDate());
		Assert.assertEquals(objectMap,
				ApexSerializationHelper.convertToMap(ApexSerializationHelper.convertToString(objectMap)));
	}

	@Test
	public void testAppendLineInCSV() throws IOException {
		Path tmpFile = ApexFileHelper.createTempFile("apex.test", ".csv");

		// Delete tmp file
		tmpFile.toFile().deleteOnExit();

		ApexSerializationHelper.appendLineInCSVFile(tmpFile, Arrays.asList("col1", "col2"));

		// handle null value
		ApexSerializationHelper.appendLineInCSVFile(tmpFile, Arrays.asList("value1", null));
	}

	@Test
	public void testAppendLineInFileOutputStream() throws IOException {
		Path tmpFile = ApexFileHelper.createTempFile("apex.test", ".csv");

		// Delete tmp file
		tmpFile.toFile().deleteOnExit();

		FileOutputStream fos = new FileOutputStream(tmpFile.toFile());

		ApexSerializationHelper.appendLineInCSVFile(fos, Arrays.asList("col1", "col2"));

		// handle null value
		ApexSerializationHelper.appendLineInCSVFile(fos, Arrays.asList("value1", null));
	}

	@Test
	public void testEscapeDoubleQuotesMax() throws IOException {
		StringWriter sw = new StringWriter();

		ApexSerializationHelper.rawAppendLineInCSVFile(sw, Arrays.asList("In\"Middle", null, "\"Wrapped\""), true, 5);

		Assert.assertEquals("\"In\"\"M\";;\"Wrapp\"", sw.toString());
	}

	@Test
	public void testEscapeDoubleQuotesNoMax() throws IOException {
		StringWriter sw = new StringWriter();

		ApexSerializationHelper.rawAppendLineInCSVFile(sw,
				Arrays.asList("In\"Middle", null, "\"Wrapped\""),
				true,
				Integer.MAX_VALUE);

		Assert.assertEquals("\"In\"\"Middle\";;\"Wrapped\"", sw.toString());
	}
}
