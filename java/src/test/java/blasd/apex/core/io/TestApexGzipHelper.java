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
package blasd.apex.core.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

public class TestApexGzipHelper {
	@Test
	public void test_ctor() {
		Assert.assertNotNull(new ApexGzipHelper());
	}

	@Test
	public void testCompressedBackAndForth() throws IOException {
		String string = "someString";

		byte[] compressed = ApexGzipHelper.deflate(string);
		byte[] notCompressed = string.getBytes(StandardCharsets.UTF_8);

		Assert.assertTrue(ApexGzipHelper.isGZIPStream(compressed));
		Assert.assertFalse(ApexGzipHelper.isGZIPStream(notCompressed));

		Assert.assertEquals(string, ApexGzipHelper.inflate(compressed));

		// optCompressed handles both compressed and not-compressed data
		Assert.assertEquals(string, ApexGzipHelper.toStringOptCompressed(compressed));
		Assert.assertEquals(string, ApexGzipHelper.toStringOptCompressed(notCompressed));
	}

	@Test
	public void testIsGzipButTooSmall() throws IOException {
		Assert.assertFalse(ApexGzipHelper.isGZIPStream(new byte[0]));
		Assert.assertFalse(ApexGzipHelper.isGZIPStream(new byte[1]));
	}

	@Test
	public void testPackToZip() throws IOException {
		Path folder = ApexFileHelper.createTempPath("TestApexGzipHelper", "testPackToZip", true);

		folder.toFile().mkdirs();

		Files.touch(folder.resolve("subFile").toFile());
		folder.resolve("subFolder").toFile().mkdirs();
		Files.touch(folder.resolve("subFolder/subSubFile").toFile());

		Path targetZip = ApexFileHelper.createTempPath("TestApexGzipHelper", ".zip", true);
		ApexGzipHelper.packToZip(folder.toFile(), targetZip.toFile());

		Assert.assertTrue(targetZip.toFile().length() > 0);
	}

	@Test
	public void testPackToGzip() throws IOException {
		Path fileToGzip = ApexFileHelper.createTempPath("TestApexGzipHelper", "testPackToGzip", true);

		Files.touch(fileToGzip.toFile());

		Path targetGzip = ApexFileHelper.createTempPath("TestApexGzipHelper", ".gz", true);
		ApexGzipHelper.packToGzip(fileToGzip.toFile(), targetGzip.toFile());

		Assert.assertTrue(targetGzip.toFile().length() > 0);
	}

}
