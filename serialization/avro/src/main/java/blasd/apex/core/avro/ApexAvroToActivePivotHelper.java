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
package blasd.apex.core.avro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.apache.spark.sql.Row;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;

import blasd.apex.core.io.ApexSerializationHelper;
import scala.collection.JavaConverters;
import scala.collection.mutable.WrappedArray;

/**
 * Helps converting avro records to ActivePivot objects
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexAvroToActivePivotHelper {
	protected ApexAvroToActivePivotHelper() {
		// hidden
	}

	public static Map<String, ?> toMap(Map<? extends String, ?> exampleTypes, IndexedRecord indexedRecord) {
		Map<String, Object> asMap = new HashMap<>();

		List<Field> fields = indexedRecord.getSchema().getFields();
		for (int i = 0; i < fields.size(); i++) {
			Schema.Field f = fields.get(i);
			String fieldName = f.name();

			// We need to convert keys from Utf8 to String
			Object exampleValue = exampleTypes.get(fieldName);
			Object cleanValue = cleanValue(indexedRecord.get(i), () -> exampleValue);
			asMap.put(fieldName, cleanValue);
		}

		return asMap;
	}

	public static Object cleanValue(Object value, Supplier<?> exampleValue) {
		if (value == null) {
			return null;
		}

		if (value instanceof Utf8) {
			// ActivePivot expects String, while parquet wrap them in an Utf8
			value = value.toString();
		} else if (value instanceof ByteBuffer) {
			// Typically happens on LocalDate
			ByteBuffer byteBuffer = (ByteBuffer) value;

			try {
				value = ApexSerializationHelper.fromBytes(byteBuffer.array());
			} catch (ClassNotFoundException | IOException e) {
				throw new RuntimeException(e);
			}
		} else if (value instanceof GenericData.Fixed) {
			// We received a predefined-length array of bytes
			GenericData.Fixed fixed = (GenericData.Fixed) value;

			Object targetType = exampleValue.get();
			if (targetType != null) {
				if (targetType instanceof double[]) {
					value = convertToDouble(fixed);
				} else if (targetType instanceof float[]) {
					value = convertToFloat(fixed);
				} else {
					throw new RuntimeException("Issue with " + targetType);
				}
			} else {
				// Guess it is double[]
				value = convertToDouble(fixed);
			}
		} else if (value instanceof List<?>) {
			List<?> asList = (List<?>) value;

			// TODO: we should read primitive directly from Parquet
			Object targetType = exampleValue.get();
			Optional<?> opt = toPrimitiveArray(targetType, asList);
			if (opt.isPresent()) {
				value = opt.get();
			}
		}

		return value;
	}

	public static double[] convertToDouble(GenericData.Fixed fixed) {
		// Convert Fixed (which wraps a byte[]) to a double[]
		ByteBuffer bytes = ByteBuffer.wrap(fixed.bytes());

		// https://stackoverflow.com/questions/3770289/converting-array-of-primitives-to-array-of-containers-in-java
		// TODO use ArrayUtils?
		DoubleBuffer asDoubleBuffer = bytes.asDoubleBuffer();
		double[] array = new double[asDoubleBuffer.capacity()];
		asDoubleBuffer.get(array);
		return array;
	}

	public static float[] convertToFloat(GenericData.Fixed fixed) {
		ByteBuffer bytes = ByteBuffer.wrap(fixed.bytes());

		// https://stackoverflow.com/questions/3770289/converting-array-of-primitives-to-array-of-containers-in-java
		// TODO use ArrayUtils?
		FloatBuffer asDoubleBuffer = bytes.asFloatBuffer();
		float[] array = new float[asDoubleBuffer.capacity()];
		asDoubleBuffer.get(array);
		return array;
	}

	public static Optional<?> toPrimitiveArray(Object targetType, List<?> asList) {
		if (asList.isEmpty()) {
			return Optional.empty();
		} else {
			final boolean targetPrimitiveFloat;
			final boolean targetPrimitiveDouble;

			if (targetType instanceof float[]) {
				targetPrimitiveFloat = true;
				targetPrimitiveDouble = false;
			} else if (targetType instanceof double[]) {
				targetPrimitiveFloat = false;
				targetPrimitiveDouble = true;
			} else {
				Object first = asList.get(0);

				if (first instanceof Float) {
					targetPrimitiveFloat = true;
					targetPrimitiveDouble = false;
				} else if (first instanceof Double) {
					targetPrimitiveFloat = false;
					targetPrimitiveDouble = true;
				} else {
					// TODO: Improve this case?
					targetPrimitiveFloat = false;
					targetPrimitiveDouble = false;
				}
			}

			Object first = asList.get(0);

			if (first instanceof GenericData.Record) {
				GenericData.Record asRecord = (GenericData.Record) first;

				if (asRecord.getSchema().getFields().size() == 1) {
					Field singleField = asRecord.getSchema().getFields().get(0);

					if (holdNumber(singleField)) {
						// TODO: this does not handle the case we haver both double and string in the union
						if (targetPrimitiveFloat) {
							float[] floats = new float[asList.size()];

							for (int i = 0; i < asList.size(); i++) {
								floats[i] = ((Number) ((GenericData.Record) asList.get(i)).get(0)).floatValue();
							}
							return Optional.of(floats);
						} else if (targetPrimitiveDouble) {
							double[] doubles = new double[asList.size()];

							for (int i = 0; i < asList.size(); i++) {
								doubles[i] = ((Number) ((GenericData.Record) asList.get(i)).get(0)).doubleValue();
							}
							return Optional.of(doubles);
						}
					}

					return Optional.empty();
				} else {
					return Optional.empty();
				}
			} else if (targetPrimitiveFloat) {
				return Optional.of(Floats.toArray((List<Number>) asList));
			} else if (targetPrimitiveDouble) {
				return Optional.of(Doubles.toArray((List<Number>) asList));
			} else {
				return Optional.empty();
			}
		}
	}

	/**
	 * Used to detect if a field holds a single number, would it be through a Union with NULL, or a record with a single
	 * number field
	 * 
	 * @param singleField
	 * @return
	 */
	private static boolean holdNumber(Field singleField) {
		return singleField.schema().getType() == Type.DOUBLE || singleField.schema().getType() == Type.FLOAT
				|| singleField.schema().getType() == Type.UNION && singleField.schema()
						.getTypes()
						.stream()
						.filter(s -> s.getType() == Type.DOUBLE || s.getType() == Type.FLOAT)
						.findAny()
						.isPresent();
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

			valueToWrite = convertFromSparkValue(field, valueToWrite);

			r.put(field.name(), valueToWrite);
		}

		return r;
	}

	public static Object convertFromSparkValue(Field field, Object valueToWrite) {
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
				try {
					valueToWrite = ByteBuffer.wrap(ApexSerializationHelper.toBytes(primitiveArray));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return valueToWrite;
	}

}
