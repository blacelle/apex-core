package blasd.apex.csv;

import java.util.function.IntConsumer;

public interface IZeroCopyIntConsumer extends IZeroCopyConsumer, IntConsumer {
	long nextValueRowIndex();
}
