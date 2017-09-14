package io.cormoran.buffer;

import java.io.IOException;
import java.nio.IntBuffer;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestApexBufferHelper {
	@Before
	public void resetConstants() {
		ApexBufferHelper.forceNoSpaceDisk = false;
		ApexBufferHelper.forceNoHeap = false;
	}

	@Test
	public void testBuffer_small() throws IOException {
		int nbInts = 123;
		try (CloseableIntBuffer buffer = ApexBufferHelper.makeIntBuffer(nbInts)) {

			IntBuffer intBuffer = buffer.asIntBuffer();
			Assertions.assertThat(intBuffer.getClass().getSimpleName()).contains("Direct");

			// By default, we are filled with 0
			Assert.assertEquals(0, intBuffer.get(0));
			Assert.assertEquals(0, intBuffer.get(nbInts - 1));
		}
	}

	@Test
	public void testBuffer_noDiskButHeap() throws IOException {
		ApexBufferHelper.forceNoSpaceDisk = true;

		try (CloseableIntBuffer buffer = ApexBufferHelper.makeIntBuffer(123)) {
			// By default, we are filled with 0
			IntBuffer intBuffer = buffer.asIntBuffer();

			Assertions.assertThat(intBuffer.getClass().getSimpleName()).contains("Heap");

			Assert.assertEquals(0, intBuffer.get(0));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testBuffer_noDiskNoHeap() throws IOException {
		ApexBufferHelper.forceNoSpaceDisk = true;
		ApexBufferHelper.forceNoHeap = true;

		ApexBufferHelper.makeIntBuffer(123);
	}
}
