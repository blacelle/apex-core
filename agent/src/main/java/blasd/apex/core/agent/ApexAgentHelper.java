package blasd.apex.core.agent;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;

/**
 * Utilities to help working with java agents
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexAgentHelper {
	protected ApexAgentHelper() {
		// hidden
	}

	public static File getHoldingJarPath(Class<?> clazz) {
		CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();

		if (codeSource == null) {
			return null;
		}

		// URI is needed to deal with spaces and non-ASCII
		// characters
		URI jarFileURI;

		try {
			jarFileURI = codeSource.getLocation().toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		File path = new File(jarFileURI);

		if (!path.getName().toLowerCase().endsWith(".jar")) {
			throw new IllegalStateException(clazz.getName() + " should be in a jar file. Found in: " + path);
		}

		return path;
	}

	/**
	 * 
	 * @return the PID of current process
	 * @see org.springframework.boot.loader.tools.AgentAttacher.attach(File)
	 */
	public static String getPIDForAgent() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		return name.substring(0, name.indexOf('@'));
	}
}
