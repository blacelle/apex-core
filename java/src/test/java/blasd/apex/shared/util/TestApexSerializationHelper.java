/**
 * The MIT License
 * Copyright (c) ${project.inceptionYear} Benoit Lacelle
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

		Assert.assertEquals(ImmutableMap.of("a", "b", "c", "d"), ApexSerializationHelper.convertToMap(asString));
	}

	@Test
	public void testConvertToMapInvalidSeparator() {
		String asString = "a=b;c=d";

		Assert.assertEquals(ImmutableMap.of("a", "b", "c", "d"), ApexSerializationHelper.convertToMap(asString));
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
