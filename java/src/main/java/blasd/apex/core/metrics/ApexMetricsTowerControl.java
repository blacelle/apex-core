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
package blasd.apex.core.metrics;

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
import com.google.common.cache.RemovalNotification;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.core.logging.ApexLogHelper;
import blasd.apex.core.memory.IApexMemoryConstants;
import blasd.apex.core.thread.ApexExecutorsHelper;
import blasd.apex.core.thread.IApexThreadDumper;

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
	 * MetricRegistry uses a String as Key. PATH_JOINER is used to Convert from {@link List} to {@link String}
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

	private static final String LOG_MESSAGE = "Task active since {} ({}): {}";
	private static final String LOG_MESSAGE_PROGRESS = "Task active since {} ({} since {}): {}";

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
				.removalListener(this::onActiveTaskRemoval)
				.build(CacheLoader.from(key -> LocalDateTime.now()));

		verySlowTasks = CacheBuilder.newBuilder()
				.expireAfterAccess(CACHE_TIMEOUT_MINUTES, TimeUnit.MINUTES)
				.maximumSize(CACHE_MAX_SIZE)
				.concurrencyLevel(ApexExecutorsHelper.DEFAULT_ACTIVE_TASKS)
				.build(CacheLoader.from(startEvent -> startEvent));
	}

	protected void onActiveTaskRemoval(RemovalNotification<StartMetricEvent, LocalDateTime> removal) {
		if (removal.getCause().equals(RemovalCause.EXPIRED)) {
			logOnFarTooMuchLongTask(removal.getKey());
		} else if (removal.getCause().equals(RemovalCause.EXPLICIT)) {
			logOnEndEvent(removal.getKey());
		}
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

		LOGGER.error("Task is marked as very-long: {} ThreadDump: {}", startEvent, threadDump);
	}

	protected void logOnEndEvent(StartMetricEvent startEvent) {
		Optional<EndMetricEvent> endEvent = startEvent.getEndEvent();

		if (!endEvent.isPresent()) {
			LOGGER.info("We closed {} without an endEvent ?!", startEvent);
		} else {
			long timeInMs = endEvent.get().durationInMs();

			long longRunningInMillis = TimeUnit.SECONDS.toMillis(longRunningCheckSeconds);
			Object lazyToString = ApexLogHelper.lazyToString(() -> endEvent.get().startEvent.toStringNoStack());
			if (timeInMs > FACTOR_FOR_TOO_OLD * longRunningInMillis) {
				LOGGER.info("End of very-long {}", lazyToString);
			} else if (timeInMs > longRunningInMillis) {
				LOGGER.info("End of long {} ended", lazyToString);
			} else {
				// Prevent building the .toString too often
				LOGGER.trace("End of {} ended", lazyToString);
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

		activeTasks.asMap().forEach((startEvent, activeSince) -> {
			int seconds = Seconds.secondsBetween(activeSince, now).getSeconds();
			Object time = ApexLogHelper.getNiceTime(seconds, TimeUnit.SECONDS);

			Object cleanKey = noNewLine(startEvent);

			if (startEvent.getProgress().isPresent()) {
				Object rate =
						ApexLogHelper.getNiceRate(startEvent.getProgress().getAsLong(), seconds, TimeUnit.SECONDS);

				if (activeSince.isBefore(oldBarrier)) {
					if (activeSince.isBefore(muchtooOldBarrier)) {
						// This task is active since more than XXX seconds
						LOGGER.warn(LOG_MESSAGE_PROGRESS, time, rate, activeSince, cleanKey);

						// If this is the first encounter as verySLow, we may have additional operations
						verySlowTasks.refresh(startEvent);
					} else if (activeSince.isBefore(tooOldBarrier)) {
						LOGGER.info(LOG_MESSAGE_PROGRESS, activeSince, rate, time, cleanKey);
					} else {
						LOGGER.debug(LOG_MESSAGE_PROGRESS, activeSince, rate, time, cleanKey);
					}
				} else {
					LOGGER.trace(LOG_MESSAGE_PROGRESS, activeSince, rate, time, cleanKey);
				}
			} else {
				if (activeSince.isBefore(oldBarrier)) {
					if (activeSince.isBefore(muchtooOldBarrier)) {
						// This task is active since more than XXX seconds
						LOGGER.warn(LOG_MESSAGE, activeSince, time, cleanKey);

						// If this is the first encounter as verySLow, we may have additional operations
						verySlowTasks.refresh(startEvent);

					} else if (activeSince.isBefore(tooOldBarrier)) {
						LOGGER.info(LOG_MESSAGE, activeSince, time, cleanKey);
					} else {
						LOGGER.debug(LOG_MESSAGE, activeSince, time, cleanKey);
					}
				} else {
					LOGGER.trace(LOG_MESSAGE, activeSince, time, cleanKey);
				}
			}
		});

	}

	protected Object noNewLine(StartMetricEvent key) {
		// Prevent the message to be too big. Else, we may end reporting very regularly about a huge MDX (e.g. 150MB
		// every 10 seconds is not reasonable)
		return ApexLogHelper.lazyToString(() -> ApexFileHelper
				.cleanWhitespaces(ApexLogHelper.getFirstChars(key, IApexMemoryConstants.MB_INT).toString()));
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
	 * @param endEvent
	 *            mark the task associated to this event as completed
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
			LOGGER.debug(
					"And EndEvent has been submitted without its StartEvent having been registered"
							+ ", or after having been already invalidated: {}",
					startEvent);
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
	 *            the full name of the activeTask to invalidate. If '*', we cancel all tasks
	 * @return true if we succeeded removing this entry
	 */
	@ManagedOperation
	public boolean invalidateActiveTasks(String nameOrStar) {
		for (StartMetricEvent startEvent : activeTasks.asMap().keySet()) {
			// Compare without the stack else it would be difficult to cancel from a JConsole
			if ("*".equals(nameOrStar) || nameOrStar.equals(startEvent.toStringNoStack())) {
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
		StartMetricEvent.setDoRememberStack(doRememberStack);
	}

	/**
	 * This ThreadDump tends to be faster as by default, it does not collect monitors
	 * 
	 * @param withoutMonitors
	 *            if true (default JConsole behavior),it skips monitors and synchronizers which is much faster and
	 *            prevent freezing the JVM
	 * @return a formatted thread-dump
	 */
	@ManagedOperation
	public String getAllThreads(boolean withoutMonitors) {
		return apexThreadDumper.getThreadDumpAsString(!withoutMonitors);
	}

}
