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
package blasd.apex.org.apache.cassandra.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import blasd.apex.shared.thread.IApexThreadDumper;
import blasd.apex.shared.util.IApexMemoryConstants;

public class TestGCInspector implements IApexMemoryConstants {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestGCInspector.class);

	/**
	 * Test by monitoring an application doing stressful memory allocation
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReporter() throws Exception {
		GCInspector gcInspector = new GCInspector(Mockito.mock(IApexThreadDumper.class));
		gcInspector.setMarksweepDurationMillisForThreadDump(1);
		gcInspector.afterPropertiesSet();

		try {
			Queue<int[]> allArrays = new LinkedBlockingQueue<>();
			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				// Allocate more and more memory to stress the GC
				int[] array = new int[i * i * i * KB_INT];

				LOGGER.info("Allocate: " + (array.length / (MB * 4L)) + "MB");

				// We keep the array in memory to have more and more objects
				allArrays.add(array);

				if (allArrays.size() > 3) {
					// Free some memory to enable GC
					allArrays.poll();
				}

				if (gcInspector.getLatestThreadDump() != null) {
					throw new OutOfMemoryError("Early quit: we stressed enough the GC");
				}
			}
			Assert.fail("We expect an OOM");
		} catch (OutOfMemoryError e) {
			LOGGER.info("We got the expected OOM");
			// We expect an OutOfMemorry as it is the best way to monitor GC
			// activity
		}

		// Tough to stress enough the GC to get a ThreadDump
		// Assert.assertNotNull(gcInspector.getLatestThreadDump());

		gcInspector.destroy();
	}

	@Test
	public void testDetectUnitTest() {
		Assert.assertTrue(GCInspector.inUnitTest());
	}

	@Test
	public void testGetThreadNameAllocatedHeap() {
		GCInspector gcInspector = new GCInspector(Mockito.mock(IApexThreadDumper.class));

		Map<String, String> allocated = gcInspector.getThreadNameToAllocatedHeapNiceString();
		Assert.assertTrue(allocated.containsKey(Thread.currentThread().getName()));

		gcInspector.markNowAsAllocatedHeapReference();

		Map<String, String> allocatedAfterMark = gcInspector.getThreadNameToAllocatedHeapNiceString();
		Assert.assertTrue(allocatedAfterMark.containsKey(Thread.currentThread().getName()));
	}

	@Test
	public void testGetThreadGroupsAllocatedHeap() {
		GCInspector gcInspector = new GCInspector(Mockito.mock(IApexThreadDumper.class));

		Map<String, String> allocated = gcInspector.getThreadGroupsToAllocatedHeapNiceString();
		Assert.assertTrue(allocated.containsKey(Thread.currentThread().getName()));

		gcInspector.markNowAsAllocatedHeapReference();

		Map<String, String> allocatedAfterMark = gcInspector.getThreadGroupsToAllocatedHeapNiceString();
		Assert.assertTrue(allocatedAfterMark.containsKey(Thread.currentThread().getName()));
	}

	@Test
	public void testGroupThreadNames() {
		GCInspector gcInspector = new GCInspector(Mockito.mock(IApexThreadDumper.class));

		Map<String, Long> detailedMap = new HashMap<>();

		detailedMap.put("SingleThread", 1L);
		detailedMap.put("GroupThread-1-0", 3L);
		detailedMap.put("GroupThread-2-1", 5L);
		detailedMap.put("GroupThread-2-234", 7L);

		Map<String, Long> grouped = gcInspector.groupThreadNames(detailedMap).asMap();
		Assert.assertEquals(ImmutableMap.of("SingleThread", 1L, "GroupThread-1-X", 3L, "GroupThread-2-X", 12L),
				grouped);
	}

	@Test
	public void testGetHeapHistogram() throws Exception {
		GCInspector gcInspector = new GCInspector(Mockito.mock(IApexThreadDumper.class));

		List<String> asList = Splitter.on('\n').splitToList(gcInspector.getHeapHistogram());

		// Check we have many rows
		Assert.assertTrue(asList.size() > 5);
	}

	@Test
	public void testTriggerFullGC() throws Exception {
		AtomicInteger nbBackToNormal = new AtomicInteger();

		AtomicLong usedHeap = new AtomicLong();
		AtomicLong maxHeap = new AtomicLong(100);

		GCInspector gcInspector = new GCInspector(Mockito.mock(IApexThreadDumper.class)) {
			@Override
			protected void onMemoryBackUnderThreshold(long heapUsed, long heapMax) {
				nbBackToNormal.incrementAndGet();
			}

			@Override
			protected long getUsedHeap() {
				return usedHeap.get();
			}

			@Override
			protected long getMaxHeap() {
				return maxHeap.get();
			}
		};

		// 10%
		usedHeap.set(10);

		gcInspector.logIfMemoryOverCap();
		Assert.assertEquals(0, nbBackToNormal.get());

		// 95%
		usedHeap.set(95);

		gcInspector.logIfMemoryOverCap();
		Assert.assertEquals(0, nbBackToNormal.get());

		// 15%
		usedHeap.set(15);

		gcInspector.logIfMemoryOverCap();
		Assert.assertEquals(1, nbBackToNormal.get());

		// Log again: still OK
		gcInspector.logIfMemoryOverCap();
		Assert.assertEquals(1, nbBackToNormal.get());
	}

	@Test
	public void limitedHeapHisto() {
		String firstRows = GCInspector.getHeapHistogramAsString(5);

		// We have skipped the initial empty row
		// +1 as we added the last rows
		Assert.assertEquals(5 + 1, firstRows.split(System.lineSeparator()).length);

		// The last row looks like: Total 1819064 141338008
		Assert.assertTrue(firstRows.split(System.lineSeparator())[5].startsWith("Total "));
	}
}
