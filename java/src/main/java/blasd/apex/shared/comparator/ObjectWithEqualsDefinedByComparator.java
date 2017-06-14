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
package blasd.apex.shared.comparator;

import java.util.Comparator;

/**
 * @see ApexDimensionHealthChecker
 * @author Benoit Lacelle
 * 
 */
public class ObjectWithEqualsDefinedByComparator {
	public Object underlying;
	@SuppressWarnings("rawtypes")
	public Comparator comparator;

	public ObjectWithEqualsDefinedByComparator(Object underlying, Comparator<?> comparator) {
		this.underlying = underlying;
		this.comparator = comparator;
	}

	@Override
	public int hashCode() {
		return underlying.hashCode();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ObjectWithEqualsDefinedByComparator) {
			if (comparator != ((ObjectWithEqualsDefinedByComparator) obj).comparator) {
				throw new RuntimeException("Different comparators");
			}

			return 0 == comparator.compare(this.underlying, ((ObjectWithEqualsDefinedByComparator) obj).underlying);
		} else {
			return 0 == comparator.compare(this.underlying, obj);
		}
	}

	@Override
	public String toString() {
		return underlying.toString();
	}
}
