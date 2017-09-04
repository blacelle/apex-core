package blasd.apex.core.primitive;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.primitive.UnsafeSubSequence;

public class TestUnsafeSubSequence {
	@Test
	public void testShiftedSubSequence() {
		UnsafeSubSequence sub = new UnsafeSubSequence("abcde", 1, 4);

		Assert.assertEquals("bcd", sub.toString());
		Assert.assertEquals("c", sub.subSequence(1, 2).toString());
	}
}
