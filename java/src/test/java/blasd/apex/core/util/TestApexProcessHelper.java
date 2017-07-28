package blasd.apex.core.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import blasd.apex.core.agent.ApexAgentHelper;
import blasd.apex.core.memory.IApexMemoryConstants;

public class TestApexProcessHelper {
	@Test
	public void testMemoryOnMac() throws IOException {
		String macMemoryOutput = Arrays
				.asList("mapped file 32.9M 10.7M 32K 0K 0K 0K 0K 139",
						"shared memory 44K 44K 44K 0K 0K 0K 0K 6",
						"=========== ======= ======== ===== ======= ======== ====== ===== =======",
						"TOTAL 2.2G 538.2M 377.3M 0K 0K 16K 0K 845",
						"TOTAL, minus reserved VM space 2.2G 538.2M 377.3M 0K 0K 16K 0K 845",
						"",
						"VIRTUAL RESIDENT DIRTY SWAPPED ALLOCATION BYTES DIRTY+SWAP REGION",
						"MALLOC ZONE SIZE SIZE SIZE SIZE COUNT ALLOCATED FRAG SIZE % FRAG COUNT",
						"=========== ======= ========= ========= ========= ========= ========= ========= ======",
						"DefaultMallocZone_0x10b7b6000 203.0M 148.4M 87.4M 0K 167902 64.5M 22.9M 27% 19",
						"GFXMallocZone_0x10b7e7000 0K 0K 0K 0K 0 0K 0K 0% 0",
						"=========== ======= ========= ========= ========= ========= ========= ========= ======",
						"TOTAL 203.0M 148.4M 87.4M 0K 167902 64.5M 22.9M 27% 19")
				.stream()
				.collect(Collectors.joining("\r"));

		long nbBytes = ApexProcessHelper.extractMemory(ApexProcessHelper.OS_MARKER_MAC,
				new ByteArrayInputStream(macMemoryOutput.getBytes()));
		Assert.assertEquals((long) (538.2D * IApexMemoryConstants.MB), nbBytes);
	}

	@Test
	public void testMemory_mac_multiplespaces() throws IOException {
		String macMemoryOutput =
				"TOTAL                                1.5G   113.3M    7208K      52K       0K      20K       0K      485 ";

		long nbBytes = ApexProcessHelper.extractMemory(ApexProcessHelper.OS_MARKER_MAC,
				new ByteArrayInputStream(macMemoryOutput.getBytes()));
		Assert.assertEquals((long) (113.3D * IApexMemoryConstants.MB), nbBytes);
	}

	@Test
	public void testMemoryOnLinux() throws IOException {
		String macMemoryOutput = Arrays.asList(" total 65512K").stream().collect(Collectors.joining("\n"));

		long nbBytes = ApexProcessHelper.extractMemory(ApexProcessHelper.OS_MARKER_LINUX,
				new ByteArrayInputStream(macMemoryOutput.getBytes()));
		Assert.assertEquals(65512 * IApexMemoryConstants.KB, nbBytes);
	}

	/**
	 * Enable to check the behavior on any system
	 * 
	 * @throws IOException
	 */
	@Test
	public void testMemoryOnCurrentSystem() throws IOException {
		long currentProcessPID = Long.parseLong(ApexAgentHelper.getPIDForAgent());
		long nbBytes = ApexProcessHelper.getProcessResidentMemory(currentProcessPID);
		Assert.assertTrue(nbBytes > 0);
	}
}
