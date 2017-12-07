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
package blasd.apex.serialization.avro;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

import blasd.apex.core.io.ApexSerializationHelper;
import blasd.apex.core.logging.ApexLogHelper;

/**
 * Various utilities frelated to Avro schema
 * 
 * @author Benoit Lacelle
 *
 */
public class AvroSchemaHelper {
	protected AvroSchemaHelper() {
		// hidden
	}

	/**
	 * 
	 * @param schema
	 *            the schema of the whole record, not only given value
	 * @param value
	 * @return
	 */
	public static Object converToAvroValue(Field schema, Object value) {
		if (value instanceof Number || value instanceof String) {
			return value;
			// } else if (value instanceof double[]) {
			// // TODO use a buffer byte[] or ByteBuffer
			// double[] doubles = (double[]) value;
			// byte[] bytes = new byte[Ints.checkedCast(doubles.length * IApexMemoryConstants.DOUBLE)];
			// ByteBuffer byteArray = ByteBuffer.wrap(bytes);
			// byteArray.asDoubleBuffer().put(doubles);
			// return new Fixed(schema.schema(), bytes);
			// } else if (value instanceof float[]) {
			// // TODO use a buffer byte[] or ByteBuffer
			// float[] floats = (float[]) value;
			// // byte[] bytes = new byte[Ints.checkedCast(doubles.length * IApexMemoryConstants.FLOAT)];
			// // ByteBuffer byteArray = ByteBuffer.wrap(bytes);
			// // byteArray.asFloatBuffer().put(doubles);
			// return Floats.asList(floats);
		} else if (value instanceof Serializable) {
			try {
				// TODO use a buffer byte[] or ByteBuffer
				return ApexSerializationHelper.toBytes((Serializable) value);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			return value;
		}
	}

	// TODO How is this related to ParquetSchemaConverter?
	public static Schema proposeSimpleSchema(Map<String, ?> schemaAsMap) {
		return proposeSimpleSchema(schemaAsMap, ImmutableBiMap.of());
	}

	public static Schema proposeSchemaForValue(Object value) {
		if (value instanceof CharSequence) {
			return Schema.create(Type.STRING);
		} else if (value instanceof Double) {
			return Schema.create(Type.DOUBLE);
		} else if (value instanceof Float) {
			return Schema.create(Type.FLOAT);
		} else if (value instanceof Long) {
			return Schema.create(Type.LONG);
		} else if (value instanceof Integer) {
			return Schema.create(Type.INT);
			// } else if (value instanceof double[]) {
			// double[] array = (double[]) value;
			//
			// return Schema.createFixed("double_array_" + array.length,
			// "doc",
			// "space",
			// Ints.checkedCast(array.length * IApexMemoryConstants.DOUBLE));
			// } else if (value instanceof float[]) {
			// // float[] array = (float[]) value;
			//
			// return Schema.createArray(Schema.create(Schema.Type.FLOAT));
			// } else if (value instanceof List<?>) {
			// List<?> asList = (List<?>) value;
			//
			// if (asList.isEmpty()) {
			// throw new IllegalArgumentException("Can not specific schema from empty list");
			// }
			//
			// Object firstValue = asList.get(0);
			//
			// return Schema.createArray(guessSchemaFromValue(firstValue));

		} else if (value instanceof Serializable) {
			return Schema.create(Type.BYTES);
		} else {
			throw new UnsupportedOperationException("Can not handle " + ApexLogHelper.getObjectAndClass(value));
		}
	}

	public static Optional<?> proposeDefaultValueForValue(Object value) {
		// If default value is set to null, we would get org.apache.avro.AvroRuntimeException: Field portfoliocode
		// type:STRING pos:1 not set and has no default value
		if (value instanceof CharSequence) {
			return Optional.empty();
		} else if (value instanceof Double) {
			return Optional.empty();
		} else if (value instanceof Float) {
			return Optional.empty();
		} else if (value instanceof Long) {
			return Optional.empty();
		} else if (value instanceof Integer) {
			return Optional.empty();
		} else if (value instanceof double[]) {
			return Optional.empty();
		} else if (value instanceof float[]) {
			return Optional.empty();
		} else if (value instanceof List<?>) {
			return Optional.empty();
		} else if (value instanceof Serializable) {
			return Optional.empty();
		} else {
			throw new UnsupportedOperationException("Can not handle " + ApexLogHelper.getObjectAndClass(value));
		}
	}

	public static Schema proposeSimpleSchema(Map<String, ?> schemaAsMap, BiMap<String, String> sourceToTarget) {
		List<Field> fields = schemaAsMap.entrySet().stream().map(entry -> {

			Schema schema;
			Optional<?> defaultValue;
			try {
				schema = proposeSchemaForValue(entry.getValue());
				defaultValue = proposeDefaultValueForValue(entry.getValue());
			} catch (RuntimeException e) {
				throw new IllegalArgumentException("Can not guess schema for key=" + entry.getKey(), e);
			}

			// The field may be renamed for target Parquet file
			String targetFieldname = sourceToTarget.getOrDefault(entry.getKey(), entry.getKey());

			if (defaultValue.isPresent()) {
				return new Field(targetFieldname, schema, null, defaultValue.get());
			} else {
				// https://stackoverflow.com/questions/22938124/avro-field-default-values
				// https://avro.apache.org/docs/1.7.7/spec.html#Unions
				return new Field(targetFieldname,
						Schema.createUnion(Schema.create(Type.NULL), schema),
						null,
						Schema.NULL_VALUE);
			}

		}).collect(Collectors.toList());
		return Schema.createRecord("myrecord", null, "space", false, fields);
	}

	public static Map<String, Object> convertSparkSchemaToExampleMap(Schema schema) {
		Map<String, Object> schemaAsMap = new HashMap<>();
		schema.getFields().forEach(f -> {
			if (f.schema().getTypes().contains(Schema.create(Type.STRING))) {
				schemaAsMap.put(f.name(), "someString");
			} else if (f.schema().getTypes().contains(Schema.create(Type.INT))) {
				schemaAsMap.put(f.name(), 1);
			} else if (f.schema().getTypes().contains(Schema.create(Type.DOUBLE))) {
				schemaAsMap.put(f.name(), 1D);
			} else if (f.schema().getTypes().stream().filter(t -> t.getType() == Type.ARRAY).findAny().isPresent()) {
				Schema arrayType =
						f.schema().getTypes().stream().filter(t -> t.getType() == Type.ARRAY).findAny().get();
				Schema elementType = arrayType.getElementType();

				if (elementType.getFields().size() == 1
						&& elementType.getFields().get(0).schema().getTypes().contains(Schema.create(Type.DOUBLE))) {
					schemaAsMap.put(f.name(), Collections.singletonList(1D));
				} else {
					throw new RuntimeException("Not handled: " + f);
				}
			} else {
				throw new RuntimeException("Not handled: " + f);
			}
		});
		return schemaAsMap;
	}
}
