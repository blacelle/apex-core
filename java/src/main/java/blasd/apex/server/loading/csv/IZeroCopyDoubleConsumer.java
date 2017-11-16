package blasd.apex.server.loading.csv;

import java.util.function.DoubleConsumer;

public interface IZeroCopyDoubleConsumer extends IZeroCopyConsumer, DoubleConsumer {
	long nextValueRowIndex();
}
