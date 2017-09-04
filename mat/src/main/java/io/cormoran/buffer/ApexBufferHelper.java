package io.cormoran.buffer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

public class ApexBufferHelper {

	public static IntBuffer makeIntBuffer(int size) throws IOException {
		if (true) {
			// TODO Check without File IntBuffer
			return IntBuffer.wrap(new int[size]);
		}

		File tmpFile = prepareIntArrayInFile(".IntArray1NWriter", size);

		FileChannel fc = new RandomAccessFile(tmpFile, "rw").getChannel();

		return fc.map(FileChannel.MapMode.READ_WRITE, 0, fc.size()).asIntBuffer();
	}

	private static File prepareIntArrayInFile(String suffix, int size) throws IOException {
		File tmpFile = File.createTempFile("mat", suffix);
		// We do not need the file to survive the JVM as the goal is just to spare heap
		tmpFile.deleteOnExit();

		// https://stackoverflow.com/questions/27570052/allocate-big-file
		try (RandomAccessFile out = new RandomAccessFile(tmpFile, "rw")) {
			out.setLength(1L * Integer.BYTES * size);
		}
		return tmpFile;
	}

}
