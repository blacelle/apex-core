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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

import blasd.apex.core.avro.ApexAvroToActivePivotHelper;
import blasd.apex.parquet.ParquetStreamFactory;

public class TestApexParquetToStream {
	@Test
	public void testCacheDefaultConfiguration() {
		Configuration config = ParquetStreamFactory.cloneDefaultConfiguration();

		Field field = ReflectionUtils.findField(Configuration.class, "properties", Properties.class);

		ReflectionUtils.makeAccessible(field);

		Properties p = (Properties) ReflectionUtils.getField(field, config);

		// Check the properties is already initialized with default configuration
		Assert.assertNotNull(p);
	}

	@Test
	public void testEmptyListNoTarget() {
		Assert.assertFalse(ApexAvroToActivePivotHelper.toPrimitiveArray(null, Arrays.asList()).isPresent());
	}

	@Test
	public void testListDoubleToFloat() {
		float listElement = 1F;
		Assert.assertArrayEquals(new double[] { 1D },
				(double[]) ApexAvroToActivePivotHelper.toPrimitiveArray(new double[0], Arrays.asList(listElement))
						.get(),
				0.001D);
	}

	@Test
	public void testListFloatToDouble() {
		Double listElement = 1D;
		Assert.assertArrayEquals(new float[] { 1F },
				(float[]) ApexAvroToActivePivotHelper.toPrimitiveArray(new float[0], Arrays.asList(listElement)).get(),
				0.001F);
	}
}
