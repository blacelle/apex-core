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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
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

	// As retrieving the stack could be expensive, this boolean has to be set to
	// true manually or with SetStaticMBean
	private static boolean doRememberStack = false;

	public static void setDoRememberStack(boolean doRememberStack) {
		StartMetricEvent.doRememberStack = doRememberStack;
	}

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

	public OptionalLong getProgress() {
		long current = progress.getAsLong();
		if (current == -1) {
			return OptionalLong.empty();
		} else {
			return OptionalLong.of(current);
		}
	}
}
