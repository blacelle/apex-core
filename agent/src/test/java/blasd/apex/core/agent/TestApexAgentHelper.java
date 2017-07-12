package blasd.apex.core.agent;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestApexAgentHelper {
	@Test
	public void jarIsFS() throws URISyntaxException {
		String path = "file:/C:/m2repo/com/github/blasd/apex/apex-core-agent/1.N/apex-core-agent-1.N.jar";

		File asFile = ApexAgentHelper.jarUriToFile(new URI(path));

		Assert.assertEquals(new File(new URI(path).getPath()), asFile);
	}

	@Ignore("Need to make a war containing a jar")
	@Test
	public void testJarInWar() throws URISyntaxException {
		String path = "jar:file:/home/user/app.war!/WEB-INF/lib/apex-core-agent-1.N.jar!/";
		File asFile = ApexAgentHelper.jarUriToFile(new URI(path));
	}

	@Test
	public void testClassExpanded() throws URISyntaxException {
		String path = "file:/C:/workspace/apex-core/agent/target/test-classes/";
		File asFile = ApexAgentHelper.jarUriToFile(new URI(path));
	}
}
