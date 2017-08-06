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

import java.nio.charset.Charset;
import java.util.List;

/**
 * Holds a CharSequence behind an array of bytes and location of quotes
 * 
 * @author Benoit Lacelle
 *
 */
public class CharSequenceOverBytes implements CharSequence {

	protected final Charset charset;
	protected final byte[] buffer;
	protected final List<Integer> escapedQuotesIndex;

	public CharSequenceOverBytes(Charset charset, byte[] buffer, List<Integer> escapedQuotesIndex) {
		this.charset = charset;
		this.buffer = buffer;
		this.escapedQuotesIndex = escapedQuotesIndex;
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return this.toString().subSequence(start, end);
		// return new CharSequenceOverBytes(buffer.subList(start * 2, end * 2));
	}

	@Override
	public int length() {
		return new String(buffer, charset).length() - escapedQuotesIndex.size();
	}

	@Override
	public char charAt(int index) {
		// CharBuffer cb = CharBuffer.allocate(10);
		// cb.clear();
		byte[] array = buffer;
		// ByteBuffer in = ByteBuffer.wrap(array);
		// in.remaining();
		// cb.remaining();
		//
		// CoderResult cr = charset.newDecoder().decode(in, cb, false);
		//
		// while (cr.isOverflow()) {
		// cb = CharBuffer.allocate(2 * cb.capacity());
		// cb.clear();
		// cr = charset.newDecoder().decode(in, cb, true);
		// }
		// return cb.get();
		return new String(array, charset).charAt(index);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(this.length());
		sb.append(this);
		return sb.toString();
	}

}
