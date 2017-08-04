package blasd.apex.core.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

public class TestTransmitInputStreamInObjectInput {

	@SuppressWarnings("resource")
	@Test
	public void testTransittingInputSteam_write_read_write_read() throws IOException, ClassNotFoundException {
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream(pis);

		byte[] bytesFrance = ApexSerializationHelper.toBytes(ImmutableMap.of("k1", "v1"));
		byte[] bytesUs = ApexSerializationHelper.toBytes(ImmutableMap.of("k2", "v2"));

		ObjectInputHandlingInputStream objectInput;
		try (ObjectOutputStream oos = new ObjectOutputStream(pos)) {
			// Write an InputStream
			ApexObjectStreamHelper.writeInputStream(oos, new ByteArrayInputStream(bytesFrance));

			// Ensure everything is submitted as we will read the pipe in the same thread
			oos.flush();

			objectInput = new ObjectInputHandlingInputStream(new ObjectInputStream(pis));
			Object nextToRead = objectInput.readObject();

			Assert.assertNotNull(nextToRead);
			Assert.assertTrue(nextToRead instanceof InputStream);
			InputStream readIS = (InputStream) nextToRead;

			// We have not close the ObjectOutputStream: the transmitter should remain open as next item could be
			// another ByteArrayMarker
			Assert.assertTrue(objectInput.pipedOutputStreamIsOpen.get());

			// Ensure we are retrieving the whole chunk
			byte[] transmitted = ByteStreams.toByteArray(readIS);
			Assert.assertArrayEquals(bytesFrance, transmitted);

			// We write a second block, but read it after closing ObjectOutputStream: the inputStream should remain
			// open
			ApexObjectStreamHelper.writeInputStream(oos, new ByteArrayInputStream(bytesUs));
		}

		// ObjectOutputStream is closed: pipedOutputStreamIsOpen should end being switched to false
		Awaitility.await().untilFalse(objectInput.pipedOutputStreamIsOpen);

		// Check reading after ObjectOutputStream is closed
		{
			Object nextToRead = objectInput.readObject();

			Assert.assertNotNull(nextToRead);
			Assert.assertTrue(nextToRead instanceof InputStream);
			InputStream readIS = (InputStream) nextToRead;

			byte[] transmitted = ByteStreams.toByteArray(readIS);
			Assert.assertArrayEquals(bytesUs, transmitted);

			// Check there is no more bytes
			Assert.assertEquals(-1, readIS.read());
		}

	}

	@Test
	public void testTransittingInputSteam_write_write_read_read() throws IOException, ClassNotFoundException {
		PipedInputStream pis = new PipedInputStream();
		PipedOutputStream pos = new PipedOutputStream(pis);

		byte[] bytesFrance = ApexSerializationHelper.toBytes(ImmutableMap.of("k1", "v1"));
		byte[] bytesUs = ApexSerializationHelper.toBytes(ImmutableMap.of("k2", "v2"));

		try (ObjectOutputStream oos = new ObjectOutputStream(pos)) {
			// Write consecuritvelly @ inputStreams
			ApexObjectStreamHelper.writeInputStream(oos, new ByteArrayInputStream(bytesFrance));
			ApexObjectStreamHelper.writeInputStream(oos, new ByteArrayInputStream(bytesUs));
		}

		try (ObjectInputHandlingInputStream objectInput =
				new ObjectInputHandlingInputStream(new ObjectInputStream(pis));) {
			{
				Object nextToRead = objectInput.readObject();

				Assert.assertNotNull(nextToRead);
				Assert.assertTrue(nextToRead instanceof InputStream);
				InputStream readIS = (InputStream) nextToRead;

				// We have not close the ObjectOutputStream: the transmitter should remain open as next item could be
				// another ByteArrayMarker
				Assert.assertTrue(objectInput.pipedOutputStreamIsOpen.get());

				// Ensure we are retrieving the whole chunk
				byte[] transmitted = ByteStreams.toByteArray(readIS);
				Assert.assertArrayEquals(bytesFrance, transmitted);
			}

			// Check reading after ObjectOutputStream is closed
			{
				Object nextToRead = objectInput.readObject();

				Assert.assertNotNull(nextToRead);
				Assert.assertTrue(nextToRead instanceof InputStream);
				InputStream readIS = (InputStream) nextToRead;

				byte[] transmitted = ByteStreams.toByteArray(readIS);
				Assert.assertArrayEquals(bytesUs, transmitted);

				// Check there is no more bytes
				Assert.assertEquals(-1, readIS.read());
			}
		}
	}
}
