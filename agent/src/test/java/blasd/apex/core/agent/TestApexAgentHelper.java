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
package blasd.apex.core.agent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.Files;

public class TestApexAgentHelper {

	@Test
	public void jarIsFS() throws URISyntaxException {
		// Consider a class guaranteed to be wrapped in a jar, but not as special as JRE class
		URI classUri = ApexAgentHelper.getHoldingJarURI(Test.class);

		File asFile = ApexAgentHelper.jarUriToFile(classUri);

		Assert.assertEquals(new File(classUri.getPath()), asFile);
	}

	// We test current test class: in target folder
	@Test
	public void testClassExpanded() throws URISyntaxException {
		URI classUri = ApexAgentHelper.getHoldingJarURI(TestApexAgentHelper.class);
		File asRawFile = new File(classUri.getPath());
		Assert.assertFalse(asRawFile.isFile());
		Assert.assertTrue(asRawFile.isDirectory());

		File asJarFile = ApexAgentHelper.jarUriToFile(classUri);

		Assert.assertTrue(asJarFile.isFile());
		Assert.assertTrue(asJarFile.getName().endsWith(".jar"));
	}

	@Test
	public void testJarInWar() throws URISyntaxException, IOException {
		// Current class is in a folder
		URI classUri = ApexAgentHelper.getHoldingJarURI(TestApexAgentHelper.class);

		// Package as a jar
		File asJarFile = ApexAgentHelper.jarUriToFile(classUri);

		// Move jar in folder and package folder as war
		File warFolder = Files.createTempDir();
		File warSubFolder = new File(warFolder, "subFolder");
		if (!warSubFolder.mkdirs()) {
			throw new IOException("Failure while creating folders");
		}

		Files.move(asJarFile, new File(warSubFolder, asJarFile.getName()));

		// Write the war file next to the folder holding the war content
		File warPath = new File(warFolder, "../testJarInWar.war");
		ApexAgentHelper.packToZip(warFolder, warPath);

		// We go through a normalization path else new URI will later fails under Windows OS
		String normalizedPath = warPath.toURI().normalize().getPath();

		// It would typically be 'jar:file:/home/user/app.war!/WEB-INF/lib/apex-core-agent-1.N.jar!/'
		String path = "jar:file:" + normalizedPath + "!/subFolder/" + asJarFile.getName() + "!/";
		// File asFile = ApexAgentHelper.jarUriToFile(new URI(path));

		// Repackage jar in War as independant jar
		File asExternalJar = ApexAgentHelper.jarUriToFile(new URI(path));

		Assert.assertTrue(asExternalJar.getName().endsWith(".jar"));
		Assert.assertTrue(asExternalJar.isFile());
	}
}
