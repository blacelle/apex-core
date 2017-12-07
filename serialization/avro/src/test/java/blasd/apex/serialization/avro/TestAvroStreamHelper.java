package blasd.apex.serialization.avro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import blasd.apex.core.thread.ApexExecutorsHelper;

public class TestAvroStreamHelper {
	@Test
	public void testToMap() {
		Schema schema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("k1", "v1", "k2", "v2", "k3", "v3"));
		IndexedRecord record = new GenericData.Record(schema);

		record.put(0, "v0");
		record.put(1, "v1");
		record.put(2, "v2");

		Map<String, ?> map = AvroStreamHelper.toJavaMap(record);

		Assert.assertEquals(ImmutableMap.of("k1", "v0", "k2", "v1", "k3", "v2"), map);

		// Ensure we maintained the original ordering
		Assert.assertEquals(Arrays.asList("k1", "k2", "k3"), new ArrayList<>(map.keySet()));
	}

	@Test
	public void testToGenericRecord_SecondMapHasMissingKey() {
		Schema schema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("k1", "v1", "k2", "v2"));

		Function<Map<String, ?>, GenericRecord> mapper = AvroStreamHelper.toGenericRecord(schema);

		GenericRecord firstRecord = mapper.apply(ImmutableMap.of("k1", "v1", "k2", "v2"));
		GenericRecord secondRecord = mapper.apply(ImmutableMap.of("k2", "v2'"));

		Assert.assertEquals("v1", firstRecord.get("k1"));
		Assert.assertEquals("v2", firstRecord.get("k2"));

		Assert.assertEquals(null, secondRecord.get("k1"));
		Assert.assertEquals("v2'", secondRecord.get("k2"));
	}

	@Test
	public void testToGenericRecord_FloatArray() {
		Schema schema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("k1", new float[] { 2F }));

		Function<Map<String, ?>, GenericRecord> mapper = AvroStreamHelper.toGenericRecord(schema);

		GenericRecord firstRecord = mapper.apply(ImmutableMap.of("k1", new float[] { 2F }));

		Assert.assertTrue(firstRecord.get("k1") instanceof byte[]);

		// No type information: keep the raw byte[]
		Map<String, ?> backToMapNoType = AvroStreamHelper.toJavaMap(firstRecord);
		Assert.assertTrue(backToMapNoType.get("k1") instanceof byte[]);

		// Exact byte[] info (float[])
		Map<String, ?> backToMapWithMap =
				AvroStreamHelper.toJavaMap(firstRecord, Collections.singletonMap("k1", new float[0]));
		Assert.assertArrayEquals(new float[] { 2F }, (float[]) backToMapWithMap.get("k1"), 0.01F);

		// Inexact byte[] info (double[]): we deserialize to float[], but should we transcode to double[]?
		Map<String, ?> backToMapWithDoubleMap =
				AvroStreamHelper.toJavaMap(firstRecord, Collections.singletonMap("k1", new double[0]));
		Assert.assertArrayEquals(new float[] { 2F }, (float[]) backToMapWithDoubleMap.get("k1"), 0.01F);
	}

	@Test
	public void testAvroToFile() throws IOException {
		ImmutableMap<String, String> singleMap = ImmutableMap.of("k1", "v1");
		Schema schema = AvroSchemaHelper.proposeSimpleSchema(singleMap);

		InputStream is =
				AvroStreamHelper.toInputStream(Stream.of(singleMap).map(AvroStreamHelper.toGenericRecord(schema)),
						() -> ApexExecutorsHelper.newSingleThreadExecutor("testAvroToFile"));

		byte[] bytes = ByteStreams.toByteArray(is);

		List<Map<String, ?>> backToList = AvroStreamHelper.toGenericRecord(new ByteArrayInputStream(bytes))
				.map(AvroStreamHelper.toJavaMap())
				.collect(Collectors.toList());

		Assert.assertEquals(1, backToList.size());
		Assert.assertEquals(singleMap, backToList.get(0));
	}

}
