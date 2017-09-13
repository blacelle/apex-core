package blasd.apex.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

/**
 * Convert an {@link InputStream} to a {@link Stream} of objects
 * 
 * @author Benoit Lacelle
 */
public class JavaBytesToStream implements IApexInputStreamToStream<Object> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(JavaBytesToStream.class);

	@Override
	public Stream<Object> stream(InputStream inputStream) {
		try {
			Collection<?> asCollection =
					(Collection<?>) ApexSerializationHelper.fromBytes(ByteStreams.toByteArray(inputStream));
			return asCollection.stream().map(o -> o);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
