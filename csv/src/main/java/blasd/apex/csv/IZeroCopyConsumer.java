package blasd.apex.csv;

public interface IZeroCopyConsumer {
	/**
	 * Called-back by the parser when the column does not appear in the CSV (e.g. there is only 3 columns while this
	 * consumer is attached to the fourth column), or this is a primitive consumer and the column content is empty
	 */
	void nextRowIsMissing();

	/**
	 * Called-back when the parser encounter invalid chars given current column (e.g. chars not parseable as an Integer
	 * when this is an IntPredicate)
	 * 
	 * @param charSequence
	 *            the charSequence would can not be consumed
	 */
	void nextRowIsInvalid(CharSequence charSequence);
}
