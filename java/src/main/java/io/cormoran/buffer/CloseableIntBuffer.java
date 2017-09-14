package io.cormoran.buffer;

import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;

import com.google.common.annotations.Beta;

import sun.nio.ch.DirectBuffer;

@Beta
public class CloseableIntBuffer implements AutoCloseable {

	protected final MappedByteBuffer buffer;
	protected final IntBuffer heapBuffer;

	public CloseableIntBuffer(MappedByteBuffer buffer) {
		this.buffer = buffer;
		this.heapBuffer = null;
	}

	public CloseableIntBuffer(IntBuffer heapBuffer) {
		this.heapBuffer = heapBuffer;
		this.buffer = null;
	}

	// https://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
	// Beware if buffer is re http://bugs.java.com/view_bug.do?bug_id=4724038
	@Override
	public void close() {
		// We clean the hook to the mapped file, else even a shutdown-hook would not remove the mapped-file
		if (this.buffer != null) {
			sun.misc.Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
			cleaner.clean();
		}
	}

	public IntBuffer asIntBuffer() {
		if (heapBuffer != null) {
			return heapBuffer;
		} else {
			return buffer.asIntBuffer();
		}
	}

}
