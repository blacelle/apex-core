package blasd.apex.core.memory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestApexReferenceHelper {

	@Before
	public void clear() {
		ApexReferenceHelper.clear();
	}

	@Test
	public void testNullRef() {
		ApexReferenceHelper.internalizeFields(null);
		ApexReferenceHelper.internalizeArray(null);
		ApexReferenceHelper.dictionarizeIterable(null);
	}

	@Test
	public void testDictionarisationOnFinal() {
		FinalField left = new FinalField("Youpi");
		FinalField right = new FinalField(new String("Youpi"));

		// Not same ref
		Assert.assertNotSame(left.oneString, right.oneString);

		ApexReferenceHelper.internalizeFields(left);
		ApexReferenceHelper.internalizeFields(right);

		Assert.assertSame(left.oneString, right.oneString);
	}

	@Test
	public void testDictionarisationOnNotFinal() {
		NotFinalField left = new NotFinalField("Youpi");
		NotFinalField right = new NotFinalField(new String("Youpi"));

		// Not same ref
		Assert.assertNotSame(left.oneString, right.oneString);

		ApexReferenceHelper.internalizeFields(left);
		ApexReferenceHelper.internalizeFields(right);

		Assert.assertSame(left.oneString, right.oneString);
	}

	@Test
	public void testDictionarisationOnNotFinal_high_cardinality() {
		for (int i = 0; i < 10000; i++) {
			NotFinalField left = new NotFinalField("Youpi" + i);

			ApexReferenceHelper.internalize(left);
		}

		// We need to ensure a high cardinality Field does not lead to a huge dictionary
		Assert.assertEquals(775,
				ApexReferenceHelper.DICTIONARY_FIELDS.get(NotFinalField.class.getDeclaredFields()[0]).size());
	}

	@Test
	public void testDictionarisationOnNotFinal_veryhigh_cardinality() {
		for (int i = 0; i < 100000; i++) {
			NotFinalField left = new NotFinalField("Youpi" + i);

			ApexReferenceHelper.internalize(left);
		}

		// We need to ensure a very-high cardinality Field leads to a removed dictionary
		Assert.assertNull(ApexReferenceHelper.DICTIONARY_FIELDS.get(NotFinalField.class.getDeclaredFields()[0]));
	}

	@Test
	public void testDictionarisationOnDerived() {
		DerivedClass left = new DerivedClass("Youpi");
		DerivedClass right = new DerivedClass(new String("Youpi"));

		// Not same ref
		Assert.assertNotSame(left.oneString, right.oneString);

		ApexReferenceHelper.internalizeFields(left);
		ApexReferenceHelper.internalizeFields(right);

		Assert.assertSame(left.oneString, right.oneString);
	}

	@Test
	public void testDictionarizeArray() {
		Object[] array = new Object[] { "Youpi", 123L, "_Youpi".substring(1) };

		Assert.assertNotSame(array[0], array[2]);

		ApexReferenceHelper.internalizeArray(array);

		Assert.assertSame(array[0], array[2]);
	}

	@Test
	public void testDictionarizeArray_high_cardinality() {
		for (int i = 0; i < 100000; i++) {
			Object[] array = new Object[] { "Youpi" + i };

			ApexReferenceHelper.internalizeArray(array);
		}

		// We need to ensure a very-high cardinality Class does not lead to a huge dictionary
		Assert.assertEquals(575, ApexReferenceHelper.DICTIONARY_ARRAY.get(String.class).size());
	}

	static class NotFinalField {
		public String oneString;

		public NotFinalField(String oneString) {
			this.oneString = oneString;
		}

	}

	static class FinalField {
		public String oneString;

		public FinalField(String oneString) {
			this.oneString = oneString;
		}

	}

	static class DerivedClass extends FinalField {

		public DerivedClass(String oneString) {
			super(oneString);
		}

	}
}
