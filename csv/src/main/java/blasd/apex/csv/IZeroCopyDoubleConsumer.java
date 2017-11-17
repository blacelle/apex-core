package blasd.apex.csv;

import java.util.function.DoubleConsumer;

/**
 * Interface for double column consumer for {@link ZeroCopyCSVParser}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IZeroCopyDoubleConsumer extends IZeroCopyConsumer, DoubleConsumer {
	long nextValueRowIndex();
}
