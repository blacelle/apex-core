package blasd.apex.serialization.avro;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.avro.generic.GenericRecord;

import com.google.common.annotations.Beta;

/**
 * 
 * @author Benoit Lacelle
 *
 */
@Beta
public interface IAvroStreamFactory {

	Stream<? extends GenericRecord> toStream(Path javaPath) throws IOException;

	long writeToPath(Path javaPath, Stream<? extends GenericRecord> rowsToWrite) throws IOException;
}
