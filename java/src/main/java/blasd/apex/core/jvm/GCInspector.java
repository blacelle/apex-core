/**
 * The MIT License
 * Copyright (c) ${project.inceptionYear} Benoit Lacelle
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
package blasd.apex.core.jvm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AtomicLongMap;
import com.sun.management.GarbageCollectionNotificationInfo;

import blasd.apex.core.jmx.ApexJMXHelper;
import blasd.apex.core.logging.ApexLogHelper;
import blasd.apex.core.memory.ApexMemoryHelper;
import blasd.apex.core.memory.IApexMemoryConstants;
import blasd.apex.core.memory.histogram.HeapHistogram;
import blasd.apex.core.memory.histogram.IHeapHistogram;
import blasd.apex.core.thread.ApexThreadDump;
import blasd.apex.core.thread.IApexThreadDumper;
import blasd.apex.server.monitoring.memory.VirtualMachineWithoutToolsJar;
import sun.misc.VM;

/**
 * 
 * This class registers itself as listener on GC events. It will produce a thread-dump when long GC pauses happens
 * 
 * @author Benoit Lacelle
 * @since Oracle Java 7 update 4 JVM
 */
@ManagedResource
public class GCInspector implements NotificationListener, InitializingBean, DisposableBean, IGCInspector {
	protected static final Logger LOGGER = LoggerFactory.getLogger(GCInspector.class);

	/**
	 * Remember the length of the first GC, which is used as heuristic to know if GC times are expressed in ms or ns
	 */
	protected final AtomicLong firstGcNotZero = new AtomicLong();

	/**
	 * If the first GC pause is bigger than this size, we expect GC pauses are expressed in NS
	 */
	protected static final long MAX_FIRST_PAUSE_MS = 5000;

	/**
	 * @deprecated One should use TimeUnit.NANOSECONDS.toMillis(timeInNs)
	 */
	@Deprecated
	public static final int NS_TO_MS = 1000000;

	public static final float BETWEEN_MINUS_ONE_AND_ZERO = -0.5F;

	/**
	 * Any time a GC lasts longer than this duration, we log details about the GC
	 */
	public static final long DEFAULT_GCDURATION_MILLIS_INFO_LOG = 200;
	protected long gcDurationMillisForInfoLog = DEFAULT_GCDURATION_MILLIS_INFO_LOG;

	/**
	 * If a MarkSweep GC lasts more than this duration, we log a ThreadDump
	 */
	public static final long DEFAULT_MARKSWEEP_MILLIS_THREADDUMP = 10000;
	protected long marksweepDurationMillisForThreadDump = DEFAULT_MARKSWEEP_MILLIS_THREADDUMP;

	public static final long DEFAULT_MARKSWEEP_MILLIS_HEAPHISTOGRAM = 10000;
	protected long marksweepDurationMillisForHeapHistogram = DEFAULT_MARKSWEEP_MILLIS_HEAPHISTOGRAM;

	public static final long DEFAULT_MAX_HEAP_GB_HEAPHISTOGRAM = 20;
	protected long maxHeapGbForHeapHistogram = DEFAULT_MAX_HEAP_GB_HEAPHISTOGRAM;

	protected static final MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();
	protected static final OperatingSystemMXBean OS_MBEAN = ManagementFactory.getOperatingSystemMXBean();
	protected static final ThreadMXBean THREAD_MBEAN = ManagementFactory.getThreadMXBean();
	protected static final List<MemoryPoolMXBean> MEMORY_POOLS_MBEAN = ManagementFactory.getMemoryPoolMXBeans();

	protected AtomicReference<LocalDateTime> latestThreadDump = new AtomicReference<>();

	private final AtomicReference<Map<? extends String, ? extends Long>> allocatedHeapReference =
			new AtomicReference<>(Collections.emptyMap());

	private final AtomicReference<Map<? extends String, ? extends Long>> heapGCNotifReference =
			new AtomicReference<>(Collections.emptyMap());

	private static final double HEAP_ALERT_THRESHOLD = 0.9D;
	private static final int HEAP_ALERT_PERIOD_IN_MINUTES = 15;
	private final AtomicReference<LocalDateTime> overHeapThresholdSince = new AtomicReference<>();

	protected final IApexThreadDumper apexThreadDumper;

	protected final AtomicLong targetMaxTotalMemory = new AtomicLong(Long.MAX_VALUE);

	public GCInspector(IApexThreadDumper apexThreadDumper) {
		this.apexThreadDumper = apexThreadDumper;
	}

	/**
	 * Default constructor with nice default
	 */
	public GCInspector() {
		this(new ApexThreadDump(ManagementFactory.getThreadMXBean()));
	}

	@ManagedAttribute
	public void setTargetMaxTotalMemory(String targetMax) {
		long asLong = ApexMemoryHelper.memoryAsLong(targetMax);

		targetMaxTotalMemory.set(asLong);
	}

	@ManagedAttribute
	public long getTargetMaxTotalMemory() {
		return targetMaxTotalMemory.get();
	}

	@Override
	public void afterPropertiesSet() throws MalformedObjectNameException, InstanceNotFoundException {
		ObjectName gcName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");

		// Register this as listener for any GC event
		for (ObjectName name : MBEAN_SERVER.queryNames(gcName, null)) {
			MBEAN_SERVER.addNotificationListener(name, this, null, null);
		}

	}

	// We prefer to submit a closing status when the bean is disposed, as the JVM may never terminate correctly in case
	// of OOM, or Dead/LiveLock
	@Deprecated
	protected void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(
				new Thread(() -> executeDuringShutdown(), this.getClass().getSimpleName() + "-ShutdownHook"));
	}

	protected void executeDuringShutdown() {
		// On shutdown, do not print too many information as, very often, it is a clean closing (e.g. unit-tests).
		// Still, if something is wrong, it is very beneficial to have core information

		if (inUnitTest()) {
			LOGGER.info("Skip GCInspector closing information as current run is a unit-test");
		} else {
			printSmartThreadDump();
			printHeapHistogram(HEAP_HISTO_LIMIT_NB_ROWS);
		}
	}

	public static boolean inUnitTest() {
		// In maven: org.apache.maven.surefire.booter.ForkedBooter.exit(ForkedBooter.java:144)
		// Bean disposing is expected to be done in the main thead: does this main thread comes from junit or surefire?

		Optional<StackTraceElement> matching =
				Arrays.stream(Thread.currentThread().getStackTrace())
						.filter(ste -> Arrays.asList(".surefire.", ".failsafe.", ".junit.")
								.stream()
								.filter(name -> ste.getClassName().contains(name))
								.findAny()
								.isPresent())
						.findAny();

		matching.ifPresent(ste -> LOGGER.info("We have detected a unit-test with: {}", ste));

		return matching.isPresent();
	}

	/**
	 * Clean the MBean registration. Else, unit-test would register several GCInexpector (one for each Context loaded)
	 */
	@Override
	public void destroy() throws Exception {
		removeNotificationListener();

		executeDuringShutdown();
	}

	protected void removeNotificationListener() throws MalformedObjectNameException, ListenerNotFoundException {
		ObjectName gcName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");

		// Register this as listener for any GC event
		for (ObjectName name : MBEAN_SERVER.queryNames(gcName, null)) {
			try {
				MBEAN_SERVER.removeNotificationListener(name, this);
			} catch (InstanceNotFoundException | RuntimeException e) {
				// Log in debug as no big-deal to fail disconnecting beans
				LOGGER.debug("Failure for " + name, e);
			}
		}
	}

	@Override
	@SuppressWarnings("restriction")
	public void handleNotification(Notification notification, Object handback) {
		String type = notification.getType();
		if (type.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
			// retrieve the garbage collection notification information
			CompositeData cd = (CompositeData) notification.getUserData();
			GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);

			doLog(info);
		}
	}

	protected long computeDurationMs(@SuppressWarnings("restriction") GarbageCollectionNotificationInfo info) {
		@SuppressWarnings("restriction")
		long rawDuration = info.getGcInfo().getDuration();

		if (rawDuration == 0) {
			return 0;
		}

		if (firstGcNotZero.compareAndSet(0, rawDuration)) {
			// We expect first GC to be less than 5 seconds. If it is
			if (firstGcNotZero.get() > MAX_FIRST_PAUSE_MS) {
				// Duration are supposed to be expressed in ms. The first pause
				// is supposed to be short. If it is very long, it probably
				// means it is expressed in ns
				// http://www.docjar.com/docs/api/com/sun/management/GcInfo.html#getDuration
				LOGGER.warn("We guess GC times are expressed in ns instead of ms since first pause lasted {}?s",
						firstGcNotZero.get());
			} else {
				LOGGER.info("We guess GC times are expressed in ms as first GC pause lasted {}?s",
						firstGcNotZero.get());
			}
		}

		if (firstGcNotZero.get() > MAX_FIRST_PAUSE_MS) {
			return TimeUnit.NANOSECONDS.toMillis(rawDuration);
		} else {
			return rawDuration;
		}
	}

	@SuppressWarnings("restriction")
	protected String makeGCMessage(GarbageCollectionNotificationInfo info) {
		long duration = computeDurationMs(info);

		String gctype = info.getGcAction();
		if ("end of minor GC".equals(gctype)) {
			gctype = "Young Gen GC";
		} else if ("end of major GC".equals(gctype)) {
			gctype = "Old Gen GC";
		}

		StringBuilder sb = new StringBuilder();

		appendCPU(sb);

		appendCurrentGCDuration(sb, info, duration);

		long totalAfterMinusbefore = 0L;

		NavigableSet<String> keys = getSortedGCKeys(info);

		long totalHeapUsedBefore = 0L;
		long totalHeapUsedAfter = 0L;
		for (String key : keys) {
			MemoryUsage before = info.getGcInfo().getMemoryUsageBeforeGc().get(key);
			MemoryUsage after = info.getGcInfo().getMemoryUsageAfterGc().get(key);
			if (after == null) {
				LOGGER.debug("No .getMemoryUsageAfterGc for {}", key);
			} else {
				long afterUsed = after.getUsed();
				long beforeUsed = before.getUsed();

				totalHeapUsedBefore += beforeUsed;
				totalHeapUsedAfter += afterUsed;

				if (afterUsed != beforeUsed) {
					long afterMinusBefore = afterUsed - beforeUsed;
					totalAfterMinusbefore += afterMinusBefore;
				}

				appendMovedMemory(sb, key, before, after);

				if (!key.equals(keys.last())) {
					// Do NTO add the separator for the last entry
					sb.append("; ");
				}
			}
		}

		appendHeap(sb, totalHeapUsedAfter);

		if (totalAfterMinusbefore != 0) {
			appendDetailsAboutMove(sb, totalAfterMinusbefore, totalHeapUsedBefore);
		}

		appendDirectMemoryAndThreads(sb);

		return sb.toString();
	}

	protected void appendMovedMemory(StringBuilder sb, String key, MemoryUsage before, MemoryUsage after) {

		long beforeUsed = before.getUsed();
		long beforeCommited = before.getCommitted();

		long afterUsed = after.getUsed();
		long afterCommited = after.getCommitted();

		if (after.getUsed() == before.getUsed()) {
			sb.append(key).append(" ==");
			appendPercentage(sb, afterUsed, afterCommited);
		} else {
			sb.append(key).append(" ");
			appendPercentage(sb, beforeUsed, beforeCommited);
			sb.append("->");
			appendPercentage(sb, afterUsed, afterCommited);

			sb.append(" (");
			if (after.getUsed() > before.getUsed()) {
				sb.append('+');
			} else {
				LOGGER.trace("A negative number already provides a '-' sign");
			}

			long afterMinusBefore = afterUsed - beforeUsed;
			appendSize(sb, afterMinusBefore);
			sb.append(")");
		}
	}

	protected NavigableSet<String> getSortedGCKeys(GarbageCollectionNotificationInfo info) {
		// Sort by lexicographical order
		return new TreeSet<>(info.getGcInfo().getMemoryUsageBeforeGc().keySet());
	}

	protected void appendCurrentGCDuration(StringBuilder sb, GarbageCollectionNotificationInfo info, long duration) {
		sb.append(info.getGcName()).append(" lasted ").append(ApexLogHelper.getNiceTime(duration)).append(". ");
	}

	protected void appendHeap(StringBuilder sb, long totalHeapUsedAfter) {
		sb.append(" - Heap:");
		appendSize(sb, totalHeapUsedAfter);
	}

	protected void appendDetailsAboutMove(StringBuilder sb, long totalAfterMinusbefore, long totalHeapUsedBefore) {
		sb.append("=");
		appendSize(sb, totalHeapUsedBefore);

		if (totalAfterMinusbefore < 0) {
			appendSize(sb, totalAfterMinusbefore);
			sb.append(" garbage collected");
		}

		// Add the name of the Thread producing the maximum amount of memory
		{
			Map<? extends String, ? extends Long> immutableCurrentHeapByThread = getThreadNameToAllocatedHeap();
			Map<? extends String, ? extends Long> previousStatus = getAndSetByThreadRef(immutableCurrentHeapByThread);

			AtomicLongMap<String> currentHeap = AtomicLongMap.create(immutableCurrentHeapByThread);

			// Compute the difference between previous status and current status
			adjustWithReference(currentHeap, previousStatus);

			if (!currentHeap.isEmpty()) {
				long sumPrevious = previousStatus.values().stream().mapToLong(Long::longValue).sum();
				long sumCurrent = immutableCurrentHeapByThread.values().stream().mapToLong(Long::longValue).sum();

				if (sumCurrent > sumPrevious) {
					long transientlyGenerated = sumCurrent - sumPrevious;

					sb.append(" after allocating ");
					appendSize(sb, transientlyGenerated);
					sb.append(" through all threads");
				}

				// Sort from big memory to small memory
				Map<String, Long> valueOrdered = ApexJMXHelper.convertToJMXValueOrderedMap(currentHeap.asMap(), true);

				// currentHeap is not empty then valueOrdered is not empty
				assert !valueOrdered.isEmpty();
				Entry<String, Long> maxEntry = valueOrdered.entrySet().iterator().next();

				if (maxEntry.getValue() > 0) {
					sb.append(" including ");
					appendSize(sb, maxEntry.getValue());
					sb.append(" from ");
					sb.append(maxEntry.getKey());

					AtomicLongMap<String> groupBy = groupThreadNames(currentHeap.asMap());
					Map<String, Long> groupByValueOrdered =
							ApexJMXHelper.convertToJMXValueOrderedMap(groupBy.asMap(), true);

					Entry<String, Long> groupByMaxEntry = groupByValueOrdered.entrySet().iterator().next();

					// Report the biggest group only if it a different thread than the single biggest thread
					if (groupByMaxEntry.getValue() > 0 && !groupByMaxEntry.getKey().equals(maxEntry.getKey())) {
						sb.append(" and ");
						appendSize(sb, groupByMaxEntry.getValue());
						sb.append(" from ");
						sb.append(groupByMaxEntry.getKey());
					}
				} else {
					LOGGER.debug("We have only decreasing in {}", valueOrdered);
				}
			}
		}
	}

	protected Map<? extends String, ? extends Long> getAndSetByThreadRef(
			Map<? extends String, ? extends Long> immutableCurrentHeapByThread) {
		return heapGCNotifReference.getAndSet(immutableCurrentHeapByThread);
	}

	protected String getCurrentMemoryStatusMessage() {
		StringBuilder sb = new StringBuilder();

		appendCPU(sb);

		long totalHeapUsedAfter = 0L;
		for (MemoryPoolMXBean key : MEMORY_POOLS_MBEAN) {
			MemoryUsage after = key.getUsage();

			if (after != null) {
				long afterUsed = after.getUsed();
				long afterCommited = after.getCommitted();

				totalHeapUsedAfter += afterUsed;

				sb.append(key.getName()).append(" ==");
				appendSize(sb, afterUsed);
				sb.append(" == ");
				appendPercentage(sb, afterUsed, afterCommited);

				sb.append("; ");
			}
		}

		sb.append(" - Heap:");
		appendSize(sb, totalHeapUsedAfter);

		appendDirectMemoryAndThreads(sb);

		return sb.toString();
	}

	@SuppressWarnings("restriction")
	protected void appendCPU(StringBuilder sb) {
		// Add information about CPU consumption
		if (OS_MBEAN instanceof com.sun.management.OperatingSystemMXBean) {
			double cpu = ((com.sun.management.OperatingSystemMXBean) OS_MBEAN).getProcessCpuLoad();

			// -1 == No CPU info
			if (cpu >= BETWEEN_MINUS_ONE_AND_ZERO) {
				sb.append("CPU=");
				appendPercentage(sb, (long) (cpu * ApexLogHelper.THOUSAND), ApexLogHelper.THOUSAND);
				sb.append(" - ");
			}
		}
	}

	protected void appendDirectMemoryAndThreads(StringBuilder sb) {
		// Add information about DirectMemory
		{
			BufferPoolMXBean directMemoryBean = directMemoryStatus();

			if (directMemoryBean != null) {
				sb.append("; ");
				sb.append("DirectMemory").append(": ");
				appendSize(sb, directMemoryBean.getMemoryUsed());
				sb.append("(allocationCount=").append(directMemoryBean.getCount()).append(')');
				sb.append(" over max=");
				appendSize(sb, getMaxDirectMemorySize());
			}
		}

		// Add the number of live threads as the OS may refuse to make new
		// threads
		{
			long nbLiveThreads = THREAD_MBEAN.getThreadCount();
			sb.append(" LiveThreadCount=");
			sb.append(nbLiveThreads);
		}
	}

	protected void appendPercentage(StringBuilder sb, long numerator, long denominator) {
		sb.append(ApexLogHelper.getNicePercentage(numerator, denominator));
	}

	public static void appendSize(StringBuilder sb, long size) {
		sb.append(ApexLogHelper.getNiceMemory(size));
	}

	public static String getNiceBytes(long size) {
		StringBuilder sw = new StringBuilder();

		appendSize(sw, size);

		return sw.toString();
	}

	@SuppressWarnings("restriction")
	protected void doLog(GarbageCollectionNotificationInfo info) {
		// Javadoc tells duration is in millis while it seems to be in micros
		long duration = computeDurationMs(info);

		{
			String gcMessage = makeGCMessage(info);
			if (duration >= gcDurationMillisForInfoLog) {
				LOGGER.info(gcMessage);
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(gcMessage);
			}
		}

		// In case we encounter a long MarkSweep
		if (isFullGC(info)) {
			onFullGC(info);
		}

		logIfMemoryOverCap();
	}

	protected void onFullGC(@SuppressWarnings("restriction") GarbageCollectionNotificationInfo info) {
		long duration = computeDurationMs(info);

		if (duration > marksweepDurationMillisForThreadDump) {
			printThreadDump();
		}

		// This block is comparable to the usage of -XX:+PrintClassHistogramAfterFullGC
		if (duration > marksweepDurationMillisForHeapHistogram) {
			long heapUsed = getUsedHeap();
			if (heapUsed < maxHeapGbForHeapHistogram * GB) {
				// Print HeapHistogram only if heap is small enough
				printHeapHistogram(HEAP_HISTO_LIMIT_NB_ROWS);
			}
		}
	}

	protected void logIfMemoryOverCap() {
		long heapUsed = getUsedHeap();
		long heapMax = getMaxHeap();
		if (isOverThreashold(heapUsed, heapMax)) {
			LocalDateTime now = new LocalDateTime();
			overHeapThresholdSince.compareAndSet(null, now);

			LocalDateTime overThresholdSince = overHeapThresholdSince.get();
			if (overThresholdSince != null
					&& overThresholdSince.isBefore(now.minusMinutes(HEAP_ALERT_PERIOD_IN_MINUTES))) {
				// We are over heapThreshold since more than 15 minutes
				overHeapThresholdSince.set(null);
				onOverHeapAlertSinceTooLong(overThresholdSince);
			}
		} else {
			overHeapThresholdSince.getAndUpdate(current -> {
				if (current != null) {
					onMemoryBackUnderThreshold(heapUsed, heapMax);
				}

				return null;
			});
		}
	}

	protected boolean isOverThreashold(long heapUsed, long heapMax) {
		return heapUsed > heapMax * HEAP_ALERT_THRESHOLD;
	}

	protected long getUsedHeap() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
	}

	protected long getMaxHeap() {
		return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
	}

	protected void onMemoryBackUnderThreshold(long heapUsed, long heapMax) {
		// We got back under a nice memory level
		LOGGER.info("The heap got back under the threashold: {} out of {}",
				getNiceBytes(heapUsed),
				getNiceBytes(heapMax));
	}

	protected void onOverHeapAlertSinceTooLong(LocalDateTime overThresholdSince) {
		long heapUsed = getUsedHeap();
		long heapMax = getMaxHeap();
		LOGGER.warn("We have a heap of {} given a max of {} since {}",
				getNiceBytes(heapUsed),
				getNiceBytes(heapMax),
				overThresholdSince);
		printThreadDump();
	}

	public static final Set<String> FULL_GC_NAMES = ImmutableSet.of("PS MarkSweep", "G1 Old Generation");

	/**
	 * Print the heap histogram only up to given % of total heap
	 */
	private static final int HEAP_HISTO_LIMIT_NB_ROWS = 20;

	@SuppressWarnings("restriction")
	protected boolean isFullGC(GarbageCollectionNotificationInfo info) {
		return FULL_GC_NAMES.contains(info.getGcName());
	}

	protected void printThreadDump() {
		LocalDateTime beforeThreadDump = new LocalDateTime();

		String threadDumpAsString = getAllThreads(true);

		this.latestThreadDump.set(beforeThreadDump);

		LOGGER.warn("Thread Dump: {}", threadDumpAsString);
	}

	protected void printSmartThreadDump() {
		LocalDateTime beforeThreadDump = new LocalDateTime();

		String threadDumpAsString = apexThreadDumper.getSmartThreadDumpAsString(false);

		this.latestThreadDump.set(beforeThreadDump);

		LOGGER.warn("Thread Dump: {}", threadDumpAsString);
	}

	protected void printHeapHistogram(int nbRows) {
		String threadDumpAsString = getHeapHistogramAsString(nbRows);

		LOGGER.warn("HeapHistogram: {}", threadDumpAsString);
	}

	public static String getHeapHistogramAsString(int nbRows) {
		OutputStream os = new ByteArrayOutputStream();

		// Do not query monitors and synchronizers are they are not the cause of
		// a FullGC: we prevent not to freeze the JVM collecting these monitors
		streamHeapHistogram(os, nbRows);

		return os.toString();
	}

	public static void streamHeapHistogram(OutputStream os, int nbRows) {
		if (VirtualMachineWithoutToolsJar.isVirtualMachineAvailable()) {
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(VirtualMachineWithoutToolsJar.heapHisto(), IHeapHistogram.JMAP_CHARSET));
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os, IHeapHistogram.JMAP_CHARSET))) {

				boolean firstRow = true;

				// We want to write the last Jmap histo row as it holds a resumee
				AtomicReference<String> lastSkippedRow = new AtomicReference<>();

				int nbWritten = 0;

				// Read a limit number of rows
				while (true) {
					String nextLine = br.readLine();
					if (nextLine == null) {
						break;
					} else if (!nextLine.isEmpty()) {

						if (nbWritten < nbRows) {
							// We are inside bounds

							if (firstRow) {
								firstRow = !firstRow;
							} else {
								bw.newLine();
							}

							bw.write(nextLine);
							nbWritten++;
						} else {
							// We are out of bounds: register current row as we want to write the last row
							lastSkippedRow.set(nextLine);
						}
					}
				}

				// Write the last row as it holds an overview of the heap
				if (lastSkippedRow.get() != null) {
					bw.newLine();
					bw.write(lastSkippedRow.get());
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			LOGGER.warn("VirtualMachine is not available for HeapHisto");
		}
	}

	/** @return the maximum direct memory size */
	@SuppressWarnings("restriction")
	protected static long getMaxDirectMemorySize() {
		return VM.maxDirectMemory();
	}

	// https://stackoverflow.com/questions/6020619/where-to-find-default-xss-value-for-sun-oracle-jvm
	protected static long getMemoryPerThread() {
		// '-XX:ThreadStackSize' or '-Xss'
		// TODO: for now, we return the default: 1MB
		return IApexMemoryConstants.MB;
	}

	protected BufferPoolMXBean directMemoryStatus() {
		return ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
				.stream()
				.filter(b -> b.getName().equals("direct"))
				.findAny()
				.orElseGet(() -> null);
	}

	@ManagedAttribute
	public Date getLatestThreadDump() {
		LocalDateTime latest = latestThreadDump.get();
		if (latest == null) {
			return null;
		} else {
			return latest.toDate();
		}
	}

	@Override

	@ManagedAttribute
	public void setMarksweepDurationMillisForThreadDump(long marksweepDurationMillisForThreadDump) {
		this.marksweepDurationMillisForThreadDump = marksweepDurationMillisForThreadDump;
	}

	@Override
	@ManagedAttribute
	public long getMarksweepDurationMillisForThreadDump() {
		return marksweepDurationMillisForThreadDump;
	}

	@Override
	@ManagedAttribute
	public void setMarksweepDurationMillisForHeapHistogram(long marksweepDurationMillisForHeapHistogram) {
		this.marksweepDurationMillisForHeapHistogram = marksweepDurationMillisForHeapHistogram;
	}

	@Override
	@ManagedAttribute
	public long getMarksweepDurationMillisForHeapHistogram() {
		return marksweepDurationMillisForHeapHistogram;
	}

	@Override
	@ManagedAttribute
	public void setMaxHeapGbForHeapHistogram(long maxHeapGbForHeapHistogram) {
		this.maxHeapGbForHeapHistogram = maxHeapGbForHeapHistogram;
	}

	@Override
	@ManagedAttribute
	public long getMaxHeapGbForHeapHistogram() {
		return maxHeapGbForHeapHistogram;
	}

	@Override
	@ManagedOperation
	public void markNowAsAllocatedHeapReference() {
		allocatedHeapReference.set(getThreadNameToAllocatedHeap());
	}

	@Override
	@ManagedOperation
	public void clearAllocatedHeapReference() {
		allocatedHeapReference.set(Collections.<String, Long>emptyMap());
	}

	protected Map<? extends String, ? extends Long> getThreadNameToAllocatedHeap() {
		if (THREAD_MBEAN instanceof com.sun.management.ThreadMXBean) {
			com.sun.management.ThreadMXBean sunThreadMBean = (com.sun.management.ThreadMXBean) THREAD_MBEAN;

			if (sunThreadMBean.isThreadAllocatedMemorySupported()) {
				if (!sunThreadMBean.isThreadAllocatedMemoryEnabled()) {
					sunThreadMBean.setThreadAllocatedMemoryEnabled(true);
				}

				// Order Thread by Name
				Map<String, Long> threadNameToAllocatedMemory = new TreeMap<>();

				// Snapshot total allocation until now
				{
					long[] liveThreadIds = sunThreadMBean.getAllThreadIds();

					ThreadInfo[] threadInfos = sunThreadMBean.getThreadInfo(liveThreadIds);

					for (int i = 0; i < liveThreadIds.length; i++) {
						ThreadInfo threadInfo = threadInfos[i];
						if (threadInfo == null) {
							LOGGER.debug("No more info about thread #{}", i);
						} else {
							long threadAllocatedBytes = sunThreadMBean.getThreadAllocatedBytes(liveThreadIds[i]);

							// We may receive -1
							if (threadAllocatedBytes > 0) {
								threadNameToAllocatedMemory.put(threadInfo.getThreadName(), threadAllocatedBytes);
							}
						}
					}
				}

				return Collections.unmodifiableMap(threadNameToAllocatedMemory);
			} else {
				return Collections.emptyMap();
			}
		} else {
			return Collections.emptyMap();
		}
	}

	@Override
	@ManagedAttribute
	public Map<String, String> getThreadNameToAllocatedHeapNiceString() {
		// Mutable for adjustWithReference
		AtomicLongMap<String> threadNameToAllocatedHeap = AtomicLongMap.create(getThreadNameToAllocatedHeap());
		adjustWithReference(threadNameToAllocatedHeap, allocatedHeapReference.get());

		Map<String, Long> orderedByDecreasingSize =
				ApexJMXHelper.convertToJMXValueOrderedMap(threadNameToAllocatedHeap.asMap(), true);

		return ApexJMXHelper.convertToJMXMapString(convertByteValueToString(orderedByDecreasingSize));
	}

	protected void adjustWithReference(AtomicLongMap<String> currentHeapToAdjust,
			Map<? extends String, ? extends Long> reference) {
		// Remove the allocation what has been previously marked
		for (String threadName : currentHeapToAdjust.asMap().keySet()) {
			Long threadReferenceHeap = reference.get(threadName);
			if (threadReferenceHeap != null) {
				currentHeapToAdjust.addAndGet(threadName, -threadReferenceHeap);
			}
		}
	}

	@Override
	@ManagedAttribute
	public Map<String, String> getThreadGroupsToAllocatedHeapNiceString() {
		// Get current heap
		AtomicLongMap<String> threadNameToAllocatedHeap = AtomicLongMap.create(getThreadNameToAllocatedHeap());

		// Adjust with the marked reference
		adjustWithReference(threadNameToAllocatedHeap, allocatedHeapReference.get());

		// Group by thread
		AtomicLongMap<String> threadGroupToAllocatedHeap = groupThreadNames(threadNameToAllocatedHeap.asMap());

		Map<String, Long> orderedByDecreasingSize =
				ApexJMXHelper.convertToJMXValueOrderedMap(threadGroupToAllocatedHeap.asMap(), true);

		return ApexJMXHelper.convertToJMXMapString(convertByteValueToString(orderedByDecreasingSize));
	}

	protected AtomicLongMap<String> groupThreadNames(Map<String, Long> threadNameToAllocatedHeap) {
		AtomicLongMap<String> threadGroupToAllocatedHeap = AtomicLongMap.create();

		// Search for trailing digits
		Pattern p = Pattern.compile("(.*?)\\d+");

		for (Entry<String, Long> entry : threadNameToAllocatedHeap.entrySet()) {
			Matcher matcher = p.matcher(entry.getKey());

			if (matcher.matches()) {
				threadGroupToAllocatedHeap.addAndGet(matcher.group(1) + "X", entry.getValue());
			} else {
				threadGroupToAllocatedHeap.addAndGet(entry.getKey(), entry.getValue());
			}
		}

		return threadGroupToAllocatedHeap;
	}

	public static <T> Map<T, String> convertByteValueToString(Map<T, Long> threadNameToAllocatedHeap) {
		// Convert byte as long to byte as Nice String
		return Maps.transformValues(threadNameToAllocatedHeap, GCInspector::getNiceBytes);
	}

	/**
	 * @param withoutMonitors
	 *            JConsole will set withoutMonitors = true by default
	 */
	@ManagedOperation
	@Override
	public String getAllThreads(boolean withoutMonitors) {
		return apexThreadDumper.getThreadDumpAsString(!withoutMonitors);
	}

	/**
	 * @param withoutMonitors
	 *            JConsole will set withoutMonitors = true by default
	 */
	@ManagedOperation
	@Override
	public String getAllThreadsSmart(boolean withoutMonitors) {
		return apexThreadDumper.getSmartThreadDumpAsString(!withoutMonitors);
	}

	@ManagedOperation
	@Override
	public String getHeapHistogram() {
		try {
			return HeapHistogram.createHeapHistogramAsString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long saveHeapDump(String path, boolean gzipped) {
		if (gzipped && !path.endsWith(".gz")) {
			// Ensure proper suffix
			path += ".gz";
		}

		try {
			return HeapHistogram.saveHeapDump(Paths.get(path).toFile(), gzipped);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@ManagedOperation
	@Override
	public String getAndLogCurrentMemoryStatus() {
		String currentMemoryStatusMessage = getCurrentMemoryStatusMessage();

		// Ensure status is written in the log file
		LOGGER.info(currentMemoryStatusMessage);

		return currentMemoryStatusMessage;
	}
}
