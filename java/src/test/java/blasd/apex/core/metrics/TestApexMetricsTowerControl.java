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

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Date;
import java.util.NavigableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

import blasd.apex.core.metrics.ApexMetricsTowerControl;
import blasd.apex.core.metrics.AutoCloseableStartMetricEvent;
import blasd.apex.core.metrics.EndMetricEvent;
import blasd.apex.core.metrics.StartMetricEvent;
import blasd.apex.core.thread.ApexExecutorsHelper;
import blasd.apex.core.thread.ApexThreadDump;

public class TestApexMetricsTowerControl {
	protected ApexMetricsTowerControl makeApexMetricsTowerControl() {
		return new ApexMetricsTowerControl(new ApexThreadDump(ManagementFactory.getThreadMXBean()));
	}

	@Test
	public void testMetricsTowerControl() {
		EventBus eventBus = new EventBus((exception, context) -> {
			throw new RuntimeException(exception);
		});

		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();
		eventBus.register(mtc);

		eventBus.post(new OutOfMemoryError("Unit-Test forcing an OOM"));

		// 1 for FailureEvent
		// Assert.assertEquals(1, mtc.metricRegistry.getCounters().size());
		//
		// // 1 for SizeMetricEvent
		// Assert.assertEquals(1, mtc.getMetricsAsString().size());
	}

	@Test
	public void testStartEndWorkFlow() {
		EventBus eventBus = new EventBus((exception, context) -> {
			throw new RuntimeException(exception);
		});

		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();
		eventBus.register(mtc);

		Assert.assertEquals(0, mtc.getActiveTasksSize());

		StartMetricEvent se = new StartMetricEvent(this, "test");
		eventBus.post(se);
		Assert.assertEquals(1, mtc.getActiveTasksSize());

		eventBus.post(new EndMetricEvent(se));
		Assert.assertEquals(0, mtc.getActiveTasksSize());
	}

	@Test
	public void testInvalidateActiveTasks() {
		EventBus eventBus = new EventBus((exception, context) -> {
			throw new RuntimeException(exception);
		});

		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();
		eventBus.register(mtc);

		StartMetricEvent se = new StartMetricEvent(this, "test");
		eventBus.post(se);
		Assert.assertEquals(1, mtc.getActiveTasks().size());

		// Invalidate something which does not exist
		Assert.assertFalse(mtc.invalidateActiveTasks("not-existing"));

		// Invalidate something that exists
		boolean invalidateResult = mtc.invalidateActiveTasks(se.toStringNoStack());
		Assert.assertTrue(invalidateResult);
		Assert.assertEquals(0, mtc.getActiveTasks().size());

		// Check things goes well when ending an invalidated event
		eventBus.post(new EndMetricEvent(se));
		Assert.assertEquals(0, mtc.getActiveTasks().size());
	}

	@Test
	public void testEndEventWithoutStartEvent() {
		EventBus eventBus = new EventBus((exception, context) -> {
			throw new RuntimeException(exception);
		});

		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();
		eventBus.register(mtc);

		StartMetricEvent notSubmittedStartEvent = new StartMetricEvent(this, "detailName");
		mtc.onEndEvent(new EndMetricEvent(notSubmittedStartEvent));
	}

	@Test
	public void testStartWithStack() {
		EventBus eventBus = new EventBus((exception, context) -> {
			throw new RuntimeException(exception);
		});

		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();
		eventBus.register(mtc);

		mtc.setDoRememberStack(true);
		try {
			StartMetricEvent se = new StartMetricEvent(this, "some");
			eventBus.post(se);

			NavigableMap<Date, String> activeTasks = mtc.getActiveTasks();

			// Check thetre is a stack
			Assert.assertTrue(activeTasks.toString().contains("org.junit.runners.model."));
		} finally {
			mtc.setDoRememberStack(false);
		}
	}

	@Test
	public void testThreadDump() {
		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();

		mtc.getAllThreads(true);
		mtc.getAllThreads(false);
	}

	@Test
	public void benchPerformance() throws InterruptedException {
		final ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();

		final EventBus eventBus = new EventBus();
		eventBus.register(mtc);

		ExecutorService es = ApexExecutorsHelper.newShrinkableFixedThreadPool(64,
				"benchApexMetricsTowerControl",
				1000,
				ApexExecutorsHelper.TIMEOUT_POLICY_1_HOUR);

		for (int i = 0; i < 1000000; i++) {
			es.execute(() -> {
				try (AutoCloseableStartMetricEvent startEvent = StartMetricEvent.post(eventBus::post, mtc, "Test")) {
					// Nothing to do, but to close the startEvent
				}
			});
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.MINUTES);
	}

	@Test
	public void testLogLongRunningWithProgress() {
		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();

		Assert.assertEquals(0, mtc.getActiveTasksSize());

		AtomicLong progress = new AtomicLong();
		StartMetricEvent se = new StartMetricEvent(this, Collections.emptyMap(), () -> progress.get(), "test");

		Assert.assertTrue(mtc.noNewLine(se).toString().contains("progress=0"));

		progress.set(123456789L);
		Assert.assertTrue(mtc.noNewLine(se).toString().contains("progress=123456789"));
	}

	@Test
	public void testLogLongRunningWithoutProgress() {
		ApexMetricsTowerControl mtc = makeApexMetricsTowerControl();

		Assert.assertEquals(0, mtc.getActiveTasksSize());

		AtomicLong progress = new AtomicLong();
		StartMetricEvent se = new StartMetricEvent(this, Collections.emptyMap(), () -> progress.get(), "test");

		progress.set(-1L);
		Assert.assertFalse(mtc.noNewLine(se).toString().contains("progress"));
	}
}
