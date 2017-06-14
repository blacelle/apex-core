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
package blasd.apex.shared.util;

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
