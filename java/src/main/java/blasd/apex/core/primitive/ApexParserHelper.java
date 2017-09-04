package blasd.apex.core.primitive;

import com.google.common.base.CharMatcher;

import javolution.text.TypeFormat;

/**
 * Try to provide faster primitive faster than FLoat.parseFLoat and Double.parseDouble, even if these method are
 * intrisic methods.
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexParserHelper {
	private static double pow10[];

	private static final CharMatcher E_UPPER_MATCHER = CharMatcher.is('E').precomputed();
	private static final CharMatcher PLUS_MATCHER = CharMatcher.is('+').precomputed();
	private static final CharMatcher MINUS_MATCHER = CharMatcher.is('-').precomputed();
	private static final CharMatcher E_LOWER_MATCHER = CharMatcher.is('e').precomputed();
	private static final CharMatcher DOT_MATCHER = CharMatcher.is('.').precomputed();

	/**
	 * Initializes the cache for sin and cos and the rest.
	 */
	public static void initialize() {
		pow10 = new double[634];
		for (int i = 0; i < pow10.length; i++) {
			pow10[i] = Double.parseDouble("1.0e" + (i - 325)); // Math.pow(10.0, i-308);
		}
	}

	/**
	 * Parses a double from a CharSequence. Performance compared to intrinsic java method varies a lot: between 20%
	 * faster to 100% faster depending on the input values. The trick is to use Long.parseLong, which is extremely fast.
	 * 
	 * @param s
	 *            The string.
	 * @return The double value.
	 */
	public static double parseDouble(CharSequence s) {
		if (pow10 == null)
			initialize();
		if (s.charAt(0) == 'N' && s.charAt(1) == 'a' && s.charAt(2) == 'N')
			return Double.NaN;
		if (s.charAt(0) == '+') {
			s = s.subSequence(1, s.length());
		}
		int exp = 0;
		int e = E_UPPER_MATCHER.indexIn(s);
		if (e < 0) {
			e = E_LOWER_MATCHER.indexIn(s);
		}
		if (e >= 0) {
			CharSequence ss = s.subSequence(e + 1, s.length());
			if (ss.charAt(0) == '+')
				ss = ss.subSequence(1, ss.length());
			exp = TypeFormat.parseShort(ss);
			s = s.subSequence(0, e);
		} else {
			if (PLUS_MATCHER.lastIndexIn(s) > 0 || MINUS_MATCHER.lastIndexIn(s) > 0)
				throw new RuntimeException("Not a number");
		}

		int p = DOT_MATCHER.indexIn(s);
		int n = s.length();
		if (p >= 0) {
			s = new ConcatCharSequence(s.subSequence(0, p), s.subSequence(p + 1, s.length()));
			exp += p - 1;
			n--;
		} else {
			exp += n - 1;
		}

		final double pow10Exp = pow10[exp + 325];
		if (n > 17)
			return (TypeFormat.parseLong(s.subSequence(0, 17)) * pow10[309]) * pow10Exp;
		if (n < 9)
			return (TypeFormat.parseInt(s) * pow10[326 - n]) * pow10Exp;

		long asLong;
		if (s instanceof String) {
			asLong = Long.parseLong((String) s);
		} else {
			asLong = TypeFormat.parseLong(s);
		}

		return (asLong * pow10[326 - n]) * pow10Exp;
	}

	public static float parseFloat(CharSequence floatAsCharSequence) {
		return (float) parseDouble(floatAsCharSequence);
	}
}
