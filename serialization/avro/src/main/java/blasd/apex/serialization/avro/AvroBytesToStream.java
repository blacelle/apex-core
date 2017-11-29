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
package blasd.apex.serialization.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helps converting an InputStream to a Stream of {@link GenericRecord}
 * 
 * @author Benoit Lacelle
 *
 */
public class AvroBytesToStream implements IBinaryToAvroStream {
	protected static final Logger LOGGER = LoggerFactory.getLogger(AvroBytesToStream.class);

	@Override
	public Stream<? extends GenericRecord> toStream(InputStream inputStream) throws IOException {
		SpecificDatumReader<GenericRecord> specificDatumReader = new SpecificDatumReader<GenericRecord>();
		DataFileStream<GenericRecord> dataFileStream = new DataFileStream<>(inputStream, specificDatumReader);

		return StreamSupport.stream(dataFileStream.spliterator(), false).onClose(() -> {
			try {
				dataFileStream.close();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}
}
