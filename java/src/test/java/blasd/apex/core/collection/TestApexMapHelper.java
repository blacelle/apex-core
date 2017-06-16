package blasd.apex.core.collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableBiMap;

public class TestApexMapHelper {
	@Test
	public void testMergeOnNullValue() {
		Map<String, Object> map = new HashMap<>();
		map.put("key", null);

		Map<String, Object> newMap = ApexMapHelper.transcodeColumns(ImmutableBiMap.of("key", "newKey"), map);

		Assert.assertEquals(Collections.singletonMap("newKey", null), newMap);
	}
}
