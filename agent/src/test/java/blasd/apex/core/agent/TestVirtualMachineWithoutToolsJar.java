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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import blasd.apex.core.agent.VirtualMachineWithoutToolsJar;

public class TestVirtualMachineWithoutToolsJar {
	@Test
	public void testFindVirtualMachineClass() throws ClassNotFoundException, MalformedURLException {
		Assert.assertEquals("class com.sun.tools.attach.VirtualMachine",
				VirtualMachineWithoutToolsJar.findVirtualMachineClass().toString());
	}

	@Test
	public void testIsJRockit() {
		Assert.assertFalse(VirtualMachineWithoutToolsJar.isJRockit());
	}

	@Test
	public void testHeapHisto() throws Exception {
		InputStream is = VirtualMachineWithoutToolsJar.heapHisto();
		String asString = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
		Assert.assertNotNull(asString);
	}

	@Test
	public void testJvmDetach() throws Exception {
		Object jvm = VirtualMachineWithoutToolsJar.getJvmVirtualMachine();
		Assert.assertNotNull(jvm);
		VirtualMachineWithoutToolsJar.detach();
	}

	@Test
	public void testIsVirtualMachineWithoutToolsJar() {
		VirtualMachineWithoutToolsJar.isVirtualMachineAvailable();
	}
}
