package blasd.apex.server.spark;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import blasd.apex.core.io.ApexFileHelper;
import blasd.apex.parquet.ParquetStreamFactory;
import blasd.apex.serialization.avro.AvroSchemaHelper;
import blasd.apex.serialization.avro.AvroStreamHelper;

/**
 * We demonstrate how to write a subset of rows from a parquet files
 * 
 * @author Benoit Lacelle
 *
 */
public class TestParquetWriteToFile {
	ParquetStreamFactory parquetStreamFactory = new ParquetStreamFactory();

	@Test(expected = IllegalArgumentException.class)
	public void testWriteParquet_FileExist() throws IOException {
		Stream<Map<String, Object>> rows = IntStream.range(0, 10)
				.mapToObj(i -> ImmutableMap.of("longField", (long) i, "stringField", "string_" + i));

		Schema avroSchema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("longField", 0L));

		Path tmpPath = ApexFileHelper.createTempPath("testWriteParquet_FromJavaStream", ".parquet", true);
		parquetStreamFactory.writeToPath(tmpPath, rows.map(AvroStreamHelper.toGenericRecord(avroSchema)));
	}

	@Test
	public void testWriteParquet_FromJavaStream() throws IOException {
		Stream<Map<String, Object>> rows = IntStream.range(0, 10)
				.mapToObj(i -> ImmutableMap.of("longField", (long) i, "stringField", "string_" + i));

		Schema avroSchema = AvroSchemaHelper.proposeSimpleSchema(ImmutableMap.of("longField", 0L));

		Path tmpPath = ApexFileHelper.createTempPath("testWriteParquet_FromJavaStream", ".parquet", true);
		long nbWritten =
				parquetStreamFactory.writeToPath(tmpPath, rows.map(AvroStreamHelper.toGenericRecord(avroSchema)));

		Assert.assertEquals(10, nbWritten);
	}
}
