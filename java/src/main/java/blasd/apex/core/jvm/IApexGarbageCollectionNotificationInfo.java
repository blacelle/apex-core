package blasd.apex.core.jvm;

import java.lang.management.MemoryUsage;
import java.util.Map;

public interface IApexGarbageCollectionNotificationInfo {

	String getGcAction();

	long getGcDuration();

	String getGcName();

	Map<String, MemoryUsage> getMemoryUsageBeforeGc();

	Map<String, MemoryUsage> getMemoryUsageAfterGc();

}
