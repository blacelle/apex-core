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
package blasd.apex.server.monitoring.memory;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

public class TestInstrumentAgent {
	protected static final Logger LOGGER = Logger.getLogger(TestInstrumentAgent.class.getName());

	@Test
	public void testPID() {
		Assert.assertTrue(Integer.parseInt(InstrumentationAgent.discoverProcessIdForRunningVM()) > 0);
	}

	@Test
	public void testGetPathToJarFileContainingThisClass() {
		try {
			Assert.assertEquals("",
					InstrumentationAgent.getPathToJarFileContainingThisClass(TestInstrumentAgent.class));
		} catch (IllegalStateException e) {
			// OK, it happens since the test classes are not compiled in a jar
			LOGGER.log(Level.FINE, "Expected exception", e);
		}
	}

	@Test
	public void testinitializeIfNeeded() {
		try {
			InstrumentationAgent.initializeIfNeeded();
		} catch (IllegalStateException e) {
			// OK, it happens since the test classes are not compiled in a jar
			LOGGER.log(Level.FINE, "Expected exception", e);
		}
	}

}
