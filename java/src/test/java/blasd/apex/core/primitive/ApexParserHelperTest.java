/*
 * Javolution - Java(TM) Solution for Real-Time and Embedded Systems
 * Copyright (C) 2012 - Javolution (http://javolution.org/)
 * All rights reserved.
 * 
 * Permission to use, copy, modify, and distribute this software is
 * freely granted, provided that this notice is preserved.
 */
package blasd.apex.core.primitive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

// https://github.com/javolution/javolution/blob/master/src/test/java/org/javolution/text/TypeFormatTest.java
public class ApexParserHelperTest {

	// @Test
	// public void parseBoolean() {
	// assertTrue("True Parsed", ApexParserHelper.parseBoolean("true"));
	// assertFalse("False Parsed", ApexParserHelper.parseBoolean("false"));
	// }
	//
	// @Test(expected = IllegalArgumentException.class)
	// public void parseBooleanThrowsExceptionIfStringDoesNotRepresentABoolean() {
	// ApexParserHelper.parseBoolean("aaaa");
	// }
	//
	// @Test
	// public void parseBooleanWithCursor() {
	// Cursor cursor = new Cursor();
	// cursor.setIndex(4);
	// assertTrue("True Parsed", ApexParserHelper.parseBoolean("aaaatrue", cursor));
	// cursor.setIndex(4);
	// assertFalse("False Parsed", ApexParserHelper.parseBoolean("aaaafalse", cursor));
	// }

	@Test
	public void parseDouble() {
		assertEquals("1.0 Parsed", 1.0, ApexParserHelper.parseDouble("1.0"), 0);
	}

	// @Test(expected = IllegalArgumentException.class)
	@Test(expected = StringIndexOutOfBoundsException.class)
	public void parseDoubleThrowsExceptionIfStringDoesNotRepresentADouble() {
		ApexParserHelper.parseDouble("aaaa");
	}

	// @Test
	// public void parseDoubleWithCursor() {
	// Cursor cursor = new Cursor();
	// cursor.setIndex(4);
	// assertEquals("1.0 Parsed", 1.0, ApexParserHelper.parseDouble("aaaa1.0", cursor), 0);
	// }

	@Test
	public void parseDoubleWithExplicitPositiveNumber() {
		assertEquals("+15 Parsed", 15.0, ApexParserHelper.parseDouble("+15.0"), 0);
	}

	@Test
	public void parseDoubleWithNegativeNumber() {
		assertEquals("-15 Parsed", -15.0, ApexParserHelper.parseDouble("-15.0"), 0);
	}

	@Test
	public void parseFloat() {
		assertEquals("1.0 Parsed", 1.0f, ApexParserHelper.parseFloat("1.0"), 0);
	}

	// @Test(expected = IllegalArgumentException.class)
	@Test(expected = StringIndexOutOfBoundsException.class)
	public void parseFloatThrowsExceptionIfStringDoesNotRepresentAFloat() {
		ApexParserHelper.parseFloat("aaaa");
	}

	// @Test
	// public void parseFloatWithCursor() {
	// Cursor cursor = new Cursor();
	// cursor.setIndex(4);
	// assertEquals("1.0 Parsed", 1.0f, ApexParserHelper.parseFloat("aaaa1.0", cursor), 0);
	// }

	@Test
	public void parseFloatWithExplicitPositiveNumber() {
		assertEquals("+15 Parsed", 15.0f, ApexParserHelper.parseFloat("+15.0"), 0);
	}

	@Test
	public void parseFloatWithNegativeNumber() {
		assertEquals("-15 Parsed", -15.0f, ApexParserHelper.parseFloat("-15.0"), 0);
	}

	// @Test
	// public void parseInt(){
	// assertEquals("1 Parsed", 1, ApexParserHelper.parseInt("1"));
	// }
	//
	// @Test(expected=IllegalArgumentException.class)
	// public void parseIntThrowsExceptionIfStringDoesNotRepresentAnInt(){
	// ApexParserHelper.parseInt("aaaa");
	// }
	//
	// @Test
	// public void parseIntWithBinaryRadix(){
	// assertEquals("0b1001 Parsed", 9, ApexParserHelper.parseInt("1001",2));
	// }
	//
	// @Test
	// public void parseIntWithCursor(){
	// Cursor cursor = new Cursor();
	// cursor.setIndex(4);
	// assertEquals("1 Parsed", 1, ApexParserHelper.parseInt("aaaa1", cursor));
	// }
	//
	// @Test
	// public void parseIntWithExplicitPositiveNumber(){
	// assertEquals("+15 Parsed", 15, ApexParserHelper.parseInt("+15"));
	// }
	//
	// @Test
	// public void parseIntWithHexRadix(){
	// assertEquals("0xAAAA Parsed", 0xAAAA, ApexParserHelper.parseInt("aaaa",16));
	// }
	//
	// @Test
	// public void parseIntWithNegativeNumber(){
	// assertEquals("-15 Parsed", -15, ApexParserHelper.parseInt("-15"));
	// }
	//
	// @Test
	// public void parseLong(){
	// assertEquals("1 Parsed", 1L, ApexParserHelper.parseLong("1"));
	// }
	//
	// @Test(expected=IllegalArgumentException.class)
	// public void parseLongThrowsExceptionIfStringDoesNotRepresentALong(){
	// ApexParserHelper.parseLong("aaaa");
	// }
	//
	// @Test
	// public void parseLongWithBinaryRadix(){
	// assertEquals("0b1001 Parsed", 9L, ApexParserHelper.parseLong("1001",2));
	// }
	//
	// @Test
	// public void parseLongWithCursor(){
	// Cursor cursor = new Cursor();
	// cursor.setIndex(4);
	// assertEquals("1 Parsed", 1L, ApexParserHelper.parseLong("aaaa1", cursor));
	// }
	//
	// @Test
	// public void parseLongWithExplicitPositiveNumber(){
	// assertEquals("+15 Parsed", 15L, ApexParserHelper.parseLong("+15"));
	// }
	//
	// @Test
	// public void parseLongWithHexRadix(){
	// assertEquals("0xAAAA Parsed", 0xAAAAL, ApexParserHelper.parseLong("aaaa",16));
	// }
	//
	// @Test
	// public void parseLongWithNegativeNumber(){
	// assertEquals("-15 Parsed", -15L, ApexParserHelper.parseLong("-15"));
	// }
	//
	// @Test
	// public void parseShort(){
	// assertEquals("1 Parsed", (short)1, ApexParserHelper.parseShort("1"));
	// }
	//
	// @Test(expected=IllegalArgumentException.class)
	// public void parseShortThrowsExceptionIfStringDoesNotRepresentAnShort(){
	// ApexParserHelper.parseShort("aaaa");
	// }
	//
	// @Test
	// public void parseShortWithBinaryRadix(){
	// assertEquals("0b1001 Parsed", 9, ApexParserHelper.parseShort("1001",2));
	// }
	//
	// @Test
	// public void parseShortWithCursor(){
	// Cursor cursor = new Cursor();
	// cursor.setIndex(4);
	// assertEquals("1 Parsed", (short)1, ApexParserHelper.parseShort("aaaa1", cursor));
	// }
	//
	// @Test
	// public void parseShortWithExplicitPositiveNumber(){
	// assertEquals("+15 Parsed", (short)15, ApexParserHelper.parseShort("+15"));
	// }
	//
	// @Test
	// public void parseShortWithHexRadix(){
	// assertEquals("0xAA Parsed", 0xAA, ApexParserHelper.parseShort("aa",16));
	// }
	//
	// @Test
	// public void parseShortWithNegativeNumber(){
	// assertEquals("-15 Parsed", (short)-15, ApexParserHelper.parseShort("-15"));
	// }
	//
	// @Test(expected=NumberFormatException.class)
	// public void parseTestShortMaxOverflow(){
	// ApexParserHelper.parseShort(String.valueOf(Integer.MAX_VALUE));
	// }
	//
	// @Test(expected=NumberFormatException.class)
	// public void parseTestShortMinOverflow(){
	// ApexParserHelper.parseShort(String.valueOf(Integer.MIN_VALUE));
	// }

}