package blasd.apex.core.jvm;

import java.lang.management.OperatingSystemMXBean;
import java.util.OptionalDouble;

import com.google.common.annotations.Beta;

/**
 * Holds all call to methods/fields not in the Java spec but present in the Oracle jvm
 * 
 * @author Benoit Lacelle
 *
 */
@Beta
public class ApexForOracleJVM {
	protected ApexForOracleJVM() {
		// hidden
	}

	// com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
	public static final String GARBAGE_COLLECTION_NOTIFICATION = "com.sun.management.gc.notification";

	public static long maxDirectMemory() {
		return sun.misc.VM.maxDirectMemory();
	}

	public static OptionalDouble getCpu(OperatingSystemMXBean osMbean) {
		if (osMbean instanceof com.sun.management.OperatingSystemMXBean) {
			double cpu = ((com.sun.management.OperatingSystemMXBean) osMbean).getProcessCpuLoad();

			return OptionalDouble.of(cpu);
		} else {
			return OptionalDouble.empty();
		}
	}
}
