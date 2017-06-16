package blasd.apex.shared.tuple;

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
