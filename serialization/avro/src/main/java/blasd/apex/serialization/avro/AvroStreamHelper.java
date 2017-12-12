package blasd.apex.serialization.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import avro.shaded.com.google.common.base.Supplier;

/**
 * Helps working with Avro GenericRecords and java8 {@link Stream}
 * 
 * @author Benoit Lacelle
 *
 */
public class AvroStreamHelper {
	protected static final Logger LOGGER = LoggerFactory.getLogger(AvroStreamHelper.class);

	protected AvroStreamHelper() {
		// hidden
	}

	/**
	 * This method helps transcoding an Avro IndexedRecord to a standard java Map. It may induce a performance penalty,
	 * typically by converting by default all Utf8 to a String
	 * 
	 * @param indexedRecord
	 *            an Avro IndexedRecord
	 * @param exampleTypes
	 *            a Map describing the expected type of each value of the output Map
	 * @return a {@link Map} equivalent o the input IndexedRecord but after having converted values to types as defined
	 *         in the example Map
	 */
	public static Map<String, ?> toJavaMap(IndexedRecord indexedRecord, Map<? extends String, ?> exampleTypes) {
		Map<String, Object> asMap = new LinkedHashMap<>();

		List<Field> fields = indexedRecord.getSchema().getFields();
		for (int i = 0; i < fields.size(); i++) {
			Field f = fields.get(i);
			String fieldName = f.name();

			// We need to convert keys from Utf8 to String
			Object exampleValue = exampleTypes.get(fieldName);
			Object cleanValue = AvroFieldHelper.cleanValue(indexedRecord.get(i), () -> exampleValue);
			asMap.put(fieldName, cleanValue);
		}

		return asMap;
	}

	public static Map<String, ?> toJavaMap(IndexedRecord indexedRecord) {
		return toJavaMap(indexedRecord, Collections.emptyMap());
	}

	/**
	 * 
	 * @param exampleTypes
	 * @return a {@link Function} enabling transcoding in a {@link Stream}
	 */
	public static Function<GenericRecord, Map<String, ?>> toJavaMap(Map<? extends String, ?> exampleTypes) {
		return record -> toJavaMap(record, exampleTypes);
	}

	public static Function<GenericRecord, Map<String, ?>> toJavaMap() {
		return toJavaMap(Collections.emptyMap());
	}

	public static Function<Map<String, ?>, GenericRecord> toGenericRecord(Schema schema) {
		return map -> {
			GenericRecordBuilder record = new GenericRecordBuilder(schema);

			map.forEach((key, value) -> {
				Field field = schema.getField(key);

				if (field == null) {
					LOGGER.trace("We received a Map with a key which does not exist in the schema: " + key);
				} else {
					record.set(key, AvroSchemaHelper.converToAvroValue(field, value));
				}
			});

			return record.build();
		};
	}

	/**
	 * 
	 * @param rowsToWrite
	 *            the stream of {@link GenericRecord} to write
	 * @param executor
	 *            an Async {@link Executor} supplier. It MUST not be synchronous else the underlying PipedInputStream
	 *            may dead-lock waiting for the PipedInputStream to be filled
	 * @return
	 * @throws IOException
	 */
	public static InputStream toInputStream(Stream<? extends GenericRecord> rowsToWrite, Supplier<Executor> executor)
			throws IOException {
		// https://avro.apache.org/docs/1.8.1/gettingstartedjava.html
		// We will use the first record to prepare a writer on the correct schema
		AtomicReference<DataFileWriter<GenericRecord>> writer = new AtomicReference<>();

		// https://stackoverflow.com/questions/5778658/how-to-convert-outputstream-to-inputstream
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream(pis);

		rowsToWrite.onClose(() -> {
			// Flush the writer
			try {
				if (writer.get() != null) {
					try {
						writer.get().close();
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			} finally {
				// Flush the pipe
				try {
					pos.close();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		});

		executor.get().execute(() -> {
			try {
				rowsToWrite.forEach(m -> {
					if (writer.get() == null) {
						try {
							Schema schema = m.getSchema();
							DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
							DataFileWriter<GenericRecord> dataFileWriter =
									new DataFileWriter<GenericRecord>(datumWriter);
							writer.set(dataFileWriter);
							dataFileWriter.create(schema, pos);
						} catch (NullPointerException e) {
							throw new IllegalStateException("Are you missing Hadoop binaries?", e);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					}

					try {
						writer.get().append(m);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			} finally {
				rowsToWrite.close();
			}
		});

		return pis;
	}

	public static Stream<GenericRecord> toGenericRecord(InputStream inputStream) throws IOException {
		return new AvroBytesToStream().toStream(inputStream);
	}
}
