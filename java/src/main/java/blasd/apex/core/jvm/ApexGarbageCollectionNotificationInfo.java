package blasd.apex.core.jvm;

import java.lang.management.MemoryUsage;
import java.util.Map;

import javax.management.openmbean.CompositeData;

import com.sun.management.GarbageCollectionNotificationInfo;

public class ApexGarbageCollectionNotificationInfo implements IApexGarbageCollectionNotificationInfo {
	protected final GarbageCollectionNotificationInfo info;

	public ApexGarbageCollectionNotificationInfo(GarbageCollectionNotificationInfo info) {
		this.info = info;
	}

	@Override
	public String getGcAction() {
		return info.getGcAction();
	}

	@Override
	public long getGcDuration() {
		return info.getGcInfo().getDuration();
	}

	@Override
	public String getGcName() {
		return info.getGcName();
	}

	public static ApexGarbageCollectionNotificationInfo from(CompositeData cd) {
		return new ApexGarbageCollectionNotificationInfo(GarbageCollectionNotificationInfo.from(cd));
	}

	@Override
	public Map<String, MemoryUsage> getMemoryUsageBeforeGc() {
		return info.getGcInfo().getMemoryUsageBeforeGc();
	}

	@Override
	public Map<String, MemoryUsage> getMemoryUsageAfterGc() {
		return info.getGcInfo().getMemoryUsageAfterGc();
	}

}
