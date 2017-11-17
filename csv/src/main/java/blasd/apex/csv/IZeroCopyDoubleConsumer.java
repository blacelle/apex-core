package blasd.apex.csv;

import java.util.function.DoubleConsumer;

public interface IZeroCopyDoubleConsumer extends IZeroCopyConsumer, DoubleConsumer {
	long nextValueRowIndex();
}
