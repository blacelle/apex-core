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
package blasd.apex.core.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import blasd.apex.core.thread.CurrentThreadStack;

/**
 * A {@link StartMetricEvent} can be used to stats the duration of some events
 * 
 * @author Benoit Lacelle
 * 
 */
public class StartMetricEvent extends AMetricEvent {
	// As retrieving the stack could be expensive, this boolean has to be set to
	// true manually or with SetStaticMBean
	public static boolean doRememberStack = false;

	public static final String KEY_USERNAME = "UserName";
	public static final String KEY_PIVOT_ID = "ActivePivot";
	public static final String KEY_ROOT_SOURCE = "RootSource";

	/**
	 * Could be Excel, Live, Distributed, or anything else like the name of feature executing queries
	 */
	public static final String KEY_CLIENT = "Client";

	// Xmla is typically used by Excel
	public static final String VALUE_CLIENT_XMLA = "XMLA";

	// Streaming is typically used by Live
	public static final String VALUE_CLIENT_STREAMING = "Streaming";

	// Remember the stack could be much helpful
	public final Optional<StackTraceElement[]> stack;

	public final long startTime = System.currentTimeMillis();

	protected final Map<String, ?> startDetails;
	protected final Map<String, Object> endDetails;

	/**
	 * Filled on EndMetricEvent construction
	 */
	final AtomicReference<EndMetricEvent> endMetricEvent = new AtomicReference<>();

	protected final LongSupplier progress;
	private static final LongSupplier NO_PROGRESS = () -> -1L;

	// By default, we have no result size
	// protected long resultSize = -1;

	private static Optional<StackTraceElement[]> fastCurrentStackIfRemembering() {
		if (doRememberStack) {
			return Optional.ofNullable(fastCurrentStack());
		} else {
			return Optional.empty();
		}
	}

	public StartMetricEvent(Object source, String firstName, String... otherNames) {
		this(source, Collections.emptyMap(), NO_PROGRESS, Lists.asList(firstName, otherNames));
	}

	public StartMetricEvent(Object source,
			Map<String, ?> details,
			LongSupplier progress,
			String firstName,
			String... otherNames) {
		this(source, details, progress, Lists.asList(firstName, otherNames), fastCurrentStackIfRemembering());
	}

	public StartMetricEvent(Object source,
			Map<String, ?> details,
			LongSupplier progress,
			List<? extends String> names) {
		this(source, details, progress, names, fastCurrentStackIfRemembering());
	}

	protected StartMetricEvent(Object source,
			Map<String, ?> details,
			LongSupplier progress,
			List<? extends String> names,
			Optional<StackTraceElement[]> stack) {
		super(source, names);

		this.startDetails = ImmutableMap.copyOf(details);
		// We are allowed to add details after the construction
		this.endDetails = new ConcurrentHashMap<>();

		this.progress = progress;

		this.stack = stack;
	}

	public static StackTraceElement[] fastCurrentStack() {
		return CurrentThreadStack.snapshotStackTrace();
	}

	public static AutoCloseableStartMetricEvent post(Consumer<? super AMetricEvent> eventBus,
			Object source,
			String firstName,
			String... otherNames) {
		return post(eventBus, source, Collections.emptyMap(), NO_PROGRESS, firstName, otherNames);
	}

	public static AutoCloseableStartMetricEvent post(Consumer<? super AMetricEvent> eventBus,
			Object source,
			Map<String, ?> details,
			LongSupplier progress,
			String firstName,
			String... otherNames) {
		StartMetricEvent startEvent = new StartMetricEvent(source, details, progress, firstName, otherNames);

		post(eventBus, startEvent);

		// This is used in try-with-resources: do not return null
		return new AutoCloseableStartMetricEvent(startEvent, eventBus);
	}

	@Override
	public String toString() {
		// Append the stack to the simple toString
		return toStringNoStack() + stack.map(s -> '\n' + Joiner.on('\n').join(s)).orElse("");
	}

	public String toStringNoStack() {
		long currentProgress = progress.getAsLong();
		String suffix = "";

		if (!startDetails.isEmpty()) {
			suffix += " startDetails=" + startDetails;
		}
		if (!endDetails.isEmpty()) {
			suffix += " endDetails=" + endDetails;
		}

		if (currentProgress < 0L) {
			return super.toString() + suffix;
		} else {
			return super.toString() + " progress=" + currentProgress + suffix;
		}
	}

	public Object getDetail(String key) {
		Object result = endDetails.get(key);
		if (result == null) {
			result = startDetails.get(key);
		}

		return result;
	}

	public void setEndDetails(Map<String, ?> moreEndDetails) {
		this.endDetails.putAll(moreEndDetails);
	}

	/**
	 * 
	 * @param endMetricEvent
	 * @return true if we succesfully registered an EndMetricEvent. Typically fails if already ended
	 */
	public boolean registerEndEvent(EndMetricEvent endMetricEvent) {
		return this.endMetricEvent.compareAndSet(null, endMetricEvent);
	}

	public Optional<EndMetricEvent> getEndEvent() {
		return Optional.ofNullable(endMetricEvent.get());
	}
}
