package blasd.apex.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public long writeInputStreamToDataOutput(InputStream inputStream, ObjectOutput objectOutput) throws IOException {
		return writeInputStreamToDataOutput(inputStream, objectOutput, IApexMemoryConstants.KB_INT);
	}

	/**
	 * Default chunk size is 1024
	 * 
	 * @param inputStream
	 *            the inputStream from which bytes are read. We consume it until EOF is encountered, however it is not
	 *            automatically closed
	 * @param objectOutput
	 *            the ObjectOutput were the data from the inputStream are written. These bytes are send through a stream
	 *            of ByteArrayMarker
	 * @param chunkSize
	 *            the size of the chunks in which the inputStream is fragmented
	 * @return the number of transmitted bytes, as reading from the InputStream
	 */
	public long writeInputStreamToDataOutput(InputStream inputStream, ObjectOutput objectOutput, int chunkSize)
			throws IOException {
		byte[] buffer = new byte[chunkSize];

		long actuallyWritten = 0;
		while (true) {
			// Read the target chunk
			int nbToCopy = ByteStreams.read(inputStream, buffer, 0, buffer.length);

			if (nbToCopy == 0) {
				// ByteStreams.read succeed copying only 0 bytes: the stream is consumed
				break;
			} else {
				// We may have read less than chunkSize bytes when the stream is consumed
				actuallyWritten += nbToCopy;
			}

			// The flow expected only objects
			objectOutput.writeObject(new ByteArrayMarker(nbToCopy));

			objectOutput.write(buffer, 0, nbToCopy);
		}

		return actuallyWritten;
	}

	public ObjectInput wrapToHandleInputStream(ObjectInput objectInput) {
		return new ObjectInputHandlingInputStream(objectInput);
	}
}
