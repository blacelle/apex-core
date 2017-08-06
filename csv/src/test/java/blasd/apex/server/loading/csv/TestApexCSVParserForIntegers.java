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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Charsets;

import blasd.apex.core.csv.ApexCSVConfiguration;
import blasd.apex.core.csv.ApexCSVParser;
import blasd.apex.core.csv.ApexCSVParserFactory;

@Ignore
public class TestApexCSVParserForIntegers {
	ApexCSVParserFactory parserFactory = new ApexCSVParserFactory(ApexCSVConfiguration.getDefaultConfiguration());

	@Test
	public void testSimpleStirng() {
		check("123;456\r7;89", Arrays.asList(123, 456, 7, 89));
	}

	protected ApexCSVParser getParser(String string) {
		ByteBuffer wrap = ByteBuffer.wrap(string.getBytes(Charsets.UTF_8));

		return parserFactory.parserCharSequence(wrap);
	}

	protected void check(String string, List<Integer> asList) {
		ApexCSVParser parser = getParser(string);

		// AtomicLong wordCount = new AtomicLong();
		asList.forEach(s -> {
			parser.parseNextAsInteger().equals(s.intValue());
			// wordCount.incrementAndGet();
		});

		Assert.assertTrue(parser.isEmpty());
		// Assert.assertEquals(, wordCount);
	}
}
