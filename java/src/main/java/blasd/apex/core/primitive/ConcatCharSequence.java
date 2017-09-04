package blasd.apex.core.primitive;

/**
 * @author Benoit Lacelle
 *
 */
public class ConcatCharSequence implements CharSequence {
	protected final CharSequence left;
	protected final CharSequence right;

	public ConcatCharSequence(CharSequence left, CharSequence right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public int length() {
		return left.length() + right.length();
	}

	@Override
	public char charAt(int index) {
		if (index >= left.length()) {
			return right.charAt(index - left.length());
		} else {
			return left.charAt(index);
		}
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		if (end < left.length()) {
			return left.subSequence(start, end);
		} else if (start >= left.length()) {
			return right.subSequence(start - left.length(), end - left.length());
		} else {
			return new ConcatCharSequence(left.subSequence(start, left.length()),
					right.subSequence(0, end - left.length()));
		}
	}

	@Override
	public String toString() {
		return left.toString() + right.toString();
	}
}
