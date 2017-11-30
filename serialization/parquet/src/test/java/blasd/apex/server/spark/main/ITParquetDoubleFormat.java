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
package blasd.apex.server.spark.main;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.avro.AvroParquetWriter.Builder;
import org.apache.parquet.column.ParquetProperties.WriterVersion;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.core.logging.ApexLogHelper;
import blasd.apex.core.memory.IApexMemoryConstants;
import blasd.apex.serialization.avro.AvroSchemaHelper;
import blasd.apex.server.spark.TestReadWrite;

/**
 * Compare the footprint of different ways to hold double arrays in Parquet
 * 
 * @author Benoit Lacelle
 *
 */
public class ITParquetDoubleFormat {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ITParquetDoubleFormat.class);

	public static final int NB_DOUBLES = 512;
	public static final int NB_ROWS = 1000;

	@Test
	public void testOriginalToListDouble() throws IllegalArgumentException, IOException {
		TestReadWrite.ensureAndAssumeHadoopEnvForTests();

		// We test 3 schema: List of Doubles, double[] primitive array, and N columns of double
		Map<String, List<Double>> listOfDoubles = ImmutableMap.of("doubles", Arrays.asList(1D));
		Map<String, double[]> primitiveArray = ImmutableMap.of("doubles", new double[NB_DOUBLES]);
		Map<String, Double> nColumns =
				IntStream.range(0, NB_DOUBLES).mapToObj(i -> "double_" + i).collect(Collectors.toMap(s -> s, s -> 1D));
		for (Map<String, ?> schemaAsMap : Arrays.asList(listOfDoubles, primitiveArray, nColumns)) {
			AtomicReference<String> format = new AtomicReference<>();

			LOGGER.info("Schema: {}", schemaAsMap);
			for (Consumer<Builder<GenericRecord>> config : Arrays.<Consumer<Builder<GenericRecord>>>asList(schema -> {
				format.set("raw");
			}, schema -> {
				format.set("GZIP");
				schema.withCompressionCodec(CompressionCodecName.GZIP);
			}, schema -> {
				format.set("Parquet v2");
				schema.withWriterVersion(WriterVersion.PARQUET_2_0);
			}, schema -> {
				format.set("GZIP + Parquet v2");
				schema.withCompressionCodec(CompressionCodecName.GZIP).withWriterVersion(WriterVersion.PARQUET_2_0);
			})) {
				{
					testOriginalToListDouble(() -> "ZERO - " + format.get(), () -> {
						return new double[512];
					}, config, schemaAsMap);
				}

				// Consider small ints
				{
					AtomicLong totalIndex = new AtomicLong();
					testOriginalToListDouble(() -> "FIRST_INTS - " + format.get(),
							() -> IntStream.range(0, NB_DOUBLES)
									.mapToDouble(i -> totalIndex.getAndIncrement())
									.toArray(),
							config,
							schemaAsMap);
				}

				{
					Random r = new Random(0);
					testOriginalToListDouble(() -> "RANDOM_DOUBLE - " + format.get(),
							() -> IntStream.range(0, NB_DOUBLES).mapToDouble(i -> r.nextDouble()).toArray(),
							config,
							schemaAsMap);
				}

				{
					// We also covers the full range of exponent
					// https://fr.wikipedia.org/wiki/IEEE_754
					Random r = new Random(0);
					testOriginalToListDouble(() -> "RANDOM_BIG_DOUBLE - " + format.get(),
							() -> IntStream.range(0, NB_DOUBLES)
									.mapToDouble(i -> r.nextDouble()
											* (r.nextBoolean() ? r.nextLong() : 1D / (double) r.nextLong()))
									.toArray(),
							config,
							schemaAsMap);
				}

				{
					Random r = new Random(0);
					testOriginalToListDouble(() -> "RANDOM_DOUBLE_OR_ZERO (50%) - " + format.get(),
							() -> IntStream.range(0, NB_DOUBLES)
									.mapToDouble(i -> r.nextBoolean() ? 0D : r.nextDouble())
									.toArray(),
							config,
							schemaAsMap);
				}

				{
					Random r = new Random(0);
					testOriginalToListDouble(() -> "RANDOM_FLOAT - " + format.get(),
							() -> IntStream.range(0, NB_DOUBLES).mapToDouble(i -> r.nextFloat()).toArray(),
							config,
							schemaAsMap);
				}
			}
		}
	}

	public void testOriginalToListDouble(Supplier<String> testName,
			Supplier<double[]> doubleSupplier,
			Consumer<Builder<GenericRecord>> configSchema,
			Map<String, ?> schemaAsMap) throws IllegalArgumentException, IOException {

		// Schema schema = ApexParquetHelper.makeSimpleSchema(ImmutableMap.of("doubles", new double[NB_DOUBLES]));
		Schema schema;

		if (schemaAsMap.values().iterator().next() instanceof List<?>) {
			schema = Schema.createRecord("myrecord",
					null,
					"space",
					false,
					Arrays.asList(new Field(schemaAsMap.keySet().iterator().next(),
							Schema.createUnion(Schema.create(Type.NULL),
									Schema.createArray(Schema.create(Schema.Type.DOUBLE))),
							null,
							Schema.NULL_VALUE)));
		} else {
			schema = AvroSchemaHelper.proposeSimpleSchema(schemaAsMap);
		}

		ParquetWriter<GenericRecord> writer;
		java.nio.file.Path outputPath;
		try {
			outputPath = ApexFileHelper.createTempPath(ITParquetDoubleFormat.class.getSimpleName(), ".parquet", true);
			Builder<GenericRecord> schemaWriter =
					AvroParquetWriter.<GenericRecord>builder(new Path(outputPath.toUri())).withSchema(schema);

			configSchema.accept(schemaWriter);

			writer = schemaWriter.build();
		} catch (NullPointerException | IOException e) {
			throw new RuntimeException("Are you missing Hadoop binaries?", e);
		}

		IntStream.range(0, NB_ROWS).forEach(index -> {
			try {
				GenericRecordBuilder builder = new GenericRecordBuilder(schema);

				double[] doubles = doubleSupplier.get();

				if (schemaAsMap.get("doubles") instanceof double[]) {
					// https://stackoverflow.com/questions/3770289/converting-array-of-primitives-to-array-of-containers-in-java
					// TODO use ArrayUtils?
					byte[] bytes = new byte[Ints.checkedCast(doubles.length * IApexMemoryConstants.DOUBLE)];

					ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
					byteBuffer.asDoubleBuffer().put(doubles);

					builder.set("doubles", byteBuffer
					// new Fixed(schema, bytes)
					);
				} else if (schemaAsMap.get("doubles") instanceof List<?>) {
					builder.set("doubles", Doubles.asList(doubles));
				} else {
					IntStream.range(0, doubles.length).forEach(i -> builder.set("double_" + i, doubles[i]));
				}

				GenericRecord record = builder.build();

				try {
					writer.write(record);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} catch (RuntimeException e) {
				throw new RuntimeException(e);
			}
		});

		try {
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		LOGGER.info("{}! Output field is {}",
				testName.get(),
				ApexLogHelper.getNiceMemory(outputPath.toFile().length()));
	}
}