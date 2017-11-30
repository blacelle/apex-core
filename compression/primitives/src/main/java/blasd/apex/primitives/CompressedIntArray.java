package blasd.apex.primitives;

import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.roaringbitmap.RoaringBitmap;

import it.unimi.dsi.fastutil.ints.AbstractIntList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class CompressedIntArray extends AbstractIntList implements RandomAccess, Cloneable, java.io.Serializable {
	private static final long serialVersionUID = 2801769711578854510L;

	public CompressedIntArray(int constantMask, byte[] values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getInt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	public static IntList compress(IntStream input) {
		AtomicBoolean firstDone = new AtomicBoolean();

		// int constantBits = 0;
		// assert 0 == Integer.bitCount(constantBits);

		// AtomicInteger constantBitCandidates = new AtomicInteger(-1);
		// assert Integer.SIZE == Integer.bitCount(constantBitCandidates.get());

		// AtomicInteger constantBitsValues = new AtomicInteger(0);

		AtomicInteger nbDifferentConstant = new AtomicInteger();
		int[] nbConstant = new int[Integer.SIZE];
		int[] constantBits = new int[Integer.SIZE];
		int[] constantMasks = new int[Integer.SIZE];

		RoaringBitmap bits = new RoaringBitmap();

		// THis will be used when it appears there is not a single constant bit
		AtomicReference<IntList> uncompressedTrail = new AtomicReference<>();

		AtomicInteger index = new AtomicInteger();
		input.forEach(i -> {
			if (firstDone.compareAndSet(false, true)) {
				// This is the first int
				// constantBitCandidates.set(-1);
				// constantBitsValues.set(i);

				nbDifferentConstant.set(0);
				nbConstant[0] = 1;
				constantBits[0] = -1;
				constantMasks[0] = i;
			} else {
				if (uncompressedTrail.get() != null) {
					// We are not compressing anymore
					uncompressedTrail.get().add(i);
					return;
				}

				int currentConstantBits = constantBits[nbDifferentConstant.get()];
				int currentMask = constantMasks[nbDifferentConstant.get()];

				int differences = (i & currentConstantBits) ^ currentMask;
				if (differences != 0) {
					// The new value is not fitting the mask

					int differentBitCount = Integer.bitCount(differences);
					nbDifferentConstant.addAndGet(differentBitCount);

					if (nbDifferentConstant.get() == Integer.SIZE) {
						// There is not a single constant bit: stop compression
						uncompressedTrail.set(new IntArrayList());

						// We are not compressing anymore
						uncompressedTrail.get().add(i);
						return;
					} else {
						int newConstantBits = currentConstantBits ^ differences;
						int newMask = i & newConstantBits;

						constantBits[nbDifferentConstant.get()] = newConstantBits;
						constantMasks[nbDifferentConstant.get()] = newMask;

						currentConstantBits = newConstantBits;
						currentMask = newMask;
					}
				}

				nbConstant[nbDifferentConstant.get()]++;

				// Constant bits remain stable
				int bitsToWrite = ~currentConstantBits;

				for (int bitIndex = 0; bitIndex < Integer.SIZE; bitIndex++) {
					int isBitToWrite = bitsToWrite & Integer.rotateLeft(1, bitIndex);
					if (isBitToWrite != 0) {
						int newPosition = index.getAndIncrement();
						if ((isBitToWrite & i) != 0) {
							bits.add(newPosition);
						}
					}
				}
			}
		});

		RunningCompressedIntArray compressed =
				new RunningCompressedIntArray(nbConstant, constantBits, constantMasks, bits);

		if (uncompressedTrail.get() == null) {
			return compressed;
		} else {
			return new ConcatIntList(compressed, uncompressedTrail.get());
		}
	}
}
