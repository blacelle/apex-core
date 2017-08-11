package org.eclipse.mat.parser.index.longroaring;

import java.util.Iterator;
import java.util.Map;

import org.roaringbitmap.IntIterator;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;

//@Beta
public class RoaringTreeMap {
	protected final Int2ObjectSortedMap<MutableRoaringBitmap> hiToBitmap = new Int2ObjectAVLTreeMap<>();
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
	}

	private long pack(int x, int y) {
		return (((long) x) << 32) | (y & 0xffffffffL);
	}

	// Should be return long?
	public int getCardinality() {
		// TODO Does IntStream.sum prevent overflow?
		return hiToBitmap.values().stream().mapToInt(b -> b.getCardinality()).sum();
	}

	public long select(final int j) {
		Iterator<Int2ObjectMap.Entry<MutableRoaringBitmap>> it = hiToBitmap.int2ObjectEntrySet().iterator();

		int indexLeft = j;

		while (it.hasNext()) {
			Int2ObjectMap.Entry<MutableRoaringBitmap> entry = it.next();
			MutableRoaringBitmap bitmap = entry.getValue();

			int cardinality = bitmap.getCardinality();
			if (cardinality > j) {
				indexLeft -= cardinality;
			} else {
				return pack(entry.getIntKey(), bitmap.select(indexLeft));
			}
		}

		// see org.roaringbitmap.buffer.ImmutableRoaringBitmap.select(int)
		throw new IllegalArgumentException("select " + j + " when the cardinality is " + this.getCardinality());
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
				return pack(currentKey, currentIt.next());
			}

			@Override
			public LongIterator clone() {
				throw new UnsupportedOperationException("TODO");
			}
		};
	}

	public int rankLong(long id) {
		int x = (int) (id >> 32);

		int rank = 0;
		for (Int2ObjectMap.Entry<MutableRoaringBitmap> e : hiToBitmap.int2ObjectEntrySet()) {
			if (e.getIntKey() < x) {
				rank += e.getValue().getCardinality();
			} else if (e.getIntKey() == x) {
				int y = (int) id;
				rank += e.getValue().rank(y);
			} else {
				assert e.getIntKey() > x;
				break;
			}
		}

		return rank;
	}
}
