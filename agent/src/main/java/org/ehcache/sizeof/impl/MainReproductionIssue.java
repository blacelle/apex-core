package org.ehcache.sizeof.impl;

import blasd.apex.core.agent.InstrumentationAgent;
import blasd.apex.core.agent.VirtualMachineWithoutToolsJar;

public class MainReproductionIssue {
	public static void main(String[] args) {
		VirtualMachineWithoutToolsJar.getJvmVirtualMachine().get();
		InstrumentationAgent.getInstrumentation().get();
	}
}
