package blasd.apex.csv;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;

/**
 * The interface for any ZeroCopyCSVParser
 * 
 * @author Benoit Lacelle
 *
 */
public interface IZeroCopyCSVParser {

	void parse(Reader reader, char separator, List<IZeroCopyConsumer> consumers) throws IOException;

	default public void parse(Reader reader, char separator, IZeroCopyConsumer... consumers) throws IOException {
		parse(reader, separator, Arrays.asList(consumers));
	}

}
