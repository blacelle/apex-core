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
package blasd.apex.server.monitoring.memory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static boolean heapHistogramEnabled = isJmapSupported();

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
		return javaVendor.contains("Sun") || javaVendor.contains("Oracle")
				|| javaVendor.contains("Apple")
				|| isJRockit();
	}

	/**
	 * @return true if JRockit
	 * @see http://www.oracle.com/technetwork/middleware/jrockit/overview/index.html
	 */
	public static boolean isJRockit() {
		// for Hudson/Jenkins
		return getJavaVendor().contains("BEA");
	}

	/**
	 * @return false if not supported or if an attach failed or an histogram failed, true if supported but not tried, or
	 *         tried successfully
	 */
	static synchronized boolean isEnabled() {
		return heapHistogramEnabled;
	}

	/**
	 * @return Singleton initialisé à la demande de l'instance de com.sun.tools.attach.VirtualMachine, null si enabled
	 *         est false
	 * @throws MalformedURLException
	 * @throws ClassNotFoundException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws Exception
	 *             e
	 */
	public static synchronized Object getJvmVirtualMachine() throws ClassNotFoundException, MalformedURLException,
			NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		// si hotspot retourne une instance de
		// sun.tools.attach.HotSpotVirtualMachine
		// cf
		// http://www.java2s.com/Open-Source/Java-Document/6.0-JDK-Modules-sun/tools/sun/tools/attach/HotSpotVirtualMachine.java.htm
		// et sous windows : sun.tools.attach.WindowsVirtualMachine
		if (JVM_VIRTUAL_MACHINE.get() == null) {
			// on utilise la réflexion pour éviter de dépendre de tools.jar du
			// jdk à la compilation
			final Class<?> virtualMachineClass = findVirtualMachineClass();
			final Method attachMethod = virtualMachineClass.getMethod("attach", String.class);
			final String pid = InstrumentationAgent.discoverProcessIdForRunningVM();
			try {
				JVM_VIRTUAL_MACHINE.set(attachMethod.invoke(null, pid));
			} finally {
				heapHistogramEnabled = JVM_VIRTUAL_MACHINE.get() != null;
			}
		}
		return JVM_VIRTUAL_MACHINE.get();
	}

	public static Class<?> findVirtualMachineClass() throws ClassNotFoundException, MalformedURLException {
		// méthode inspirée de javax.tools.ToolProvider.Lazy.findClass
		// http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b27/javax/tools/ToolProvider.java#ToolProvider.Lazy.findClass%28%29
		final String virtualMachineClassName = "com.sun.tools.attach.VirtualMachine";
		try {
			// try loading class directly, in case tools.jar is in the classpath
			return Class.forName(virtualMachineClassName);
		} catch (final ClassNotFoundException e) {
			// exception ignored, try looking in the default tools location
			// (lib/tools.jar)
			File file = new File(System.getProperty("java.home"));
			if ("jre".equalsIgnoreCase(file.getName())) {
				file = file.getParentFile();
			}
			final String[] defaultToolsLocation = { "lib", "tools.jar" };
			for (final String name : defaultToolsLocation) {
				file = new File(file, name);
			}
			// if tools.jar not found, no point in trying a URLClassLoader
			// so rethrow the original exception.
			if (!file.exists()) {
				throw e;
			}

			final URL url = file.toURI().toURL();
			final ClassLoader cl;
			// if (ClassLoader.getSystemClassLoader() instanceof URLClassLoader)
			// {
			// // The attachment API relies on JNI, so if we have other code in
			// the JVM that tries to use the attach API
			// // (like the monitoring of another webapp), it'll cause a failure
			// (issue 398):
			// // "UnsatisfiedLinkError: Native Library C:\Program
			// Files\Java\jdk1.6.0_35\jre\bin\attach.dll already loaded in
			// another classloader
			// // [...] com.sun.tools.attach.AttachNotSupportedException: no
			// providers installed"
			// // So we try to load tools.jar into the system classloader, so
			// that later attempts to load tools.jar will see it.
			// cl = ClassLoader.getSystemClassLoader();
			// // The URLClassLoader.addURL method is protected
			// final Method addURL =
			// URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			// addURL.setAccessible(true);
			// addURL.invoke(cl, url);
			// } else {
			final URL[] urls = { url };
			cl = URLClassLoader.newInstance(urls);
			// }
			return Class.forName(virtualMachineClassName, true, cl);
		} catch (java.lang.UnsupportedClassVersionError e) {
			throw new ClassNotFoundException("Wrong Java version", e);
		}
	}

	/**
	 * Détachement du singleton.
	 * 
	 * @throws Exception
	 *             e
	 */
	public static synchronized void detach() throws Exception { // NOPMD
		if (JVM_VIRTUAL_MACHINE.get() != null) {
			final Class<?> virtualMachineClass = JVM_VIRTUAL_MACHINE.get().getClass();
			final Method detachMethod = virtualMachineClass.getMethod("detach");
			detachMethod.invoke(JVM_VIRTUAL_MACHINE.get());
			JVM_VIRTUAL_MACHINE.set(null);
		}
	}

	/**
	 * @return flux contenant l'histogramme mémoire comme retourné par jmap -histo
	 * @throws Exception
	 *             e
	 */
	public static InputStream heapHisto() throws Exception {
		if (!isJmapSupported()) {
			throw new UnsupportedOperationException("Current JVM does not support HeapHistogram: " + getJavaVendor());
		}
		if (!isEnabled()) {
			throw new UnsupportedOperationException("heap_histo_non_actif");
		}

		return invokeForInputStream("heapHisto", "-all");
	}

	/**
	 * @param string
	 * @param string2
	 * @return
	 * @throws Exception
	 */
	protected static InputStream invokeForInputStream(String methodName, String... argument) throws Exception {
		try {
			final Class<?> virtualMachineClass = getJvmVirtualMachine().getClass();

			return invokeForInputStream(virtualMachineClass, methodName, argument);
		} catch (final ClassNotFoundException e) {
			throw new UnsupportedOperationException("You should use a JDK instead of a JRE", e);
		} catch (final Exception e) {
			// si on obtient com.sun.tools.attach.AttachNotSupportedException:
			// no providers installed
			if ("com.sun.tools.attach.AttachNotSupportedException".equals(e.getClass().getName())) {
				throw new UnsupportedOperationException("Jmap is not available", e);
			}
			throw e;
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
	protected static InputStream invokeForInputStream(Class<?> virtualMachineClass,
			String methodName,
			String... argument) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			ClassNotFoundException, MalformedURLException, NoSuchMethodException {
		// https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr014.html#BABJIIHH
		// http://docs.oracle.com/javase/7/docs/technotes/tools/share/jmap.html
		final Method heapHistoMethod = virtualMachineClass.getMethod(methodName, Object[].class);

		return (InputStream) heapHistoMethod.invoke(getJvmVirtualMachine(), new Object[] { argument });
	}

	public static InputStream heapDump() throws Exception {
		if (!isJmapSupported()) {
			throw new UnsupportedOperationException("Current JVM does not support Jmap: " + getJavaVendor());
		}
		if (!isEnabled()) {
			throw new UnsupportedOperationException("Jmap operations are not available");
		}

		return invokeForInputStream("dumpHeap", "-all");
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
