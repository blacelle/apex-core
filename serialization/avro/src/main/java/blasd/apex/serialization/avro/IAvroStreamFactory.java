package blasd.apex.serialization.avro;

import java.io.IOException;
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

	Stream<? extends GenericRecord> toStream(java.nio.file.Path javaPath) throws IOException;

	Stream<? extends GenericRecord> toStream(org.apache.hadoop.fs.Path hadoopPath) throws IOException;

	long writeToPath(java.nio.file.Path javaPathOnDisk, Stream<? extends GenericRecord> rowsToWrite) throws IOException;
}
