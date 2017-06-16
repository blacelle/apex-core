/**
 * The MIT License
 * Copyright (c) ${project.inceptionYear} Benoit Lacelle
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
package blasd.apex.core.collection;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;

/**
 * Various helpers for Map
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexMapHelper {
	protected ApexMapHelper() {
		// hidden
	}

	public static <K1, V, K2 extends K1> Map<K1, V> transcodeColumns(BiMap<?, ? extends K2> mapping, Map<K1, V> map) {
		return map.entrySet().stream().collect(Collectors.toMap(e -> {
			K1 newKey;

			if (mapping.containsKey(e.getKey())) {
				newKey = mapping.get(e.getKey());
			} else {
				newKey = e.getKey();
			}
			return newKey;
		}, Entry::getValue));
	}

	public static <K, V> Map<K, V> fromLists(List<? extends K> keys, List<? extends V> values) {
		throw new UnsupportedOperationException("TODO");
	}
}
