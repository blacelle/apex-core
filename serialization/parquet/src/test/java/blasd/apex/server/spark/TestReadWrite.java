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

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Fixed;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.hadoop.ApexHadoopHelper;
import blasd.apex.parquet.ParquetBytesToStream;
import blasd.apex.serialization.avro.AvroStreamHelper;

// https://github.com/Parquet/parquet-mr/blob/master/parquet-avro/src/test/java/parquet/avro/TestAvroSchemaConverter.java
public class TestReadWrite {
	public static void ensureAndAssumeHadoopEnvForTests() {
		ApexHadoopHelper.isHadoopReady();
		ApexSparkTestHelper.assumeHadoopEnv();
	}

	@Test
	public void testMapWithNulls() throws Exception {
		ensureAndAssumeHadoopEnvForTests();

		// https://avro.apache.org/docs/1.8.1/spec.html#schema_primitive
		// parse(Resources.getResource("map_with_nulls.avsc").openStream());

		java.nio.file.Path pathOnDisk = ApexFileHelper.createTempPath(getClass().getSimpleName(), ".tmp", true);
		Path file = new Path(pathOnDisk.toFile().getPath());

		Schema doubleType = Schema.create(Type.DOUBLE);
		Schema schema = Schema.createRecord("myrecord",
				null,
				"space",
				false,
				Arrays.asList(new Field("dealId", Schema.create(Type.INT), null, -1),
						new Field("scenario1", doubleType, "scenario1", (Object) null),
						new Field("scenario2", doubleType, "scenario2", (Object) null),
						new Field("scenario3", doubleType, "scenario3", (Object) null),
						new Field("scenarioValues", Schema.createArray(doubleType), "scenarioValues", (Object) null),
						new Field("scenarioValuesAsBytes",
								Schema.createFixed("double_array_3", "doc", "space", 3 * 8),
								"scenarioValuesAsBytes",
								(Object) null)));

		ParquetWriter<GenericRecord> writer;
		try {
			writer = AvroParquetWriter.<GenericRecord>builder(file).withSchema(schema).build();
		} catch (NullPointerException e) {
			throw new RuntimeException("Are you missing Hadoop binaries?", e);
		}

		double[] doubles = new double[] { 1D, 2D, 3D };
		byte[] bytes = new byte[doubles.length * 8];
		ByteBuffer byteArray = ByteBuffer.wrap(bytes);
		byteArray.asDoubleBuffer().put(doubles);

		GenericData.Record record = new GenericRecordBuilder(schema).set("dealId", 123)
				.set("scenarioValues", doubles)
				.set("scenario1", doubles[0])
				.set("scenario2", doubles[1])
				.set("scenario3", doubles[2])
				.set("scenarioValuesAsBytes", new Fixed(schema, bytes))
				.build();
		writer.write(record);
		writer.close();

		Stream<? extends Map<String, ?>> asMapStream =
				new ParquetBytesToStream().stream(new FileInputStream(new File(file.toString())))
						.map(AvroStreamHelper.toStandardJava(Collections.emptyMap()));
		Iterator<? extends Map<String, ?>> asMapIterator = asMapStream.iterator();

		Map<String, ?> nextRecord = asMapIterator.next();

		Assert.assertNotNull(nextRecord);

		Assert.assertEquals(123, nextRecord.get("dealId"));

		Assert.assertArrayEquals(new double[] { 1D, 2D, 3D }, (double[]) nextRecord.get("scenarioValues"), 0.001D);

		Assert.assertEquals(doubles[0], (Double) nextRecord.get("scenario1"), 0.001);
		Assert.assertEquals(doubles[1], (Double) nextRecord.get("scenario2"), 0.001);
		Assert.assertEquals(doubles[2], (Double) nextRecord.get("scenario3"), 0.001);

		double[] doubleAsBytes = (double[]) nextRecord.get("scenarioValuesAsBytes");
		Assert.assertEquals(doubles[0], doubleAsBytes[0], 0.001);
		Assert.assertEquals(doubles[1], doubleAsBytes[1], 0.001);
		Assert.assertEquals(doubles[2], doubleAsBytes[2], 0.001);
	}

}