package blasd.apex.core.io;

import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestApexFileHelperInSpringContext.class)
public class TestApexFileHelperInSpringContext {
	protected static final Logger LOGGER = LoggerFactory.getLogger(TestApexFileHelperInSpringContext.class);

	@Autowired
	private ResourceLoader resourceLoader;

	// Nothing is magic: a folder alone is resolved relatively to process root path
	@Test
	public void testResolveToPath_FolderInResources() {
		Path path = ApexFileHelper.resolveToPath(resourceLoader, "TEST_DATA");
		Assert.assertTrue("Failed on " + path, path.toFile().isDirectory());
	}

	@Test
	public void testResolveToPath_FolderInResources_FromProcessRoot() {
		Path path = ApexFileHelper.resolveToPath(resourceLoader, "src/test/resources/TEST_DATA");
		Assert.assertTrue("Failed on " + path, path.toFile().isDirectory());
	}

	@Test
	public void testResolveToPath_SpringClassPath_folder() {
		Path path = ApexFileHelper.resolveToPath(resourceLoader, "classpath:TEST_DATA");
		Assert.assertTrue("Failed on " + path, path.toFile().isDirectory());
	}

	@Test
	public void testResolveToPath_SpringClassPath_file() {
		Path path = ApexFileHelper.resolveToPath(resourceLoader, "classpath:TEST_DATA/empty.csv");
		Assert.assertTrue("Failed on " + path, path.toFile().isFile());
	}

	@Test
	public void testResolveToPath_SpringClassPath_PrefixSlash_folder() {
		Path path = ApexFileHelper.resolveToPath(resourceLoader, "classpath:/TEST_DATA");
		Assert.assertTrue("Failed on " + path, path.toFile().isDirectory());
	}

	@Test
	public void testResolveToPath_SpringClassPath_PrefixSlash_file() {
		Path path = ApexFileHelper.resolveToPath(resourceLoader, "classpath:/TEST_DATA/empty.csv");
		Assert.assertTrue("Failed on " + path, path.toFile().isFile());
	}
}
