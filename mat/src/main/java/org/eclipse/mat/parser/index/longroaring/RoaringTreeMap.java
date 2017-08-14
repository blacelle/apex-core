package org.eclipse.mat.parser.index.longroaring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.roaringbitmap.IntIterator;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongArrays;

//@Beta
public class RoaringTreeMap {
	// protected final Int2ObjectSortedMap<MutableRoaringBitmap> hiToBitmap = new Int2ObjectAVLTreeMap<>();
	protected final Int2ObjectSortedMap<MutableRoaringBitmap> hiToBitmap = new Int2ObjectRBTreeMap<>();

	// Prevent recomputing all cardinalities when requesting consecutive ranks
	protected boolean allValid = false;
	protected int firstHiNotValid = Integer.MIN_VALUE;

	protected final LongArrayList sortedCumulatedCardinality = new LongArrayList();
	protected final IntArrayList sortedHighs = new IntArrayList();
	protected final List<MutableRoaringBitmap> linkedBitmaps = new ArrayList<>();

	// Prevent indirection when writing consecutive Integers
	private transient Int2ObjectMap.Entry<MutableRoaringBitmap> latest = null;

	// https://stackoverflow.com/questions/12772939/java-storing-two-ints-in-a-long
	public void addLong(long id) {
		// long l = (((long)x) << 32) | (y & 0xffffffffL);
		int x = (int) (id >> 32);
		int y = (int) id;

		Map.Entry<Integer, MutableRoaringBitmap> local = latest;
		if (local != null && local.getKey().intValue() == x) {
			local.getValue().add(y);
		} else {
			MutableRoaringBitmap bitmap = hiToBitmap.computeIfAbsent(x, k -> new MutableRoaringBitmap());
			bitmap.add(y);
			latest = new AbstractInt2ObjectMap.BasicEntry<MutableRoaringBitmap>(x, bitmap);
		}

		// The cardinalities after this bucket may not be valid anymore
		firstHiNotValid = Math.min(firstHiNotValid, x);
	}

	private long pack(int x, int y) {
		return (((long) x) << 32) | (y & 0xffffffffL);
	}

	// Should be return long?
	public long getCardinality() {
		if (hiToBitmap.isEmpty()) {
			return 0L;
		}

		ensureCumulatives(Integer.MAX_VALUE);

		return sortedCumulatedCardinality.getLong(sortedCumulatedCardinality.size() - 1);
	}

	public long select(final long j) {
		ensureCumulatives(Integer.MAX_VALUE);

		int position =
				LongArrays.binarySearch(sortedCumulatedCardinality.elements(), 0, sortedCumulatedCardinality.size(), j);

		if (position >= 0) {
			// There is a bucket leading to this cardinality: the j-th element is the first element of next bucket
			MutableRoaringBitmap nextBitmap = linkedBitmaps.get(position + 1);
			return pack(sortedHighs.getInt(position + 1), nextBitmap.first());
		} else {
			// // see org.roaringbitmap.buffer.ImmutableRoaringBitmap.select(int)
			// throw new IllegalArgumentException("select " + j + " when the cardinality is " + this.getCardinality());
			// There is no bucket with this cardinality
			int insertionPoint = -position - 1;

			final long previousBucketCardinality;
			if (insertionPoint == 0) {
				previousBucketCardinality = 0L;
			} else {
				previousBucketCardinality = sortedCumulatedCardinality.getLong(insertionPoint - 1);
			}

			// We get a 'select' query for a single bitmap: should fit in an int
			final int givenBitmapSelect = (int) (j - previousBucketCardinality);

			MutableRoaringBitmap bitmaps = linkedBitmaps.get(insertionPoint);

			int low = bitmaps.select(givenBitmapSelect);

			int high = sortedHighs.getInt(insertionPoint);

			return pack(high, low);
		}
	}

	public LongIterator iterator() {
		Iterator<Int2ObjectMap.Entry<MutableRoaringBitmap>> it = hiToBitmap.int2ObjectEntrySet().iterator();

		return new LongIterator() {

			protected int currentKey;
			protected IntIterator currentIt;

			@Override
			public boolean hasNext() {
				if (currentIt == null) {
					// Were initially empty
					if (!moveToNextEntry(it)) {
						return false;
					}
				}

				while (true) {
					if (currentIt.hasNext()) {
						return true;
					} else {
						if (!moveToNextEntry(it)) {
							return false;
						}
					}
				}
			}

			/**
			 * 
			 * @param it
			 * @return true if we MAY have more entries. false if there is definitely nothing more
			 */
			private boolean moveToNextEntry(Iterator<Int2ObjectMap.Entry<MutableRoaringBitmap>> it) {
				if (it.hasNext()) {
					Int2ObjectMap.Entry<MutableRoaringBitmap> next = it.next();
					currentKey = next.getIntKey();
					currentIt = next.getValue().getIntIterator();

					// We may have more long
					return true;
				} else {
					// We know there is nothing more
					return false;
				}
			}

			@Override
			public long next() {
				if (hasNext()) {
					return pack(currentKey, currentIt.next());
				} else {
					throw new IllegalStateException("empty");
				}
			}

			@Override
			public LongIterator clone() {
				throw new UnsupportedOperationException("TODO");
			}
		};
	}

	public long rankLong(long id) {
		int x = (int) (id >> 32);
		int y = (int) id;

		ensureCumulatives(x);

		int bitmapPosition = IntArrays.binarySearch(sortedHighs.elements(), 0, sortedHighs.size(), x);

		if (bitmapPosition >= 0) {
			// There is a bucket holding this item

			final long previousBucketCardinality;
			if (bitmapPosition == 0) {
				previousBucketCardinality = 0;
			} else {
				previousBucketCardinality = sortedCumulatedCardinality.getLong(bitmapPosition - 1);
			}

			MutableRoaringBitmap bitmap = linkedBitmaps.get(bitmapPosition);

			// Rank is previous cardinality plus rank in current bitmap
			return previousBucketCardinality + bitmap.rankLong(y);
		} else {
			// There is no bucket holding this item: insertionPoint is previous bitmap
			int insertionPoint = -bitmapPosition - 1;

			if (insertionPoint == 0) {
				// this key is before all inserted keys
				return 0;
			} else {
				// The rank is the cardinality of this previous bitmap
				return sortedCumulatedCardinality.getLong(insertionPoint - 1);
			}
		}
	}

	protected void ensureCumulatives(int x) {
		// Check if missing data to handle this rank
		if (!allValid && firstHiNotValid <= x) {
			// For each deprecated buckets
			Int2ObjectSortedMap<MutableRoaringBitmap> tailMap = hiToBitmap.tailMap(firstHiNotValid);

			for (Entry<MutableRoaringBitmap> e : tailMap.int2ObjectEntrySet()) {
				int currentHigh = e.getIntKey();
				int index = IntArrays.binarySearch(sortedHighs.elements(), 0, sortedHighs.size(), currentHigh);

				if (index >= 0) {
					// This bitmap has already been registered
					MutableRoaringBitmap bitmap = e.getValue();
					assert bitmap == hiToBitmap.get(index);

					final long previousCardinality;
					if (currentHigh >= 1) {
						previousCardinality = sortedCumulatedCardinality.getLong(currentHigh - 1);
					} else {
						previousCardinality = 0;
					}
					sortedCumulatedCardinality.set(index, previousCardinality + bitmap.getCardinality());

					if (currentHigh == Integer.MAX_VALUE) {
						allValid = true;
						firstHiNotValid = currentHigh;
					} else {
						firstHiNotValid = currentHigh + 1;
					}
					if (e.getIntKey() > x) {
						// No need to compute more than needed
						break;
					}
				} else {
					int insertionPosition = -index - 1;

					// This is a new key
					sortedHighs.add(insertionPosition, currentHigh);
					linkedBitmaps.add(insertionPosition, e.getValue());

					final long previousCardinality;
					if (insertionPosition >= 1) {
						previousCardinality = sortedCumulatedCardinality.getLong(insertionPosition - 1);
					} else {
						previousCardinality = 0;
					}

					sortedCumulatedCardinality.add(insertionPosition,
							previousCardinality + e.getValue().getLongCardinality());

					if (currentHigh == Integer.MAX_VALUE) {
						allValid = true;
						firstHiNotValid = currentHigh;
					} else {
						firstHiNotValid = currentHigh + 1;
					}
				}
			}
		}
		
		if (x == Integer.MAX_VALUE) {
			allValid = true;
		}
	}
}
