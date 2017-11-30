package blasd.apex.primitives;

import java.util.Arrays;

import org.roaringbitmap.RoaringBitmap;

import com.google.common.primitives.Ints;

import it.unimi.dsi.fastutil.ints.AbstractIntList;

public class RunningCompressedIntArray extends AbstractIntList implements Cloneable, java.io.Serializable {
	private static final long serialVersionUID = 2801769711578854510L;

	protected final int[] nbConstant;
	protected final int[] constantBits;
	protected final int[] constantMasks;
	protected final RoaringBitmap bits;

	public RunningCompressedIntArray(int[] nbConstant, int[] constantBits, int[] constantMasks, RoaringBitmap bits) {
		this.nbConstant = nbConstant;
		this.constantBits = constantBits;
		this.constantMasks = constantMasks;
		this.bits = bits;
	}

	@Override
	public int getInt(int index) {
		int block = 0;
		int previousSize = 0;

		long bitShift = 0;

		int indexInBlock = index;
		while (block < Integer.SIZE) {
			int blockSize = nbConstant[block];
			if (index >= previousSize + blockSize) {
				previousSize += blockSize;
				indexInBlock -= blockSize;

				bitShift += (blockSize * 1L) * block;

				block++;
			} else {
				break;
			}
		}

		if (nbConstant[block] == 0) {
			throw new ArrayIndexOutOfBoundsException("index=" + index + " while size=" + size());
		}
		
		bitShift += indexInBlock * block;

		// Initialize the value given input mask
		int value = constantMasks[block];

		// Which bits have to be set out of the mask
		int differentBits = ~constantBits[block];

		for (int bitIndex = 0; bitIndex < Integer.SIZE; bitIndex++) {
			if ((differentBits & Integer.rotateLeft(1, bitIndex)) != 0) {

				if (bits.contains(Ints.checkedCast(bitShift))) {
					value |= Integer.rotateLeft(1, bitIndex);
				}

				bitShift++;
			}
		}

		return value;
	}

	@Override
	public int size() {
		return Arrays.stream(nbConstant).sum();
	}

}
