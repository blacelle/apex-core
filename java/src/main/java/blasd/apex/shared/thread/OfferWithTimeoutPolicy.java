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
package blasd.apex.shared.thread;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link RejectedExecutionHandler} which enables waiting for a given timeout before rejecting tasks if queue is full
 * 
 * @author Benoit Lacelle
 *
 */
@Deprecated
public class OfferWithTimeoutPolicy implements RejectedExecutionHandler {

	protected static final Logger LOGGER = LoggerFactory.getLogger(OfferWithTimeoutPolicy.class);

	protected int timeout;
	protected TimeUnit unit;

	/**
	 * This boolean makes sure we monitor the time to go through the queue not to often
	 */
	protected final AtomicBoolean isGoingToLog = new AtomicBoolean();

	public OfferWithTimeoutPolicy(int timeout, TimeUnit unit) {
		this.timeout = timeout;
		this.unit = unit;
	}

	@Override
	public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
		if (isGoingToLog.compareAndSet(false, true)) {
			try {
				final long start = System.currentTimeMillis();
				boolean result = executor.getQueue().offer(() -> {
					r.run();

					long time = System.currentTimeMillis() - start;
					if (time > ApexExecutorsHelper.DEFAULT_LOG_ON_SLOW_QUEUE_MS) {
						LOGGER.warn("The pool {} is full and it took {} ms for the first rejected task to be processed",
								executor,
								time);

					}
					isGoingToLog.set(false);
				}, timeout, unit);

				if (!result) {
					throw new RuntimeException("We failed pushing the task " + r + " after waiting " + timeout + unit);
				}
			} catch (InterruptedException e) {
				isGoingToLog.set(false);
				throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
			}
		} else {
			try {
				boolean result = executor.getQueue().offer(r, timeout, unit);

				if (!result) {
					throw new RuntimeException("We failed pushing the task " + r + " after waiting " + timeout + unit);
				}
			} catch (InterruptedException e) {
				throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
			}
		}
	}
}