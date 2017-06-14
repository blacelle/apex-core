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
package blasd.apex.shared.monitoring.setstatic;

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TestSetStatic {
	public static String STRING_STATIC = "-";
	public static LocalDate LOCALDATE_STATIC = new LocalDate();
	private static final double DOUBLE_STATIC = 0D;

	@Test
	public void testSetStatic() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		SetStaticMBean setSTatic = new SetStaticMBean();

		// Modify from current value
		String newValue = STRING_STATIC + "-";

		// Do the modification
		setSTatic.setStatic(TestSetStatic.class.getName(), "STRING_STATIC", newValue);

		Assert.assertEquals(newValue, STRING_STATIC);
	}

	@Test
	public void testSetStaticLocalDate() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		SetStaticMBean setSTatic = new SetStaticMBean();

		LocalDate initialDate = LOCALDATE_STATIC;

		// Modify from current value
		String newValue = initialDate.minusDays(1).toString();

		// Do the modification
		setSTatic.setStatic(TestSetStatic.class.getName(), "LOCALDATE_STATIC", newValue);

		Assert.assertEquals(initialDate.minusDays(1), LOCALDATE_STATIC);
	}

	@Test
	public void testGetStaticLocalDate() throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		SetStaticMBean setSTatic = new SetStaticMBean();

		LocalDate initialDate = LOCALDATE_STATIC;

		String className = TestSetStatic.class.getName();
		Assert.assertEquals(initialDate, setSTatic.getStatic(className, "LOCALDATE_STATIC"));
		Assert.assertEquals(initialDate.toString(), setSTatic.getStaticAsString(className, "LOCALDATE_STATIC"));
	}

	@Ignore
	@Test
	public void testSetStaticPrivateFinalDouble()
			throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
		SetStaticMBean setSTatic = new SetStaticMBean();

		double initialDouble = DOUBLE_STATIC;

		// Modify from current value
		String newValue = initialDouble + 1D + "";

		// Do the modification
		setSTatic.setStatic(TestSetStatic.class.getName(), "DOUBLE_STATIC", newValue);

		Assert.assertEquals(initialDouble + 1D, DOUBLE_STATIC, 0.0001D);
	}
}
