package blasd.apex.core.jvm;

/**
 * Helpers related to math and numbers
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexMathHelper {

	protected ApexMathHelper() {
		// hidden
	}

	/**
	 * This method is useful to understand the precision available through floats
	 * 
	 * @param input
	 *            any Float
	 * @return the smallest float strictly bigger the the input
	 */
	public static float nextFloat(float input) {
		if (Float.isInfinite(input)) {
			// Infinite + finite value remains infinite
			return input;
		} else {
			// https://stackoverflow.com/questions/3658174/how-to-alter-a-float-by-its-smallest-increment-in-java
			return Float.intBitsToFloat(Float.floatToIntBits(input) + 1);
		}
	}
}
