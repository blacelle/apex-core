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
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.loader.tools.AgentAttacher;

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

	// SLF4J in not available in the Agents
	protected static final Logger LOGGER = Logger.getLogger(InstrumentationAgent.class.getName());

	// The initialization can fail for many reasons (tools.jar not available,...)
	private static final AtomicBoolean INIT_TRIED = new AtomicBoolean(false);

	private static final AtomicReference<Instrumentation> INTRUMENTATION_REF = new AtomicReference<Instrumentation>();

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

		INTRUMENTATION_REF.set(instr);
	}

	public static void agentmain(String args, Instrumentation instr) {
		System.out.println(InstrumentationAgent.class + ": agentmain");

		INTRUMENTATION_REF.set(instr);
	}

	public static void ensureAgentInitialisation() {
		// Try only once to hook the agent
		if (INIT_TRIED.compareAndSet(false, true)) {
			try {
				File holdingJarPath = ApexAgentHelper.getOrMakeHoldingJarPath(InstrumentationAgent.class);
				if (holdingJarPath != null) {
					// TODO we may want a custom .attach method to enable options
					// https://blogs.oracle.com/corejavatechtips/the-attach-api
					// https://github.com/avaje-common/avaje-agentloader/blob/master/src/main/java/org/avaje/agentloader/AgentLoader.java
					String suffix = InstrumentationAgent.class + " from " + holdingJarPath;
					LOGGER.log(Level.INFO, "Attaching the agent for " + suffix);
					AgentAttacher.attach(holdingJarPath);
					LOGGER.log(Level.INFO, "Attached successfully the agent for " + suffix);
				} else {
					LOGGER.log(Level.SEVERE, "Can not find a jar holding the class " + InstrumentationAgent.class);
				}
			} catch (RuntimeException e) {
				// makes sure the exception gets printed at
				// least once
				LOGGER.log(Level.SEVERE, "Ouch", e);
				throw e;
			}
		}
	}

	/**
	 * 
	 * @return an {@link Instrumentation} instance as instantiated by the JVM itself
	 */
	public static Instrumentation getInstrumentation() {
		InstrumentationAgent.ensureAgentInitialisation();

		return INTRUMENTATION_REF.get();
	}

	/**
	 * 
	 * @return an {@link Instrumentation} instance as instantiated by the JVM itself, or null if anything bad happened
	 */
	public static Instrumentation safeGetInstrumentation() {
		try {
			return getInstrumentation();
		} catch (Throwable e) {
			Throwable s = e;
			while (s != null) {
				System.out.println(s.getMessage());
				System.out.println(Arrays.asList(s.getStackTrace()));
				if (s == e.getCause()) {
					break;
				} else {
					s = e.getCause();
				}
			}
			LOGGER.log(Level.INFO, "Issue while getting instrumentation", e);
			return null;
		}
	}
}