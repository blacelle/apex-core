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

import java.util.function.Consumer;

/**
 * Help usage of StartMetricEvent with try-finally blocks
 * 
 * @author Benoit Lacelle
 *
 */
public class AutoCloseableStartMetricEvent implements AutoCloseable {
	protected final StartMetricEvent decorated;
	protected final Consumer<? super AMetricEvent> eventBus;

	public AutoCloseableStartMetricEvent(StartMetricEvent startEvent, Consumer<? super AMetricEvent> eventBus) {
		this.decorated = startEvent;
		this.eventBus = eventBus;
	}

	@Override
	public void close() {
		// Will be handled by ApexMetricsTowerControl.onEndEvent(EndMetricEvent)
		EndMetricEvent.postEndEvent(eventBus, decorated);
	}

	public StartMetricEvent getStartEvent() {
		return decorated;
	}

}
