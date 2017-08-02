package blasd.apex.shared.util;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import blasd.apex.core.agent.InstrumentationAgent;
import blasd.apex.core.memory.ApexMemoryHelper;

public class TestApexMemoryMeter {
	@BeforeClass
	public static void assumeAgentLoaded() {
		Assume.assumeTrue("We failed retrieving an Instrumentation",
				InstrumentationAgent.getInstrumentation().isPresent());
	}

	@Test
	public void testStringWeight() {
		Assert.assertEquals(56, ApexMemoryHelper.deepSize("Youpi"));

		if (false) {
			// Adding a single char add 2 bytes. As the JVM packes by block of 8 bytes, it may not be enough to grow the
			// estimated size
			Assert.assertTrue(ApexMemoryHelper.deepSize("Youpi") < ApexMemoryHelper.deepSize("Youpi+"));
		}
		// Adding 4 chars leads to adding 8 bytes: the actual JVM size is increased
		Assert.assertTrue(ApexMemoryHelper.deepSize("Youpi") < ApexMemoryHelper.deepSize("Youpi1234"));
	}

	@Test
	public void testImmutableMapWeight() {
		Assertions.assertThat(ApexMemoryHelper.deepSize(ImmutableMap.of("key", "Value"))).isBetween(100L, 250L);
	}

	@Test
	public void testRecursiveMapWeight() {
		// Consider a Map referencing itself
		Map<String, Object> recursiveMap = new HashMap<>();
		recursiveMap.put("myself", recursiveMap);

		long deepSize = ApexMemoryHelper.deepSize(recursiveMap);
		Assert.assertEquals(216, deepSize);

		// Change the Map so it does not reference itself: the object graph should have the same size
		Map<String, Object> withoutRecursivity = new HashMap<>();
		withoutRecursivity.put("myself", null);

		long notdeepSize = ApexMemoryHelper.deepSize(withoutRecursivity);
		Assert.assertEquals(notdeepSize, deepSize);
	}

	@Test
	public void testArrayWeight() {
		Object[] array = new Object[2];

		long sizeEmpty = ApexMemoryHelper.deepSize(array);
		Assert.assertEquals(24, sizeEmpty);

		array[0] = new LocalDate();
		array[1] = new LocalDate();

		long sizeFull = ApexMemoryHelper.deepSize(array);

		// We have different memory consumptions depending on the env/jdk/run
		Assertions.assertThat(sizeFull).isBetween(900L, 9200L);

		Assert.assertTrue(sizeFull > sizeEmpty);
	}
}
