/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.shared.file;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.jar.JarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import com.google.common.annotations.Beta;
import com.google.common.base.Strings;

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
			LOGGER.error("Failure while creating a top tmp folder");
			// Keep printStackTrace as we are in a static block: SLF4J may not be initialized yet
			e.printStackTrace();
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

	public static File getResourceFile(String path) {
		try {
			return new ClassPathResource(path).getFile();
		} catch (IOException e) {
			// Do not throw the explicit IOException so this method can be used for field definition in tests
			throw new RuntimeException(e);
		}
	}

	public static URL getResourceURL(String path) {
		try {
			return new ClassPathResource(path).getURL();
		} catch (IOException e) {
			// Do not throw the explicit IOException so this method can be used for field definition in tests
			throw new RuntimeException(e);
		}
	}

	public static Path getResourcePath(String path) {
		try {
			return new ClassPathResource(path).getFile().toPath();
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

	// Useful to expand a .jar holding .csv files to be loaded by ActivePivot CSV source
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
					diskPath.toFile().mkdir();
				} else {
					// Copy the file content to disk
					try (java.io.InputStream is = jar.getInputStream(file)) {
						Files.copy(is, diskPath);
					}
				}
			}
		}
	}
}
