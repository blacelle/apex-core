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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.parquet.ParquetStreamFactory;
import blasd.apex.serialization.avro.AvroSchemaHelper;
import blasd.apex.serialization.avro.AvroStreamHelper;
import blasd.apex.serialization.avro.IAvroStreamFactory;

public class TestParquetBytesToStream {
	@Test
	public void testConvertListMapBackAndForth() throws IOException {
		TestReadWrite.ensureAndAssumeHadoopEnvForTests();

		String stringField = "stringField";
		String doubleField = "doubleField";
		String doubleArrayField = "doubleArrayField";

		Schema schema = AvroSchemaHelper.proposeSimpleSchema(
				ImmutableMap.of(stringField, "anyString", doubleField, 0D, doubleArrayField, new double[2]));

		List<Map<String, Object>> list = Arrays.asList(ImmutableMap
				.of(stringField, "stringValue", doubleField, 123D, doubleArrayField, new double[] { 234D, 345D }));

		java.nio.file.Path pathOnDisk = ApexFileHelper.createTempPath(getClass().getSimpleName(), ".tmp", true);
		IAvroStreamFactory factory = new ParquetStreamFactory();
		factory.writeToPath(pathOnDisk, list.stream().map(AvroStreamHelper.toGenericRecord(schema)));

		Stream<? extends Map<String, ?>> asMapStream =
				factory.toStream(pathOnDisk).map(AvroStreamHelper.toStandardJava(Collections.emptyMap()));

		List<Map<String, ?>> asMapList = asMapStream.collect(Collectors.toList());

		Assert.assertEquals(asMapList.size(), list.size());

		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> originalItem = list.get(i);
			Map<String, ?> rereadItem = asMapList.get(i);
			Assert.assertEquals(rereadItem.keySet(), originalItem.keySet());

			for (String key : originalItem.keySet()) {
				Object originalValue = originalItem.get(key);
				Object rereadValue = rereadItem.get(key);

				if (rereadValue instanceof double[]) {
					Assert.assertArrayEquals(key, (double[]) originalValue, (double[]) rereadValue, 0.001D);
				} else {
					Assert.assertEquals(key, originalValue, rereadValue);
				}
			}
		}
	}
}
