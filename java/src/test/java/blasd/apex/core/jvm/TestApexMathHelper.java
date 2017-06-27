package blasd.apex.core.jvm;

import org.junit.Assert;
import org.junit.Test;

public class TestApexMathHelper {
	@Test
	public void testNextFloat() {
		Assert.assertEquals(1F, ApexMathHelper.nextFloat(1F), 0.000001F);
	}

	@Test
	public void testNextFloat_Nan() {
		Assert.assertTrue(Float.isNaN(ApexMathHelper.nextFloat(Float.NaN)));
	}

	@Test
	public void testNextFloat_Infinite() {
		float greaterThanInfinity = ApexMathHelper.nextFloat(Float.POSITIVE_INFINITY);
		Assert.assertTrue(Float.isInfinite(greaterThanInfinity));
		Assert.assertTrue(greaterThanInfinity > 0);
	}

	@Test
	public void testNextFloat_NegativeInfinite() {
		float greaterThanInfinity = ApexMathHelper.nextFloat(Float.NEGATIVE_INFINITY);
		Assert.assertTrue(Float.isInfinite(greaterThanInfinity));
		Assert.assertTrue(greaterThanInfinity < 0);
	}
}
