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
package blasd.apex.shared.monitoring.spring;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.RequiredModelMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.SpringModelMBean;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

/**
 * Extends {@link MBeanExporter} to add logging when a remote call failed. Else, the exception is forwarded to the
 * client, which may not render the exception correctly (e.g. the JConsole does not show the whole stacktrace)
 * 
 * @author Benoit Lacelle
 */
// http://docs.spring.io/autorepo/docs/spring/3.2.x/spring-framework-reference/html/jmx.html
public class LoggingMethodCallAnnotationMBeanExporter extends AnnotationMBeanExporter {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingMethodCallAnnotationMBeanExporter.class);

	// http://stackoverflow.com/questions/5767747/pmd-cpd-ignore-bits-of-code-using-comments
	@SuppressWarnings("CPD-START")
	@Override
	protected ModelMBean createModelMBean() throws MBeanException {
		ModelMBean superModelMBean = super.createModelMBean();

		if (superModelMBean instanceof SpringModelMBean) {
			return new SpringModelMBean() {
				@Override
				public Object invoke(String opName, Object[] opArgs, String[] sig)
						throws MBeanException, ReflectionException {
					try {
						return super.invoke(opName, opArgs, sig);
					} catch (MBeanException | ReflectionException | RuntimeException | Error e) {
						onErrorInRemoteCall(e);
						throw e;
					}
				}
			};
		} else {
			return new RequiredModelMBean() {
				@Override
				public Object invoke(String opName, Object[] opArgs, String[] sig)
						throws MBeanException, ReflectionException {
					try {
						return super.invoke(opName, opArgs, sig);
					} catch (MBeanException | ReflectionException | RuntimeException | Error e) {
						onErrorInRemoteCall(e);
						throw e;
					}
				}
			};
		}
	}

	protected void onErrorInRemoteCall(Throwable e) {
		LOGGER.warn("Issue on a remote call", e);
	}
}
