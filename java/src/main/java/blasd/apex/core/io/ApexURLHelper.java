package blasd.apex.core.io;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import com.google.common.base.CharMatcher;

/**
 * Various utilities related to URL
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexURLHelper {
	protected ApexURLHelper() {
		// hidden
	}

	/**
	 * URL.equals can be slow and not return the expected result as it will relies on the resolution on the host given
	 * current JVM network configuration. One may prefer to rely on the actual String comparison
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	// https://stackoverflow.com/questions/3771081/proper-way-to-check-for-url-equality
	public static boolean equalsUrl(URL left, URL right) {
		if (left == null) {
			return right == null;
		} else if (right == null) {
			return left == null;
		} else {
			// none is null
			return Objects.equals(left.toExternalForm(), right.toExternalForm());
		}
	}

	private static final String DEFAULT_PROTOCOL = "http";

	/**
	 * 
	 * @param asString
	 * @return an URL associated to given String, adding the protocol "http://" by default
	 */
	public static URL toHttpURL(String asString) {
		try {
			int indexOfSemiColumn = asString.indexOf(':');

			final boolean addHttpPrefix;
			if (indexOfSemiColumn < 0) {
				addHttpPrefix = true;
			} else {
				OptionalInt notProtocolChar = asString.chars()
						.limit(indexOfSemiColumn)
						.filter(c -> !CharMatcher.javaLetter().matches((char) c))
						.findFirst();

				if (notProtocolChar.isPresent()) {
					addHttpPrefix = true;
				} else {
					addHttpPrefix = false;
				}
			}

			if (addHttpPrefix) {
				// No protocol
				return new URL(DEFAULT_PROTOCOL + "://" + asString);
			} else {
				return new URL(asString);
			}

			// if (asString.startsWith("http://") || asString.startsWith("https://")) {
			//
			// } else {
			// }
		} catch (MalformedURLException e) {
			throw new RuntimeException("Issue while converting '" + asString + "'");
		}
	}

	public static Optional<ApexHostDescriptor> getHost(URL url) {
		Objects.requireNonNull(url);

		return ApexHostDescriptor.parseHost(url.getHost());
	}

	public static Optional<ApexHostDescriptor> getHost(String string) {
		return getHost(toHttpURL(string));
	}

	/**
	 * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
	 * 
	 * @param baseUrl
	 *            the existing absolute base URL
	 * @param relUrl
	 *            the relative URL to resolve. (If it's already absolute, it will be returned)
	 * @return an absolute URL if one was able to be generated, or the empty string if not
	 */
	// see org.jsoup.helper.StringUtil
	public static String resolve(final String baseUrl, final String relUrl) {
		URL base;
		try {
			try {
				base = new URL(baseUrl);
			} catch (MalformedURLException e) {
				// the base is unsuitable, but the attribute/rel may be abs on its own, so try that
				URL abs = new URL(relUrl);
				return abs.toExternalForm();
			}
			return resolve(base, relUrl).toExternalForm();
		} catch (MalformedURLException e) {
			return "";
		}

	}

	/**
	 * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
	 * 
	 * @param base
	 *            the existing absolute base URL
	 * @param relUrl
	 *            the relative URL to resolve. (If it's already absolute, it will be returned)
	 * @return the resolved absolute URL
	 * @throws MalformedURLException
	 *             if an error occurred generating the URL
	 */
	public static URL resolve(URL base, String relUrl) throws MalformedURLException {
		// workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
		if (relUrl.startsWith("?"))
			relUrl = base.getPath() + relUrl;
		// workaround: //example.com + ./foo = //example.com/./foo, not //example.com/foo
		if (relUrl.indexOf('.') == 0 && base.getFile().indexOf('/') != 0) {
			base = new URL(base.getProtocol(), base.getHost(), base.getPort(), "/" + base.getFile());
		}
		return new URL(base, relUrl);
	}
}
