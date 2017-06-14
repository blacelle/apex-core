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

import java.io.Serializable;

/**
 * Enable to mark an ObjectOutputStream than what is following is a byte[]. This way, it would be streamed, instead of
 * materialize in memory
 * 
 * @author Benoit Lacelle
 *
 */
public class ByteArrayMarker implements Serializable {
	private static final long serialVersionUID = -3032117473402808084L;

	protected final long nbBytes;

	public ByteArrayMarker(long nbBytes) {
		this.nbBytes = nbBytes;
	}

	public long getNbBytes() {
		return nbBytes;
	}
}
