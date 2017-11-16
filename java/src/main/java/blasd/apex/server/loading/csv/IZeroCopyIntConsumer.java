package blasd.apex.server.loading.csv;

import java.util.function.IntConsumer;

public interface IZeroCopyIntConsumer extends IZeroCopyConsumer, IntConsumer {
	long nextValueRowIndex();
}
