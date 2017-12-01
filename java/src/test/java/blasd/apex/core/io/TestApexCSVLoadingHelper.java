package blasd.apex.core.io;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

/**
 * 
 * @author Benoit Lacelle
 * 
 */
public class TestApexCSVLoadingHelper {
	protected static final Logger LOGGER = LoggerFactory.getLogger(TestApexCSVLoadingHelper.class);

	@Test
	public void testMakePathMatcher() {
		char backSlash = '\\';

		String fileInFolder = "a" + backSlash + "file.csv";
		String fileMatcher = "a" + backSlash + "file.{csv,txt}";
		PathMatcher pathMatcher = ApexFileHelper.makePathMatcher(fileMatcher, true);

		if (SystemUtils.IS_OS_WINDOWS) {
			// Accept Absolute path
			Assert.assertTrue(pathMatcher.matches(Paths.get("C:", "root", fileInFolder)));

			// Reject .csv.bak
			Assert.assertFalse(pathMatcher.matches(Paths.get("C:", "root", fileInFolder + ".bak")));
		} else {
			LOGGER.error("TODO Check this test under Linux");
		}
	}

	@Test
	public void testAdvancedMatch() {
		String fileMatcher = Joiner.on(File.separatorChar).join("[0-9][0-9][0-9][0-9]", "sub", "*.{csv,zip,gz}");
		String[] pathChain = { "root", "Data", "env", "2016", "sub", "name.csv" };

		Path path;
		if (SystemUtils.IS_OS_WINDOWS) {
			path = Paths.get("C:", pathChain);
		} else {
			path = Paths.get("/root", pathChain);
		}

		// Automatic handling of absolute pathes
		Assert.assertTrue(ApexFileHelper.makePathMatcher(fileMatcher, true).matches(path));

		// Path-matcher does not handle absolute pathes
		Assert.assertFalse(ApexFileHelper.makePathMatcher(fileMatcher, false).matches(path));
	}

	@Test
	public void testAdvancedMatchAlreadyAbsolute() {
		String fileMatcher = Joiner.on(File.separatorChar).join("**", "[0-9][0-9][0-9][0-9]", "sub", "*.{csv,zip,gz}");
		String[] pathChain = { "root", "Data", "env", "2016", "sub", "name.csv" };

		Path path;
		if (SystemUtils.IS_OS_WINDOWS) {
			path = Paths.get("C:", pathChain);
		} else {
			path = Paths.get("/root", pathChain);
		}

		Assert.assertTrue(ApexFileHelper.makePathMatcher(fileMatcher, false).matches(path));
		Assert.assertTrue(ApexFileHelper.makePathMatcher(fileMatcher, true).matches(path));
	}

	@Test
	public void testAdvancedMatchAlreadyAbsoluteWindowsMatcher() {
		// We force the use of '\' as separator in matcher
		String fileMatcher = Joiner.on('\\').join("**", "[0-9][0-9][0-9][0-9]", "sub", "*.{csv,zip,gz}");
		String[] pathChain = { "root", "Data", "env", "2016", "sub", "name.csv" };

		Path path;
		if (SystemUtils.IS_OS_WINDOWS) {
			path = Paths.get("C:", pathChain);
		} else {
			path = Paths.get("/root", pathChain);
		}

		Assert.assertTrue(ApexFileHelper.makePathMatcher(fileMatcher, false).matches(path));
		Assert.assertTrue(ApexFileHelper.makePathMatcher(fileMatcher, true).matches(path));
	}

	@Test
	public void testMatchAlreadyGlobbed() {
		String fileMatcher = "glob:*.{csv,zip,gz}";
		Path path = Paths.get("name.csv");

		PathMatcher pathMatcher = ApexFileHelper.makePathMatcher(fileMatcher, false);

		Assert.assertTrue(pathMatcher.matches(path));
	}

}
