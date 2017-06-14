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
package blasd.apex.shared.monitoring.metrics;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.joda.time.LocalDateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import blasd.apex.shared.file.ApexFileHelper;
import blasd.apex.shared.logging.ApexLogHelper;
import blasd.apex.shared.thread.ApexExecutorsHelper;
import blasd.apex.shared.thread.IApexThreadDumper;

/**
 * This class centralized events which should end being available in the JConsole, to provide details about number of
 * events, size of events, active events
 * 
 * @author Benoit Lacelle
 * 
 */
@ManagedResource
public class ApexMetricsTowerControl implements IApexMetricsTowerControl, InitializingBean {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexMetricsTowerControl.class);

	/**
	 * {@link MetricRegistry} uses a String as Key. PATH_JOINER is used to Convert from {@link List} to {@link String}
	 */
	public static final String PATH_JOINER = ".";

	/**
	 * A {@link StartMetricEvent} event will be discarded if we don't receive its {@link EndMetricEvent} event after
	 * this amount of time
	 */
	public static final int CACHE_TIMEOUT_MINUTES = 60;

	/**
	 * Do not maintain more than this amount of active tasks
	 */
	public static final int CACHE_MAX_SIZE = 1000;

	public static final int DEFAULT_LONGRUNNINGCHECK_SECONDS = 10;
	protected int longRunningCheckSeconds = DEFAULT_LONGRUNNINGCHECK_SECONDS;

	// By default, it means 3*10=30 seconds
	private static final int FACTOR_FOR_OLD = 3;

	// By default, it means 12*10= 2 minutes
	private static final int FACTOR_FOR_TOO_OLD = 12;

	/**
	 * Cache the {@link StartMetricEvent} which have not ended yet.
	 */
	protected final LoadingCache<StartMetricEvent, LocalDateTime> activeTasks;
	protected final LoadingCache<StartMetricEvent, StartMetricEvent> verySlowTasks;

	// We expect a single longRunningTask to be active at a time
	protected final AtomicReference<ScheduledFuture<?>> scheduledFuture = new AtomicReference<>();
	protected final ScheduledExecutorService logLongRunningES =
			ApexExecutorsHelper.newSingleThreadScheduledExecutor(this.getClass().getSimpleName());

	protected final IApexThreadDumper apexThreadDumper;

	public ApexMetricsTowerControl(IApexThreadDumper apexThreadDumper) {
		this.apexThreadDumper = apexThreadDumper;

		activeTasks = CacheBuilder.newBuilder()
				.expireAfterAccess(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
				.maximumSize(CACHE_MAX_SIZE)
				.concurrencyLevel(ApexExecutorsHelper.DEFAULT_ACTIVE_TASKS)
				.<StartMetricEvent, LocalDateTime>removalListener(removal -> {
					if (removal.getCause().equals(RemovalCause.EXPIRED)) {
						logOnFarTooMuchLongTask(removal.getKey());
					} else if (removal.getCause().equals(RemovalCause.EXPLICIT)) {
						logOnEndEvent(removal.getKey());
					}
				})
				.build(CacheLoader.from(key -> LocalDateTime.now()));

		verySlowTasks = CacheBuilder.newBuilder()
				.expireAfterAccess(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
				.maximumSize(CACHE_MAX_SIZE)
				.concurrencyLevel(ApexExecutorsHelper.DEFAULT_ACTIVE_TASKS)
				.build(CacheLoader.from(startEvent -> startEvent));
	}

	protected void logOnFarTooMuchLongTask(StartMetricEvent startEvent) {
		String threadDump = apexThreadDumper.getSmartThreadDumpAsString(false);

		LOGGER.error("Task still active after {} {}. We stop monitoring it: {}. ThreadDump: {}",
				CACHE_TIMEOUT_MINUTES,
				TimeUnit.MINUTES,
				startEvent,
				threadDump);
	}

	protected void logOnDetectingVeryLongTask(StartMetricEvent startEvent) {
		String threadDump = apexThreadDumper.getSmartThreadDumpAsString(false);

		LOGGER.error("Very-Long task: {} ThreadDump: {}", startEvent, threadDump);
	}

	protected void logOnEndEvent(StartMetricEvent startEvent) {
		Optional<EndMetricEvent> endEvent = startEvent.getEndEvent();

		if (!endEvent.isPresent()) {
			LOGGER.info("We closed {} without an endEvent ?!", startEvent);
		} else {
			long timeInMs = endEvent.get().durationInMs();

			long longRunningInMillis = TimeUnit.SECONDS.toMillis(longRunningCheckSeconds);
			if (timeInMs > FACTOR_FOR_TOO_OLD * longRunningInMillis) {
				LOGGER.warn("Very-long {} ended",
						ApexLogHelper.lazyToString(() -> endEvent.get().startEvent.toStringNoStack()));
			} else if (timeInMs > longRunningInMillis) {
				LOGGER.info("Long {} ended",
						ApexLogHelper.lazyToString(() -> endEvent.get().startEvent.toStringNoStack()));
			} else {
				// Prevent building the .toString too often
				LOGGER.trace("{} ended", ApexLogHelper.lazyToString(() -> endEvent.get().startEvent.toStringNoStack()));
			}
		}
	}

	@ManagedAttribute
	@Override
	public int getLongRunningCheckSeconds() {
		return longRunningCheckSeconds;
	}

	@ManagedAttribute
	@Override
	public void setLongRunningCheckSeconds(int longRunningCheckSeconds) {
		this.longRunningCheckSeconds = longRunningCheckSeconds;

		if (scheduledFuture.get() != null) {
			scheduleLogLongRunningTasks();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		scheduleLogLongRunningTasks();
	}

	protected void scheduleLogLongRunningTasks() {
		ScheduledFuture<?> cancelMe = scheduledFuture.getAndSet(logLongRunningES
				.scheduleWithFixedDelay(() -> logLongRunningTasks(), 1, longRunningCheckSeconds, TimeUnit.SECONDS));

		if (cancelMe != null) {
			// Cancel the task with previous delay
			cancelMe.cancel(true);
		}
	}

	protected void logLongRunningTasks() {
		LocalDateTime now = LocalDateTime.now();

		// We are interested in events old enough
		// By default: log in debug until 30 seconds
		LocalDateTime oldBarrier = now.minusSeconds(longRunningCheckSeconds);
		// By default: log in in info from 30 seconds
		LocalDateTime tooOldBarrier = now.minusSeconds(FACTOR_FOR_OLD * longRunningCheckSeconds);
		// By default: log in warn if above 1min30
		LocalDateTime muchtooOldBarrier = now.minusSeconds(FACTOR_FOR_TOO_OLD * longRunningCheckSeconds);

		String logMessage = "Task active since ({} seconds) {}: {}";
		for (Entry<StartMetricEvent, LocalDateTime> active : activeTasks.asMap().entrySet()) {
			LocalDateTime activeSince = active.getValue();
			int seconds = Seconds.secondsBetween(activeSince, now).getSeconds();

			StartMetricEvent startEvent = active.getKey();
			Object cleanKey = noNewLine(startEvent);
			if (activeSince.isBefore(muchtooOldBarrier)) {
				// This task is active since more than XXX seconds
				LOGGER.warn(logMessage, seconds, activeSince, cleanKey);

				// If this is the first encounter as verySLow, we may have additional operations
				verySlowTasks.refresh(startEvent);

			} else if (activeSince.isBefore(tooOldBarrier)) {
				LOGGER.info(logMessage, seconds, activeSince, cleanKey);
			} else if (activeSince.isBefore(oldBarrier)) {
				LOGGER.debug(logMessage, seconds, activeSince, cleanKey);
			} else {
				LOGGER.trace(logMessage, seconds, activeSince, cleanKey);
			}
		}
	}

	protected Object noNewLine(StartMetricEvent key) {
		return ApexLogHelper.lazyToString(() -> ApexFileHelper.cleanWhitespaces(key.toString()));
	}

	/**
	 * It also starts a Timer
	 * 
	 * @param startEvent
	 */
	@Subscribe
	@AllowConcurrentEvents
	public void onStartEvent(StartMetricEvent startEvent) {
		if (startEvent.source == null) {
			LOGGER.debug("Discard StartEvent which is missing a Source: {}", startEvent);
		} else {
			// .refresh would rewrite the startTime on multiple events
			activeTasks.getUnchecked(startEvent);
		}
	}

	/**
	 * A StartEvent is immediately associated to a {@link CounterMetricEvent} with value -1, to count the number of
	 * active tasks
	 * 
	 * @param startEvent
	 */
	@Subscribe
	@AllowConcurrentEvents
	public void onEndEvent(EndMetricEvent endEvent) {
		long timeInMs = endEvent.durationInMs();
		if (timeInMs < 0) {
			// May happen when several EndEvent happens, for instance on a Query failure
			LOGGER.debug("An EndEvent has been submitted without its StartEvent Context having been started: {}",
					endEvent);
		} else {
			// Invalidation of the key will generate a log if the task was slow
			invalidateStartEvent(endEvent.startEvent);
		}
	}

	@Subscribe
	@AllowConcurrentEvents
	public void onThrowable(Throwable t) {
		LOGGER.error("Not managed exception", t);
	}

	protected void invalidateStartEvent(StartMetricEvent startEvent) {
		if (activeTasks.getIfPresent(startEvent) == null) {
			LOGGER.debug("And EndEvent has been submitted without its StartEvent having been registered"
					+ ", or after having been already invalidated: {}", startEvent);
		} else {
			invalidate(startEvent);
		}
	}

	@ManagedAttribute
	@Override
	public long getActiveTasksSize() {
		return activeTasks.size();
	}

	@ManagedAttribute
	@Override
	public long getRootActiveTasksSize() {
		Set<StartMetricEvent> startMetricEvent = activeTasks.asMap().keySet();

		return startMetricEvent.stream().map(s -> {
			Object root = s.getDetail(StartMetricEvent.KEY_ROOT_SOURCE);
			if (root == null) {
				return s.source;
			} else {
				return root;
			}
		}).distinct().count();
	}

	/**
	 * 
	 * @return a {@link Map} from the start date of the currently running operation, to the name of the operation
	 */
	@ManagedAttribute
	@Override
	public NavigableMap<Date, String> getActiveTasks() {
		return convertToMapDateString(activeTasks.asMap());
	}

	protected NavigableMap<Date, String> convertToMapDateString(ConcurrentMap<?, LocalDateTime> asMap) {
		NavigableMap<Date, String> dateToName = new TreeMap<>();

		for (Entry<?, LocalDateTime> entry : asMap.entrySet()) {
			Date dateToInsert = entry.getValue().toDate();

			// Ensure there is not 2 entries with the same date
			while (dateToName.containsKey(dateToInsert)) {
				// Change slightly the start date
				dateToInsert = new Date(dateToInsert.getTime() + 1);
			}

			String fullName = String.valueOf(entry.getKey());

			dateToName.put(dateToInsert, fullName);
		}

		return dateToName;
	}

	/**
	 * In some cases, we may have ghosts active tasks. One can invalidate them manually through this method
	 * 
	 * @param name
	 *            the full name of the activeTask to invalidate
	 * @return true if we succeeded removing this entry
	 */
	@ManagedOperation
	public boolean invalidateActiveTasks(String name) {
		for (StartMetricEvent startEvent : activeTasks.asMap().keySet()) {
			// Compare without the stack else it would be difficult to cancel from a JConsole
			if (name.equals(startEvent.toStringNoStack())) {
				invalidate(startEvent);
				return true;
			}
		}

		return false;
	}

	protected void invalidate(StartMetricEvent startEvent) {
		activeTasks.invalidate(startEvent);
		verySlowTasks.invalidate(startEvent);
	}

	@ManagedOperation
	public void setDoRememberStack(boolean doRememberStack) {
		StartMetricEvent.doRememberStack = doRememberStack;
	}

	/**
	 * This ThreadDump tends to be faster as by default, it does not collect monitors
	 * 
	 * @param withoutMonitors
	 *            if true (default JConsole behavior),it skips monitors and synchronizers which is much faster and
	 *            prevent freezing the JVM
	 * @return
	 */
	@ManagedOperation
	public String getAllThreads(boolean withoutMonitors) {
		return apexThreadDumper.getThreadDumpAsString(!withoutMonitors);
	}

}
