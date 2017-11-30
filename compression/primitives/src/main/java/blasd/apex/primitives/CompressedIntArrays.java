package blasd.apex.primitives;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.roaringbitmap.RoaringBitmap;

import com.google.common.annotations.Beta;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * Provide helps to compressed int arrays
 * 
 * @author Benoit Lacelle
 *
 */
@Beta
public class CompressedIntArrays {

	protected CompressedIntArrays() {
		// hidden
	}

	public static IntList compress(IntStream input) {
		AtomicBoolean firstDone = new AtomicBoolean();

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
