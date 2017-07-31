package blasd.apex.core.agent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;

public class TestApexAgentHelper {
	@Test
	public void testURISpecialCharacters() throws IOException, URISyntaxException {
		// '@' is a special characters leading to issues when converting back and forth to URL
		Path file = Files.createTempFile("TestApexAgentHelper", "special@char");

		URI asURI = file.toUri();
		URL asURL = asURI.toURL();

		File backToFile = new File(asURI);
		File backToFile2 = new File(asURI.getPath());
		File backToFile3 = new File(asURL.toURI().getPath());

		Assert.assertEquals(file, backToFile.toPath());
		Assert.assertEquals(file, backToFile2.toPath());
		Assert.assertEquals(file, backToFile3.toPath());
	}

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
		Path warFolder = Files.createTempDirectory(asJarFile.getParentFile().toPath(), "testJarInWar");
		Path warSubFolder = warFolder.resolve("subFolder");
		warSubFolder.toFile().mkdirs();

		Files.move(asJarFile.toPath(), warSubFolder.resolve(asJarFile.getName()));

		// Write the war file next to the folder holding the war content
		Path warPath = warFolder.resolve("../testJarInWar.war").normalize();
		ApexAgentHelper.pack(warFolder, warPath);

		// It would typically be 'jar:file:/home/user/app.war!/WEB-INF/lib/apex-core-agent-1.N.jar!/'
		String path = "jar:file:" + warPath + "!/subFolder/" + asJarFile.getName() + "!/";
		// File asFile = ApexAgentHelper.jarUriToFile(new URI(path));

		// Repackage jar in War as independant jar
		File asExternalJar = ApexAgentHelper.jarUriToFile(new URI(path));

		Assert.assertTrue(asExternalJar.getName().endsWith(".jar"));
		Assert.assertTrue(asExternalJar.isFile());
	}
}
