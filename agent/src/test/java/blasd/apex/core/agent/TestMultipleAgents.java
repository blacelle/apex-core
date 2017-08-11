package blasd.apex.core.agent;

import org.junit.Test;

// We used to have issues related to class-loading leading to issues with Library loading
public class TestMultipleAgents {
	@Test
	public void testVMThenAgent() {
		VirtualMachineWithoutToolsJar.getJvmVirtualMachine().get();
		InstrumentationAgent.getInstrumentation().get();
	}

	@Test
	public void testAgentThenVM() {
		InstrumentationAgent.getInstrumentation().get();
		VirtualMachineWithoutToolsJar.getJvmVirtualMachine().get();
	}
}
