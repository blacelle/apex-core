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
package blasd.apex.core.csv;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.common.primitives.Bytes;

import javolution.text.TypeFormat;

/**
 * A CSV parser trying not to allocate any heap
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexCSVParser {

	protected final IApexCSVConfiguration apexCSVConfiguration;

	protected final Charset charset;
	protected final ByteBuffer wrap;

	protected final AtomicInteger columnIndex = new AtomicInteger();
	protected final AtomicLong rowIndex = new AtomicLong();

	protected final List<BloomFilter<byte[]>> columnToBloomFilter = new CopyOnWriteArrayList<>();

	protected final Map<List<Byte>, String> recurrentByteArrayToString = new ConcurrentHashMap<>();

	public ApexCSVParser(IApexCSVConfiguration apexCSVConfiguration, Charset charset, ByteBuffer wrap) {
		this.apexCSVConfiguration = apexCSVConfiguration;
		this.charset = charset;
		this.wrap = wrap;
	}

	public CharSequence parseNextAsCharSequence() {
		int currentColumnIndex = columnIndex.getAndIncrement();
		long currentRowIndex = rowIndex.get();

		final List<Byte> buffer = new ArrayList<>();

		final boolean insideQuotes;

		// http://stackoverflow.com/questions/5290182/how-many-bytes-does-one-unicode-character-take
		Set<Integer> bytesPerChar = new HashSet<>();

		AtomicInteger pendingBytes = new AtomicInteger();
		if (isDoubleQuotes(buffer, pendingBytes, bytesPerChar)) {
			insideQuotes = true;

			// Get ride of these bytes as they are just decoration
			buffer.clear();
		} else {
			insideQuotes = false;
		}

		boolean rightAfterNotEscapedQuote = false;

		List<Integer> escapedQuotesIndex = new ArrayList<>();

		while (true) {
			if (isEndOfFile()) {
				if (rightAfterNotEscapedQuote) {
					// This is an endOfLine after a closing quotes: get ride of the closing quotes
					return newCharSequence(currentRowIndex, currentColumnIndex, buffer, escapedQuotesIndex, "", "\"");
				} else {
					// The quotes are not closed: no big deal
					return newCharSequence(currentRowIndex, currentColumnIndex, buffer, escapedQuotesIndex, "", "");
				}
			} else if (isDoubleQuotes(buffer, pendingBytes, bytesPerChar)) {
				if (rightAfterNotEscapedQuote) {
					// We are escaping a double-quote by repeating it
					escapedQuotesIndex.add(buffer.size() - pendingBytes.get());

					// These quotes are escaped
					rightAfterNotEscapedQuote = false;
				} else {
					// This quote may be escaped, or it is end of current column, or end of line,or end of file
					rightAfterNotEscapedQuote = true;
				}
			} else if (!insideQuotes && isNewLineQuotes(buffer, pendingBytes, bytesPerChar)) {
				rowIndex.incrementAndGet();
				return newCharSequence(currentRowIndex, currentColumnIndex, buffer, escapedQuotesIndex, "", "\r");
			} else if (!insideQuotes && isNewColumn(buffer, pendingBytes, bytesPerChar)) {
				// We have closed current column
				return newCharSequence(currentRowIndex,
						currentColumnIndex,
						buffer,
						escapedQuotesIndex,
						"",
						apexCSVConfiguration.getColumnSeparator());
			} else {
				// wrap.mark();
				// wrap.
				// charset.newDecoder().decode(in, charBuffer, true);
				// wrap.reset();

				// Move bytes correspond to any char as current column
				if (pendingBytes.get() > 0) {
					pendingBytes.decrementAndGet();
				} else {
					// It is very strange we have not pre-loaded any bytes: read a byte and consider it as consumed
					// right-away
					buffer.add(wrap.get());
				}
			}
		}
	}

	static final int ONE_THOUSAND = 1000;
	static final int ONE_HUNDRED = 100;
	static final int TEN = 10;

	private CharSequence newCharSequence(long currentRowIndex,
			int currentColumnIndex,
			List<Byte> buffer,
			List<Integer> escapedQuotesIndex,
			String prefixToRemove,
			String suffixToRemove) {
		byte[] actualBytes = Bytes.toArray(buffer.subList(prefixToRemove.getBytes(charset).length,
				buffer.size() - suffixToRemove.getBytes(charset).length));

		if (columnToBloomFilter.size() <= currentColumnIndex) {
			while (columnToBloomFilter.size() <= currentColumnIndex) {
				columnToBloomFilter.add(BloomFilter.create(Funnels.byteArrayFunnel(), ONE_THOUSAND));
			}
		}

		BloomFilter<byte[]> bloomFilter = columnToBloomFilter.get(currentColumnIndex);
		bloomFilter.put(actualBytes);

		// Consider a String is recurrent only after the 100th row
		if (currentRowIndex >= ONE_HUNDRED && bloomFilter.approximateElementCount() * TEN < currentRowIndex) {
			// Each value seems to appear quite often

			return recurrentByteArrayToString.computeIfAbsent(Bytes.asList(actualBytes),
					ba -> new CharSequenceOverBytes(charset, actualBytes, escapedQuotesIndex).toString());
		} else {
			return new CharSequenceOverBytes(charset, actualBytes, escapedQuotesIndex);
		}
	}

	public boolean isEndOfFile() {
		return !wrap.hasRemaining();
	}

	private boolean isNewColumn(List<Byte> buffer, AtomicInteger pendingBytes, Set<Integer> bytesPerChar) {
		return nextAre(buffer, pendingBytes, apexCSVConfiguration.getColumnSeparator(), bytesPerChar);
	}

	private boolean isNewLineQuotes(List<Byte> buffer, AtomicInteger pendingBytes, Set<Integer> bytesPerChar) {
		// TODO: consumer \r\n, and not only \r
		return nextAre(buffer, pendingBytes, "\r", bytesPerChar) || nextAre(buffer, pendingBytes, "\n", bytesPerChar);
	}

	private boolean isDoubleQuotes(List<Byte> buffer, AtomicInteger pendingBytes, Set<Integer> bytesPerChar) {
		return nextAre(buffer, pendingBytes, "\"", bytesPerChar);
	}

	private boolean nextAre(List<Byte> buffer,
			AtomicInteger pendingBytes,
			String searchedString,
			Set<Integer> bytesPerChar) {
		byte[] bytes = searchedString.getBytes(charset);

		bytesPerChar.add(bytes.length);

		for (int i = 0; i < Math.min(bytes.length, pendingBytes.get()); i++) {
			if (bytes[i] != buffer.get(buffer.size() - i - pendingBytes.get()).byteValue()) {
				// Not the expected chars
				return false;
			}
		}

		for (int i = Math.min(bytes.length, pendingBytes.get()); i < bytes.length; i++) {
			if (!wrap.hasRemaining()) {
				return false;
			}
			byte nextByte = wrap.get();

			// Right now, this byte as added as pending for next chars
			pendingBytes.incrementAndGet();

			buffer.add(nextByte);
			if (bytes[i] != nextByte) {
				// Not the expected chars
				return false;
			}
		}

		// Consumed bytes for given char
		pendingBytes.addAndGet(-bytes.length);
		return true;
	}

	protected void closeFile(Collection<Byte> buffer, List<Integer> escapedQuotesIndex) {
		// Dangerous as may be called because actually sending the latest columns
	}

	public void registerOnNewLine(Runnable onNewLine) {
		onNewLine.run();
	}

	public boolean isEmpty() {
		return !wrap.hasRemaining();
	}

	public Object parseNextAsInteger() {
		return TypeFormat.parseInt(parseNextAsCharSequence());
	}

	public void forEachValue(Consumer<CharSequence> valueConsumer) {
		while (!isEndOfFile()) {
			valueConsumer.accept(parseNextAsCharSequence());
		}
	}

}
