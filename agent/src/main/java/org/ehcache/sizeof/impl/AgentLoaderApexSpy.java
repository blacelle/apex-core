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
package org.ehcache.sizeof.impl;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

	// AgentLoader loads the VirutalMachine in a brand new URLClassLoader on each try: it leads to stacks like:
	// java.lang.UnsatisfiedLinkError: Native Library attach.dll already loaded in another classloader
	// or
	// Caused by: com.sun.tools.attach.AttachNotSupportedException: no providers installed
	private static final AtomicReference<Class<?>> VM_CLASS_CACHE = new AtomicReference<Class<?>>();

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

	/**
	 * Soft access to com.sun.tools.attach.VirtualMachine, as it may not be available in the classpath
	 * 
	 * @return if available, the Class of the VirtualMachine object
	 */
	public static Optional<? extends Class<?>> getVirtualMachineClass()
			throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = VM_CLASS_CACHE.get();

		if (clazz == null) {
			// !!! Do not call getVirtualMachineClass else it would load the VirtualMachine in a different
			// URLClassLoader (see AgentLoader.getVirtualMachineClass implementation)
			Field f = AgentLoader.class.getDeclaredField("VIRTUAL_MACHINE_ATTACH");

			f.setAccessible(true);

			// AgentLoader has filled a filled with a Method object
			Object rawAttachMethod = f.get(null);

			if (rawAttachMethod instanceof Method) {
				Method attachMethod = (Method) rawAttachMethod;

				// The VirtualMachine class in the class declaring the method '.attach'
				Class<?> vmClass = attachMethod.getDeclaringClass();

				VM_CLASS_CACHE.compareAndSet(null, vmClass);
			}
		}

		return Optional.fromNullable(VM_CLASS_CACHE.get());
	}
}
