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
package blasd.apex.core.memory.histogram;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import blasd.apex.server.monitoring.memory.VirtualMachineWithoutToolsJar;

/**
 * Histogramme mémoire.
 * 
 * @author Emeric Vernat
 */
// TODO: restrict to 95% of the Heap
public class HeapHistogram implements IHeapHistogram, Serializable {
	private static final long serialVersionUID = 2163916067335213382L;

	private final List<ClassInfo> classes;
	private final List<ClassInfo> permGenClasses;
	private final Date time;
	private long totalHeapBytes;
	private long totalHeapInstances;
	private long totalPermGenBytes;
	private long totalPermgenInstances;
	private boolean sourceDisplayed;

	HeapHistogram(InputStream in, boolean jrockit) {
		time = new Date();
		final Scanner sc = new Scanner(in, JMAP_CHARSET.toString());
		final List<ClassInfo> classInfos = scan(sc, jrockit);

		classes = new ArrayList<ClassInfo>();
		permGenClasses = new ArrayList<ClassInfo>();

		for (final ClassInfo classInfo : classInfos) {
			if (classInfo.isPermGen()) {
				permGenClasses.add(classInfo);
				totalPermGenBytes += classInfo.getBytes();
				totalPermgenInstances += classInfo.getInstancesCount();
			} else {
				classes.add(classInfo);
				totalHeapBytes += classInfo.getBytes();
				totalHeapInstances += classInfo.getInstancesCount();
			}
			if (!sourceDisplayed && classInfo.getSource() != null) {
				sourceDisplayed = true;
			}
		}
		if (!jrockit) {
			sc.next("Total");
			final long totalInstances = sc.nextLong();
			final long totalBytes = sc.nextLong();
			assert totalInstances == totalPermgenInstances + totalHeapInstances;
			assert totalBytes == totalPermGenBytes + totalHeapBytes;
		}
		sort();
	}

	private void addClassInfo(ClassInfo newClInfo, Map<String, ClassInfo> map) {
		final ClassInfo oldClInfo = map.get(newClInfo.getName());
		if (oldClInfo == null) {
			map.put(newClInfo.getName(), newClInfo);
		} else {
			oldClInfo.add(newClInfo);
		}
	}

	protected Date getTime() {
		return time;
	}

	protected List<ClassInfo> getHeapHistogram() {
		return Collections.unmodifiableList(classes);
	}

	protected long getTotalHeapInstances() {
		return totalHeapInstances;
	}

	@Override
	public long getTotalHeapBytes() {
		return totalHeapBytes;
	}

	List<ClassInfo> getPermGenHistogram() {
		return Collections.unmodifiableList(permGenClasses);
	}

	long getTotalPermGenInstances() {
		return totalPermgenInstances;
	}

	long getTotalPermGenBytes() {
		return totalPermGenBytes;
	}

	boolean isSourceDisplayed() {
		return sourceDisplayed;
	}

	private void sort() {
		final Comparator<ClassInfo> classInfoReversedComparator = Collections.reverseOrder(new ClassInfoComparator());
		Collections.sort(permGenClasses, classInfoReversedComparator);
		Collections.sort(classes, classInfoReversedComparator);
	}

	protected void skipHeader(Scanner sc, boolean jrockit) {
		// num #instances #bytes class name
		// --------------------------------------
		sc.nextLine();
		sc.nextLine();
		if (!jrockit) {
			sc.skip("-+");
			sc.nextLine();
		}
	}

	private static final int DECIMAL_RADIX = 10;

	protected List<ClassInfo> scan(Scanner sc, boolean jrockit) {
		final Map<String, ClassInfo> classInfoMap = new HashMap<String, ClassInfo>();
		sc.useRadix(DECIMAL_RADIX);

		skipHeader(sc, jrockit);

		final String nextLine;
		if (jrockit) {
			nextLine = "[0-9.]+%";
		} else {
			// 1: 1414 6013016 [I
			nextLine = "[0-9]+:";
		}
		while (sc.hasNext(nextLine)) {
			final ClassInfo newClInfo = new ClassInfo(sc, jrockit);
			addClassInfo(newClInfo, classInfoMap);
		}
		return new ArrayList<>(classInfoMap.values());
	}

	/**
	 * @return l'histogramme mémoire
	 * @throws Exception
	 *             e
	 */
	public static HeapHistogram createHeapHistogram() throws Exception {
		try (InputStream input = VirtualMachineWithoutToolsJar.heapHisto()) {
			return new HeapHistogram(input, VirtualMachineWithoutToolsJar.isJRockit());
		}
	}

	public static String createHeapHistogramAsString() throws Exception {
		byte[] byteArray = ByteStreams.toByteArray(VirtualMachineWithoutToolsJar.heapHisto());
		return new String(byteArray, JMAP_CHARSET);
	}

	/**
	 * 
	 * @param file
	 * @param gzipped
	 * @return the number of written bytes
	 * @throws Exception
	 */
	public static long saveHeapDump(File file, boolean gzipped) throws Exception {
		try (InputStream input = VirtualMachineWithoutToolsJar.heapDump()) {
			InputStream wrapped;
			if (gzipped) {
				wrapped = new GZIPInputStream(input);
			} else {
				wrapped = input;
			}

			// According to FileWriteMode, by default we truncate the file
			return Files.asByteSink(file).writeFrom(wrapped);
		}
	}
}