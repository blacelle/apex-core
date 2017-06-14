/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.core.jvm;

import java.util.Map;

import blasd.apex.core.memory.IApexMemoryConstants;

/**
 * Interface for GC activity monitoring
 * 
 * @author Benoit Lacelle
 *
 */
public interface IGCInspector extends IApexMemoryConstants {

	/**
	 * We want to see at least 2 digits: after printing 9999B, we print 10KB
	 */
	long BARRIER_FOR_SIZE_IN_LOG = 10;

	String getAndLogCurrentMemoryStatus();

	String getHeapHistogram();

	long saveHeapDump(String path, boolean gzipped);

	/**
	 * 
	 * @param withoutMonitors
	 *            by default withoutMonitors=true in JConsole MBean for faster access to fasfter method
	 * @return
	 */
	String getAllThreads(boolean withoutMonitors);

	String getAllThreadsSmart(boolean withoutMonitors);

	Map<String, String> getThreadGroupsToAllocatedHeapNiceString();

	Map<String, String> getThreadNameToAllocatedHeapNiceString();

	void clearAllocatedHeapReference();

	void markNowAsAllocatedHeapReference();

	long getMaxHeapGbForHeapHistogram();

	void setMaxHeapGbForHeapHistogram(long maxHeapGbForHeapHistogram);

	long getMarksweepDurationMillisForHeapHistogram();

	void setMarksweepDurationMillisForHeapHistogram(long marksweepDurationMillisForHeapHistogram);

	long getMarksweepDurationMillisForThreadDump();

	void setMarksweepDurationMillisForThreadDump(long marksweepDurationMillisForThreadDump);

}
