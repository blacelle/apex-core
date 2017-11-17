package blasd.apex.server.loading.csvsource;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

import blasd.apex.csv.IZeroCopyIntConsumer;
import blasd.apex.csv.IZeroCopyLongConsumer;
import blasd.apex.csv.ZeroCopyCSVParser;

public class TestZeroCopyCSVParser {
	protected static final Logger LOGGER = LoggerFactory.getLogger(TestZeroCopyCSVParser.class);

	ZeroCopyCSVParser parser = new ZeroCopyCSVParser();

	AtomicLong nbEvents = new AtomicLong();
	AtomicLong nbMissing = new AtomicLong();
	AtomicLong nbInvalid = new AtomicLong();

	@Test
	public void testEmptyString_NoConsumer() throws IOException {
		parser.parse(new StringReader(""), ',', Collections.emptyList());
	}

	@Test
	public void testEmptyString_OneConsumer() throws IOException {
		parser.parse(new StringReader(""), ',', Collections.singletonList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		// We may discuss if we should consider the first row is missing or not
		// Assert.assertEquals(1, nbMissing.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertEquals(0, nbEvents.get());
	}

	@Test
	public void testEmptyStringAndSeparator_OneConsumer() throws IOException {
		parser.parse(new StringReader(","), ',', Collections.singletonList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(1, nbMissing.get());
		Assert.assertEquals(0, nbEvents.get());
	}

	@Test
	public void testSingleColumn_IntColumn_OneIntConsumer() throws IOException {
		parser.parse(new StringReader("123"), ',', Collections.singletonList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertEquals(1, nbEvents.get());
	}

	@Test
	public void testSingleColumn_IntColumn_OneIntConsumer_MultipleEOL() throws IOException {
		parser.parse(new StringReader("123\r\n\r\n"), ',', Collections.singletonList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertEquals(1, nbEvents.get());
	}

	@Test
	public void testSingleColumn_IntColumn_OneIntConsumer_TwoRows() throws IOException {
		parser.parse(new StringReader("123\r234"), ',', Collections.singletonList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertEquals(2, nbEvents.get());
	}

	@Test
	public void testTwoColumn_IntColumns_OneIntConsumer() throws IOException {
		parser.parse(new StringReader("123,234"), ',', Collections.singletonList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		// We have no consumer or the second column
		Assert.assertEquals(1, nbEvents.get());
	}

	@Test
	public void testTwoColumn_IntColumns_OneIntConsumerOneLongConsumer() throws IOException {
		parser.parse(new StringReader("123,234"), ',', Arrays.asList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}, new IZeroCopyLongConsumer() {

			@Override
			public void accept(long value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertEquals(2, nbEvents.get());
	}

	@Test
	public void testTwoColumn_IntColumns_OneIntConsumerOneLongConsumer_TwoRows() throws IOException {
		parser.parse(new StringReader("123,234\n345,456"), ',', Arrays.asList(new IZeroCopyIntConsumer() {

			@Override
			public void accept(int value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}, new IZeroCopyLongConsumer() {

			@Override
			public void accept(long value) {
				nbEvents.incrementAndGet();
			}

			@Override
			public void nextRowIsMissing() {
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				throw new UnsupportedOperationException();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertEquals(4, nbEvents.get());
	}

	@Test
	public void testParseToPrimitiveArrays() throws IOException {
		int[] firstColumn = new int[2];
		long[] secondColumn = new long[2];

		parser.parse(new StringReader("123,234\n345,456"), ',', Arrays.asList(new IZeroCopyIntConsumer() {
			AtomicLong rowIndex = new AtomicLong();

			@Override
			public void accept(int value) {
				firstColumn[Ints.checkedCast(rowIndex.getAndIncrement())] = value;
			}

			@Override
			public void nextRowIsMissing() {
				rowIndex.getAndIncrement();
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				return rowIndex.get();
			}
		}, new IZeroCopyLongConsumer() {

			AtomicLong rowIndex = new AtomicLong();

			@Override
			public void accept(long value) {
				secondColumn[Ints.checkedCast(rowIndex.getAndIncrement())] = value;
			}

			@Override
			public void nextRowIsMissing() {
				rowIndex.getAndIncrement();
				nbMissing.incrementAndGet();
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				LOGGER.error("Invalid: '{}'", charSequence);
				nbInvalid.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				return rowIndex.get();
			}
		}));

		Assert.assertEquals(0, nbInvalid.get());
		Assert.assertEquals(0, nbMissing.get());
		Assert.assertArrayEquals(new int[] { 123, 345 }, firstColumn);
		Assert.assertArrayEquals(new long[] { 234, 456 }, secondColumn);
	}
}
