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
package blasd.apex.core.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.primitives.Ints;

import blasd.apex.core.memory.IApexMemoryConstants;

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

	public static boolean isGZIPStream(byte[] bytes) {
		return bytes[0] == (byte) GZIPInputStream.GZIP_MAGIC
				&& bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >>> GZIP_MAGIC_SHIFT);
	}

	public static String toStringOptCompressed(byte[] bytes) throws IOException {
		if (isGZIPStream(bytes)) {
			return toStringCompressed(bytes);
		} else {
			return new String(bytes, StandardCharsets.UTF_8);
		}
	}

	public static String toStringCompressed(byte[] bytes) throws IOException {
		InputStreamReader isr =
				new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(bytes)), StandardCharsets.UTF_8);
		StringWriter sw = new StringWriter();
		char[] chars = new char[Ints.saturatedCast(IApexMemoryConstants.KB)];
		int len = -1;
		while (true) {
			len = isr.read(chars);
			if (len <= 0) {
				break;
			}
			sw.write(chars, 0, len);
		}
		return sw.toString();
	}

	public static byte[] compress(String html) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(new GZIPOutputStream(baos), StandardCharsets.UTF_8);

		osw.write(html);

		return baos.toByteArray();
	}

}
