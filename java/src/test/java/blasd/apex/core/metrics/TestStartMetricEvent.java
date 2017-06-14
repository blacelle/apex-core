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

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;

import blasd.apex.core.metrics.EndMetricEvent;
import blasd.apex.core.metrics.StartMetricEvent;

public class TestStartMetricEvent {

	@After
	public void resetDoRememberStack() {
		StartMetricEvent.doRememberStack = false;
	}

	@Test
	public void testStackGeneration() {
		StartMetricEvent.doRememberStack = true;

		StartMetricEvent startEvent = new StartMetricEvent("detailName", "source", "names");

		Assert.assertTrue(startEvent.toString().contains("StartMetricEvent.<init>("));
		Assert.assertFalse(startEvent.toStringNoStack().contains("StartMetricEvent.<init>("));
	}

	@Test
	public void testStackNoGeneration() {
		StartMetricEvent.doRememberStack = false;

		StartMetricEvent startEvent = new StartMetricEvent("detailName", "source", "names");

		Assert.assertFalse(startEvent.toString().contains("StartMetricEvent.<init>("));
	}

	@Test
	public void testToStringWithUser() {
		StartMetricEvent.doRememberStack = false;

		StartMetricEvent startEvent = new StartMetricEvent("sourceObject",
				ImmutableMap.of(StartMetricEvent.KEY_USERNAME, "Benoit"),
				() -> -1L,
				"Test");

		Assertions.assertThat(startEvent.toString()).contains("Benoit");

		Assert.assertEquals("StartMetricEvent{names=[Test], source=sourceObject} startDetails={UserName=Benoit}",
				startEvent.toString());
	}

	@Test
	public void testToStringWithEndDetails() {
		StartMetricEvent.doRememberStack = false;

		StartMetricEvent startEvent = new StartMetricEvent("sourceObject", "Test");

		startEvent.setEndDetails(ImmutableMap.of("endKey", "endValue"));

		Assert.assertEquals("StartMetricEvent{names=[Test], source=sourceObject} endDetails={endKey=endValue}",
				startEvent.toString());
	}

	@Test
	public void testCloseSeveralTIme() {
		StartMetricEvent startEvent = new StartMetricEvent("detailName", "source", "names");

		EventBus eventBus = Mockito.spy(new EventBus());

		// Check we submit an endEvent
		EndMetricEvent end1 = EndMetricEvent.postEndEvent(eventBus::post, startEvent);
		Mockito.verify(eventBus).post(end1);

		// Check we did not submitted a second endEvent
		EndMetricEvent end2 = EndMetricEvent.postEndEvent(eventBus::post, startEvent);
		Mockito.verify(eventBus).post(end1);

		// Ensure closing several time the same event leads to a single end event
		Assert.assertSame(end1, end2);
	}
}
