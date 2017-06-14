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
package blasd.apex.shared.thread;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

import org.junit.Assert;
import org.junit.Test;

public class TestApexThreadDump {
	protected final ApexThreadDump td = new ApexThreadDump(ManagementFactory.getThreadMXBean());

	@Test
	public void testGetThreadDumpWithMonitor() {
		Assert.assertTrue(td.getThreadDumpAsString(true)
				.contains(this.getClass().getName() + ".testGetThreadDumpWithMonitor(TestApexThreadDump.java:"));
	}

	@Test
	public void testGetThreadDumpWithoutMonitor() {
		Assert.assertTrue(td.getThreadDumpAsString(false)
				.contains(this.getClass().getName() + ".testGetThreadDumpWithoutMonitor(TestApexThreadDump.java:"));
	}

	@Test
	public void testSmartThreadDump() {
		td.getSmartThreadDumpAsString(false);
	}

	@Test
	public void testHasFooter() throws UnsupportedEncodingException, IOException {
		final ThreadInfo[] threads = td.dumpAllThreads(true, true);
		ThreadInfo firstThreadInfo = threads[0];

		for (ThreadInfo ti : threads) {
			StringWriter writer = new StringWriter();
			td.appendThreadFooter(writer, ti);

			if (td.hasFooter(firstThreadInfo)) {
				Assert.assertFalse(writer.toString().isEmpty());
			} else {
				Assert.assertTrue(writer.toString().isEmpty());
			}
		}

	}
}
