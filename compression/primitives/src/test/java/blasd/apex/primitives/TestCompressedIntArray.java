package blasd.apex.primitives;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blasd.apex.core.logging.ApexLogHelper;
import blasd.apex.core.memory.ApexMemoryHelper;
import it.unimi.dsi.fastutil.ints.IntList;

public class TestCompressedIntArray {
	protected static final Logger LOGGER = LoggerFactory.getLogger(TestCompressedIntArray.class);

	@Test
	public void testGrowingBy1() {
		int size = 1024 * 1024;

		IntList array = CompressedIntArray.compress(IntStream.range(0, size));

		LOGGER.info("testGrowingBy1 CompressedSize: {}", ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(array)));
		LOGGER.info("testGrowingBy1 RawSize: {}",
				ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(IntStream.range(0, size).toArray())));
	}

	@Test
	public void testManyVerySmall() {
		int size = 1024 * 1024;

		IntList array = CompressedIntArray.compress(IntStream.range(0, size).map(i -> i % 16));

		LOGGER.info("testManyVerySmall CompressedSize: {}",
				ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(array)));
		LOGGER.info("testManyVerySmall RawSize: {}",
				ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(IntStream.range(0, size).toArray())));
	}

	@Test
	public void testManySmall() {
		int size = 1024 * 1024;

		IntList array = CompressedIntArray.compress(IntStream.range(0, size).map(i -> i % 1024));

		LOGGER.info("testManySmall CompressedSize: {}", ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(array)));
		LOGGER.info("testManySmall RawSize: {}",
				ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(IntStream.range(0, size).toArray())));
	}

	@Test
	public void testManyRandom() {
		int size = 1024 * 1024;
		Random r = new Random(0);

		IntList array = CompressedIntArray.compress(IntStream.range(0, size).map(i -> r.nextInt()));

		LOGGER.info("testManySmall CompressedSize: {}", ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(array)));
		LOGGER.info("testManySmall RawSize: {}",
				ApexLogHelper.getNiceMemory(ApexMemoryHelper.deepSize(IntStream.range(0, size).toArray())));
	}

}
