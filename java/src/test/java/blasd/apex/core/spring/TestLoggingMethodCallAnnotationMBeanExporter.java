package blasd.apex.core.spring;

import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.modelmbean.ModelMBean;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestLoggingMethodCallAnnotationMBeanExporter {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestLoggingMethodCallAnnotationMBeanExporter.class);

	@Test
	public void doLog() throws MBeanException, ReflectionException {
		LoggingMethodCallAnnotationMBeanExporter exporter = new LoggingMethodCallAnnotationMBeanExporter();

		ModelMBean mbean = exporter.createModelMBean();

		try {
			mbean.invoke("actionName", new Object[0], new String[0]);
		} catch (MBeanException e) {
			LOGGER.trace("Expected", e);
		}

		Assert.assertEquals(1, exporter.getNbErrors());
	}

	@Test
	public void doLog_classLoaderNotExposed() throws MBeanException, ReflectionException {
		LoggingMethodCallAnnotationMBeanExporter exporter = new LoggingMethodCallAnnotationMBeanExporter();

		// We instanciate a different kind of bean in this boolean is false
		exporter.setExposeManagedResourceClassLoader(false);

		ModelMBean mbean = exporter.createModelMBean();

		try {
			mbean.invoke("actionName", new Object[0], new String[0]);
		} catch (MBeanException e) {
			LOGGER.trace("Expected", e);
		}

		Assert.assertEquals(1, exporter.getNbErrors());
	}
}
