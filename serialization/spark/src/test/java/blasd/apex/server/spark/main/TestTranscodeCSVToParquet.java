package blasd.apex.server.spark.main;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.hadoop.ApexHadoopHelper;
import blasd.apex.spark.run.RunCsvToParquet;

/**
 * Split Parquet files from HDFS and transcode columns in a Spark job
 * 
 * @author Benoit Lacelle
 */
public final class TestTranscodeCSVToParquet {

	protected static final Logger LOGGER = LoggerFactory.getLogger(TestTranscodeCSVToParquet.class);

	@Before
	public void assumeHaddopIsReady() {
		Assume.assumeTrue(ApexHadoopHelper.isHadoopReady());
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

		RunCsvToParquet.csvToParquet(csvPath, tmpParquetPath);

		// TODO: it is unclear if delete on exit will delete the folder recursively
		csvPath.toFile().delete();
		FileUtils.deleteDirectory(tmpParquetPath.toFile());
	}

}