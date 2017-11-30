package blasd.apex.csv;

import java.util.function.Consumer;

/**
 * Interface for String column consumer for {@link ZeroCopyCSVParser}
 * 
 * @author Benoit Lacelle
 *
 */
public interface IZeroCopyStringConsumer extends IZeroCopyConsumer, Consumer<String> {

}
