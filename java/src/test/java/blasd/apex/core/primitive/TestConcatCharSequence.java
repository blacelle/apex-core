package blasd.apex.core.primitive;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.primitive.ConcatCharSequence;

public class TestConcatCharSequence {
	@Test
	public void testConcatCharSequence_subSequence() {
		Assert.assertEquals("a", new ConcatCharSequence("ab", "cd").subSequence(0, 1).toString());
		Assert.assertEquals("d", new ConcatCharSequence("ab", "cd").subSequence(3, 4).toString());
		Assert.assertEquals("abcd", new ConcatCharSequence("ab", "cd").subSequence(0, 4).toString());
		Assert.assertEquals("bc", new ConcatCharSequence("ab", "cd").subSequence(1, 3).toString());
	}

	@Test
	public void testConcatCharSequence_charIt() {
		Assert.assertEquals('a', new ConcatCharSequence("ab", "cd").charAt(0));
		Assert.assertEquals('b', new ConcatCharSequence("ab", "cd").charAt(1));
		Assert.assertEquals('c', new ConcatCharSequence("ab", "cd").charAt(2));
		Assert.assertEquals('d', new ConcatCharSequence("ab", "cd").charAt(3));
	}
}
