package blasd.apex.core.agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities to help working with java agents
 * 
 * @author Benoit Lacelle
 *
 */
// Some details handled in
// https://github.com/avaje-common/avaje-agentloader/blob/master/src/main/java/org/avaje/agentloader/AgentLoader.java
public class ApexAgentHelper {
	// SLF4J in not available in the Agents
	protected static final Logger LOGGER = Logger.getLogger(InstrumentationAgent.class.getName());

	protected ApexAgentHelper() {
		// hidden
	}

	public static File getHoldingJarPath(Class<?> clazz) {
		URI jarFileURI = getHoldingJarURI(clazz);

		if (jarFileURI == null) {
			LOGGER.log(Level.WARNING,
					"codeSource=null for " + clazz + " with protectedDomain=" + clazz.getProtectionDomain());
			return null;
		}

		if (!jarFileURI.getPath().toLowerCase().endsWith(".jar")) {
			throw new IllegalStateException(clazz.getName() + " should be in a jar file. Found in: " + jarFileURI);
		}

		final File path;
		try {
			path = new File(jarFileURI);

		} catch (RuntimeException e) {
			throw new RuntimeException("Issue with " + jarFileURI, e);
		}

		return path;
	}

	public static File getOrMakeHoldingJarPath(Class<?> clazz) {
		URI jarFileURI = getHoldingJarURI(clazz);

		if (jarFileURI == null) {
			return null;
		}

		if (!jarFileURI.getPath().toLowerCase().endsWith(".jar")) {
			throw new IllegalStateException(clazz.getName() + " should be in a jar file. Found in: " + jarFileURI);
		}

		return jarUriToFile(jarFileURI);
	}

	public static File jarUriToFile(URI jarFileURI) {
		if (jarFileURI.getScheme().equals("jar")) {
			// jar:file:/home/user/app.war!/WEB-INF/lib/apex-core-agent-1.N.jar!/
			if (jarFileURI.toString().endsWith("!/")) {
				// A jar inside a war
				String jarPathInWar = jarFileURI.toString().substring("jar:".length(),
						jarFileURI.toString().length() - "!/".length());

				String warPath = jarPathInWar.substring(0, jarPathInWar.indexOf("!/"));
				String pathInsideJar = jarPathInWar.substring(jarPathInWar.indexOf("!/") + "!/".length());

				try {
					// https://stackoverflow.com/questions/344920/can-i-extract-a-file-from-a-jar-that-is-3-directories-deep
					JarFile jarFile = new JarFile(warPath);

					try {
						File tmpFile = File.createTempFile("apex-agent", ".jar");

						InputStream jarIS = jarFile.getInputStream(jarFile.getEntry(pathInsideJar));
						byteStreamsDotCopy(jarIS, new FileOutputStream(tmpFile));

						return tmpFile;
					} finally {
						jarFile.close();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				// A folder inside a jar
				throw new RuntimeException("We do not handle jar URI: " + jarFileURI);
			}
		} else {
			// A jar on the file system: OK as an agent jar
			try {
				return new File(jarFileURI);
			} catch (RuntimeException e) {
				throw new RuntimeException("Issue with " + jarFileURI, e);
			}
		}
	}

	/**
	 * Duplicated from Guava com.google.common.io.ByteStreams
	 */
	private static long byteStreamsDotCopy(InputStream from, OutputStream to) throws IOException {
		byte[] buf = new byte[8192];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1) {
				break;
			}
			to.write(buf, 0, r);
			total += r;
		}
		return total;
	}

	public static URI getHoldingJarURI(Class<?> clazz) {
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
		return jarFileURI;
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
