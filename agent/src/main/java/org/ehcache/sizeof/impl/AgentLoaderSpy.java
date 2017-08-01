package org.ehcache.sizeof.impl;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

public class AgentLoaderSpy {
	public static boolean loadAgent() {
		return AgentLoader.loadAgent();
	}

	public static Instrumentation getInstrumentation() {
		AgentLoaderSpy.loadAgent();
		try {
			Field f = AgentLoader.class.getDeclaredField("instrumentation");

			f.setAccessible(true);
			return (Instrumentation) f.get(null);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			return null;
		} catch (SecurityException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}
}
