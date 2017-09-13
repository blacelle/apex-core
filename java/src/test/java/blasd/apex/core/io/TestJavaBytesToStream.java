package blasd.apex.core.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestJavaBytesToStream {
	@Test
	public void test() throws IOException {
		JavaBytesToStream streamConverter = new JavaBytesToStream();

		ArrayList<? extends Object> original = Lists.newArrayList("A", 123);
		Stream<Object> stream =
				streamConverter.stream(new ByteArrayInputStream(ApexSerializationHelper.toBytes(original)));

		List<Object> streamedAsList = stream.collect(Collectors.toList());
		Assert.assertEquals(original, streamedAsList);
		Assert.assertNotSame(original, streamedAsList);
	}
}
