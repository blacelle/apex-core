package blasd.apex.core.io;

import java.io.InputStream;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Abstract the logic converting a stream of bytes (i.e. an {@link InputStream}) to a Stream of objects
 * 
 * @author Benoit Lacelle
 *
 */
public interface IApexInputStreamToStream<T> extends Function<InputStream, Stream<T>> {
	Stream<T> stream(InputStream inputStream);

	@Override
	default Stream<T> apply(InputStream inputStream) {
		return stream(inputStream);
	}
}
