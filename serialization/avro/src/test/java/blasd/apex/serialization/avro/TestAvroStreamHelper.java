package blasd.apex.serialization.avro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class TestAvroStreamHelper {
	@Test
	public void testToMap() {
		Schema schema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("k1", "v1", "k2", "v2", "k3", "v3"));
		IndexedRecord record = new GenericData.Record(schema);

		record.put(0, "v0");
		record.put(1, "v1");
		record.put(2, "v2");

		Map<String, ?> map = AvroStreamHelper.toMap(Collections.emptyMap(), record);

		Assert.assertEquals(ImmutableMap.of("k1", "v0", "k2", "v1", "k3", "v2"), map);

		// Ensure we maintained the original ordering
		Assert.assertEquals(Arrays.asList("k1", "k2", "k3"), new ArrayList<>(map.keySet()));
	}
}
