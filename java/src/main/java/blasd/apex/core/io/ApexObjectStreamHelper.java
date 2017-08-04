package blasd.apex.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;

import blasd.apex.core.memory.IApexMemoryConstants;

/**
 * Various helpers related to Object stream
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexObjectStreamHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexObjectStreamHelper.class);

	@VisibleForTesting
	static final int DEFAULT_CHUNK_SIZE = IApexMemoryConstants.KB_INT;

	protected ApexObjectStreamHelper() {
		// hidden
	}

	/**
	 * Default chunk size is 1024
	 * 
	 * @param inputStream
	 * @param objectOutput
	 * @return the number of transmitted bytes, as reading from the InputStream
	 * @throws IOException
	 */
	public static long writeInputStream(ObjectOutput objectOutput, InputStream inputStream) throws IOException {
		return writeInputStream(objectOutput, inputStream, DEFAULT_CHUNK_SIZE);
	}

	/**
	 * Default chunk size is 1024
	 * 
	 * @param objectOutput
	 *            the ObjectOutput were the data from the inputStream are written. These bytes are send through a stream
	 *            of ByteArrayMarker
	 * @param inputStream
	 *            the inputStream from which bytes are read. We consume it until EOF is encountered, however it is not
	 *            automatically closed
	 * @param chunkSize
	 *            the size of the chunks in which the inputStream is fragmented
	 * @return the number of transmitted bytes, as reading from the InputStream. It does NOT include the overhead
	 *         induced by the protocol (e.g. ByteArrayMarker own bytes)
	 */
	public static long writeInputStream(ObjectOutput objectOutput, InputStream inputStream, int chunkSize)
			throws IOException {
		byte[] buffer = new byte[chunkSize];

		long totalWritten = 0;
		while (true) {
			// Read the target chunk
			int nbToCopy = ByteStreams.read(inputStream, buffer, 0, buffer.length);

			if (nbToCopy == 0) {
				// ByteStreams.read succeed copying only 0 bytes: the stream is consumed
				break;
			} else {
				// We may have read less than chunkSize bytes when the stream is consumed
				totalWritten += nbToCopy;
			}

			// The flow expected only objects
			objectOutput.writeObject(new ByteArrayMarker(nbToCopy, false));

			objectOutput.write(buffer, 0, nbToCopy);

			LOGGER.trace("We have transmitted {} bytes", totalWritten);
		}

		// We need to indicate when there is no more data: instead of checking of the InputSTream has a next block, we
		// simply submit a trailing empty block
		objectOutput.writeObject(new ByteArrayMarker(0, true));
		objectOutput.write(buffer, 0, 0);

		return totalWritten;
	}

	/**
	 * 
	 * @param objectInput
	 * @return an ObjectInput which may return InputStream on .readObject
	 */
	public static ObjectInput wrapToHandleInputStream(ObjectInput objectInput) {
		return new ObjectInputHandlingInputStream(objectInput);
	}

}
