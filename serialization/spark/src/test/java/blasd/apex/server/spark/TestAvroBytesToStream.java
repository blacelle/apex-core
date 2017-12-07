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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import com.fasterxml.jackson.dataformat.avro.AvroSchema;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.primitives.Doubles;

import blasd.apex.serialization.avro.AvroBytesToStream;
import blasd.apex.serialization.avro.AvroStreamHelper;
import blasd.apex.spark.ApexSparkHelper;

public class TestAvroBytesToStream {
	// https://github.com/FasterXML/jackson-dataformats-binary/blob/master/avro/src/test/java/com/fasterxml/jackson/dataformat/avro/MapTest.java

	private final static String MAP_OR_NULL_SCHEMA_JSON =
			SchemaBuilder.map().values(Schema.create(Schema.Type.STRING)).toString();

	private final AvroMapper MAPPER = getMapper();

	protected AvroMapper _sharedMapper;

	protected AvroMapper getMapper() {
		if (_sharedMapper == null) {
			_sharedMapper = newMapper();
		}
		return _sharedMapper;
	}

	protected AvroMapper newMapper() {
		return new AvroMapper();
	}

	@Ignore("Not much interested in submitting Maps. We prefer submitted IndexedRecord")
	@Test
	public void testMapOrNull() throws Exception {
		AvroSchema schema = MAPPER.schemaFrom(MAP_OR_NULL_SCHEMA_JSON);

		schema.getAvroSchema().getType();

		DatumWriter<Map<?, ?>> userDatumWriter = new SpecificDatumWriter<Map<?, ?>>(schema.getAvroSchema());

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Use DataFileWriter to write the schema in the bytes
		try (DataFileWriter<Map<?, ?>> fileWriter = new DataFileWriter<>(userDatumWriter)) {
			fileWriter.create(schema.getAvroSchema(), baos);
			fileWriter.append(ImmutableMap.of("x", "y"));
		}

		List<? extends Map<String, ?>> avroStream =
				new AvroBytesToStream().stream(new ByteArrayInputStream(baos.toByteArray()))
						.map(AvroStreamHelper.toJavaMap())
						.collect(Collectors.toList());

		Assert.assertEquals(1, avroStream.size());
		Assert.assertEquals(ImmutableMap.of("x", "y"), avroStream.get(0));
	}

	@Test
	public void testAvroToStream() throws IOException {
		GenericRowWithSchema row = new GenericRowWithSchema(new Object[] { "someValue" },
				new StructType(new StructField[] { new StructField("ccy", DataTypes.StringType, true, null) }));

		Schema outputSchema = Schema.createRecord("testSchema",
				"doc",
				"namespace",
				false,
				Arrays.asList(new Field("Currency", Schema.create(Type.STRING), null, Schema.NULL_VALUE)));

		BiMap<String, String> mapping = ImmutableBiMap.of("ccy", "Currency");
		InputStream stream = ApexSparkHelper.toAvro(outputSchema, Iterators.singletonIterator(row), mapping);

		List<?> resultAsList =
				new AvroBytesToStream().stream(stream).map(AvroStreamHelper.toJavaMap()).collect(Collectors.toList());

		Assert.assertEquals(1, resultAsList.size());
		Assert.assertEquals(ImmutableMap.of("Currency", "someValue"), resultAsList.get(0));
	}

	@Test
	public void testAvroToStream_doublearray() throws IOException {
		// Avro works with Collection by default
		GenericRowWithSchema row = new GenericRowWithSchema(new Object[] { Doubles.asList(1D, 2D, 3D) },
				new StructType(new StructField[] { new StructField("doubleArray", DataTypes.StringType, true, null) }));

		Schema outputSchema = Schema.createRecord("testSchema",
				"doc",
				"namespace",
				false,
				Arrays.asList(new Field("DoubleArray",
						Schema.createArray(Schema.create(Schema.Type.DOUBLE)),
						null,
						Schema.NULL_VALUE)));

		BiMap<String, String> mapping = ImmutableBiMap.of("doubleArray", "DoubleArray");
		InputStream stream = ApexSparkHelper.toAvro(outputSchema, Iterators.singletonIterator(row), mapping);

		List<?> resultAsList = new AvroBytesToStream().stream(stream)
				.map(AvroStreamHelper.toJavaMap(ImmutableMap.of("DoubleArray", new double[0])))
				.collect(Collectors.toList());

		Assert.assertEquals(1, resultAsList.size());
		Map<?, ?> singleOutput = (Map<?, ?>) resultAsList.get(0);
		Assert.assertEquals(ImmutableSet.of("DoubleArray"), singleOutput.keySet());
		Assert.assertArrayEquals(new double[] { 1D, 2D, 3D },
				(double[]) singleOutput.values().iterator().next(),
				0.0001D);
	}
}
