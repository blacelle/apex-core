package blasd.apex.serialization.avro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.generic.IndexedRecord;

/**
 * Helps working with Avro GenericRecords and java8 {@link Stream}
 * 
 * @author Benoit Lacelle
 *
 */
public class AvroStreamHelper {
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

			map.forEach(
					(key, value) -> record.set(key, AvroSchemaHelper.converToAvroValue(schema.getField(key), value)));

			return record.build();
		};

	}
}
