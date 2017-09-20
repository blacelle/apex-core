package io.cormoran.buffer;

import java.lang.reflect.InvocationTargetException;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;

@Beta
public class CloseableIntBuffer implements AutoCloseable {
	protected static final Logger LOGGER = LoggerFactory.getLogger(CloseableIntBuffer.class);

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
			try {
				// sun.misc.Cleaner cleaner = ((DirectBuffer) buffer).cleaner();
				// cleaner.clean();
				Class<?> directBufferClass = Class.forName("sun.nio.ch.DirectBuffer");
				Object cleaner = directBufferClass.getMethod("cleaner").invoke(buffer);

				Class<?> cleanerClass = Class.forName("sun.misc.Cleaner");
				cleanerClass.getMethod("clean").invoke(cleaner);
			} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOGGER.trace("Ouch", e);
				// JDK9?
				return;
			}

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
