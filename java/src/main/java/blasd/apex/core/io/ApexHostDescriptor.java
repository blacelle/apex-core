package blasd.apex.core.io;

import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.base.Strings;

public class ApexHostDescriptor {
	protected final String host;
	protected final boolean hostIsIp;
	protected final boolean hostIsValid;

	protected ApexHostDescriptor(String host, boolean hostIsIp, boolean hostIsValid) {
		this.host = host;
		this.hostIsIp = hostIsIp;
		this.hostIsValid = hostIsValid;
	}

	// http://stackoverflow.com/questions/10306690/domain-name-validation-with-regex
	// public static final Pattern DOMAIN_PATTERN =
	// Pattern.compile("^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$");
	// We encounter some very long extentions like ".website", ".reviews",
	// "marketing", "properties", "international"
	private static final Pattern DOMAIN_PATTERN = Pattern.compile("^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,}$");

	// https://stackoverflow.com/questions/15875013/extract-ip-addresses-from-strings-using-regex?rq=1
	private static final Pattern IP_PATTERN = Pattern
			.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

	public static Optional<ApexHostDescriptor> parseHost(String host) {
		if (Strings.isNullOrEmpty(host)) {
			// Happens on 'mailto:' for instance
			return Optional.empty();
		}

		boolean hostIsValid = DOMAIN_PATTERN.matcher(host).matches();
		boolean hostIsIp = IP_PATTERN.matcher(host).matches();

		// https://bugs.openjdk.java.net/browse/JDK-8050208
		if (host.endsWith("#")) {
			host = host.substring(0, host.length() - 1);
		}

		String lowerCaseHost = host.toLowerCase();
		return Optional.of(new ApexHostDescriptor(lowerCaseHost, hostIsIp, hostIsValid));
	}

	public boolean getIsIP() {
		return hostIsIp;
	}

	/**
	 * 
	 * @return true if the host is a valid hostname. For instance, a host is not valid if it holds more less than 3
	 *         characters
	 */
	public boolean getIsValid() {
		return hostIsValid;
	}

	public String getHost() {
		return host;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ApexHostDescriptor other = (ApexHostDescriptor) obj;
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "ApexHostDescriptor [host=" + host + ", hostIsIp=" + hostIsIp + ", hostIsValid=" + hostIsValid + "]";
	}

}
