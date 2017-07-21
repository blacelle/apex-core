package blasd.apex.core.io;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class TestApexURLHelper {
	@Test
	public void testEqualsURL() throws MalformedURLException {
		URL left = new URL("http://youpi.com");
		URL right = new URL("http://youpi.com");
		Assert.assertTrue(ApexURLHelper.equalsUrl(left, right));
	}

	@Test
	public void testToUrl() throws MalformedURLException {
		Assert.assertEquals("http://youpi.com", ApexURLHelper.toHttpURL("youpi.com").toExternalForm());
	}

	@Test
	public void testGetHost_lowerCase() throws MalformedURLException {
		ApexHostDescriptor host = ApexURLHelper.getHost("YOUpi.com").get();

		Assert.assertFalse(host.getIsIP());
		Assert.assertTrue(host.getIsValid());
		Assert.assertEquals("youpi.com", host.getHost());
	}

	@Test
	public void testGetHost_hashRightAfterHost() {
		// In some JDK version, "http://host.com#youpi" as parsed as URL "http://host.com##youpi". It seems
		// "http://host.com/#youpi" is parsed correctly

		ApexHostDescriptor host = ApexURLHelper.getHost("http://youpi.com#arf").get();

		Assert.assertFalse(host.getIsIP());
		Assert.assertTrue(host.getIsValid());
		Assert.assertEquals("youpi.com", host.getHost());
	}

	@Test
	public void testToUrl_mailto() {
		URL host = ApexURLHelper.toHttpURL("mailto:adresse@serveur.com");

		Assert.assertEquals("mailto:adresse@serveur.com", host.toExternalForm());
	}

	@Test
	public void testGetHost_mailto() {
		Assert.assertFalse(ApexURLHelper.getHost("mailto:adresse@serveur.com").isPresent());
	}
}
