package blasd.apex.core.io;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;

import blasd.apex.core.thread.ApexExecutorsHelper;

public class ObjectInputHandlingInputStream implements ObjectInput {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ObjectInputHandlingInputStream.class);

	protected final ObjectInput decorated;

	// The main thread will process the objects: we need an async process to read bytes
	protected final ExecutorService inputStreamFiller;

	protected final AtomicBoolean pipedOutputStreamIsOpen = new AtomicBoolean(false);
	protected final AtomicReference<Exception> ouch = new AtomicReference<>();

	/**
	 * Build a ObjectInputHandlingInputStream with an asynchronous single-thread executor handling inputStream reading
	 * 
	 * @param decorated
	 */
	public ObjectInputHandlingInputStream(ObjectInput decorated) {
		this(decorated, ApexExecutorsHelper.newSingleThreadExecutor("ObjectInputHandling"));
	}

	public ObjectInputHandlingInputStream(ObjectInput decorated, ExecutorService inputStreamFiller) {
		this.decorated = decorated;
		this.inputStreamFiller = inputStreamFiller;
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

			// Wait for PipedOutputStream to be connected before returning the PipedInputStream
			CountDownLatch connectedCdl = new CountDownLatch(1);

			// DO not auto-close as this inputStream will be consumed out of this loop
			PipedInputStream pis = new PipedInputStream();

			// Connect a PipedOutputStream in which we will write the transmitted InputStream
			if (!pipedOutputStreamIsOpen.compareAndSet(false, true)) {
				throw new IllegalStateException("Pipe was already open");
			}

			inputStreamFiller.execute(() -> {
				// PipedInputStream.read will throw if not connected: PipedOutputStream should be connected before
				// leaving main thread
				try (PipedOutputStream pos = new PipedOutputStream(pis)) {
					// Indicate the pipe is connected
					connectedCdl.countDown();

					ByteArrayMarker nextByteMarker = (ByteArrayMarker) next;
					while (true) {
						byte[] bytes = new byte[Ints.checkedCast(nextByteMarker.getNbBytes())];

						// Read the expected number of bytes
						try {
							decorated.readFully(bytes);
						} catch (IOException e) {
							throw new RuntimeException(
									"Failure while retrieveing a chunk with nbBytes=" + nextByteMarker.getNbBytes(), e);
						}
						// Transfer these bytes in the pipe
						pos.write(bytes);

						if (nextByteMarker.getIsFinished()) {
							break;
						}

						Object localNext = decorated.readObject();

						if (localNext instanceof ByteArrayMarker) {
							// We received another chunk of bytes: push it in current InputStream
							nextByteMarker = (ByteArrayMarker) localNext;
						} else {
							throw new IllegalStateException(
									"We received ByteArrayMarker with isFinished=false while next object was a "
											+ localNext);
						}
					}
				} catch (IOException | ClassNotFoundException e) {
					if (!ouch.compareAndSet(null, e)) {
						throw new RuntimeException(
								"We encountered a new exception while previous one has not been reported", e);
					}
				} finally {
					pipedOutputStreamIsOpen.set(false);
				}
			});

			try {
				if (!connectedCdl.await(1, TimeUnit.MINUTES)) {
					pis.close();
					throw new RuntimeException("It took too long to connect the pipes");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException(e);
			}

			// return the PipedInputStream as it should be consumed externally
			// Beware no call to ObjectInput.read should be done before the PipedOutputStream is done
			return pis;
		} else {
			Exception pendingException = ouch.getAndSet(null);
			// The caller requests for nextObject, but it
			if (pendingException != null) {
				if (pendingException instanceof EOFException) {
					// The calling code may rely on Exception type for such case
					throw (EOFException) pendingException;
				} else if (pendingException instanceof IOException) {
					// TODO: Is there other special kind of IOException?
					throw new IOException(pendingException);
				} else {
					throw new RuntimeException(pendingException);
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
