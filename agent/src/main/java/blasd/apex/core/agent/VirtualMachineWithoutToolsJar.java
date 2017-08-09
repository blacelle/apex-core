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
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.ehcache.sizeof.impl.AgentLoaderApexSpy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Classe d'attachement dynamique utilisée ici pour obtenir l'histogramme de la mémoire. <br/>
 * Cette classe nécessite tools.jar du jdk pour être exécutée (ok dans tomcat), mais pas pour être compilée. <br/>
 * 
 * @see <a href=
 *      "http://java.sun.com/javase/6/docs/jdk/api/attach/spec/com/sun/tools/attach/VirtualMachine.html#attach(java.lang.String)"
 *      >VirtualMachine</a>
 * 
 * @author Emeric Vernat
 */
// https://github.com/javamelody/javamelody/blob/master/javamelody-core/src/main/java/net/bull/javamelody/VirtualMachine.java
public class VirtualMachineWithoutToolsJar {
	protected static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachineWithoutToolsJar.class);

	// http://cr.openjdk.java.net/~malenkov/8022746.8.1/jdk/src/share/classes/sun/tools/jmap/JMap.java.html
	private static final String LIVE_OBJECTS_OPTION = "-live";
	private static final String ALL_OBJECTS_OPTION = "-all";

	// Switched to true if incompatible JVM, or attach failed
	private static final AtomicBoolean WILL_NOT_WORK = new AtomicBoolean(false);

	private static final AtomicReference<Object> JVM_VIRTUAL_MACHINE = new AtomicReference<Object>();

	protected VirtualMachineWithoutToolsJar() {
		// hidden
	}

	/**
	 * @return true if heap histogram is supported
	 */
	static boolean isJmapSupported() {
		// pour nodes Hudson/Jenkins, on réévalue sans utiliser de constante
		final String javaVendor = getJavaVendor();
		// http://www.oracle.com/technetwork/middleware/jrockit/overview/index.html
		return javaVendor.contains("Sun") || javaVendor.contains("Oracle")
				|| javaVendor.contains("Apple")
				|| isJRockit();
	}

	/**
	 * @return true if current JVM is a JRockIt JVM
	 */
	public static boolean isJRockit() {
		return getJavaVendor().contains("BEA");
	}

	public static synchronized Optional<Object> getJvmVirtualMachine() {
		try {
			return getUnsafeJvmVirtualMachine();
		} catch (Throwable e) {
			LOGGER.warn("Issue while loading VirtualMachine", e);
			return Optional.absent();
		}
	}

	public static synchronized Optional<Object> getUnsafeJvmVirtualMachine() throws ClassNotFoundException,
			MalformedURLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (WILL_NOT_WORK.get()) {
			return Optional.absent();
		}

		// si hotspot retourne une instance de
		// sun.tools.attach.HotSpotVirtualMachine
		// cf
		// http://www.java2s.com/Open-Source/Java-Document/6.0-JDK-Modules-sun/tools/sun/tools/attach/HotSpotVirtualMachine.java.htm
		// et sous windows : sun.tools.attach.WindowsVirtualMachine
		if (JVM_VIRTUAL_MACHINE.get() == null) {
			final Optional<? extends Class<?>> virtualMachineClass = findVirtualMachineClass();

			if (virtualMachineClass.isPresent()) {
				final Method attachMethod = virtualMachineClass.get().getMethod("attach", String.class);
				final String pid = ApexAgentHelper.getPIDForAgent();
				try {
					JVM_VIRTUAL_MACHINE.set(attachMethod.invoke(null, pid));
				} finally {
					if (JVM_VIRTUAL_MACHINE.get() == null) {
						LOGGER.warn("Failure attaching VirtualMachine");
						WILL_NOT_WORK.set(true);
					} else {
						Class<? extends Object> vmClass = JVM_VIRTUAL_MACHINE.get().getClass();
						LOGGER.info("VirtualMachine has been loaded: {}. Available methods: {}",
								vmClass.getName(),
								Arrays.asList(vmClass.getMethods()));
					}
				}
			}
		}
		return Optional.fromNullable(JVM_VIRTUAL_MACHINE.get());
	}

	public static Optional<? extends Class<?>> findVirtualMachineClass() {
		try {
			return AgentLoaderApexSpy.getVirtualMachineClass();
		} catch (Throwable e) {
			LOGGER.warn("Issue while getting VirtualMachine class", e);
			return Optional.absent();
		}
	}

	/**
	 * Détachement du singleton.
	 * 
	 * @throws Exception
	 *             e
	 */
	public static synchronized void detach() throws Exception {
		// Ensure VirtualMachine reference will not be used anymore
		Object localRef = JVM_VIRTUAL_MACHINE.getAndSet(null);
		if (localRef != null) {
			// We have an attached VirtualMachine : detach it
			final Method detachMethod = localRef.getClass().getMethod("detach");
			detachMethod.invoke(localRef);
		}
	}

	/**
	 * @return flux contenant l'histogramme mémoire comme retourné par jmap -histo
	 * @throws Exception
	 *             e
	 */
	public static Optional<InputStream> heapHisto() {
		Optional<InputStream> asInputStream = getJvmVirtualMachine().transform(new Function<Object, InputStream>() {

			@Override
			public InputStream apply(Object vm) {
				try {
					return invokeForInputStream(vm, "heapHisto", ALL_OBJECTS_OPTION);
				} catch (Throwable e) {
					throw new RuntimeException("Issue on invoking 'heapHisto -all'", e);
				}
			}

		});

		if (!asInputStream.isPresent()) {
			LOGGER.warn("'heapHisto' seems not available. Java-version: {}", getJavaVendor());
		}

		return asInputStream;
	}

	/**
	 * 
	 * @param allObjectsElseLive
	 * @return if true, use "-all" option, else use "-live" but beware it will trigger a full GC
	 */
	public static Optional<InputStream> heapDump(final File targetFile, final boolean allObjectsElseLive) {
		final File absoluteFile = targetFile.getAbsoluteFile();
		if (absoluteFile.exists()) {
			throw new IllegalArgumentException("Can not write heap-dump as file already exists: " + absoluteFile);
		}

		Optional<InputStream> asInputStream = getJvmVirtualMachine().transform(new Function<Object, InputStream>() {

			@Override
			public InputStream apply(Object vm) {
				String option = getAllorLiveOption(allObjectsElseLive);
				try {
					return invokeForInputStream(vm, "dumpHeap", absoluteFile.getPath(), option);
				} catch (Throwable e) {
					throw new RuntimeException("Issue on invoking 'dumpHeap " + option + "'", e);
				}
			}

		});

		if (!asInputStream.isPresent()) {
			LOGGER.warn("'dumpHeap' seems not available. Java-version: {}", getJavaVendor());
		}

		return asInputStream;
	}

	protected static String getAllorLiveOption(boolean allObjectsElseLive) {
		if (allObjectsElseLive) {
			return ALL_OBJECTS_OPTION;
		} else {
			return LIVE_OBJECTS_OPTION;
		}
	}

	/**
	 * @param string
	 *            the methodName
	 * @param string2
	 * @return
	 * @throws Exception
	 */
	protected static InputStream invokeForInputStream(String methodName, String... argument) {
		try {
			final Class<?> virtualMachineClass = getJvmVirtualMachine().getClass();

			return invokeForInputStream(virtualMachineClass, methodName, argument);
		} catch (final ClassNotFoundException e) {
			throw new UnsupportedOperationException("You should use a JDK instead of a JRE", e);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param virtualMachineClass
	 * @param methodName
	 * @param argument
	 * @return
	 * @throws NoSuchMethodException
	 * @throws MalformedURLException
	 * @throws ClassNotFoundException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	protected static InputStream invokeForInputStream(Object virtualMachine, String methodName, String... argument)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException,
			MalformedURLException, NoSuchMethodException {
		if (virtualMachine == null) {
			throw new IllegalArgumentException("VirtualMachine is null");
		}

		Class<?> vmClass = virtualMachine.getClass();

		// https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr014.html#BABJIIHH
		// http://docs.oracle.com/javase/7/docs/technotes/tools/share/jmap.html
		final Method methodForInputStream = vmClass.getMethod(methodName, Object[].class);

		LOGGER.info("About to invoke {} on {}", methodName, vmClass);
		return (InputStream) methodForInputStream.invoke(virtualMachine, new Object[] { argument });
	}

	private static String getJavaVendor() {
		return System.getProperty("java.vendor");
	}

	public static boolean isVirtualMachineAvailable() {
		try {
			if (getJvmVirtualMachine() != null) {
				return true;
			} else {
				return false;
			}
		} catch (Throwable e) {
			// Whatever the reason is, the VirtualMachine is not available. It
			// could be an error if we load from an incompatible java version
			LOGGER.trace("VirtualMachine is not available", e);
			return false;
		}
	}
}
