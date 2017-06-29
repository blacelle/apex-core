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
package blasd.apex.core.logging;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import blasd.apex.core.memory.IApexMemoryConstants;

/**
 * Various helpers for logging
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexLogHelper {
	private static final long HUNDRED = 100L;

	/**
	 * We want to see at least 2 digits: after printing 9999B, we print 10KB
	 */
	protected static final int BARRIER_FOR_SIZE_IN_LOG = 10;

	public static final int THOUSAND = 1000;
	public static final int TEN_F = 10;

	private static final long LIMIT_ENTRY_PER_XXX = 10;

	protected ApexLogHelper() {
		// hidden
	}

	/**
	 * This methods facilitates using logging framework at trace level, without using .isTraceEnabled, as the .toString
	 * 
	 * @param toStringMe
	 *            the object from which a .toString should be computed lazily
	 * @return an Object which has as .toString the one provided by the Supplier
	 */
	public static Object lazyToString(Supplier<String> toStringMe) {
		return new Object() {
			@Override
			public String toString() {
				return toStringMe.get();
			}
		};
	}

	public static Object getNicePercentage(long progress, long max) {
		return lazyToString(() -> {
			if (progress < 0L || max <= 0L) {
				return "-%";
			} else {
				long ratio = progress * HUNDRED / max;

				if (ratio == 0) {
					long smallRatio = progress * THOUSAND / max;

					if (smallRatio == 0) {
						long verySmallRatio = progress * HUNDRED * HUNDRED / max;
						return "0.0" + Long.toString(verySmallRatio) + "%";
					} else {
						return "0." + Long.toString(smallRatio) + "%";
					}
				} else if (ratio < TEN_F) {
					long smallRatio = (progress - ratio * TEN_F) * THOUSAND / max;

					// We prefer having at least 2 digits
					return ratio + "." + Long.toString(smallRatio) + "%";
				} else {
					return Long.toString(ratio) + "%";
				}
			}
		});
	}

	public static Object getNiceMemory(long size) {
		return lazyToString(() -> {
			long absSize = Math.abs(size);
			if (absSize < BARRIER_FOR_SIZE_IN_LOG * IApexMemoryConstants.KB) {
				return size + "B";
			} else if (absSize < BARRIER_FOR_SIZE_IN_LOG * IApexMemoryConstants.MB) {
				return (size / IApexMemoryConstants.KB) + "KB";
			} else if (absSize < BARRIER_FOR_SIZE_IN_LOG * IApexMemoryConstants.GB) {
				return (size / IApexMemoryConstants.MB) + "MB";
			} else if (absSize < BARRIER_FOR_SIZE_IN_LOG * IApexMemoryConstants.TB) {
				return (size / IApexMemoryConstants.GB) + "GB";
			} else {
				return (size / IApexMemoryConstants.TB) + "TB";
			}
		});
	}

	public static Object getObjectAndClass(Object o) {
		return lazyToString(() -> {
			if (o == null) {
				return null + "(null)";
			} else {
				return o.toString() + "(" + o.getClass().getName() + ")";
			}
		});
	}

	public static Object getNiceDouble(Double value) {
		if (value == null) {
			return "null";
		} else {
			return lazyToString(() -> {
				final String pattern = ".##";
				// final String pattern = "###,###,##0.00";
				final DecimalFormat myFormatter = new DecimalFormat(pattern);

				return myFormatter.format(value);
			});
		}
	}

	public static Object getNiceTime(long timeInMs) {
		return getNiceTime(timeInMs, TimeUnit.MILLISECONDS);
	}

	private static final String HOURS_PREFIX = "hours";
	private static final String MINUTES_PREFIX = "min";
	private static final String SECONDS_PREFIX = "sec";
	private static final String MILLIS_PREFIX = "ms";

	public static Object getNiceTime(long time, TimeUnit timeUnit) {
		return ApexLogHelper.lazyToString(() -> {
			long timeInMs = timeUnit.toMillis(time);

			if (timeInMs >= TimeUnit.HOURS.toMillis(1)) {
				String minString = TimeUnit.MILLISECONDS.toHours(timeInMs) + HOURS_PREFIX;

				long minutes = timeInMs - TimeUnit.HOURS.toMillis(1) * TimeUnit.MILLISECONDS.toHours(timeInMs);

				if (minutes > 0) {
					return minString + " " + TimeUnit.MILLISECONDS.toMinutes(minutes) + MINUTES_PREFIX;
				} else {
					return minString;
				}
			} else if (timeInMs >= TimeUnit.MINUTES.toMillis(1)) {
				String minString = TimeUnit.MILLISECONDS.toMinutes(timeInMs) + MINUTES_PREFIX;

				long seconds = timeInMs - TimeUnit.MINUTES.toMillis(1) * TimeUnit.MILLISECONDS.toMinutes(timeInMs);

				if (seconds > 0) {
					return minString + " " + TimeUnit.MILLISECONDS.toSeconds(seconds) + SECONDS_PREFIX;
				} else {
					return minString;
				}

			} else if (timeInMs >= TimeUnit.SECONDS.toMillis(1)) {
				String minString = TimeUnit.MILLISECONDS.toSeconds(timeInMs) + SECONDS_PREFIX;

				long millis = timeInMs - TimeUnit.SECONDS.toMillis(1) * TimeUnit.MILLISECONDS.toSeconds(timeInMs);

				if (millis > 0) {
					return minString + " " + millis + MILLIS_PREFIX;
				} else {
					return minString;
				}
			} else {
				return timeInMs + MILLIS_PREFIX;
			}
		});
	}

	public static Object getNiceRate(long nbEntries, long time, TimeUnit timeUnit) {
		return lazyToString(() -> {
			if (time <= 0) {
				// Edge case
				return nbEntries + "#/0" + timeUnit;
			}

			// We prefer rate per second, without any /
			long entryPerSecond = TimeUnit.SECONDS.convert(nbEntries / time, timeUnit);

			if (entryPerSecond > LIMIT_ENTRY_PER_XXX) {
				// if rate is too high, we switch to rate per minute
				long entryPerMinute = TimeUnit.MINUTES.convert(nbEntries / time, timeUnit);

				if (entryPerMinute > LIMIT_ENTRY_PER_XXX) {
					// Rate per minute is high enough: stick to it
					return entryPerMinute + "#/minute";
				} else {
					// Rate per second is nice
					return entryPerSecond + "#/second";
				}
			} else {
				long nbSeconds = TimeUnit.SECONDS.convert(time, timeUnit);

				if (nbSeconds > 0) {
					// The window is 1 second wide
					long entryPerSecond2 = nbEntries / nbSeconds;

					return entryPerSecond2 + "#/second";
				} else {
					// The window is not 1 second wide: it can safely be turned to nanos
					long entryPerNano = TimeUnit.NANOSECONDS.convert(nbEntries, timeUnit) / time;

					long entryPerSecond2 = TimeUnit.SECONDS.convert(entryPerNano, TimeUnit.NANOSECONDS);

					if (entryPerSecond2 > 0) {
						// Rate per second is nice
						return entryPerSecond2 + "#/second";
					} else {
						long entryPerMillis = TimeUnit.MILLISECONDS.convert(entryPerNano, TimeUnit.NANOSECONDS);
						return entryPerMillis + "#/ms";
					}
				}
				//
				// // Rate per second is too low: switch to rate per millis
				// long entryPerMillis = nbEntries / TimeUnit.MILLISECONDS.convert(time, timeUnit);
				//
				// if (entryPerMillis >= 0) {
				// return entryPerMillis + "#/ms";
				// } else {
				//
				// }
			}
		});
	}
}
