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
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;

import blasd.apex.core.io.ApexSerializationHelper;

/**
 * Helps converting avro records to ActivePivot objects
 * 
 * @author Benoit Lacelle
 *
 */
public class AvroFieldHelper {
	protected AvroFieldHelper() {
		// hidden
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
		} else if (value instanceof byte[]) {
			Object targetType = exampleValue.get();

			if (targetType != null) {
				try {
					value = ApexSerializationHelper.fromBytes((byte[]) value);
				} catch (ClassNotFoundException | IOException e) {
					throw new RuntimeException(e);
				}
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

}
