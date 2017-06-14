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

import java.util.Objects;
import java.util.function.Consumer;

/**
 * An {@link EndMetricEvent} should be published to clese a previous {@link StartMetricEvent}
 * 
 * @author Benoit Lacelle
 * 
 */
public class EndMetricEvent extends AMetricEvent {
	public static final String KEY_RESULT_SIZE = "resultSize";
	public final StartMetricEvent startEvent;

	/**
	 * Generally called by EndMetricEvent.post
	 * 
	 * @param startEvent
	 */
	protected EndMetricEvent(StartMetricEvent startEvent) {
		super(startEvent.source, startEvent.names);
		this.startEvent = Objects.requireNonNull(startEvent);
	}

	/**
	 * return now minus startTime in milliseconds
	 */
	public long durationInMs() {
		return System.currentTimeMillis() - startEvent.startTime;
	}

	public static EndMetricEvent postEndEvent(Consumer<? super AMetricEvent> eventBus, StartMetricEvent startEvent) {
		if (startEvent == null) {
			LOGGER.info("No StartMetricEvent has been provided");
			return null;
		} else {
			EndMetricEvent endMetricEvent = new EndMetricEvent(startEvent);
			if (startEvent.registerEndEvent(endMetricEvent)) {
				post(eventBus, endMetricEvent);
				return endMetricEvent;
			} else {
				return startEvent.endMetricEvent.get();
			}
		}
	}
}
