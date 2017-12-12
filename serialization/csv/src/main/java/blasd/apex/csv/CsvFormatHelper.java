package blasd.apex.csv;

import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalInt;

import com.google.common.base.CharMatcher;

/**
 * Some helpers for CSV files,typically to help inferring the CSV format
 * 
 * @author Benoit Lacelle
 *
 */
public class CsvFormatHelper {
	protected CsvFormatHelper() {
		// hidden
	}

	public static String defaultSeparatorCandidates() {
		return ";,|\t";
	}

	public static OptionalInt guessSeparator(String row) {
		return guessSeparator(CharMatcher.anyOf(defaultSeparatorCandidates()), row);
	}

	public static OptionalInt guessSeparator(CharMatcher allowedSeparators, String row) {
		if (row == null || row.isEmpty()) {
			return OptionalInt.empty();
		} else {
			// Search a separator amongst an hardcoded list of good candidates
			// mapToObj for custom comparator

			String candidateSeparators = allowedSeparators.retainFrom(row);

			Optional<Integer> optMax = candidateSeparators.chars().mapToObj(Integer::valueOf).max(
					Comparator.comparing(i -> CharMatcher.is((char) i.intValue()).countIn(row)));

			if (optMax.isPresent()) {
				int max = optMax.get().intValue();

				return OptionalInt.of(max);
			} else {
				return OptionalInt.empty();
			}
		}
	}
}
