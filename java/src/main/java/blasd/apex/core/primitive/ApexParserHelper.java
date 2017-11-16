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
package blasd.apex.core.primitive;

import com.google.common.base.CharMatcher;

import blasd.apex.server.loading.csv.Jdk9CharSequenceParsers;

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
	public static synchronized void initialize() {
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

		int first = 0;
		int last = s.length();

		if (PLUS_MATCHER.matches(s.charAt(0))) {
			// s = s.subSequence(1, s.length());
			first = 1;
		}

		int exp = 0;
		int e = E_UPPER_MATCHER.indexIn(s);
		if (e < 0) {
			e = E_LOWER_MATCHER.indexIn(s);
		}
		if (e >= 0) {
			// CharSequence ss = "0";// s.subSequence(e + 1, s.length());
			if (PLUS_MATCHER.matches(s.charAt(e + 1))) {
				// ss = "0";// ss.subSequence(1, ss.length());
				exp = Jdk9CharSequenceParsers.parseShort(s, e + 2, s.length(), 10);
			} else {
				exp = Jdk9CharSequenceParsers.parseShort(s, e + 1, s.length(), 10);
			}
			last = e;
			// s = "0";// s.subSequence(0, e);
		} else {
			if (PLUS_MATCHER.lastIndexIn(s) > 0 || MINUS_MATCHER.lastIndexIn(s) > 0)
				throw new RuntimeException("Not a number");
		}

		final int n;
		{
			int p = DOT_MATCHER.indexIn(s);

			if (p >= 0) {
				while (first < p && s.charAt(first) == '0') {
					// Skip the initial 0
					first++;
				}

				if (first == p) {
					s = s.subSequence(p + 1, last);
				} else {
					s = new ConcatCharSequence(s.subSequence(first, p), s.subSequence(p + 1, last));
				}
				// Adjust with first to handle the optional initial '+'
				exp += p - 1 - first;

				n = last - first - 1;
			} else {
				n = last - first;
				exp += n - 1;
			}
		}

		final double pow10Exp = pow10[exp + 325];
		if (n > 17)
			return (Jdk9CharSequenceParsers.parseLong(s, 0, 17, 10) * pow10[309]) * pow10Exp;
		if (n < 9)
			return (Jdk9CharSequenceParsers.parseInt(s, 0, s.length(), 10) * pow10[326 - n]) * pow10Exp;

		final long asLong = Jdk9CharSequenceParsers.parseLong(s, 0, s.length(), 10);

		return (asLong * pow10[326 - n]) * pow10Exp;
	}

	public static float parseFloat(CharSequence floatAsCharSequence) {
		return (float) parseDouble(floatAsCharSequence);
	}
}
