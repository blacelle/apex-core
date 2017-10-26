package org.roaringbitmap;

import java.util.Arrays;

/**
 * Enable faster .rank and .select by caching the cardinality of underlying buckets
 * 
 * @author Benoit Lacelle
 *
 */
public class FastRankRoaringBitmap extends RoaringBitmap {
	private int[] highToCumulatedCardinality = null;

	@Override
	public void add(long rangeStart, long rangeEnd) {
		highToCumulatedCardinality = null;

		super.add(rangeStart, rangeEnd);
	}

	@Override
	public void add(int x) {
		highToCumulatedCardinality = null;

		super.add(x);
	}

	@Override
	public void add(int... dat) {
		highToCumulatedCardinality = null;

		super.add(dat);
	}

	@Override
	public long rankLong(int x) {
		if (highToCumulatedCardinality == null) {
			highToCumulatedCardinality = new int[highLowContainer.size()];

			if (highToCumulatedCardinality.length == 0) {
				return 0;
			}
			highToCumulatedCardinality[0] = highLowContainer.getContainerAtIndex(0).getCardinality();

			for (int i = 1; i < highToCumulatedCardinality.length; i++) {
				highToCumulatedCardinality[i] =
						highToCumulatedCardinality[i - 1] + highLowContainer.getContainerAtIndex(i).getCardinality();
			}
		}

		short xhigh = Util.highbits(x);

		int index = Util.hybridUnsignedBinarySearch(this.highLowContainer.keys, 0, this.highLowContainer.size(), xhigh);

		boolean hasBitmapOnIdex;
		if (index < 0) {
			hasBitmapOnIdex = false;
			index = -1 - index;
		} else {
			hasBitmapOnIdex = true;
		}

		long size = 0;
		if (index > 0) {
			size += highToCumulatedCardinality[index - 1];
		}

		long rank = size;
		if (hasBitmapOnIdex) {
			rank = size + this.highLowContainer.getContainerAtIndex(index).rank(Util.lowbits(x));
		}

		// assert rank == super.rankLong(x);

		return rank;
	}

	@Override
	public int select(int j) {
		if (highToCumulatedCardinality == null) {
			highToCumulatedCardinality = new int[highLowContainer.size()];

			if (highToCumulatedCardinality.length == 0) {
				return 0;
			}
			highToCumulatedCardinality[0] = highLowContainer.getContainerAtIndex(0).getCardinality();

			for (int i = 1; i < highToCumulatedCardinality.length; i++) {
				highToCumulatedCardinality[i] =
						highToCumulatedCardinality[i - 1] + highLowContainer.getContainerAtIndex(i).getCardinality();
			}
		}

		int index = Arrays.binarySearch(highToCumulatedCardinality, j);

		int fixedIndex;

		long leftover = Util.toUnsignedLong(j);

		if (index == highToCumulatedCardinality.length - 1) {
			return this.last();
		} else if (index >= 0) {
			int keycontrib = this.highLowContainer.getKeyAtIndex(index + 1) << 16;

			// If first bucket has cardinality 1 and we select 1: we actual select the first item of second bucket
			int output = keycontrib + this.highLowContainer.getContainerAtIndex(index + 1).first();

			// assert output == super.select(j);

			return output;
		} else {
			fixedIndex = -1 - index;
			if (fixedIndex > 0) {
				leftover -= highToCumulatedCardinality[fixedIndex - 1];
			}
		}

		int keycontrib = this.highLowContainer.getKeyAtIndex(fixedIndex) << 16;
		int lowcontrib =
				Util.toIntUnsigned(this.highLowContainer.getContainerAtIndex(fixedIndex).select((int) leftover));
		int value = lowcontrib + keycontrib;

		// assert value == super.select(j);

		return value;

		// throw new IllegalArgumentException("select " + j + " when the cardinality is " + this.getCardinality());
	}
}
