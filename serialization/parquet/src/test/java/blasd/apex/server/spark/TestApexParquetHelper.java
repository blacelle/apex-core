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
package blasd.apex.server.spark;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.GenericRecordBuilder;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import blasd.apex.core.avro.ApexAvroToActivePivotHelper;
import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.hadoop.ApexHadoopHelper;
import blasd.apex.parquet.ParquetStreamFactory;
import blasd.apex.serialization.avro.ApexAvroSchemaHelper;
import blasd.apex.serialization.avro.AvroBytesToStream;

public class TestApexParquetHelper {
	@BeforeClass
	public static void checkHadoop() {
		ApexHadoopHelper.isHadoopReady();
		ApexSparkTestHelper.assumeHadoopEnv();
	}

	ParquetStreamFactory factory = new ParquetStreamFactory();

	@Test
	public void testSchemaFromMap() {
		Map<String, ?> asMap = ImmutableMap.<String, Object>builder()
				.put("stringField", "anyString")
				.put("doubleField", 0D)
				.put("floatField", 0F)
				.put("intField", 0)
				.put("LongField", 0L)
				.put("doubleArrayField", new double[2])
				.put("floatArrayField", new float[2])
				.put("doubleList", Collections.singletonList(1D))
				.build();
		Schema schema = ApexAvroSchemaHelper.proposeSimpleSchema(asMap);

		// We use an union to allow the field to hold null
		// Assert.assertEquals("array", schema.getField("doubleList").schema().getType().getName());
		Assert.assertEquals("union", schema.getField("doubleList").schema().getType().getName());
	}

	@Test
	public void testSchemaForDoubleArray() throws IOException {
		double[] doubles = new double[] { 123D };
		Map<String, ?> asMap = ImmutableMap.of("k", doubles);
		Schema schema = ApexAvroSchemaHelper.proposeSimpleSchema(asMap);

		// We use an union to allow the field to hold null
		// Assert.assertEquals("array", schema.getField("doubleList").schema().getType().getName());
		Assert.assertEquals("union", schema.getField("k").schema().getType().getName());

		{
			Path path = ApexFileHelper.createTempPath("apex", "parquet", true);
			factory.writeToPath(path,
					Stream.of(ImmutableMap.of("k", doubles)).map(ApexAvroSchemaHelper.genericRecords(schema)));

			Map<String, ?> asMapAgain = factory.toStream(path)
					.map(AvroBytesToStream.toStandardJava(Collections.emptyMap()))
					.iterator()
					.next();
			Assert.assertArrayEquals(doubles, (double[]) asMapAgain.get("k"), 0.0001D);
		}
	}

	@Test
	public void testSchemaForFloatArray() throws IOException {
		float[] doubles = new float[] { 123F };
		Map<String, ?> asMap = ImmutableMap.of("k", doubles);
		Schema schema = ApexAvroSchemaHelper.proposeSimpleSchema(asMap);

		// We use an union to allow the field to hold null
		// Assert.assertEquals("array", schema.getField("doubleList").schema().getType().getName());
		Assert.assertEquals("union", schema.getField("k").schema().getType().getName());

		{
			Path path = ApexFileHelper.createTempPath("apex", "parquet", true);
			factory.writeToPath(path,
					Stream.of(ImmutableMap.of("k", doubles)).map(ApexAvroSchemaHelper.genericRecords(schema)));

			Map<String, ?> asMapAgain =
					ParquetStreamFactory.readParquetAsStream(path, ImmutableMap.of()).iterator().next();
			Assert.assertArrayEquals(doubles, (float[]) asMapAgain.get("k"), 0.0001F);
		}
	}

	@Test
	public void testWriteFloatArray() {
		Object floatArray = ApexAvroSchemaHelper.converToParquetValue(null, new float[] { 1F });

		Assert.assertTrue(floatArray instanceof byte[]);
	}

	@Test
	public void testSchemaForNullLong() {
		Map<String, ?> asMap = ImmutableMap.of("LongField", 0L);
		Schema schema = ApexAvroSchemaHelper.proposeSimpleSchema(asMap);
		GenericData.Record record = new GenericRecordBuilder(schema).set("LongField", null).build();

		Assert.assertNull(record.get(0));
	}

	@Test
	public void testSchemaForLocalDate() throws IOException {
		Map<String, ?> asMap = ImmutableMap.of("DateField", LocalDate.now());

		Schema schema = ApexAvroSchemaHelper.proposeSimpleSchema(asMap);

		LocalDate date = new LocalDate();
		{
			GenericData.Record record = new GenericRecordBuilder(schema).set("DateField", date).build();
			Assert.assertEquals(date, record.get(0));
		}

		{
			ApexHadoopHelper.isHadoopReady();
			ApexSparkTestHelper.assumeHadoopEnv();

			Path path = ApexFileHelper.createTempPath("apex", "parquet", true);

			factory.writeToPath(path,
					Stream.of(ImmutableMap.of("DateField", date)).map(ApexAvroSchemaHelper.genericRecords(schema)));

			Map<String, ?> asMapAgain =
					ParquetStreamFactory.readParquetAsStream(path, ImmutableMap.of()).iterator().next();
			Assert.assertEquals(date, asMapAgain.get("DateField"));
		}
	}

	@Test
	public void testReadComplexFloatArray() throws IOException {
		Schema elementSchema = Schema.createRecord("arrayElement",
				"doc",
				"namespace",
				false,
				Arrays.asList(new Field("name",
						Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.FLOAT)),
						"doc",
						(Object) null)));
		Schema schema = Schema.createRecord("wholeRecord",
				"doc",
				"namespace",
				false,
				Arrays.asList(new Field("arrayField", elementSchema, "doc", (Object) null)));

		Record element1 = new GenericRecordBuilder(elementSchema).set("name", 123F).build();
		Record element2 = new GenericRecordBuilder(elementSchema).set("name", 234F).build();
		Record topRecord =
				new GenericRecordBuilder(schema).set("arrayField", Arrays.asList(element1, element2)).build();

		// Read as float[]
		{
			Map<String, ?> asMap =
					ApexAvroToActivePivotHelper.toMap(ImmutableMap.of("arrayField", new float[0]), topRecord);

			Assert.assertTrue(asMap.get("arrayField") instanceof float[]);
		}

		// Read as double[]
		{
			Map<String, ?> asMap =
					ApexAvroToActivePivotHelper.toMap(ImmutableMap.of("arrayField", new double[0]), topRecord);

			Assert.assertTrue(asMap.get("arrayField") instanceof double[]);
		}

		// Read as List<Float>
		{
			Map<String, ?> asMap =
					ApexAvroToActivePivotHelper.toMap(ImmutableMap.of("arrayField", Arrays.asList(1F)), topRecord);

			Assert.assertTrue(asMap.get("arrayField") instanceof List);
		}

		// Read as List<Double>
		{
			Map<String, ?> asMap =
					ApexAvroToActivePivotHelper.toMap(ImmutableMap.of("arrayField", Arrays.asList(1D)), topRecord);

			Assert.assertTrue(asMap.get("arrayField") instanceof List);
		}
	}
}
