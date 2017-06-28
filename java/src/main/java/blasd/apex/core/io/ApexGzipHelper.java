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
package blasd.apex.core.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.io.CharStreams;

/**
 * Various utilities for GZip
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexGzipHelper {
	private static final int GZIP_MAGIC_SHIFT = 8;

	protected ApexGzipHelper() {
		// hidden
	}

	/**
	 * 
	 * @param bytes
	 * @return true if this byte array seems to hold a gzip compressed message
	 */
	public static boolean isGZIPStream(byte[] bytes) {
		return bytes != null && bytes.length >= 2
				&& bytes[0] == (byte) GZIPInputStream.GZIP_MAGIC
				&& bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >>> GZIP_MAGIC_SHIFT);
	}

	/**
	 * This is able to handle byte arrays, would it hold the compressed String, or just plain data
	 * 
	 * @param bytes
	 * @return the deflated String
	 * @throws IOException
	 */
	public static String toStringOptCompressed(byte[] bytes) throws IOException {
		if (isGZIPStream(bytes)) {
			return toStringCompressed(bytes);
		} else {
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

	public static String toStringCompressed(byte[] bytes) throws IOException {
		return inflate(bytes);
	}

	/**
	 * @deprecated Prefer .deflate
	 */
	@Deprecated
	public static byte[] compress(String someString) throws IOException {
		return deflate(someString);
	}

	public static byte[] deflate(String someString) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try (OutputStreamWriter osw = new OutputStreamWriter(new GZIPOutputStream(baos), StandardCharsets.UTF_8)) {
			osw.write(someString);
		}

		return baos.toByteArray();
	}

	public static String inflate(byte[] inflated) throws IOException {
		GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(inflated));
		InputStreamReader osw = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8);

		return CharStreams.toString(osw);
	}

}
