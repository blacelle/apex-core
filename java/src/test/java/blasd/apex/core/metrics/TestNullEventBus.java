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

import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.eventbus.EventBus;

import blasd.apex.core.metrics.EndMetricEvent;
import blasd.apex.core.metrics.StartMetricEvent;

public class TestNullEventBus {
	@Test
	public void testNull() {
		EventBus eventBus = Mockito.mock(EventBus.class);
		StartMetricEvent.post(eventBus::post, this, "name");
		StartMetricEvent.post(null, this, "name");
		StartMetricEvent.post(null, this, "name");

		StartMetricEvent.post(eventBus::post, this, "name");
		StartMetricEvent.post(null, this, "name");

		EndMetricEvent.postEndEvent(eventBus::post, null);
	}
}
