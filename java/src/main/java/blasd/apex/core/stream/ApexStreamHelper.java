/**
 * The MIT License
 * Copyright (c) ${project.inceptionYear} Benoit Lacelle
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
package blasd.apex.core.stream;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.annotations.Beta;
import com.google.common.base.Predicate;
import com.google.common.collect.Streams;

/**
 * Various helpers for Java8 {@link Stream}
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexStreamHelper {
	protected ApexStreamHelper() {
		// hidden
	}

	@Deprecated
	public static <T> Stream<T> toStream(Iterator<T> iterator) {
		// http://stackoverflow.com/questions/24511052/how-to-convert-an-iterator-to-a-stream
		return Streams.stream(iterator);
	}

	/**
	 * The safer way to produce a Stream: not parallel. Typically because ActivePivot re-use Object[] or IRecordReader
	 * while traversing an ICursor
	 * 
	 * @param iterable
	 * @return
	 */
	@Deprecated
	public static <T> Stream<T> toStream(Iterable<T> iterable) {
		return Streams.stream(iterable);
	}

	public static <T> Stream<T> singleton(T item) {
		return Collections.singleton(item).stream();
	}

	/**
	 * Justfor the sake of the example
	 * 
	 * @param list
	 * @param predicate
	 * @return
	 */
	@Beta
	public static <T> OptionalInt indexOf(List<T> list, Predicate<T> predicate) {
		// http://stackoverflow.com/questions/38963338/stream-way-to-get-index-of-first-element-matching-boolean
		return IntStream.range(0, list.size()).filter(i -> predicate.apply(list.get(i))).findFirst();
	}

	@Beta
	public static <T> long consumeByPartition(Supplier<? extends BlockingQueue<T>> queueSupplier,
			Stream<T> parallelTuplized,
			Consumer<Queue<T>> consumer) {
		AtomicLong nbConsumed = new AtomicLong();

		Queue<T> leftOvers = parallelTuplized.collect(queueSupplier, (queue, tuple) -> {
			queue.add(tuple);
			if (queue.remainingCapacity() == 0) {
				consumer.accept(queue);
				nbConsumed.addAndGet(queue.size());
				queue.clear();
			}
		}, (l, r) -> {
			// r has to be drained to l
			r.drainTo(l, l.remainingCapacity());
			if (!r.isEmpty()) {
				// We need to submit a batch
				consumer.accept(l);
				nbConsumed.addAndGet(l.size());
				l.clear();

				// We can fully drain as r is supposed to have same capacity than l
				r.drainTo(l);
			}

		});

		// The last transaction
		consumer.accept(leftOvers);
		nbConsumed.addAndGet(leftOvers.size());

		return nbConsumed.get();
	}

	/**
	 * Prevfent the requirement for a diamond
	 * 
	 * @return an empty Stream
	 */
	public static <T> Stream<T> emptyStream() {
		return Collections.<T>emptyList().stream();
	}

	private static <T> BinaryOperator<T> throwingMerger() {
		return (u, v) -> {
			throw new IllegalStateException(String.format("Duplicate key %s", u));
		};
	}

	/**
	 * 
	 * http://stackoverflow.com/questions/31004899/java-8-collectors-tomap-sortedmap
	 * 
	 * @param keyMapper
	 * @param valueMapper
	 * @param mapSupplier
	 * @return
	 */
	public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(Function<? super T, ? extends K> keyMapper,
			Function<? super T, ? extends U> valueMapper,
			Supplier<M> mapSupplier) {
		return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), mapSupplier);
	}
}