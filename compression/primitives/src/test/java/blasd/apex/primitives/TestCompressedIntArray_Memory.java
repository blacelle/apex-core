package blasd.apex.primitives;

import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntList;

public class TestCompressedIntArray_Memory {
	@Test
	public void testEmpty() {
		IntList array = CompressedIntArray.compress(IntStream.empty());

		Assert.assertTrue(array.isEmpty());
	}

	@Test
	public void testOnly0() {
		IntList array = CompressedIntArray.compress(IntStream.of(0));

		Assert.assertEquals(1, array.size());
		Assert.assertArrayEquals(new int[] { 0 }, array.toIntArray());
	}

	@Test
	public void testOnly1() {
		IntList array = CompressedIntArray.compress(IntStream.of(1));

		Assert.assertEquals(1, array.size());
		Assert.assertArrayEquals(new int[] { 1 }, array.toIntArray());
	}

	@Test
	public void testZeroAndZero() {
		IntList array = CompressedIntArray.compress(IntStream.of(0, 0));

		Assert.assertEquals(2, array.size());
		Assert.assertArrayEquals(new int[] { 0, 0 }, array.toIntArray());
	}

	@Test
	public void testZeroAndOne() {
		IntList array = CompressedIntArray.compress(IntStream.of(0, 1));

		Assert.assertEquals(2, array.size());
		Assert.assertArrayEquals(new int[] { 0, 1 }, array.toIntArray());
	}

	@Test
	public void testOneAndZero() {
		IntList array = CompressedIntArray.compress(IntStream.of(1, 0));

		Assert.assertEquals(2, array.size());
		Assert.assertArrayEquals(new int[] { 1, 0 }, array.toIntArray());
	}

	@Test
	public void testZeroAndTwo() {
		IntList array = CompressedIntArray.compress(IntStream.of(0, 2));

		Assert.assertEquals(2, array.size());
		Assert.assertArrayEquals(new int[] { 0, 2 }, array.toIntArray());
	}

	@Test
	public void testTwoAndZero() {
		IntList array = CompressedIntArray.compress(IntStream.of(2, 0));

		Assert.assertEquals(2, array.size());
		Assert.assertArrayEquals(new int[] { 2, 0 }, array.toIntArray());
	}

	@Test
	public void testOneAndOne() {
		IntList array = CompressedIntArray.compress(IntStream.of(1, 1));

		Assert.assertEquals(2, array.size());
		Assert.assertArrayEquals(new int[] { 1, 1 }, array.toIntArray());
	}

	@Test
	public void testZeroAndOneAndTwo() {
		IntList array = CompressedIntArray.compress(IntStream.of(0, 1, 2));

		Assert.assertEquals(3, array.size());
		Assert.assertArrayEquals(new int[] { 0, 1, 2 }, array.toIntArray());
	}

	@Test
	public void testTwoAndOneAndZero() {
		IntList array = CompressedIntArray.compress(IntStream.of(2, 1, 0));

		Assert.assertEquals(3, array.size());
		Assert.assertArrayEquals(new int[] { 2, 1, 0 }, array.toIntArray());
	}

	@Test
	public void testAllSingleBit() {
		IntList array =
				CompressedIntArray.compress(IntStream.range(0, Integer.SIZE).map(i -> Integer.rotateLeft(1, i)));

		Assert.assertEquals(32, array.size());
		Assert.assertEquals(1, array.getInt(0));
		Assert.assertEquals(Integer.MIN_VALUE, array.getInt(31));
	}

	@Test
	public void testAllSingleBit_WithOverflow() {
		IntList array =
				CompressedIntArray.compress(IntStream.range(0, Integer.SIZE + 1).map(i -> Integer.rotateLeft(1, i)));

		Assert.assertEquals(33, array.size());
		Assert.assertEquals(1, array.getInt(0));
		Assert.assertEquals(Integer.MIN_VALUE, array.getInt(31));
		Assert.assertEquals(1, array.getInt(32));
	}
}
