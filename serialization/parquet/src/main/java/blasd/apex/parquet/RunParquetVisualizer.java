package blasd.apex.parquet;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunParquetVisualizer {

	protected static final Logger LOGGER = LoggerFactory.getLogger(RunParquetVisualizer.class);

	public static void main(String[] args) throws IOException {
		if (args == null || args.length < 1) {
			throw new IllegalArgumentException("We expect at least one argument being the path top the parquet file");
		}

		String pathAsString = args[0];
		Path path = Paths.get(pathAsString);

		if (!path.toFile().isFile()) {
			throw new IllegalArgumentException(path + " is not a file");
		}

		AtomicLong rowIndex = new AtomicLong();
		new ParquetBytesToStream().stream(path).forEach(row -> {
			LOGGER.info("row #{}: {}", rowIndex.getAndIncrement(), row);
		});
	}
}
