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
package blasd.apex.shared.util;

import java.util.Date;
import java.util.Properties;

import org.junit.Test;

public class TestApexPropertiesHelper {

	@Test
	public void testCheck() {
		Properties defaultproperties = new Properties();
		defaultproperties.put("a", 1);
		Properties otherproperties = new Properties(defaultproperties);
		otherproperties.put("c", new Date());

		// A small test to check default properties are handlded

		ApexPropertiesHelper.checkProperties(otherproperties);
	}
}
