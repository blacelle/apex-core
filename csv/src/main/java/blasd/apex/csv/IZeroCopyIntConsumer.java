package blasd.apex.csv;

import java.util.function.IntConsumer;

/**
 * Interface for integer column consumer for {@link ZeroCopyCSVParser}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IZeroCopyIntConsumer extends IZeroCopyConsumer, IntConsumer {
	long nextValueRowIndex();
}
