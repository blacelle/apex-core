package blasd.apex.core.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

import blasd.apex.core.thread.ApexExecutorsHelper;

public class ObjectInputHandlingInputStream implements ObjectInput {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ObjectInputHandlingInputStream.class);

	protected final ObjectInput decorated;
	protected final ExecutorService inputStreamFiller =
			ApexExecutorsHelper.newSingleThreadExecutor("ObjectInputHandling");

	protected final AtomicBoolean pipedOutputStreamIsOpen = new AtomicBoolean(false);

	private static final Object EOF_MARKER = new Object();
	protected Object nextNotByteMarker;

	public ObjectInputHandlingInputStream(ObjectInput decorated) {
		this.decorated = decorated;
	}

	@Override
	public Object readObject() throws ClassNotFoundException, IOException {
		if (pipedOutputStreamIsOpen.get()) {
			// TODO: should we rather block until the stream is consumed? This may lead to deadlocks
			throw new RuntimeException(
					"We can not read next object as previous were a ByteArrayMarker and not all of them have been read");
		}

		Object next = decorated.readObject();

		if (next instanceof ByteArrayMarker) {
			// We received an ByteArrayMarker: it has to be converted to an InputStream

			// DO not auto-close as this inputStream will be consumed out of this loop
			PipedInputStream pis = new PipedInputStream();

			// Connect a PipedOutputStream in which we will write the transmitted InputStream
			pipedOutputStreamIsOpen.set(true);
			PipedOutputStream pos = new PipedOutputStream(pis);
			AtomicReference<Exception> ouch = new AtomicReference<>();
			inputStreamFiller.execute(() -> {
				try {
					ByteArrayMarker nextByteMarker = (ByteArrayMarker) next;
					while (true) {
						byte[] bytes = new byte[Ints.checkedCast(nextByteMarker.getNbBytes())];

						// Read the expected number of bytes
						try {
							decorated.readFully(bytes);
						} catch (IOException e) {
							throw new RuntimeException(
									"Failure while retrieveing a chunk with nbBytes=" + nextByteMarker.getNbBytes(),
									e);
						}
						// Transfer these bytes in the pipe
						pos.write(bytes);

						try {
							Object localNext = decorated.readObject();

							if (localNext instanceof ByteArrayMarker) {
								// We received another chunk of bytes: push it in current InputStream
								nextByteMarker = (ByteArrayMarker) localNext;
							} else {
								// Next item is NOT a chunk for current InputStream: give it back as next action
								// in the stream of action
								nextNotByteMarker = localNext;
								break;
							}
						} catch (EOFException e) {
							// http://stackoverflow.com/questions/2626163/java-fileinputstream-objectinputstream-reaches-end-of-file-eof
							LOGGER.trace(
									"This EOF probably means there is no commands left, or the stream has been truncated",
									e);
							nextNotByteMarker = EOF_MARKER;
							break;
						}
					}
				} catch (IOException | ClassNotFoundException e) {
					ouch.set(e);
				} finally {
					try {
						pos.close();
					} catch (IOException e) {
						LOGGER.trace("Ouch", e);
					}
					pipedOutputStreamIsOpen.set(false);
				}
			});

			// return the PipedInputStream as it should be consumed externally
			// Beware no call to ObjectInput.read should be done before the PipedOutputStream is done
			return pis;
		} else {
			if (nextNotByteMarker != null) {
				nextNotByteMarker = null;

				if (nextNotByteMarker == EOF_MARKER) {
					// TODO: should we rethrow the EOFException?
					return null;
				} else {
					return nextNotByteMarker;
				}
			}

			// There is nothing to do over this object
			return next;
		}
	}

	@Override
	public void readFully(byte[] b) throws IOException {
		decorated.readFully(b);
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		decorated.readFully(b, off, len);
	}

	@Override
	public int skipBytes(int n) throws IOException {
		return decorated.skipBytes(n);
	}

	@Override
	public boolean readBoolean() throws IOException {
		return decorated.readBoolean();
	}

	@Override
	public byte readByte() throws IOException {
		return decorated.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException {
		return decorated.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException {
		return decorated.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException {
		return decorated.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException {
		return decorated.readChar();
	}

	@Override
	public int readInt() throws IOException {
		return decorated.readInt();
	}

	@Override
	public long readLong() throws IOException {
		return decorated.readLong();
	}

	@Override
	public float readFloat() throws IOException {
		return decorated.readFloat();
	}

	@Override
	public double readDouble() throws IOException {
		return decorated.readDouble();
	}

	@Override
	public String readLine() throws IOException {
		return decorated.readLine();
	}

	@Override
	public String readUTF() throws IOException {
		return decorated.readUTF();
	}

	@Override
	public int read() throws IOException {
		return decorated.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		return decorated.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return decorated.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		return decorated.skip(n);
	}

	@Override
	public int available() throws IOException {
		return decorated.available();
	}

	@Override
	public void close() throws IOException {
		decorated.close();
	}

}
