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
import java.nio.CharBuffer;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

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

		CharBuffer charBuffer = CharBuffer.allocate(bufferSize);

		// Do like we were at the end of the buffer
		charBuffer.position(charBuffer.limit());

		// We need to read at least once
		boolean moreToRead = true;

		int columnIndex = 0;
		int firstValueCharIndex = -1;

		char[] buffer = new char[charBuffer.capacity()];

		while (moreToRead) {
			// System.out.println(ApexLogHelper.getNiceMemory(
			// ApexForOracleJVM.getThreadAllocatedBytes(THREAD_MBEAN, Thread.currentThread().getId())));

			// int leftover = charBuffer.remaining();

			// Try to read more data without compacting: if we had async process on the buffer, these would be allowed
			// to pursue there work freely
			// charBuffer.position(charBuffer.limit());
			// charBuffer.limit(charBuffer.capacity());
			// int nbRead = reader.read(charBuffer);

			// if (nbRead == 0) {
			// If we have async process, we need to wait for them as the underlying buffer is about to be mutated

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
					flushColumn(consumers, charBuffer, firstValueCharIndex, columnIndex);
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
						flushColumn(consumers, charBuffer, firstValueCharIndex, columnIndex);
						// Move to the leftover: set a wide limit, and then fix a corrected new position
						charBuffer.limit(oldLimit);
						charBuffer.position(oldPosition);
						firstValueCharIndex = -1;
					} else {
						// empty row (or \r\n)
					}

					warnConsumersWithoutColumn(consumers, columnIndex);

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
					flushColumn(consumers, charBuffer, firstValueCharIndex, columnIndex);
				} else {
					// empty row (or \r\n)
				}
				warnConsumersWithoutColumn(consumers, columnIndex);
			}
		}
	}

	protected void warnConsumersWithoutColumn(List<IZeroCopyConsumer> consumers, int columnIndex) {
		if (columnIndex < consumers.size() - 1) {
			// Warn the consumers that will not receive any data
			// We have no constrain about the order in which consumers are notified for a given row
			for (int i = columnIndex + 1; i < consumers.size(); i++) {
				IZeroCopyConsumer consumer = consumers.get(i);
				if (consumer != null) {
					consumer.nextRowIsMissing();
				}
			}
		}
	}

	protected void flushColumn(List<IZeroCopyConsumer> consumers,
			CharBuffer charBuffer,
			int firstValueCharIndex,
			int flushedColumnIndex) {
		if (flushedColumnIndex >= consumers.size()) {
			// We are not interested in these overflowing columns
			return;
		}

		IZeroCopyConsumer consumer = consumers.get(flushedColumnIndex);
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
}
