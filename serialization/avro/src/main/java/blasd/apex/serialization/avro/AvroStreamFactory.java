package blasd.apex.serialization.avro;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;

/**
 * A {@link IAvroStreamFactory} producing Avro binary data
 * 
 * @author Benoit Lacelle
 *
 */
public class AvroStreamFactory implements IAvroStreamFactory {

	@Override
	public Stream<GenericRecord> toStream(Path javaPath) throws IOException {
		return new AvroBytesToStream().stream(javaPath.toUri().toURL().openStream());
	}

	@Override
	public long writeToPath(Path javaPath, Stream<? extends GenericRecord> rowsToWrite) throws IOException {
		// https://avro.apache.org/docs/1.8.1/gettingstartedjava.html
		// We will use the first record to prepare a writer on the correct schema
		AtomicReference<DataFileWriter<GenericRecord>> writer = new AtomicReference<>();

		AtomicLong nbRows = new AtomicLong();
		rowsToWrite.forEach(m -> {

			if (nbRows.get() == 0) {

				try {
					Schema schema = m.getSchema();
					DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<GenericRecord>(schema);
					DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
					writer.set(dataFileWriter);
					dataFileWriter.create(schema, javaPath.toFile());
				} catch (NullPointerException e) {
					throw new IllegalStateException("Are you missing Hadoop binaries?", e);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}

			try {
				writer.get().append(m);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			nbRows.incrementAndGet();
		});

		if (writer.get() != null) {
			writer.get().close();
		}

		return nbRows.get();
	}

}
