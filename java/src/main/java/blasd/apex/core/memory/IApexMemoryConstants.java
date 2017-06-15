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
package blasd.apex.core.memory;

import com.google.common.primitives.Ints;

/**
 * Provides some constants relatively to memory footprint in a JVM
 * 
 * @author Benoit Lacelle
 *
 */
public interface IApexMemoryConstants {

	long KB = 1024L;
	// Useful when allocating an array where an int is expected
	int KB_INT = Ints.saturatedCast(KB);

	/** One megabyte */
	long MB = KB * KB;
	int MB_INT = Ints.saturatedCast(MB);

	/** One gigabyte */
	long GB = KB * KB * KB;
	/** One terabyte */
	long TB = KB * KB * KB * KB;

	long CHAR = 4;
	long INT = 4;
	long LONG = 8;
	long OBJECT = 8;

	long FLOAT = 4;
	long DOUBLE = 8;
}
