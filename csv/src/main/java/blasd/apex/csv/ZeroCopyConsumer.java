package blasd.apex.csv;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.LongBinaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

public class ZeroCopyConsumer {
	protected static final Logger LOGGER = LoggerFactory.getLogger(ZeroCopyConsumer.class);

	public static IZeroCopyConsumer intConsumer(IntConsumer intConsumer) {
		AtomicLong rowIndex = new AtomicLong();

		return new IZeroCopyIntConsumer() {

			@Override
			public void nextRowIsMissing() {
				rowIndex.incrementAndGet();
				LOGGER.trace("No data for row #{}", rowIndex);
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				rowIndex.incrementAndGet();
				LOGGER.trace("Invalid data for row #{}: {}", rowIndex, charSequence);
			}

			@Override
			public void accept(int value) {
				intConsumer.accept(value);

				rowIndex.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				return rowIndex.get();
			}
		};
	}

	public static IZeroCopyConsumer intBinaryOperator(IntBinaryOperator intBinaryOperator) {
		AtomicLong rowIndex = new AtomicLong();

		return new IZeroCopyIntConsumer() {

			@Override
			public void nextRowIsMissing() {
				rowIndex.incrementAndGet();
				LOGGER.trace("No data for row #{}", rowIndex);
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				rowIndex.incrementAndGet();
				LOGGER.trace("Invalid data for row #{}: {}", rowIndex, charSequence);
			}

			@Override
			public void accept(int value) {
				intBinaryOperator.applyAsInt(Ints.checkedCast(rowIndex.get()), value);

				rowIndex.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				return rowIndex.get();
			}
		};
	}

	public static IZeroCopyConsumer longBinaryOperator(LongBinaryOperator intBinaryOperator) {
		AtomicLong rowIndex = new AtomicLong();

		return new IZeroCopyLongConsumer() {

			@Override
			public void nextRowIsMissing() {
				rowIndex.incrementAndGet();
				LOGGER.trace("No data for row #{}", rowIndex);
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				rowIndex.incrementAndGet();
				LOGGER.trace("Invalid data for row #{}: {}", rowIndex, charSequence);
			}

			@Override
			public void accept(long value) {
				intBinaryOperator.applyAsLong(rowIndex.get(), value);

				rowIndex.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				return rowIndex.get();
			}
		};
	}

	public static IZeroCopyConsumer doubleBinaryOperator(DoubleBinaryOperator intBinaryOperator) {
		AtomicLong rowIndex = new AtomicLong();

		return new IZeroCopyDoubleConsumer() {

			@Override
			public void nextRowIsMissing() {
				rowIndex.incrementAndGet();
				LOGGER.trace("No data for row #{}", rowIndex);
			}

			@Override
			public void nextRowIsInvalid(CharSequence charSequence) {
				rowIndex.incrementAndGet();
				LOGGER.trace("Invalid data for row #{}: {}", rowIndex, charSequence);
			}

			@Override
			public void accept(double value) {
				intBinaryOperator.applyAsDouble(rowIndex.get(), value);

				rowIndex.incrementAndGet();
			}

			@Override
			public long nextValueRowIndex() {
				return rowIndex.get();
			}
		};
	}

}
