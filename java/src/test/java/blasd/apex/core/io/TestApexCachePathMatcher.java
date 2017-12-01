package blasd.apex.core.io;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestApexCachePathMatcher {
	@Test
	public void testCache() {
		PathMatcher decorated = Mockito.mock(PathMatcher.class);
		ApexCachePathMatcher cachedMatcher = new ApexCachePathMatcher(decorated, "someRegex");

		Path path = Mockito.mock(Path.class);

		// Return true then false: as we cache, we should always return true
		Mockito.when(decorated.matches(path)).thenReturn(true, false);

		// First try: we get true
		Assert.assertTrue(cachedMatcher.matches(path));

		// Second try: we get cached true
		Assert.assertTrue(cachedMatcher.matches(path));

		// Ensure underlying is actually false
		Assert.assertFalse(decorated.matches(path));
		Assert.assertTrue(cachedMatcher.matches(path));

	}
}
