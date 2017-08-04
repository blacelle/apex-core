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
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/**
 * Utilities to help working with java agents
 * 
 * @author Benoit Lacelle
 *
 */
// Some details handled in
// https://github.com/avaje-common/avaje-agentloader/blob/master/src/main/java/org/avaje/agentloader/AgentLoader.java
public class ApexAgentHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexAgentHelper.class);

	protected ApexAgentHelper() {
		// hidden
	}

	public static File getHoldingJarPath(Class<?> clazz) {
		URI jarFileURI = getHoldingJarURI(clazz);

		if (jarFileURI == null) {
			LOGGER.warn("codeSource=null for {} with protectedDomain={}", clazz, clazz.getProtectionDomain());
			return null;
		}

		File asFile = fronURIToJarFile(jarFileURI);

		// Optional is not available as restricted to JRE6
		if (asFile == null) {
			throw new IllegalStateException(clazz.getName() + " should be in a jar file. Found in: " + jarFileURI);
		} else {
			return asFile;
		}
	}

	public static File fronURIToJarFile(URI jarURI) {
		if (!jarURI.getPath().toLowerCase().endsWith(".jar")) {
			return null;
		}

		final File path;
		try {
			path = new File(jarURI);

		} catch (RuntimeException e) {
			throw new RuntimeException("Issue with " + jarURI, e);
		}

		return path;
	}

	public static File getOrMakeHoldingJarPath(Class<?> clazz) {
		URI jarFileURI = getHoldingJarURI(clazz);

		if (jarFileURI == null) {
			return null;
		}

		File asFile = fronURIToJarFile(jarFileURI);

		// Optional is not available as restricted to JRE6
		if (asFile == null) {
			return jarUriToFile(jarFileURI);
		} else {
			return asFile;
		}
	}

	public static File jarUriToFile(URI jarFileURI) {
		if (jarFileURI.getScheme().equals("file")) {
			// TODO: This will not work if some characters has been encoded (e.g. path with a ' ' (-> %20), or with a
			// '@')
			// https://stackoverflow.com/questions/8885204/how-to-get-the-file-path-from-uri
			File asFile = new File(jarFileURI.getPath());

			if (asFile.isFile()) {
				// A file in the file-system: OK if it is a jar
				if (!jarFileURI.getPath().toLowerCase().endsWith(".jar")) {
					LOGGER.warn("We have a jar in a file not ending by .jar: {}", jarFileURI);
				}

				return asFile;
			} else if (asFile.isDirectory()) {
				// Else if it is a folder, need to wrap in a jar
				try {
					File tmpFile = File.createTempFile("AgentHelper", ".jar");
					packToZip(asFile, tmpFile);
					tmpFile.deleteOnExit();
					return tmpFile;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			} else {
				throw new RuntimeException("Can not handle " + jarFileURI + " which is neither a file or a directory");
			}
		} else if (jarFileURI.getScheme().equals("jar")) {
			String jarFilePath = jarFileURI.toString();

			// jar:file:/home/user/app.war!/WEB-INF/lib/apex-core-agent-1.N.jar!/
			if (jarFilePath.endsWith("!/")) {
				// A jar inside a war
				String jarPathInWar = jarFilePath.substring("jar:".length(), jarFilePath.length() - "!/".length());

				String warPath = jarPathInWar.substring(0, jarPathInWar.indexOf("!/"));
				String pathInsideJar = jarPathInWar.substring(jarPathInWar.indexOf("!/") + "!/".length());

				try {
					// https://stackoverflow.com/questions/344920/can-i-extract-a-file-from-a-jar-that-is-3-directories-deep
					String warCleanPath = new URI(warPath).getPath();
					JarFile jarFile = new JarFile(warCleanPath);

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
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			} else {
				// A folder inside a jar
				throw new RuntimeException("We do not handle jar URI: " + jarFileURI);
			}
		} else {
			throw new RuntimeException("Not handlded case: " + jarFileURI);
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

	/**
	 * We prefer returning an URI as URL .equals is not safe
	 * 
	 * @param clazz
	 * @return the URI of the .jar or folder holding this class
	 */
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
	 * @param folder
	 *            a file-system folder were to read files and folders
	 * @param zipFilePath
	 *            a file-system path where to create a new archive
	 * @throws IOException
	 */
	public static void packToZip(final File folder, final File zipFilePath) throws IOException {
		FileOutputStream fos = new FileOutputStream(zipFilePath);
		try {
			final ZipOutputStream zos = new ZipOutputStream(fos);
			try {
				// https://stackoverflow.com/questions/15968883/how-to-zip-a-folder-itself-using-java
				Iterator<File> iterator = Files.fileTreeTraverser().preOrderTraversal(folder).iterator();

				while (iterator.hasNext()) {
					File next = iterator.next();

					if (next.isDirectory()) {
						// https://stackoverflow.com/questions/204784/how-to-construct-a-relative-path-in-java-from-two-absolute-paths-or-urls
						zos.putNextEntry(new ZipEntry(folder.toURI().relativize(next.toURI()).getPath() + "/"));
						zos.closeEntry();
					} else if (next.isFile()) {
						LOGGER.debug("Adding {} in {}", next, zipFilePath);
						zos.putNextEntry(new ZipEntry(folder.toURI().relativize(next.toURI()).getPath()));
						Files.copy(next, zos);
						zos.closeEntry();
					}
				}
			} catch (IOException e) {
				// Delete this tmp file
				zipFilePath.delete();

				throw new IOException("Issue while writing in " + zipFilePath, e);
			} finally {
				zos.close();
			}
		} finally {
			fos.close();
		}
	}

	/**
	 * Gzip enable compressing a single file
	 * 
	 * @param inputPath
	 * @param zipFilePath
	 * @throws IOException
	 */
	public static void packToGzip(final File inputPath, final File zipFilePath) throws IOException {
		FileOutputStream fos = new FileOutputStream(zipFilePath);

		try {
			final GZIPOutputStream zos = new GZIPOutputStream(fos);

			try {
				Files.copy(inputPath, zos);
			} finally {
				zos.close();
			}
		} finally {
			fos.close();
		}
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
