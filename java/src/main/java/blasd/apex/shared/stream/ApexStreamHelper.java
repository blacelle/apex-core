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
package blasd.apex.shared.stream;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
}
