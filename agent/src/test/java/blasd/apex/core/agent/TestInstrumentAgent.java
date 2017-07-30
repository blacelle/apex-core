/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package blasd.apex.core.agent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class TestInstrumentAgent {
	protected static final Logger LOGGER = Logger.getLogger(TestInstrumentAgent.class.getName());

	@Test
	public void test_ctor() {
		Assert.assertNotNull(new InstrumentationAgent());
	}

	@Test
	public void testPID() {
		Assert.assertTrue(Integer.parseInt(ApexAgentHelper.getPIDForAgent()) > 0);
	}

	@Test
	public void testMain() {
		Instrumentation mock = Mockito.mock(Instrumentation.class);

		InstrumentationAgent.agentmain("args", mock);
	}

	@Test
	public void testPreMain() {
		Instrumentation mock = Mockito.mock(Instrumentation.class);

		InstrumentationAgent.premain("args", mock);
	}

	@Test
	public void testGetPathToJarFileContainingThisClass() {
		try {
			Assert.assertEquals("", ApexAgentHelper.getHoldingJarPath(TestInstrumentAgent.class));
		} catch (IllegalStateException e) {
			// OK, it happens since the test classes are not compiled in a jar
			LOGGER.log(Level.FINE, "Expected exception", e);
		}
	}

	@Test
	public void testGetOrMakePathToJarFile() {
		File jarFile = ApexAgentHelper.getOrMakeHoldingJarPath(TestInstrumentAgent.class);

		Assert.assertTrue(jarFile.isFile());
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
