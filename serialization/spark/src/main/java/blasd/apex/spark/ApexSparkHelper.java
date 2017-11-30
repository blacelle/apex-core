package blasd.apex.spark;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.primitives.Doubles;

import blasd.apex.core.io.ApexSerializationHelper;
import scala.collection.JavaConverters;
import scala.collection.mutable.WrappedArray;

/**
 * Some basic utilities for Spark
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexSparkHelper {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexSparkHelper.class);

	protected ApexSparkHelper() {
		// hidden
	}

	public static InputStream toAvro(Schema outputSchema,
			Iterator<Row> f,
			BiMap<String, String> inputToOutputColumnMapping) throws IOException {
		// We write IndexedRecord instead of Map<?,?> as it is implied by the schema: a schema holding a Map would not
		// defines the fields
		DatumWriter<IndexedRecord> userDatumWriter = new SpecificDatumWriter<IndexedRecord>(outputSchema);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// Use DataFileWriter to write the schema in the bytes
		try (DataFileWriter<IndexedRecord> fileWriter = new DataFileWriter<>(userDatumWriter)) {
			fileWriter.create(outputSchema, baos);

			Streams.stream(f).forEach(row -> {
				try {
					Map<String, ?> asMap = rowToMap(outputSchema, row, inputToOutputColumnMapping);
					IndexedRecord record = mapToIndexedRecord(outputSchema, asMap);
					fileWriter.append(record);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}

		return new ByteArrayInputStream(baos.toByteArray());
	}

	private static Map<String, ?> rowToMap(Schema outputSchema, Row row, BiMap<String, String> columnMapping) {
		return outputSchema.getFields()
				.stream()
				.map(f -> columnMapping.inverse().getOrDefault(f.name(), f.name()))
				.collect(
						Collectors.toMap(fName -> columnMapping.getOrDefault(fName, fName), fName -> row.getAs(fName)));
	}

	private static IndexedRecord mapToIndexedRecord(Schema schema, Map<?, ?> row) {
		Record r = new Record(schema);

		for (Field field : r.getSchema().getFields()) {
			Object valueToWrite = row.get(field.name());

			valueToWrite = convertFromSparkToAvro(field, valueToWrite, s -> {
				try {
					return ByteBuffer.wrap(ApexSerializationHelper.toBytes(s));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});

			r.put(field.name(), valueToWrite);
		}

		return r;
	}

	public static Object convertFromSparkToAvro(Field field,
			Object valueToWrite,
			Function<Serializable, ByteBuffer> serializer) {
		if (valueToWrite instanceof WrappedArray<?>) {
			List<?> asList = ImmutableList
					.copyOf(JavaConverters.asJavaCollectionConverter(((WrappedArray<?>) valueToWrite).toIterable())
							.asJavaCollection());
			valueToWrite = asList;

			if (field.schema().getType() == Schema.Type.UNION
					&& field.schema().getTypes().contains(Schema.create(Schema.Type.BYTES))) {
				// byte[] bytes = new byte[Ints.checkedCast(IApexMemoryConstants.DOUBLE * asList.size())];
				// ByteBuffer.wrap(bytes).asDoubleBuffer().put(primitiveArray);

				double[] primitiveArray = Doubles.toArray((Collection<? extends Number>) asList);

				// Avro requires a ByteBuffer. See org.apache.avro.generic.GenericData.getSchemaName(Object)
				// Parquet seems to handle both byte[] and ByteBuffer
				valueToWrite = serializer.apply(primitiveArray);
			}
		}
		return valueToWrite;
	}
}
