package blasd.apex.server.loading.csv;

import java.util.function.LongConsumer;

public interface IZeroCopyLongConsumer extends IZeroCopyConsumer, LongConsumer {
	long nextValueRowIndex();
}
