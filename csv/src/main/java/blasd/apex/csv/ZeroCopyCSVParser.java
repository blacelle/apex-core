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
package blasd.apex.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blasd.apex.core.primitive.ApexParserHelper;
import blasd.apex.core.primitive.Jdk9CharSequenceParsers;

/**
 * Default implementation for {@link IZeroCopyCSVParser}
 * 
 * @author Benoit Lacelle
 *
 */
public class ZeroCopyCSVParser implements IZeroCopyCSVParser {

	private final class Youpi implements IZeroCopyConsumer, Consumer<CharSequence> {
		private final List<String> pendingStrings;

		private Youpi(List<String> pendingStrings) {
			this.pendingStrings = pendingStrings;
		}

		@Override
		public void nextRowIsMissing() {
			pendingStrings.add("");
		}

		@Override
		public void nextRowIsInvalid(CharSequence charSequence) {
			pendingStrings.add(charSequence.toString());
		}

		@Override
		public void accept(CharSequence t) {
			pendingStrings.add(t.toString());
		}
	}

	protected static final Logger LOGGER = LoggerFactory.getLogger(ZeroCopyCSVParser.class);

	private static final int DEFAULT_BUFFER_SIZE = 1024;

	protected final int bufferSize;

	public ZeroCopyCSVParser() {
		bufferSize = DEFAULT_BUFFER_SIZE;
	}

	public ZeroCopyCSVParser(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	// Minimal memory consumption: bufferSize * (4(CharBuffer) + 4 (char[] buffer))
	@Override
	public void parse(Reader reader, char separator, List<IZeroCopyConsumer> consumers) throws IOException {
		consumers.stream().filter(Objects::nonNull).forEach(consumer -> {
			if (consumer instanceof IntConsumer) {
				LOGGER.trace("We like IntConsumer");
			} else if (consumer instanceof LongConsumer) {
				LOGGER.trace("We like LongConsumer");
			} else if (consumer instanceof DoubleConsumer) {
				LOGGER.trace("We like DoubleConsumer");
			} else if (consumer instanceof Consumer<?>) {
				LOGGER.trace("We hope it is a Consumer<CharSequence>");
			} else {
				throw new IllegalArgumentException("You need to be any java.util.function.*Consumer");
			}
		});

		IntFunction<IZeroCopyConsumer> indexToConsumer = index -> {
			if (index < consumers.size()) {
				return consumers.get(index);
			} else {
				return null;
			}
		};

		CharBuffer charBuffer = CharBuffer.allocate(bufferSize);

		// Do like we were at the end of the buffer
		charBuffer.position(charBuffer.limit());

		// We need to read at least once
		boolean moreToRead = true;

		int columnIndex = 0;
		int firstValueCharIndex = -1;

		char[] buffer = new char[charBuffer.capacity()];

		while (moreToRead) {
			if (firstValueCharIndex >= 0) {
				// Keep as active these interesting characters
				charBuffer.position(firstValueCharIndex);
			}

			charBuffer.compact();
			if (firstValueCharIndex >= 0) {
				firstValueCharIndex = 0;
			}

			// We do not use Reader.read(CharBuffer) as it would allocate a transient char[]
			int nbRead = reader.read(buffer, 0, Math.min(buffer.length, charBuffer.remaining()));
			if (nbRead > 0) {
				charBuffer.put(buffer, 0, nbRead);
			}

			if (nbRead == 0) {
				// Is it legal ? We may have 0 bytes if it is buffered but the buffer is not filled yet
				// Or is it a bug in our code? Or is it the buffer is too small?
				throw new IllegalStateException("Unable to read data");
			}
			charBuffer.flip();

			if (nbRead > 0) {
			} else if (nbRead < 0) {
				moreToRead = false;
			}

			while (charBuffer.hasRemaining()) {
				// for (int charIndex = leftover; charIndex < charBuffer.limit(); charIndex++) {
				// Next char is the one not yet processed
				// int nextChar = charBuffer.get(charIndex);
				int nextChar = charBuffer.get();

				if (nextChar == separator) {
					// We are closing a column: publish the column content
					int oldLimit = charBuffer.limit();
					int oldPosition = charBuffer.position();
					charBuffer.limit(oldPosition - 1);
					flushColumn(indexToConsumer, charBuffer, firstValueCharIndex, columnIndex);
					// Move to the leftover: set a wide limit, and then fix a corrected new position
					charBuffer.limit(oldLimit);
					charBuffer.position(oldPosition);
					firstValueCharIndex = -1;

					// Indicate we have spot a new column
					columnIndex++;
				} else if (nextChar == '\r' || nextChar == '\n') {
					if (firstValueCharIndex >= 0) {
						int oldLimit = charBuffer.limit();
						int oldPosition = charBuffer.position();
						charBuffer.limit(oldPosition - 1);
						flushColumn(indexToConsumer, charBuffer, firstValueCharIndex, columnIndex);
						// Move to the leftover: set a wide limit, and then fix a corrected new position
						charBuffer.limit(oldLimit);
						charBuffer.position(oldPosition);
						firstValueCharIndex = -1;
					} else {
						// empty row (or \r\n)
					}

					warnConsumersWithoutColumn(indexToConsumer, columnIndex, consumers.size());

					// Reset the columnIndex
					columnIndex = 0;
				} else {
					// We have an interesting character
					// doFlush = false;
					if (firstValueCharIndex < 0) {
						// Remember the start of the interesting characters
						firstValueCharIndex = charBuffer.position() - 1;
					}
				}
			}

			if (!moreToRead) {
				// We are at the end of the file
				// We have detected the beginning of an input and then encounter EOF: we have to flush the column
				if (firstValueCharIndex >= 0) {
					flushColumn(indexToConsumer, charBuffer, firstValueCharIndex, columnIndex);
				} else {
					// empty row (or \r\n)
				}
				warnConsumersWithoutColumn(indexToConsumer, columnIndex, consumers.size());
			}
		}
	}

	protected void warnConsumersWithoutColumn(IntFunction<IZeroCopyConsumer> consumers, int columnIndex, int maxIndex) {
		for (int i = columnIndex; i < maxIndex; i++) {
			// Warn the consumers that will not receive any data
			// We have no constrain about the order in which consumers are notified for a given row
			IZeroCopyConsumer consumer = consumers.apply(i);
			if (consumer != null) {
				consumer.nextRowIsMissing();
			}
		}
	}

	protected void flushColumn(IntFunction<IZeroCopyConsumer> consumers,
			CharBuffer charBuffer,
			int firstValueCharIndex,
			int flushedColumnIndex) {
		IZeroCopyConsumer consumer = consumers.apply(flushedColumnIndex);
		if (consumer != null) {
			if (firstValueCharIndex >= 0) {
				// We do have data to flush
				charBuffer.position(firstValueCharIndex);

				flushContent(consumer, charBuffer, flushedColumnIndex);
			} else {
				// No data to flush
				consumer.nextRowIsMissing();
			}
		}
	}

	protected void flushContent(IZeroCopyConsumer consumer, CharBuffer charBuffer, int columnIndex) {
		if (consumer == null) {
			return;
		}
		// We have a consumer: let's process the column
		CharSequence subSequence = charBuffer;
		// .subSequence(charBuffer.position(), charBuffer.limit());

		try {
			if (consumer instanceof IntConsumer) {
				if (subSequence.length() == 0) {
					consumer.nextRowIsMissing();
				} else {
					((IntConsumer) consumer)
							.accept(Jdk9CharSequenceParsers.parseInt(subSequence, 0, charBuffer.length(), 10));
				}
			} else if (consumer instanceof LongConsumer) {
				if (subSequence.length() == 0) {
					consumer.nextRowIsMissing();
				} else {
					((LongConsumer) consumer)
							.accept(Jdk9CharSequenceParsers.parseLong(subSequence, 0, charBuffer.length(), 10));
				}
			} else if (consumer instanceof DoubleConsumer) {
				if (subSequence.length() == 0) {
					consumer.nextRowIsMissing();
				} else {
					((DoubleConsumer) consumer).accept(ApexParserHelper.parseDouble(subSequence));
				}
			} else if (consumer instanceof Consumer<?>) {
				// You have better to be a CharSequence consumer
				((Consumer) consumer).accept(subSequence);
			} else {
				throw new IllegalArgumentException("Not a consumer ?!");
			}
		} catch (NumberFormatException e) {
			if (LOGGER.isTraceEnabled()) {
				// check.isTraceEnabled to spare the transient memory of the message
				LOGGER.trace("Ouch on " + subSequence, e);
			}
			consumer.nextRowIsInvalid(subSequence);
		}
	}

	public Stream<String[]> parseAsStringArrays(Reader reader, char separator) {
		CharBuffer charBuffer = CharBuffer.allocate(bufferSize);

		// Do like we were at the end of the buffer
		charBuffer.position(charBuffer.limit());

		// We need to read at least once
		AtomicBoolean moreToRead = new AtomicBoolean(true);

		AtomicInteger columnIndex = new AtomicInteger(0);
		AtomicInteger firstValueCharIndex = new AtomicInteger(-1);

		final char[] buffer = new char[charBuffer.capacity()];

		List<String> pendingStrings = new ArrayList<>();

		IZeroCopyConsumer consumer = new Youpi(pendingStrings);

		IntFunction<IZeroCopyConsumer> indexToConsumer = index -> consumer;

		return StreamSupport.stream(new Spliterators.AbstractSpliterator<String[]>(Long.MAX_VALUE, 0) {

			@Override
			public boolean tryAdvance(Consumer<? super String[]> action) {
				boolean actionHasBeenTriggered = false;

				// Continue until there is bytes to process
				// Stop if no more bytes, or else if a row have been processed
				while (moreToRead.get() && !actionHasBeenTriggered) {
					if (firstValueCharIndex.get() >= 0) {
						// Keep as active these interesting characters
						charBuffer.position(firstValueCharIndex.get());
					}

					charBuffer.compact();
					if (firstValueCharIndex.get() >= 0) {
						firstValueCharIndex.set(0);
					}

					// We do not use Reader.read(CharBuffer) as it would allocate a transient char[]
					int nbRead;
					try {
						nbRead = reader.read(buffer, 0, Math.min(buffer.length, charBuffer.remaining()));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
					if (nbRead > 0) {
						charBuffer.put(buffer, 0, nbRead);
					}

					if (nbRead == 0) {
						// Is it legal ? We may have 0 bytes if it is buffered but the buffer is not filled yet
						// Or is it a bug in our code? Or is it the buffer is too small?
						throw new IllegalStateException("Unable to read data");
					}
					charBuffer.flip();

					if (nbRead > 0) {
					} else if (nbRead < 0) {
						moreToRead.set(false);
					}

					while (charBuffer.hasRemaining()) {
						// for (int charIndex = leftover; charIndex < charBuffer.limit(); charIndex++) {
						// Next char is the one not yet processed
						// int nextChar = charBuffer.get(charIndex);
						int nextChar = charBuffer.get();

						if (nextChar == separator) {
							// We are closing a column: publish the column content
							int oldLimit = charBuffer.limit();
							int oldPosition = charBuffer.position();
							charBuffer.limit(oldPosition - 1);
							flushColumn(indexToConsumer, charBuffer, firstValueCharIndex.get(), columnIndex.get());
							// Move to the leftover: set a wide limit, and then fix a corrected new position
							charBuffer.limit(oldLimit);
							charBuffer.position(oldPosition);
							firstValueCharIndex.set(-1);

							// Indicate we have spot a new column
							columnIndex.incrementAndGet();
						} else if (nextChar == '\r' || nextChar == '\n') {
							if (firstValueCharIndex.get() >= 0) {
								int oldLimit = charBuffer.limit();
								int oldPosition = charBuffer.position();
								charBuffer.limit(oldPosition - 1);
								flushColumn(indexToConsumer, charBuffer, firstValueCharIndex.get(), columnIndex.get());
								// Move to the leftover: set a wide limit, and then fix a corrected new position
								charBuffer.limit(oldLimit);
								charBuffer.position(oldPosition);
								firstValueCharIndex.set(-1);
							} else {
								// empty row (or \r\n)
							}

							warnConsumersWithoutColumn(indexToConsumer, columnIndex.get(), -1);

							if (!pendingStrings.isEmpty()) {
								action.accept(pendingStrings.toArray(new String[pendingStrings.size()]));
								pendingStrings.clear();
								actionHasBeenTriggered = true;
							}

							// Reset the columnIndex
							columnIndex.set(0);
						} else {
							// We have an interesting character
							// doFlush = false;
							if (firstValueCharIndex.get() < 0) {
								// Remember the start of the interesting characters
								firstValueCharIndex.set(charBuffer.position() - 1);
							}
						}
					}

					if (!moreToRead.get()) {
						// We are at the end of the file
						// We have detected the beginning of an input and then encounter EOF: we have to flush the
						// column
						if (firstValueCharIndex.get() >= 0) {
							flushColumn(indexToConsumer, charBuffer, firstValueCharIndex.get(), columnIndex.get());
						} else {
							// empty row (or \r\n)
						}
						warnConsumersWithoutColumn(indexToConsumer, columnIndex.get(), -1);
					}
				}

				return actionHasBeenTriggered;
			}
		}, false);
	}
}
