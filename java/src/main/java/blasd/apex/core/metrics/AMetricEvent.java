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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;

/**
 * Parent class for events to be published in an {@link EventBus}. They will be handled by
 * {@link ApexMetricsTowerControl}
 * 
 * @author Benoit Lacelle
 * 
 */
public class AMetricEvent {
	protected static final Logger LOGGER = LoggerFactory.getLogger(AMetricEvent.class);

	// This UUID is constant through the whole applciation lifecycle. It can be
	// used to seggregate events from different application runs
	public static final String INSTANCE_UUID = UUID.randomUUID().toString();

	private static final AtomicLong EVENT_INCREMENTER = new AtomicLong();

	// This id is unique amongst a given INSTANCE_UUID
	public final long eventId = EVENT_INCREMENTER.getAndIncrement();

	public static final Set<Class<?>> SOURCE_CLASSES = Sets.newConcurrentHashSet();

	public final Object source;
	public final List<? extends String> names;

	public AMetricEvent(Object source, List<? extends String> names) {
		this.source = Objects.requireNonNull(source);
		if (names == null) {
			this.names = Collections.emptyList();
		} else {
			this.names = names;
		}

		SOURCE_CLASSES.add(source.getClass());
	}

	public AMetricEvent(Object source, String firstName, String... otherNames) {
		this(source, Lists.asList(firstName, otherNames));
	}

	public static void post(Consumer<? super AMetricEvent> eventBus, AMetricEvent simpleEvent) {
		if (eventBus == null) {
			logNoEventBus(simpleEvent.source, simpleEvent.names);
		} else {
			eventBus.accept(simpleEvent);
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("names", names).add("source", source).toString();
	}

	protected static void logNoEventBus(Object source, List<?> names) {
		LOGGER.info("No EventBus has been injected for {} on {}", names, source);
	}

	public static Set<Class<?>> getSourceClasses() {
		return Collections.unmodifiableSet(SOURCE_CLASSES);
	}
}
