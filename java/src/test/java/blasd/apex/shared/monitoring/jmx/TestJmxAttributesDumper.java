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
package blasd.apex.shared.monitoring.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blasd.apex.core.jmx.ApexBasicConnectionDTO;
import blasd.apex.core.jmx.JmxAttributesDumper;
import nl.jqno.equalsverifier.EqualsVerifier;

public class TestJmxAttributesDumper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestJmxAttributesDumper.class);

	@Test
	public void dumpJmx() throws InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException,
			IOException, MalformedObjectNameException {
		JmxAttributesDumper dumper = new JmxAttributesDumper();

		Map<ObjectName, Map<String, Object>> output =
				dumper.dump(ManagementFactory.getPlatformMBeanServer(), null, null);

		Assert.assertFalse(output.isEmpty());

		// Check some key supposed to be present on any JVM
		Map<String, Object> threadMBean = output.get(new ObjectName("java.lang:type=Threading"));
		Assert.assertNotNull(threadMBean);

		Assert.assertNotNull("", threadMBean.get("ThreadCount"));
	}

	@Test
	public void testMain() throws IOException {
		try {
			JmxAttributesDumper.main(new String[0]);
		} catch (Exception e) {
			LOGGER.trace("Exception exception as no host", e);
		}
	}

	@Test
	public void testPrepareConnectionDetails() {
		ApexBasicConnectionDTO details =
				JmxAttributesDumper.prepareConnection(Arrays.asList("host", "123", "user", "pw"));

		Assert.assertEquals("host", details.host);
		Assert.assertEquals(123, details.port);
		Assert.assertEquals("user", details.userName);
		Assert.assertEquals("pw", details.password);
	}

	@Test
	public void testApexBasicConnectionDTOEqualsContract() {
		EqualsVerifier.forClass(ApexBasicConnectionDTO.class).verify();
	}

}
