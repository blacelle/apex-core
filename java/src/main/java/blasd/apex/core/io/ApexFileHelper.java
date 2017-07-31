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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.jar.JarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import com.google.common.annotations.Beta;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

/**
 * Various utility methods related to files
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexFileHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexFileHelper.class);

	static {
		try {
			initParentTmpFolder();
		} catch (IOException e) {
			LOGGER.error("Failure while creating a top tmp folder", e);
			applicationTemporaryFolder = null;
		}
	}

	private static Path applicationTemporaryFolder;

	private static synchronized void initParentTmpFolder() throws IOException {
		applicationTemporaryFolder = Files.createTempDirectory("apex");
	}

	protected ApexFileHelper() {
		// hidden
	}

	/**
	 * 
	 * @param resource
	 *            a resource to be made available as a Path
	 * @return the path to the input resource if already on disk, else a tmp file where the resource content has been
	 *         copied
	 */
	public static Path getResourceAsPath(Resource resource) {
		try {
			if (resource.exists()) {
				try {
					return resource.getFile().toPath();
				} catch (FileNotFoundException e) {
					// This tmp file will be removed on JVM shutdown
					Path tmpFile = ApexFileHelper.createTempFile("ApexFileHelper", resource.getFilename());

					byte[] from = ByteStreams.toByteArray(resource.getInputStream());

					Files.write(tmpFile, from);

					return tmpFile;
				}
			} else {
				// Let it fails as it does not exist anyway
				return resource.getFile().toPath();
			}
		} catch (IOException e) {
			// Do not throw the explicit IOException so this method can be used for field definition in tests
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @param path
	 *            a path like '/folder/file' in folder in a folder in the classpath
	 * @return
	 */
	public static URL getResourceURL(String path) {
		try {
			return new ClassPathResource(path).getURL();
		} catch (IOException e) {
			// Do not throw the explicit IOException so this method can be used for field definition in tests
			throw new RuntimeException(e);
		}
	}

	// A shortcut to workaround several classes named Files
	@Beta
	public static Path createTempFile(String prefix, String suffix) throws IOException {
		return Files.createTempFile(prefix, suffix);
	}

	/**
	 * @return a path to a non-existing but unique temporary file
	 * @throws IOException
	 */
	// http://stackoverflow.com/questions/1293655/how-to-create-tmp-file-name-with-out-creating-file
	public static Path createTempPath(String prefix, String suffix) throws IOException {
		if (applicationTemporaryFolder == null) {
			initParentTmpFolder();
		}

		String fileName = prefix + UUID.randomUUID() + suffix;

		Path tmpFilePath = applicationTemporaryFolder.resolve(fileName);

		if (tmpFilePath.toFile().exists()) {
			throw new IllegalStateException("We failed creating a non-existing tmp file");
		}

		return tmpFilePath;
	}

	/**
	 * 
	 * @param prefix
	 * @param suffix
	 * @return a Path to a non-existing file in a tmp folder
	 * @throws IOException
	 */
	public static Path createTestPath(String prefix, String suffix) throws IOException {
		// By default, we want to remove tmp path on JVM exit
		return createTestPath(prefix, suffix, true);
	}

	public static Path createTestPath(String prefix, String suffix, boolean deleteOnExit) throws IOException {
		Path testPath = createTempPath(prefix, suffix);

		if (deleteOnExit) {
			testPath.toFile().deleteOnExit();
		}

		return testPath;
	}

	public static String cleanWhitespaces(String mdx) {
		if (Strings.isNullOrEmpty(mdx)) {
			return "";
		} else {
			return mdx.replaceAll("\\s+", " ");
		}
	}

	// Useful to expand a .jar holding .csv files while these are expected to be available as File
	@Beta
	public static void expandJarToDisk(Path jarPath, Path targetPath) throws IOException {
		// https://stackoverflow.com/questions/1529611/how-to-write-a-java-program-which-can-extract-a-jar-file-and-store-its-data-in-s
		try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarPath.toFile())) {
			java.util.Enumeration<JarEntry> enumEntries = jar.entries();

			File destDir = targetPath.toFile();
			destDir.mkdirs();

			while (enumEntries.hasMoreElements()) {
				JarEntry file = enumEntries.nextElement();
				Path diskPath = targetPath.resolve(file.getName());
				if (file.isDirectory()) {
					// if its a directory, create it
					if (!diskPath.toFile().mkdir()) {
						throw new IllegalStateException("Failed creating " + file);
					}
				} else {
					// We may receive files without their parent directories
					diskPath.getParent().toFile().mkdirs();

					// Copy the file content to disk
					try (java.io.InputStream is = jar.getInputStream(file)) {
						Files.copy(is, diskPath);
					}
				}
			}
		}
	}

	public static Optional<Path> getHoldingJarPath(URI resource) throws IOException {
		// "jar:file:/C:/HOMEWARE/ITEC-Toolbox/apps/jdk/sunjdk180_112_x64/jre/lib/rt.jar!/java/lang/String.class";
		if (resource.getScheme().toLowerCase().equals("jar")) {
			String path = resource.getRawSchemeSpecificPart();

			int indexOfInsideJar = path.indexOf("!/");
			DefaultResourceLoader defaultResourceLoader = new DefaultResourceLoader();
			if (indexOfInsideJar == -1) {
				return Optional.of(defaultResourceLoader.getResource(resource.toString()).getFile().toPath());
			} else {
				String jarOnly = path.substring(0, indexOfInsideJar);
				return Optional.of(defaultResourceLoader.getResource(jarOnly).getFile().toPath());
			}
		} else {
			return Optional.empty();
		}
	}
}
