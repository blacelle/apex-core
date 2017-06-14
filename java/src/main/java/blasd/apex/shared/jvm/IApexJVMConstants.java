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
package blasd.apex.shared.jvm;

/**
 * 
 * Typical JVM arguments
 * 
 * Class memory parameterization
 * 
 * -XX:+UseG1GC -XX:+ExplicitGCInvokesConcurrent -Xmx3G -Xms3G -XX:MaxDirectMemorySize=7G -XX:MaxPermSize=512M
 * 
 * Enable HeapDump on OutOfMemoryError
 * 
 * -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/disk2/dumps
 * 
 * 
 * 
 * -DbufferAllocatorClass=com.qfs.buf.impl.HeapBufferAllocator
 * -DchunkAllocatorClass=com.qfs.chunk.buffer.impl.HeapBufferChunkAllocator -DdefaultChunkSize=131072
 * -DchunkAllocatorClass=com.qfs.chunk.direct.impl.DirectChunkAllocator
 * -DchunkAllocatorClass=com.qfs.chunk.direct.impl.MmapDirectChunkAllocator
 * -DchunkAllocatorClass=com.qfs.chunk.direct.allocator.impl.SlabMemoryAllocator
 * 
 * 
 * java -XX:+PrintFlagsFinal -version > flags.log
 * 
 * 
 * -XX:+PrintGCApplicationStoppedTime - it prints all STW pauses not only related to GC
 * 
 * -XX:+PrintSafepointStatistics - prints safe points details
 * 
 * -XX:PrintSafepointStatisticsCount=1 - make JVM report every safe point
 * 
 * <%p> will add the PID in the gcLogFile <%t> will add the startup date in the gcLogFile
 * 
 * 
 * https://bugs.openjdk.java.net/browse/JDK-6950794
 * 
 * Minimum logs in sysout
 * 
 * -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps
 * 
 * -Xloggc:../log/jvm_gc.%p.%t.log -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintGCDetails
 * -XX:+PrintClassHistogramBeforeFullGC -XX:+PrintClassHistogramAfterFullGC -XX:+PrintGCApplicationStoppedTime
 * -XX:+PrintSafepointStatistics –XX:PrintSafepointStatisticsCount=1
 * 
 * GC rolling
 * 
 * -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 XX:GCLogFileSize=10M
 * 
 * In Prod -XX:-OmitStackTraceInFastThrow will prevent cutting stacks, even if at least the first stack occurence has
 * been complete
 * 
 * JGroups does not work with IPv6 -Djava.net.preferIPv4Stack=true
 * 
 * # CheckIP with: hostname -i
 * 
 * -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=1088
 * -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
 * -Djava.rmi.server.hostname=<OUTPUT OF "hostname -i">
 * 
 * Typical monitoring commands
 * 
 * jmap <pid>
 * 
 * jmap -histo <pid>
 * 
 * jmap -histo -F <pid> > some.file
 * 
 * jmap -dump:format=b,file=<filename> <pid> -J-Dsun.tools.attach.attachTimeout=<milliseconds>
 * 
 * jstat -gclog <pid>
 * 
 * jstack <pid>
 * 
 * jstack -F <pid>
 * 
 * Add debug in tomcat: "-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n"
 * 
 * JIT Class Compilation audit
 * 
 * -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -XX:+TraceClassLoading
 * 
 * Optionally add -XX:+PrintAssembly
 * 
 * https://github.com/AdoptOpenJDK/jitwatch/
 * 
 * https://github.com/AdoptOpenJDK/jitwatch/wiki/Instructions
 * 
 * Profiling based on ThreadDumps http://techblog.netflix.com/2015/07/java-in-flames.html
 * 
 * Ensure stack-traces are present: -XX:-OmitStackTraceInFastThrow
 * http://stackoverflow.com/questions/2411487/nullpointerexception-in-java-with-no-stacktrace
 * 
 * @author Benoit Lacelle
 *
 */
public interface IApexJVMConstants {
	/**
	 * 
	 * Enable Java Mission Control http://docs.oracle.com/cd/E15289_01/doc.40/e15070/usingjfr.htm
	 * -XX:+UnlockCommercialFeatures -XX:+FlightRecorder
	 * 
	 * https://docs.oracle.com/cd/E15289_01/doc.40/e15070/config_rec_data.htm
	 * 
	 * Start from startup. Default conf is in <java_home>\jre\lib\jfr
	 * 
	 * -XX:FlightRecorderOptions=defaultrecording=true -XX:FlightRecorderOptions =defaultrecording=true,settings=default
	 * 
	 * @author Benoit Lacelle
	 *
	 */
	interface IApexJMCConstants {

	}

}
