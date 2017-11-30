package blasd.apex.primitives;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blasd.apex.core.logging.ApexLogHelper;
import it.unimi.dsi.fastutil.ints.IntList;

public class TestCompressedIntArray_Performance {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestCompressedIntArray_Performance.class);

	@Test
	public void testCompresseeDecompress_Smalls() {
		int size = 8 * 1024 * 1024;

		long start = System.currentTimeMillis();
		IntList array = CompressedIntArrays.compress(IntStream.range(0, size).map(i -> i % 1024));
		long compressed = System.currentTimeMillis();

		AtomicLong sum = new AtomicLong(0);
		array.iterator().forEachRemaining((IntConsumer) i -> sum.addAndGet(i));
		long decompressed = System.currentTimeMillis();

		LOGGER.info("Time to compress: {}, Time to decompress: {}",
				ApexLogHelper.getNiceTime(compressed - start),
				ApexLogHelper.getNiceTime(decompressed - start));
	}

}
