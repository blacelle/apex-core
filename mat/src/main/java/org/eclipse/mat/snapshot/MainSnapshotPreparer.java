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
package org.eclipse.mat.snapshot;

import java.io.File;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.util.ConsoleProgressListener;

/**
 * Typically used to parse a heap-dump. While trying to diminish the heap of mat: -XX:+HeapDumpOnOutOfMemoryError
 * -XX:HeapDumpPath=/disk2/dumps
 * 
 * @author Benoit Lacelle
 *
 */
public class MainSnapshotPreparer {
	public static void main(String[] args) throws SnapshotException {
		// SnapshotFactory.openSnapshot(
		// new File("D:\\blacelle112212\\HeapDUmp\\20170811 Grommet Equity\\crosstie.77831.hprof"),
		// new ConsoleProgressListener(System.out));

		SnapshotFactory.openSnapshot(new File("C:\\NB5419\\HeapDumps\\java_pid21052.hprof"),
				new ConsoleProgressListener(System.out));
	}
}
