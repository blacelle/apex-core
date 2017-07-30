package blasd.apex.core.eventbus;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.eventbus.EventBus;

public class TestApexEventBusHelper {

	@Test
	public void test_ctor_coverage() {
		Assert.assertNotNull(new ApexEventBusHelper());
	}

	@Test
	public void testAsConsumer() {
		EventBus eventBus = new EventBus();

		Optional<Consumer<Object>> asConsumer = ApexEventBusHelper.asConsumer(eventBus);

		Assert.assertTrue(asConsumer.isPresent());
	}

	@Test
	public void testAsConsumer_null() {
		Optional<Consumer<Object>> asConsumer = ApexEventBusHelper.asConsumer(null);

		Assert.assertFalse(asConsumer.isPresent());
	}
}
