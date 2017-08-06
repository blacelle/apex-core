/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package blasd.apex.server.loading.csv;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Charsets;

import blasd.apex.core.csv.ApexCSVConfiguration;
import blasd.apex.core.csv.ApexCSVParser;
import blasd.apex.core.csv.ApexCSVParserFactory;

/**
 * Main class to parse a sample file
 * 
 * @author Benoit Lacelle
 *
 */
public class MainParseFile {
	protected MainParseFile() {
		// hidden
	}

	public static void main(String[] args) throws IOException {
		Path path = Paths.get("/Users/blasd/workspace/csv-parsers-comparison/src/main/resources/worldcitiespop.txt");

		try (FileChannel fc = FileChannel.open(path)) {
			MappedByteBuffer wrap = fc.map(MapMode.READ_ONLY, 0, path.toFile().length());
			ApexCSVParser parser = new ApexCSVParserFactory(ApexCSVConfiguration.getDefaultConfiguration())
					.parserCharSequence(Charsets.ISO_8859_1, wrap);

			parser.forEachValue(cs -> System.out.println(cs));
		}
	}
}
