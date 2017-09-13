package org.roaringbitmap;

import java.util.Random;

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

//		assert rank == super.rankLong(x);

		return rank;
	}
}
