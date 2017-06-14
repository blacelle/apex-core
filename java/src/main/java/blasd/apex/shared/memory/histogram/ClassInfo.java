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
package blasd.apex.shared.memory.histogram;

import java.io.Serializable;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import blasd.apex.shared.util.IApexMemoryConstants;

/**
 * Histogramme mémoire.
 * 
 * @author Emeric Vernat
 */
public class ClassInfo implements Serializable {
	private static final long serialVersionUID = 6283636454450216347L;
	private static Map<Character, String> arrayTypes = new HashMap<Character, String>();
	static {
		arrayTypes.put('Z', "boolean");
		arrayTypes.put('C', "char");
		arrayTypes.put('B', "byte");
		arrayTypes.put('S', "short");
		arrayTypes.put('I', "int");
		arrayTypes.put('J', "long");
		arrayTypes.put('F', "float");
		arrayTypes.put('D', "double");
		arrayTypes.put('L', "object");
	}

	private long instances;
	private long bytes;
	private final String jvmName;
	private final String name;
	private final boolean permGen;
	private final String source;

	ClassInfo(Scanner sc, boolean jrockit) {
		super();

		sc.next();
		if (jrockit) {
			bytes = parseLongWithK(sc.next());
			instances = sc.nextLong();
		} else {
			instances = sc.nextLong();
			bytes = sc.nextLong();
		}
		jvmName = sc.next();
		permGen = jvmName.charAt(0) == '<';
		name = convertJVMName();
		source = findSource();
	}

	void add(ClassInfo classInfo) {
		assert getName().equals(classInfo.getName());
		this.bytes += classInfo.getBytes();
		this.instances += classInfo.getInstancesCount();
	}

	String getName() {
		return name;
	}

	long getInstancesCount() {
		return instances;
	}

	long getBytes() {
		return bytes;
	}

	boolean isPermGen() {
		return permGen;
	}

	String getSource() {
		return source;
	}

	private String findSource() {
		// on exclue les classes de PermGen et les classes générées
		// dynamiquement
		if (jvmName.endsWith("Klass>") || jvmName.startsWith("sun.reflect.")) {
			return null;
		}
		try {
			final Class<?> clazz = Class.forName(jvmName);
			return findSource(clazz);
		} catch (final LinkageError e) {
			// dans jonas en OSGI, par exemple avec des classes Quartz, il
			// peut survenir
			// des LinkageError (rq: NoClassDefFoundError hérite également
			// de LinkageError)
			return null;
		} catch (final ClassNotFoundException e) {
			// on suppose qu'il y a une seule webapp et que la plupart des
			// classes peuvent être chargées
			// sinon il y a une exception et on retourne null
			return null;
		}
	}

	private static String findSource(Class<?> clazz) {
		final CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
		if (codeSource != null && codeSource.getLocation() != null) {
			String src = codeSource.getLocation().toString();
			if (src.startsWith("file:/")) {
				src = src.substring("file:/".length());
			} else if (src.startsWith("vfs:/")) {
				// "vfs:/" pour jboss 6.0
				src = src.substring("vfs:/".length());
			} else if (src.startsWith("reference:file:/")) {
				// "reference:file:/" pour les bundles jonas
				src = src.substring("reference:file:/".length());
			}
			if (src.endsWith(".jar") || src.endsWith(".war")) {
				src = src.intern();
			}
			return src;
		}
		return null;
	}

	private String convertJVMName() {
		String result;
		final int index = jvmName.lastIndexOf('[');

		if (index != -1) {
			final char code = jvmName.charAt(index + 1);
			if (code == 'L') {
				result = jvmName.substring(index + 2, jvmName.length() - 1);
			} else {
				result = arrayTypes.get(code);
				if (result == null) {
					result = jvmName;
				}
			}
			final StringBuilder sb = new StringBuilder(result);
			for (int i = 0; i <= index; i++) {
				sb.append("[]");
			}
			result = sb.toString();
		} else {
			result = jvmName;
		}
		return result.intern();
	}

	static long parseLongWithK(String text) {
		assert text.length() > 0;
		if (text.charAt(text.length() - 1) == 'k') {
			String t = text.substring(0, text.length() - 1);
			if (t.charAt(0) == '+') {
				t = t.substring(1);
			}
			return IApexMemoryConstants.KB * Long.parseLong(t);
		}
		// inutile car le total n'est pas lu
		// else if (text.endsWith("kB")) {
		// return 1024 * Long.parseLong(text.substring(0, text.length() -
		// 2));
		// }
		return Long.parseLong(text);
	}
}