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
package blasd.apex.core.primitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Not thread-safe, Mutable, no .hashcode/.equals subsequence, but does not allocate any memory
 * 
 * @author Benoit Lacelle
 *
 */
public class UnsafeSubSequence implements CharSequence {
	protected static final Logger LOGGER = LoggerFactory.getLogger(UnsafeSubSequence.class);

	protected final CharSequence undelrying;
	protected int start;
	protected int end;

	public UnsafeSubSequence(CharSequence undelrying) {
		this.undelrying = undelrying;
		this.start = -1;
		this.end = -1;
	}

	public UnsafeSubSequence(CharSequence undelrying, int from, int to) {
		this.undelrying = undelrying;
		this.start = from;
		this.end = to;
	}

	// Not-thread-safe
	public void resetWindow(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public boolean isValid() {
		return start >= 0 && end >= start;
	}

	@Override
	public int length() {
		// Empty String if end == start: OK
		return end - start;
	}

	@Override
	public char charAt(int index) {
		if (index >= end) {
			throw new IndexOutOfBoundsException(index + " is above " + index + " on " + this);
		} else if (index < 0) {
			throw new IndexOutOfBoundsException(index + " is below " + 0 + " on " + this);
		} else if (start + index >= undelrying.length()) {
			throw new IndexOutOfBoundsException("start=" + start
					+ " + index="
					+ index
					+ " is above underlying length="
					+ undelrying.length()
					+ " on "
					+ this);
		}
		return undelrying.charAt(start + index);
	}

	@Override
	public CharSequence subSequence(int subStart, int subEnd) {
		if (undelrying instanceof UnsafeSubSequence) {
			UnsafeSubSequence unsafeUnderlying = (UnsafeSubSequence) undelrying;
			return new UnsafeSubSequence(unsafeUnderlying.undelrying,
					unsafeUnderlying.start + subStart,
					unsafeUnderlying.start + subEnd);
		} else {
			return new UnsafeSubSequence(undelrying, this.start + subStart, this.start + subEnd);
		}
	}

	@Override
	public int hashCode() {
		throw new RuntimeException("UNsafe");
	}

	@Override
	public boolean equals(Object obj) {
		throw new RuntimeException("UNsafe");
	}

	@Override
	public String toString() {
		if (isValid()) {
			return undelrying.subSequence(start, end).toString();
		} else {
			LOGGER.warn("We unexpectedly used .toString on a not initialized {}", getClass());
			return this.getClass() + " Not Initialized";
		}
	}
}
