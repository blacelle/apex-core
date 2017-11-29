package blasd.apex.serialization.avro;

import java.util.HashMap;
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

	public static Map<String, ?> toMap(Map<? extends String, ?> exampleTypes, IndexedRecord indexedRecord) {
		Map<String, Object> asMap = new HashMap<>();

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

	public static Function<Map<String, ?>, GenericRecord> toGenericRecord(Schema schema) {
		return map -> {
			GenericRecordBuilder record = new GenericRecordBuilder(schema);

			map.forEach(
					(key, value) -> record.set(key, AvroSchemaHelper.converToAvroValue(schema.getField(key), value)));

			return record.build();
		};

	}

	public static Function<GenericRecord, Map<String, ?>> toStandardJava(Map<? extends String, ?> exampleTypes) {
		return record -> toMap(exampleTypes, record);
	}
}
