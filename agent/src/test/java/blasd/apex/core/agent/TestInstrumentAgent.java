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
import org.slf4j.bridge.SLF4JBridgeHandler;

public class TestInstrumentAgent {
	protected static final Logger LOGGER = Logger.getLogger(TestInstrumentAgent.class.getName());

	// When run from Eclipse, logging.properties is not found (unlike in maven where surefire config refers to
	// logging.properties)
	static {
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

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
			InstrumentationAgent.ensureAgentInitialisation();
		} catch (RuntimeException e) {
			// OK, it happens since the test classes are not compiled in a jar: we should have succeed wrapping them in
			// a jar, but we may be missing the manifest file
			// Expected RuntimeException from org.springframework.boot.loader.tools.AgentAttacher.attach(File)
			LOGGER.log(Level.FINE, "Expected exception", e);

			Assert.assertNotNull(e.getCause());
			Assert.assertNotNull(e.getCause().getCause());

			// The root-cause is actually that the manifest is missing
			Throwable rootCause = e.getCause().getCause();
			Assert.assertEquals("com.sun.tools.attach.AgentLoadException", rootCause.getClass().getName());
			Assert.assertEquals("Agent JAR not found or no Agent-Class attribute", rootCause.getMessage());
		}
	}

}
