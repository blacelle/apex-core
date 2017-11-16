package blasd.apex.server.loading.csvsource;

import java.util.function.LongConsumer;

import blasd.apex.server.loading.csv.IZeroCopyConsumer;

public interface IZeroCopyLongConsumer extends IZeroCopyConsumer, LongConsumer {

	long nextValueRowIndex();

}
