package blasd.apex.server.loading.csvsource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blasd.apex.csv.ZeroCopyCSVParser;

public class ITOnlineCSV {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ITOnlineCSV.class);

	public static void main(String[] args) throws MalformedURLException, IOException {
		// http://download.maxmind.com/download/worldcities/worldcitiespop.txt.gz
		Path path = Paths.get("/Users/blasd/workspace/csv-parsers-comparison/src/main/resources/worldcitiespop.txt");
		new ZeroCopyCSVParser().parse(new InputStreamReader(new GZIPInputStream(path.toUri().toURL().openStream())),
				',');
	}
}
