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
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.CodeSource;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

/**
 * The entry point for the instrumentation agent.
 * 
 * Either the premain method is called by adding the JVM arg -javaagent. Or one could call the
 * {@link InstrumentationAgent#getInstrumentation()} method. Anyway, tools.jar will have to be present at Runtime
 * 
 * @author Benoit Lacelle
 * 
 */
public class InstrumentationAgent {
	private static volatile Instrumentation instrumentation;

	protected InstrumentationAgent() {
		// Hide the constructor
	}

	/**
	 * This premain method is called by adding a JVM argument:
	 * 
	 * -javaagent:path\to\jar\monitor-1.SNAPSHOT.jar
	 * 
	 */
	public static void premain(String args, Instrumentation instr) {
		System.out.println(InstrumentationAgent.class + ": premain");

		instrumentation = instr;
	}

	public static void agentmain(String args, Instrumentation instr) {
		System.out.println(InstrumentationAgent.class + ": agentmain");

		instrumentation = instr;
	}

	public static void initializeIfNeeded() {
		if (instrumentation == null) {
			try {
				String pid = discoverProcessIdForRunningVM();

				final VirtualMachine vm = attachToThisVM(pid);

				loadAgentAndDetachFromThisVM(vm, getPathToJarFileContainingThisClass(InstrumentationAgent.class));

				if (instrumentation == null) {
					throw new RuntimeException("The loading of the agent failed");
				}

			} catch (RuntimeException e) {
				// makes sure the exception gets printed at
				// least once
				e.printStackTrace();
				throw e;
			}
		}
	}

	public static String getPathToJarFileContainingThisClass(Class<?> clazz) {
		CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();

		if (codeSource == null) {
			return null;
		}

		// URI is needed to deal with spaces and non-ASCII
		// characters
		URI jarFileURI;

		try {
			jarFileURI = codeSource.getLocation().toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		String path = new File(jarFileURI).getPath();

		if (!path.toLowerCase().endsWith(".jar")) {
			throw new IllegalStateException(
					InstrumentationAgent.class + " should be in a jar file. It has been found in: " + path);
		}

		return path;
	}

	private static void loadAgentAndDetachFromThisVM(VirtualMachine vm, String jarFilePath) {
		try {
			System.out.println("Loading Agent: " + vm + " from " + jarFilePath);
			// noinspection ConstantConditions
			vm.loadAgent(jarFilePath);
			vm.detach();
		} catch (AgentLoadException e) {
			throw new RuntimeException(e);
		} catch (AgentInitializationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String discoverProcessIdForRunningVM() {
		String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
		// first, reliable with sun jdk
		// (http://golesny.de/wiki/code:javahowtogetpid)
		/* tested on: */
		/* - windows xp sp 2, java 1.5.0_13 */
		/* - mac os x 10.4.10, java 1.5.0 */
		/* - debian linux, java 1.5.0_13 */
		/* all return pid@host, e.g 2204@antonius */
		int p = nameOfRunningVM.indexOf('@');

		return nameOfRunningVM.substring(0, p);
	}

	private static VirtualMachine attachToThisVM(String pid) {
		try {
			return VirtualMachine.attach(pid);
		} catch (AttachNotSupportedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 
	 * @return an {@link Instrumentation} instance as instanciated by the JVM itself
	 */
	public static Instrumentation getInstrumentation() {
		if (instrumentation == null) {
			InstrumentationAgent.initializeIfNeeded();
		}
		return instrumentation;
	}
}