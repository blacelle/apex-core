package blasd.apex.core.lambda;

import java.io.IOException;
import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.io.ApexSerializationHelper;
import blasd.apex.core.lamda.NoOpRunnable;

public class TestNoOpRunnable {
	@Test
	public void testRunNoOp() throws IOException {
		NoOpRunnable noOpRunnable = new NoOpRunnable();
		noOpRunnable.run();

		Assert.assertTrue(noOpRunnable instanceof Serializable);

		Assert.assertEquals("rO0ABXNyACJibGFzZC5hcGV4LmNvcmUubGFtZGEuTm9PcFJ1bm5hYmxlyv5gVq2UgTgCAAB4cA==",
				ApexSerializationHelper.toString(noOpRunnable));
	}
}
