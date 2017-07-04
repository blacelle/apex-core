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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.io.ApexFileHelper;

public class TestApexFileHelper {
	@Test
	public void testCreateTempPath() throws IOException {
		Path tmpFile = ApexFileHelper.createTempPath("apex.test", ".csv");

		// Check the path does not exist
		Assert.assertFalse(tmpFile.toFile().exists());
	}

	@Test
	public void testNoNewLine() {
		Assert.assertEquals("A B C D", ApexFileHelper.cleanWhitespaces("A\tB  C\rD"));
	}

	@Test
	public void testExpandJarToDisk() throws IOException {
		// Choose a class in a small jar so the test remains fast
		String pathToResourceInJar = "/org/slf4j/Logger.class";
		URL resource = ApexFileHelper.getResourceURL(pathToResourceInJar);

		Path jarPath = ApexFileHelper.getHoldingJarPath(resource).get();

		Path tmpPath = ApexFileHelper.createTempPath("apex", "testExpandJarToDisk");
		ApexFileHelper.expandJarToDisk(jarPath, tmpPath);

		Assert.assertTrue(new File(tmpPath.toFile(), pathToResourceInJar).exists());
	}
}
