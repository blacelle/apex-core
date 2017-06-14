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
package blasd.apex.core.thread;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

/**
 * Enable naming of FOrkJoinPool threads
 * 
 * @author Benoit Lacelle
 *
 * @see http://stackoverflow.com/questions/34303094/is-it-not-possible-to-supply-a-thread-facory-or-name-pattern-to-forkjoinpool
 */
public class NamingForkJoinWorkerThreadFactory implements ForkJoinWorkerThreadFactory {

	protected final String threadPrefix;

	public NamingForkJoinWorkerThreadFactory(String threadPrefix) {
		this.threadPrefix = threadPrefix;
	}

	@Override
	public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
		final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
		worker.setName(threadPrefix + "-" + worker.getPoolIndex());
		return worker;
	}
}
