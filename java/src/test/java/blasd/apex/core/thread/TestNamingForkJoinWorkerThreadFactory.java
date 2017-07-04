package blasd.apex.core.thread;

import java.util.concurrent.ForkJoinPool;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TestNamingForkJoinWorkerThreadFactory {
	@Test
	public void testNaming() {
		NamingForkJoinWorkerThreadFactory factory = new NamingForkJoinWorkerThreadFactory("customPrefix");

		Assertions.assertThat(factory.newThread(ForkJoinPool.commonPool()).getName()).startsWith("customPrefix");
	}
}
