package blasd.apex.core.jvm;

import java.lang.management.MemoryUsage;
import java.util.Map;

/**
 * Wraps the not-specified but much useful information in com.sun.management.GarbageCollectionNotificationInfo
 * 
 * @author Benoit Lacelle
 *
 */
public interface IApexGarbageCollectionNotificationInfo {

	String getGcAction();

	long getGcDuration();

	String getGcName();

	Map<String, MemoryUsage> getMemoryUsageBeforeGc();

	Map<String, MemoryUsage> getMemoryUsageAfterGc();

}
