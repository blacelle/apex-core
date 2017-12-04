package blasd.apex.serialization.avro;

import org.apache.avro.generic.GenericRecord;

import blasd.apex.core.io.IBinaryToStream;

/**
 * Specialization of {@link IBinaryToStream} for Avro {@link GenericRecord}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IBinaryToAvroStream extends IBinaryToStream<GenericRecord> {

}
