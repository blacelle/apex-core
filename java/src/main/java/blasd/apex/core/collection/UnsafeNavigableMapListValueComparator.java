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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link Comparator} enable the comparison of {@link NavigableMap}
 * 
 * @author Benoit Lacelle
 * 
 */
public class UnsafeNavigableMapListValueComparator
		implements Comparator<NavigableMap<?, ? extends List<?>>>, Serializable {

	// http://findbugs.sourceforge.net/bugDescriptions.html#SE_COMPARATOR_SHOULD_BE_SERIALIZABLE
	private static final long serialVersionUID = 7928339315645573854L;

	protected static final Logger LOGGER = LoggerFactory.getLogger(NavigableMapComparator.class);

	@Override
	public int compare(NavigableMap<?, ? extends List<?>> o1, NavigableMap<?, ? extends List<?>> o2) {
		return NavigableMapListValueComparator.staticCompare(o1, o2, KEY_COMPARATOR, LIST_VALUES_COMPARATOR);
	}

	protected static final Comparator<Object> KEY_COMPARATOR = new Comparator<Object>() {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public int compare(Object key, Object key2) {
			if (key == key2) {
				// Fast check
				return 0;
			} else if (key instanceof Comparable<?>) {
				if (key2 instanceof Comparable<?>) {
					return ((Comparable) key).compareTo(key2);
				} else {
					throw new RuntimeException(key2 + " is not Comparable");
				}
			} else {
				throw new RuntimeException(key + " is not Comparable");
			}
		}
	};

	protected static final Comparator<List<?>> LIST_VALUES_COMPARATOR = new Comparator<List<?>>() {

		@Override
		public int compare(List<?> key, List<?> key2) {
			int commonDepth = Math.min(key.size(), key2.size());
			for (int i = 0; i < commonDepth; i++) {
				int compareResult = KEY_COMPARATOR.compare(key.get(i), key2.get(i));

				if (compareResult != 0) {
					// [A,B,C] is before [A,D] as B is before D
					return compareResult;
				}
			}

			if (key.size() == key2.size()) {
				// Same lists
				return 0;
			} else {
				// We consider as first the shorter path ([A] is before [A,B])
				return key.size() - key2.size();
			}
		}
	};
}
