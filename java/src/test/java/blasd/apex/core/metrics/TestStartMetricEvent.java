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
		StartMetricEvent.setDoRememberStack(false);
	}

	@Test
	public void testStackGeneration() {
		StartMetricEvent.setDoRememberStack(true);

		StartMetricEvent startEvent = new StartMetricEvent("detailName", "source", "names");

		Assert.assertTrue(startEvent.toString().contains("StartMetricEvent.<init>("));
		Assert.assertFalse(startEvent.toStringNoStack().contains("StartMetricEvent.<init>("));
	}

	@Test
	public void testStackNoGeneration() {
		StartMetricEvent.setDoRememberStack(false);

		StartMetricEvent startEvent = new StartMetricEvent("detailName", "source", "names");

		Assert.assertFalse(startEvent.toString().contains("StartMetricEvent.<init>("));
	}

	@Test
	public void testToStringWithUser() {
		StartMetricEvent.setDoRememberStack(false);

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
		StartMetricEvent.setDoRememberStack(false);

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
