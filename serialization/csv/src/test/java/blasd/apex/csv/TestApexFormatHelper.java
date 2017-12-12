package blasd.apex.csv;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.CharMatcher;

public class TestApexFormatHelper {
	@Test
	public void testGuessSeparator_empty() {
		String row = "";

		Assert.assertFalse(CsvFormatHelper.guessSeparator(row).isPresent());
	}

	@Test
	public void testGuessSeparator_onlyWord() {
		String row = "field1field2field3";

		Assert.assertFalse(CsvFormatHelper.guessSeparator(row).isPresent());
	}

	@Test
	public void testGuessSeparator_Coma() {
		String row = "field1,field2,field3";

		Assert.assertEquals(',', CsvFormatHelper.guessSeparator(row).getAsInt());
	}

	@Test
	public void testGuessSeparator_Tabulation() {
		String row = "field1\tfield2\tfield3";

		Assert.assertEquals('\t', CsvFormatHelper.guessSeparator(row).getAsInt());
	}

	@Test
	public void testGuessSeparator_CustomWordChare() {
		String row = "youApiAerf";

		Assert.assertEquals('A', CsvFormatHelper.guessSeparator(CharMatcher.anyOf("abAB"), row).getAsInt());
	}
}
