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
package blasd.apex.shared.memory.histogram;

import java.nio.charset.Charset;

import com.google.common.base.Charsets;

/**
 * Provides heap histogram, including the number of instance per class and the memory associated to each instance
 * 
 * @author Benoit Lacelle
 *
 */
public interface IHeapHistogram {
	// TODO: Provide a source confirming this is a guarantee
	Charset JMAP_CHARSET = Charsets.UTF_8;

	long getTotalHeapBytes();

}
