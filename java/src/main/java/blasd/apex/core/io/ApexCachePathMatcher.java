package blasd.apex.core.io;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enable caching of regex resolution against a Path. It is especially useful when a directory with many files is
 * checked very regularly
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexCachePathMatcher implements PathMatcher {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexCachePathMatcher.class);

	// We use String as cacheKey to prevent maintaining any FS object
	protected final Map<String, Boolean> alreadyLogged = new ConcurrentHashMap<>();

	protected final PathMatcher decorated;
	protected final String pattern;

	public ApexCachePathMatcher(PathMatcher decorated, String pattern) {
		this.decorated = decorated;
		this.pattern = pattern;
	}

	@Override
	public boolean matches(Path path) {
		String cacheKey = path.toString();

		boolean match = alreadyLogged.computeIfAbsent(cacheKey, key -> {
			boolean matches = decorated.matches(path);

			// Prevent logging too often: we log in debug only if adding in the cache
			LOGGER.debug("PathMatcher {} on {} returned {}", pattern, path, matches);

			return matches;
		});

		// Log in trace anyway (it will log twice on the first encounter)
		LOGGER.trace("PathMatcher {} on {} returned {}", pattern, path, match);

		return match;
	}

	@Override
	public String toString() {
		return pattern;
	}
}
