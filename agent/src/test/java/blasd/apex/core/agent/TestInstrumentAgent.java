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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.agent.ApexAgentHelper;
import blasd.apex.core.agent.InstrumentationAgent;

public class TestInstrumentAgent {
	protected static final Logger LOGGER = Logger.getLogger(TestInstrumentAgent.class.getName());

	@Test
	public void testPID() {
		Assert.assertTrue(Integer.parseInt(ApexAgentHelper.getPIDForAgent()) > 0);
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
	public void testinitializeIfNeeded() {
		try {
			InstrumentationAgent.initializeIfNeeded();
		} catch (IllegalStateException e) {
			// OK, it happens since the test classes are not compiled in a jar
			LOGGER.log(Level.FINE, "Expected exception", e);
		}
	}

}
