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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Comparator} which compare first the keyset, then the values
 * 
 * @author Benoit Lacelle
 * 
 */
public class NavigableMapComparator implements Comparator<NavigableMap<String, String>>, Serializable {
	// http://findbugs.sourceforge.net/bugDescriptions.html#SE_COMPARATOR_SHOULD_BE_SERIALIZABLE
	private static final long serialVersionUID = 7928339315645573854L;

	protected static final Logger LOGGER = LoggerFactory.getLogger(NavigableMapComparator.class);

	@Override
	public int compare(NavigableMap<String, String> o1, NavigableMap<String, String> o2) {
		Iterator<Entry<String, String>> itKey1 = o1.entrySet().iterator();
		Iterator<Entry<String, String>> itKey2 = o2.entrySet().iterator();

		while (itKey1.hasNext() && itKey2.hasNext()) {
			Entry<String, String> next1 = itKey1.next();
			Entry<String, String> next2 = itKey2.next();

			int keyCompare = next1.getKey().compareTo(next2.getKey());

			if (keyCompare != 0) {
				// key1 is before key2: then map1 is before map2
				return keyCompare;
			}
		}

		if (itKey1.hasNext()) {
			// then it2 does not have next: it2 is bigger: it1 is smaller
			return -1;
		} else if (itKey2.hasNext()) {
			// then it1 does not have next: it1 is bigger: it2 is smaller
			return 1;
		} else {
			// both it1 and it2 does not have next: then, their keys are equal:
			// do the same for values

			Iterator<Entry<String, String>> itValue1 = o1.entrySet().iterator();
			Iterator<Entry<String, String>> itValue2 = o2.entrySet().iterator();

			while (itValue1.hasNext() && itValue2.hasNext()) {
				Entry<String, String> next1 = itValue1.next();
				Entry<String, String> next2 = itValue2.next();

				int valueCompare = next1.getValue().compareTo(next2.getValue());

				if (valueCompare != 0) {
					return valueCompare;
				}
			}

			// All keys equals and all values equals: Map are equals
			assert o1.equals(o2);

			return 0;
		}
	}
}
