package blasd.apex.csv;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.csv.ZeroCopyCSVParser;

public class TestZeroCopyCSVParser_StringArray {
	ZeroCopyCSVParser parser = new ZeroCopyCSVParser();

	@Test
	public void toListOfStringArray() {
		int smallProblemSize = 100;

		String smallProblem =
				TestZeroCopyCSVParserMemory.streamOfValues(smallProblemSize).mapToObj(i -> i + "\r\n").collect(
						Collectors.joining());
		List<String[]> asList =
				parser.parseAsStringArrays(new StringReader(smallProblem), ',').collect(Collectors.toList());

		Iterator<String> stream = TestZeroCopyCSVParserMemory.streamOfValues(smallProblemSize)
				.mapToObj(i -> String.valueOf(i))
				.iterator();
		for (int i = 0; i < smallProblemSize; i++) {
			String[] parsed = asList.get(i);
			String[] expected = new String[] { stream.next() };

			Assert.assertArrayEquals(expected, parsed);
		}
	}
}
