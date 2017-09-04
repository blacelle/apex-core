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
package io.cormoran.buffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

import com.google.common.annotations.Beta;

/**
 * Helpers related to Buffers. TYpically enable quick and easy allocating of a ByteBuffer over a blank memory mapped
 * file
 * 
 * @author Benoit Lacelle
 *
 */
@Beta
public class ApexBufferHelper {

	public static IntBuffer makeIntBuffer(int size) throws IOException {
		File tmpFile = prepareIntArrayInFile(".IntArray1NWriter", size);

		FileChannel fc = new RandomAccessFile(tmpFile, "rw").getChannel();

		return fc.map(FileChannel.MapMode.READ_WRITE, 0, fc.size()).asIntBuffer();
	}

	private static File prepareIntArrayInFile(String suffix, int size) throws IOException {
		File tmpFile = File.createTempFile("mat", suffix);
		// We do not need the file to survive the JVM as the goal is just to spare heap
		tmpFile.deleteOnExit();

		// https://stackoverflow.com/questions/27570052/allocate-big-file
		try (RandomAccessFile out = new RandomAccessFile(tmpFile, "rw")) {
			out.setLength(1L * Integer.BYTES * size);
		}
		return tmpFile;
	}

}
