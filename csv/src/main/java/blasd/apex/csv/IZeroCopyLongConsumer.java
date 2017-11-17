package blasd.apex.csv;

import java.util.function.LongConsumer;

/**
 * Interface for long column consumer for {@link ZeroCopyCSVParser}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IZeroCopyLongConsumer extends IZeroCopyConsumer, LongConsumer {
	long nextValueRowIndex();
}
