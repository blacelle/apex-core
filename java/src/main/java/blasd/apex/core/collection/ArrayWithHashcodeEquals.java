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
package blasd.apex.core.collection;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is useful when one want to consider a generic List of Object as key in a hashed structure.
 * 
 * @author Benoit Lacelle
 *
 */
public final class ArrayWithHashcodeEquals {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ArrayWithHashcodeEquals.class);

	public static final int COLLISION_COUNT_LOG = 1000000;

	// precompute the hashcode for performance consideration
	protected final int hashcode;

	protected final Object[] array;

	protected static final AtomicLong COLLISION_COUNTER = new AtomicLong();

	public ArrayWithHashcodeEquals(Object[] array) {
		// We do not copy the array, but this originating array should not be mutated
		this.array = array;

		// Accept null array
		hashcode = Objects.hash(array);
	}

	@Override
	public int hashCode() {
		return hashcode;
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
		ArrayWithHashcodeEquals other = (ArrayWithHashcodeEquals) obj;
		if (hashcode != other.hashcode) {
			return false;
		}
		if (!Arrays.equals(array, other.array)) {
			incrementCollision();
			return false;
		}
		return true;
	}

	/**
	 * It may be useful to log if we have many collisions on this data-structure
	 */
	private static void incrementCollision() {
		if (0 == COLLISION_COUNTER.incrementAndGet() % COLLISION_COUNT_LOG) {
			LOGGER.warn("{} collisions on {}", COLLISION_COUNTER, ArrayWithHashcodeEquals.class);
		}
	}

}
