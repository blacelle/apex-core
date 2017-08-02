package org.ehcache.sizeof.impl;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * Introspect org.ehcache.sizeof.impl.AgentLoader to retrieve a reference to Instrumentation
 * 
 * @author Benoit Lacelle
 *
 */
public class AgentLoaderApexSpy {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AgentLoaderApexSpy.class);

	private static final AtomicBoolean HAS_TRIED_LOADING_AGENT = new AtomicBoolean();

	public static boolean loadAgent() {
		return AgentLoader.loadAgent();
	}

	public static Optional<Instrumentation> getInstrumentation()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		if (HAS_TRIED_LOADING_AGENT.compareAndSet(false, true)) {
			LOGGER.info("Initializing Agent to provide a reference to {}", Instrumentation.class.getName());

			// Load the agent as first try
			AgentLoaderApexSpy.loadAgent();
		}
		Field f = AgentLoader.class.getDeclaredField("instrumentation");

		f.setAccessible(true);

		return Optional.fromNullable((Instrumentation) f.get(null));
	}

	public static Optional<? extends Class<?>> getVirtualMachineClass() throws NoSuchMethodException, SecurityException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method f = AgentLoader.class.getDeclaredMethod("getVirtualMachineClass");

		f.setAccessible(true);

		Class<?> vmClass = (Class<?>) f.invoke(null);
		return Optional.fromNullable(vmClass);
	}
}
