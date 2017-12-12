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
package blasd.apex.parquet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.avro.generic.GenericRecord;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.serialization.avro.IBinaryToAvroStream;

/**
 * Automatic transformation of a Parquet InputStream to a Stream of objects. It requires to write the file first on the
 * file-system
 * 
 * @author Benoit Lacelle
 *
 */
public class ParquetBytesToStream implements IBinaryToAvroStream {

	protected final AtomicReference<Path> persisted = new AtomicReference<>();

	protected void persist(InputStream inputStream) throws IOException {
		if (persisted.get() != null) {
			throw new RuntimeException("Already persisted on " + persisted.get());
		}

		// TODO: We may not want to delete on exit in order to keep a local cache. But one way rather move to file to a
		// proper place
		boolean deleteOnExit = true;
		// Write the InputStream to FileSystem as Parquet expect a SeekableInputStream as metadata are at the end
		// https://github.com/apache/parquet-mr/blob/master/parquet-common/src/main/java/org/apache/parquet/io/SeekableInputStream.java
		Path tmp = ApexFileHelper.createTempPath(getClass().getSimpleName(), ".tmp", deleteOnExit);

		// Copy InputStream to tmp file
		Files.copy(inputStream, tmp, StandardCopyOption.REPLACE_EXISTING);

		persisted.set(tmp);
	}

	@Override
	public Stream<GenericRecord> stream(InputStream inputStream) throws IOException {
		persist(inputStream);

		return new ParquetStreamFactory().toStream(persisted.get());
	}

	public Stream<GenericRecord> stream(Path path) throws IOException {
		return new ParquetStreamFactory().toStream(path);
	}
}
