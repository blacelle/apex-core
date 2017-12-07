package blasd.apex.server.spark.main;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.hadoop.ApexHadoopHelper;
import blasd.apex.parquet.ParquetStreamFactory;

/**
 * Split Parquet files from HDFS and transcode columns in a Spark job
 * 
 * @author Benoit Lacelle
 */
public final class TestTranscodeCSVToParquet {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestTranscodeCSVToParquet.class);

	protected TestTranscodeCSVToParquet() {
		// hidden
	}

	@Before
	public void assumeHaddopIsReady() {
		Assume.assumeTrue(ApexHadoopHelper.isHadoopReady());
	}

	public static void main(String[] args) throws Exception {
		if (!ApexHadoopHelper.isHadoopReady()) {
			throw new IllegalStateException("Hadoop is not ready");
		}

		Path tmpPath = ApexFileHelper.createTempPath("TestTranscodeCSVToParquet", ".csv", true);

		Path tmpParquetPath = ApexFileHelper.createTempPath("TestTranscodeCSVToParquet", ".parquet", true);

		try (BufferedWriter writer = Files.newWriter(tmpPath.toFile(), StandardCharsets.UTF_8)) {
			writer.write("A|2");
			writer.newLine();

			writer.write("B|2");
			writer.newLine();
		}

		csvToParquet(tmpPath, tmpParquetPath);
	}

	@Test
	public void testCSVToParquet() throws IOException {
		Path csvPath = ApexFileHelper.createTempPath("TestTranscodeCSVToParquet", ".csv", true);

		Path tmpParquetPath = ApexFileHelper.createTempPath("TestTranscodeCSVToParquet", ".parquet", true);
		// Ensure the file does not exist, else Spark fails writing into it
		tmpParquetPath.toFile().delete();

		try (BufferedWriter writer = Files.newWriter(csvPath.toFile(), StandardCharsets.UTF_8)) {
			writer.write("A|2");
			writer.newLine();

			writer.write("B|2");
			writer.newLine();
		}

		csvToParquet(csvPath, tmpParquetPath);

		// TODO: it is unclear if delete on exit will delete the folder recursively
		csvPath.toFile().delete();
		FileUtils.deleteDirectory(tmpParquetPath.toFile());
	}

	public static void csvToParquet(Path csvPath, Path parquetTargetPath) throws FileNotFoundException, IOException {
		LOGGER.info("About to convert {} into folder {}", csvPath, parquetTargetPath);

		if (parquetTargetPath.toFile().isFile()) {
			throw new IllegalArgumentException(
					"Can not write parquet files in folder which is already a file: " + parquetTargetPath);
		}

		// http://stackoverflow.com/questions/38008330/spark-error-a-master-url-must-be-set-in-your-configuration-when-submitting-a
		// https://jaceklaskowski.gitbooks.io/mastering-apache-spark/spark-local.html
		try (SparkSession spark =
				SparkSession.builder().appName("CsvToParquet").config("spark.master", "local[*]").getOrCreate()) {

			try (JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext())) {
				// http://bytepadding.com/big-data/spark/read-write-parquet-files-using-spark/
				SQLContext sqlContext = spark.sqlContext();
				Dataset<Row> inputDf = sqlContext.read().csv(csvPath.toAbsolutePath().toString());

				inputDf.write().parquet(parquetTargetPath.toAbsolutePath().toString());
			}
		}

		Arrays.stream(
				parquetTargetPath.toFile().listFiles(file -> file.isFile() && file.getName().endsWith(".parquet")))
				.forEach(file -> {
					LOGGER.info("Parquet file: {}", file);

					try {
						ParquetStreamFactory.readParquetAsStream(file.toPath(), Collections.emptyMap()).forEach(row -> {
							LOGGER.info("Row: {}", row);
						});
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
	}

}